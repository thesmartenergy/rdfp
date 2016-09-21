/*
 * Copyright 2016 ITEA 12004 SEAS Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.rdfp.preneg;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Used to annotate a Jersey resource method (resp. Jersey resource method
 * parameter) to specify the URI of the rdfg:GraphDescription that can be used to
 * present output RDF Graphs (resp. input RDF Graphs).
 * 
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Inherited
@Target(value = {METHOD, PARAMETER})
@Retention(value = RUNTIME)
@Documented
public @interface GraphDescription {
    /**
     * @return The URI of a dereferenceable rdfg:GraphDescription. Must be a
     * valid URI.
     */
    String value();
}
