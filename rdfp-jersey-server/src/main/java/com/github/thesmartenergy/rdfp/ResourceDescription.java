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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ResourceDescription {

    private RDFNode node;
    private Model model;

    public ResourceDescription(String uri) {
        try {
            uri = (new URI(uri)).toURL().toString();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Illegal resource IRI: " + uri, ex); 
        }
        model = RDFDataMgr.loadModel(uri);
        if (model == null) {
            throw new IllegalArgumentException("Could not find a description for graph type " + uri);
        }
        node = model.getResource(uri);
    }

    public ResourceDescription(RDFNode node, Model model) {
        if (node == null) {
            throw new IllegalArgumentException("arguments must not be null."); 
        }
        if (model == null) {
            throw new IllegalArgumentException("arguments must not be null.");
        }
        if (!model.containsResource(node)) {
            throw new IllegalArgumentException("resource must be described in model !");
        }
        this.node = node;
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public RDFNode getNode() {
        return node;
    }

}
