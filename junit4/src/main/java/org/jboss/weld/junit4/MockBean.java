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
package org.jboss.weld.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;

/**
 * {@link Bean} implementation used for mocking.
 *
 * @author Martin Kouba
 *
 * @param <T>
 * @see WeldInitiator.Builder#addBean(Bean)
 */
public class MockBean<T> implements Bean<T> {

    /**
     * By default, the bean:
     * <ul>
     * <li>has no name</li>
     * <li>has {@link Dependent} scope</li>
     * <li>has {@link Any} qualifier and {@link Default} is added automatically if no other qualifiers are set</li>
     * <li>has {@link Object} bean type</li>
     * <li>has no stereotypes</li>
     * <li>is not an alternative</li>
     * </ul>
     *
     * @return a new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private final Set<Class<? extends Annotation>> stereotypes;

    private final boolean alternative;

    private final String name;

    private final Set<Annotation> qualifiers;

    private final Set<Type> types;

    private final Class<? extends Annotation> scope;

    private final CreateFunction<T> createCallback;

    private final DestroyFunction<T> destroyCallback;

    private MockBean(Set<Class<? extends Annotation>> stereotypes, boolean alternative, String name,
            Set<Annotation> qualifiers, Set<Type> types, Class<? extends Annotation> scope,
            CreateFunction<T> createCallback, DestroyFunction<T> destroyCallback) {
        this.stereotypes = stereotypes;
        this.alternative = alternative;
        this.name = name;
        this.qualifiers = qualifiers;
        this.types = types;
        this.scope = scope;
        this.createCallback = createCallback;
        this.destroyCallback = destroyCallback;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return createCallback.create(creationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (destroyCallback != null) {
            destroyCallback.destroy(instance, creationalContext);
        }
    }

    @Override
    public Class<?> getBeanClass() {
        return WeldJunit4Extension.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return stereotypes;
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    /**
     * A builder instance should not be reused or shared.
     *
     * @author Martin Kouba
     *
     * @param <T>
     */
    public static class Builder<T> {

        private Set<Class<? extends Annotation>> stereotypes;

        private boolean alternative;

        private String name;

        private Set<Annotation> qualifiers;

        private Set<Type> types;

        private Class<? extends Annotation> scope;

        private CreateFunction<T> createCallback;

        private DestroyFunction<T> destroyCallback;

        Builder() {
            this.stereotypes = new HashSet<>();
            this.alternative = false;
            this.qualifiers = new HashSet<>();
            this.qualifiers.add(AnyLiteral.INSTANCE);
            this.scope = Dependent.class;
            this.types = new HashSet<>();
            this.types.add(Object.class);
        }

        /**
         *
         * @param scope
         * @return self
         * @see Bean#getScope()
         */
        public Builder<T> scope(Class<? extends Annotation> scope) {
            this.scope = scope;
            return this;
        }

        /**
         *
         * @param name
         * @return self
         * @see Bean#getName()
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         *
         * @param types
         * @return self
         * @see Bean#getTypes()
         */
        public Builder<T> types(Type... types) {
            this.types = new HashSet<>();
            Collections.addAll(this.types, types);
            return this;
        }

        /**
         *
         * @param qualifiers
         * @return self
         * @see Bean#getQualifiers()
         */
        public Builder<T> qualifiers(Annotation... qualifiers) {
            this.qualifiers = new HashSet<>();
            Collections.addAll(this.qualifiers, qualifiers);
            return this;
        }

        /**
         *
         * @param value
         * @return self
         * @see Bean#isAlternative()
         */
        public Builder<T> alternative(boolean value) {
            this.alternative = value;
            return this;
        }

        /**
         *
         * @param stereotypes
         * @return self
         * @see Bean#getStereotypes()
         */
        @SuppressWarnings("unchecked")
        public Builder<T> stereotypes(Class<? extends Annotation>... stereotypes) {
            this.stereotypes = new HashSet<>();
            Collections.addAll(this.stereotypes, stereotypes);
            return this;
        }

        /**
         * Each invocation of {@link Bean#create(CreationalContext)} will return the same instance.
         *
         * @param instance
         * @return self
         */
        public Builder<T> creating(final T instance) {
            this.createCallback = new CreateFunction<T>() {
                @Override
                public T create(CreationalContext<T> creationalContext) {
                    return instance;
                }
            };
            return this;
        }

        /**
         *
         * @param callback
         * @return self
         * @see Bean#create(CreationalContext)
         */
        public Builder<T> create(CreateFunction<T> callback) {
            this.createCallback = callback;
            return this;
        }

        /**
         *
         * @param callback
         * @return self
         * @see Bean#destroy(Object, CreationalContext)
         */
        public Builder<T> destroy(DestroyFunction<T> callback) {
            this.destroyCallback = callback;
            return this;
        }

        /**
         *
         * @return a new {@link MockBean} instance
         */
        public MockBean<T> build() {
            if (createCallback == null) {
                throw new IllegalStateException("Create callback must not be null");
            }
            if (qualifiers.size() == 1) {
                qualifiers.add(DefaultLiteral.INSTANCE);
            }
            return new MockBean<>(stereotypes, alternative, name, qualifiers, types, scope,
                    createCallback, destroyCallback);
        }

    }

    public interface CreateFunction<T> {

        /**
         *
         * @param creationalContext
         * @return a new bean instance
         */
        T create(CreationalContext<T> creationalContext);

    }

    public interface DestroyFunction<T> {

        /**
         *
         * @param instance
         * @param creationalContext
         * @return a new bean instance
         */
        void destroy(T instance, CreationalContext<T> creationalContext);

    }

    @SuppressWarnings("all")
    static class AnyLiteral extends AnnotationLiteral<Any> implements Any {

        private static final long serialVersionUID = 1L;

        public static final Any INSTANCE = new AnyLiteral();

        private AnyLiteral() {
        }

    }

    @SuppressWarnings("all")
    static class DefaultLiteral extends AnnotationLiteral<Default> implements Default {

        public static final Default INSTANCE = new DefaultLiteral();

        private DefaultLiteral() {
        }

    }

}
