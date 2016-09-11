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
package com.github.thesmartenergy.rdfp;

import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class RDFP {

    public static final MediaType APPLICATION_RDFXML_TYPE = MediaType.valueOf("application/rdf+xml");
    
    public static final MediaType TEXT_TURTLE_TYPE = MediaType.valueOf("text/turtle");

    public static final String NS = "https://w3id.org/rdfp/";
    
    public static final String CONTENT_PRESENTATION = "content-presentation";

    public static final String ACCEPT_PRESENTATION = "accept-presentation";
    
    public static final Property presentedBy = ResourceFactory.createProperty(NS + "presentedBy");
    
    public static final Property loweringRule = ResourceFactory.createProperty(NS + "loweringRule");

    public static final Property liftingRule = ResourceFactory.createProperty(NS + "liftingRule");
    
    public static final Resource Graph = ResourceFactory.createResource(NS + "Graph");

    public static final Resource Resource = ResourceFactory.createResource(NS + "Resource");

    public static final Property representedBy = ResourceFactory.createProperty(NS + "representedBy");

    public static final Property mediaType = ResourceFactory.createProperty(NS + "mediaType");

    public static final Property alias = ResourceFactory.createProperty(NS + "alias");

}
