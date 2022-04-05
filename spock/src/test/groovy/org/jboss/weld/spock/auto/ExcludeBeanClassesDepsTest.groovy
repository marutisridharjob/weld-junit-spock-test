/*
 * JBoss, Home of Professional Open Source
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.weld.spock.auto

import jakarta.enterprise.inject.Produces
import org.jboss.weld.spock.EnableWeld
import org.jboss.weld.spock.basic.unsatisfied.FooDeps
import org.jboss.weld.spock.basic.unsatisfied.SomeFooDeps
import spock.lang.Specification

/**
 * @author Björn Kautler
 */
@EnableWeld(automagic = true)
@AddBeanClasses(SomeFooDeps)
@ExcludeBeanClasses(FooDeps)
class ExcludeBeanClassesDepsTest extends Specification {
    /**
     * FooDeps injects the Baz bean which has an unsatisfied dependency "bar-value". Excluding FooDeps should ensure
     * that its specific dependencies are not included via scanning and therefore don't need to be provided for
     * testing.
     */

    @Produces
    FooDeps fakeFooDeps = new FooDeps()

    def '@ExcludeBeanClasses should exclude any specific dependencies of the excluded classes'(FooDeps myFooDeps) {
        expect:
            myFooDeps != null
    }
}
