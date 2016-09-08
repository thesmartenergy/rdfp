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

import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.RDFPException;
import com.github.thesmartenergy.rdfp.ResourceDescription;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Provider
public class PresentationResponseFilter implements ContainerResponseFilter {
    
    private static final Logger LOG = Logger.getLogger(PresentationResponseFilter.class.getSimpleName());
    
    @Context
    Request rsRequest;
    
    @Inject
    PresentationUtils presentationUtils;
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!(responseContext.getEntity() instanceof Model)) {
            return;
        }
        Model entity = (Model) responseContext.getEntity();
        // highest priority: if request contains Accept-Presentation
        String presentationURI = requestContext.getHeaderString(RDFP.ACCEPT_PRESENTATION);
        if (presentationURI != null) {
            // dereference presentation
            ResourceDescription presentation;
            try {
                presentation = new ResourceDescription(presentationURI);
                responseContext.getHeaders().add("presentation-description", presentation);
            } catch (Exception ex) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                responseContext.setEntity("Could not dereference presentation <" + presentationURI + ">. Error was: " + ex.getMessage());
                return;
            }
            Annotation[] annotations = responseContext.getEntityAnnotations();
            MediaType mediaType;
            try {
                mediaType = presentationUtils.presentationAcceptedMediaType(presentation);
            } catch (RDFPException ex) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                responseContext.setEntity("Could not parse mediaType for presentation <" + presentationURI + ">. Error was: " + ex.getMessage());
                return;
            }
            if (rsRequest.selectVariant(Variant.mediaTypes(mediaType).build()) == null) {
                responseContext.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                responseContext.setEntity("Presentation <" + presentation.getNode() + "> is for media type: \"" + mediaType + "\" while request accepts media type: \"" + requestContext.getHeaderString("Accept") + "\"");
                return;
            }
            responseContext.setEntity(entity, annotations, mediaType);
            responseContext.getHeaders().add(RDFP.CONTENT_PRESENTATION, presentationURI);
        } else {
           Annotation[] annotations = responseContext.getEntityAnnotations();
            String graphTypeURI = null;
            ResourceDescription graphType = null;
            try {
                graphTypeURI = presentationUtils.getGraphTypeUri(annotations);
            } catch (Exception ex) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                responseContext.setEntity("Illegal URI for graph type: <" + graphTypeURI + ">. Error was: " + ex.getMessage());
                return;
            }
            if(graphTypeURI != null) {
                try {
                    graphType = new ResourceDescription(graphTypeURI);
                    responseContext.getHeaders().add("graph-description", graphType);
                } catch (Exception ex) {
                    responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                    responseContext.setEntity("Could not dereference graph type <" + graphTypeURI + ">. Error was: " + ex.getMessage());
                    return;
                }
            }
            List<Variant> variants = presentationUtils.graphAcceptedMediaTypes(graphType);
            // if graphType==null, then variants contains text/turtle and application/rdf+xml
            Variant v = rsRequest.selectVariant(variants);
            if (v == null) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                if(graphTypeURI != null) {
                    responseContext.setEntity("Graph type <" + graphTypeURI + "> declares no presentation for any acceptable media types. \n  Request accepts: \"" + requestContext.getHeaderString("Accept") + "\"\n  while Graph <" + graphTypeURI + "> contains presentations for:\n" + variants);
                } else {
                    responseContext.setEntity("No presentation for any acceptable media types exists. \n  Request accepts: \"" + requestContext.getHeaderString("Accept") + "\"\n  while default presentations are for:\n" + variants);
                }
                return;
            }
            MediaType mediaType = MediaType.valueOf(v.getMediaType().getType() + "/" + v.getMediaType().getSubtype());
            responseContext.setEntity(entity, annotations, mediaType);
        }
    }
    
    
}
