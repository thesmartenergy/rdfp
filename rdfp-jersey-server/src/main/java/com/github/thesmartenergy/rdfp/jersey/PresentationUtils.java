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
package com.github.thesmartenergy.rdfp.jersey;

import com.github.thesmartenergy.ontop.ONTOP;
import com.github.thesmartenergy.rdfp.preneg.GraphDescription;
import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.RDFPException;
import com.github.thesmartenergy.rdfp.ResourceDescription;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Dependent
public class PresentationUtils {
    
    private static final Logger LOG = Logger.getLogger(PresentationUtils.class.getSimpleName());

    public String getGraphTypeUri(Annotation[] annotations) throws RDFPException {
        try {
            for (Annotation a : annotations) {
                if (a.annotationType().equals(GraphDescription.class)) {
                    return (new URI(((GraphDescription) a).value())).toURL().toString();
                }
            }
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new RDFPException("Illegal URI for graph type: ", ex);
        }
        return null;
    }
    
    public MediaType presentationAcceptedMediaType(ResourceDescription presentation) throws RDFPException {
        Statement mediaTypeStatement = presentation.getModel().getProperty(presentation.getNode().asResource(), ONTOP.mediaType);
        if(mediaTypeStatement == null) {
            throw new RDFPException("No declared mediatype here !");
        }
        RDFNode acceptedMediaTypeNode = mediaTypeStatement.getObject();
        if (acceptedMediaTypeNode == null) {
            throw new RDFPException("presentation <" + presentation.getNode() + "> must declare a mediaType.");
        }
        if (!acceptedMediaTypeNode.isLiteral()) {
            throw new RDFPException("mediaType of presentation <" + presentation.getNode() + "> should be a literal. Got " + acceptedMediaTypeNode);
        }
        try {
            return MediaType.valueOf(acceptedMediaTypeNode.asLiteral().getLexicalForm());
        } catch (Exception ex) {
            throw new RDFPException("error while parsing \"" + acceptedMediaTypeNode.asLiteral().getLexicalForm() + "\" as a mediaType");
        }
    }
    
    public List<Variant> graphAcceptedMediaTypes(ResourceDescription graphType) {
        Set<MediaType> mediaTypes = new HashSet<>();
        mediaTypes.add(MediaType.valueOf("text/turtle;q=1.0"));
        mediaTypes.add(MediaType.valueOf("application/rdf+xml;q=0.8"));
        if (graphType == null) {
            return Variant.mediaTypes(mediaTypes.toArray(new MediaType[0])).build();
        }
        NodeIterator nit = graphType.getModel().listObjectsOfProperty(graphType.getNode().asResource(), RDFP.presentedBy);
        while (nit.hasNext()) {
            ResourceDescription presentation = new ResourceDescription(nit.next(), graphType.getModel());
            try {
                mediaTypes.add(presentationAcceptedMediaType(presentation));
            } catch (RDFPException ex) {
                LOG.log(Level.WARNING, "error while reading mediaType for presentation <" + presentation.getNode() + ">: " + ex.getMessage());
            }
        }
        return Variant.mediaTypes(mediaTypes.toArray(new MediaType[0])).build();
    }
}
