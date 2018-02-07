/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.junit5;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.junit.AbstractWeldInitiator;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * JUnit 5 extension allowing to bootstrap Weld SE container for each @Test method and tear it down afterwards. Also allows to
 * inject CDI beans as parameters to @Test methods and resolves all @Inject fields in test class.
 *
 * By default (if no {@link WeldInitiator} field annotated with {@link WeldSetup} is present), Weld is configured with the
 * result of {@link WeldInitiator#createWeld()} method and all the classes from the test class package are added:
 *
 * <pre>
 * &#64;ExtendWith(WeldJunit5Extension.class)
 * public class SimpleTest {
 *
 *     // Injected automatically
 *     &#64;Inject
 *     Foo foo;
 *
 *     &#64;Test
 *     public void testFoo() {
 *         // Weld container is started automatically
 *         assertEquals("baz", foo.getBaz());
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class WeldJunit5Extension implements AfterAllCallback, TestInstancePostProcessor, AfterTestExecutionCallback, ParameterResolver {

    // variables used to identify object in Store
    private static final String INITIATOR = "weldInitiator";
    private static final String CONTAINER = "weldContainer";
    private static final String EXPLICIT_PARAM_INJECTION = "explicitParamInjection";

    // global system property
    public static final String GLOBAL_EXPLICIT_PARAM_INJECTION = "org.jboss.weld.junit5.explicitParamInjection";

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (determineTestLifecycle(context).equals(PER_CLASS)) {
            getInitiatorFromStore(context).shutdownWeld();
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (determineTestLifecycle(context).equals(PER_METHOD)) {
            getInitiatorFromStore(context).shutdownWeld();
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        // store info about explicit param injection, either from global settings or from annotation on the test class
        storeExplicitParamResolutionInformation(context);

        WeldInitiator initiator = null;
        // First try to find @WeldSetup field
        for (Field field : testInstance.getClass().getFields()) {
            if (field.isAnnotationPresent(WeldSetup.class)) {
                if (initiator != null) {
                    // multiple fields found, throw exception
                    throw new IllegalStateException("Multiple @WeldSetup annotated fields found, please use only one such field.");
                }
                Object fieldInstance;
                try {
                    fieldInstance = field.get(testInstance);
                } catch (IllegalAccessException e) {
                    // In case we cannot get to the field, we need to set accessibility as well
                    field.setAccessible(true);
                    fieldInstance = field.get(testInstance);
                }
                // if it's null, we can still store it, it will be created with default settings later on
                if (fieldInstance != null && fieldInstance instanceof WeldInitiator) {
                    initiator = (WeldInitiator) fieldInstance;
                } else {
                    // Field with other type than WeldInitiator was annotated with @WeldSetup
                    throw new IllegalStateException("@WeldSetup annotation should only be used on a field of type WeldInitiator.");
                }
            }
        }
        // WeldInitiator may still be null if user didn't specify it at all, we need to create it
        if (initiator == null) {
            initiator = WeldInitiator.from(AbstractWeldInitiator.createWeld().addPackage(false, testInstance.getClass())).build();
        }
        getStore(context).put(INITIATOR, initiator);

        // and finally, init Weld
        getStore(context).put(CONTAINER, initiator.initWeld(testInstance));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        // we did our checks in supportsParameter() method, now we can do simple resolution
        if (getContainerFromStore(extensionContext) != null) {
            List<Annotation> qualifiers = resolveQualifiers(parameterContext, getContainerFromStore(extensionContext).getBeanManager());
            return getContainerFromStore(extensionContext)
                .select(parameterContext.getParameter().getType(), qualifiers.toArray(new Annotation[qualifiers.size()])).get();
        }
        return null;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        // if weld container isn't up yet or if its not Method, we don't resolve it
        if (getContainerFromStore(extensionContext) == null || (!(parameterContext.getDeclaringExecutable() instanceof Method))) {
            return false;
        }
        List<Annotation> qualifiers = resolveQualifiers(parameterContext, getContainerFromStore(extensionContext).getBeanManager());
        // if we require explicit parameter injection (via global settings or annotation) and there are no qualifiers we don't resolve it
        if ((getExplicitInjectionInfoFromStore(extensionContext) || (methodRequiresExplicitParamInjection(parameterContext))) && qualifiers.isEmpty()) {
            return false;
        } else {
            return getContainerFromStore(extensionContext).select(parameterContext.getParameter().getType(), qualifiers.toArray(new Annotation[qualifiers.size()]))
                .isResolvable();
        }
    }

    private List<Annotation> resolveQualifiers(ParameterContext pc, BeanManager bm) {
        List<Annotation> qualifiers = new ArrayList<>();
        if (pc.getParameter().getAnnotations().length == 0) {
            return Collections.emptyList();
        } else {
            for (Annotation annotation : pc.getParameter().getAnnotations()) {
                // use BeanManager.isQualifier to be able to detect custom qualifiers which don't need to have @Qualifier
                if (bm.isQualifier(annotation.annotationType())) {
                    qualifiers.add(annotation);
                }
            }
        }
        return qualifiers;
    }

    private boolean methodRequiresExplicitParamInjection(ParameterContext pc) {
        for (Annotation annotation : pc.getDeclaringExecutable().getAnnotations()) {
            if (annotation.annotationType().equals(ExplicitParamInjection.class)) {
                return true;
            }
        }
        return false;
    }

    private TestInstance.Lifecycle determineTestLifecycle(ExtensionContext ec) {
        // check the test for import org.junit.jupiter.api.TestInstance annotation
        TestInstance annotation = ec.getRequiredTestClass().getAnnotation(TestInstance.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return TestInstance.Lifecycle.PER_METHOD;
        }
    }

    private void storeExplicitParamResolutionInformation(ExtensionContext ec) {
        // check system property which may have set the global explicit param injection
        Boolean globalSettings = Boolean.valueOf(System.getProperty(GLOBAL_EXPLICIT_PARAM_INJECTION));
        if (globalSettings) {
            getStore(ec).put(EXPLICIT_PARAM_INJECTION, globalSettings);
            return;
        }
        // check class-level annotation
        for (Annotation annotation : ec.getRequiredTestClass().getAnnotations()) {
            if (annotation.annotationType().equals(ExplicitParamInjection.class)) {
                getStore(ec).put(EXPLICIT_PARAM_INJECTION, true);
                break;
            }
        }

    }

    /**
     * We use custom namespace based on this extension class and test class
     */
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass()));
    }

    /**
     * Can return null if WeldInitiator isn't stored yet
     */
    private WeldInitiator getInitiatorFromStore(ExtensionContext context) {
        return getStore(context).get(INITIATOR, WeldInitiator.class);
    }

    /**
     * Return boolean indicating whether explicit parameter injection is enabled
     */
    private Boolean getExplicitInjectionInfoFromStore(ExtensionContext context) {
        Boolean result = getStore(context).get(EXPLICIT_PARAM_INJECTION, Boolean.class);
        return (result == null) ? false : result;
    }

    /**
     * Can return null if WeldContainer isn't stored yet
     */
    private WeldContainer getContainerFromStore(ExtensionContext context) {
        return getStore(context).get(CONTAINER, WeldContainer.class);
    }

}
