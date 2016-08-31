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

import com.github.thesmartenergy.rdfp.preneg.LiftingHandler;
import com.github.thesmartenergy.rdfp.preneg.ForMediaType;
import com.github.thesmartenergy.rdfp.resources.ResourceDescription;
import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.ResourcePlatformException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Provider
public class ModelReader implements MessageBodyReader<Model> {

    private static final Logger LOG = Logger.getLogger(ModelReader.class.getSimpleName());

    @Inject
    @Any
    private Instance<LiftingHandler> liftingHandlerInstance;

    @Inject
    PresentationUtils presentationUtils;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Model.class.isAssignableFrom(type);
    }

    @Override
    public Model readFrom(Class<Model> type,
            Type genericType,
            Annotation[] annotations,
            final MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {
        // first forget about media type "presentation" parameter
        // if there is header content-presentation, then use this.
        String presentationURI = (String) httpHeaders.getFirst(RDFP.CONTENT_PRESENTATION);
        if (presentationURI != null) {
            LOG.log(Level.INFO, "use content-presentation " + presentationURI);
            // use this presentation
            ResourceDescription presentation;
            try {
                presentation = new ResourceDescription(presentationURI);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "presentation description could not be found.");
                String message = "A description for presentation <" + presentationURI + "> could not be found. Cause is: " + ex.getMessage();
                Response response = Response.serverError().entity(message).build();
                throw new InternalServerErrorException(message, response);
            }
            return lift(mediaType, presentation, entityStream);
        }
        List<String> messages = new ArrayList<>();
        String graphTypeURI = null;
        try {
            graphTypeURI = presentationUtils.getGraphTypeUri(annotations);
            LOG.log(Level.INFO, "use graph type " + graphTypeURI);
        } catch (ResourcePlatformException ex) {
            String message = "Graph type URI not valid: " + ex.getMessage();
            Response response = Response.serverError().entity(message).build();
            throw new InternalServerErrorException(message, response);
        }
        ResourceDescription graphType;
        try {
            graphType = new ResourceDescription(graphTypeURI);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "graph description could not be found: " + ex.getMessage());
            // use only presentation-agnostic lifters
            return lift(mediaType, entityStream);
        }
        // use first presentation that declares a mediaType compatible with the content media type
        Model graphTypeModel = graphType.getModel();
        NodeIterator nit = graphTypeModel.listObjectsOfProperty(graphType.getNode().asResource(), RDFP.presentedBy);
        while (nit.hasNext()) {
            RDFNode n = nit.next();
            LOG.log(Level.INFO, "try presentation " + n);
            ResourceDescription presentation = null;
            try {
                presentation = new ResourceDescription(n, graphTypeModel);
            } catch (IllegalArgumentException ex) {
                String message = "Error while dereferencing node " + n + ": " + ex.getMessage();
                messages.add(message);
                LOG.log(Level.INFO, message);
            }
            MediaType presentationMediaType = null;
            try {
                presentationMediaType = presentationUtils.presentationAcceptedMediaType(presentation);
            } catch (Exception ex) {
                String message = "Error while retrieving supported media type for presentation " + n + ": " + ex.getMessage();
                messages.add(message);
                LOG.log(Level.INFO, message);
            }
            if (presentationMediaType.isCompatible(mediaType)) {
                try {
                    return lift(mediaType, presentation, entityStream);
                } catch (Exception ex) {
                    String message = "Error while lifting with presentation " + n + ": " + ex.getMessage();
                    messages.add(message);
                    LOG.log(Level.INFO, message);
                }
            }
        }
        try {
            return lift(mediaType, entityStream);
        } catch (Exception ex) {
            String message = "Error while lifting with no presentation: " + ex.getMessage();
            messages.add(message);
            LOG.log(Level.INFO, message);
        }
        String message = "Found no ways to read model with presentation <" + presentationURI + "> and media type \"" + mediaType + "\". Errors were: " + String.join("\n", messages);
        Response response = Response.serverError().entity(message).build();
        throw new InternalServerErrorException(message, response);
    }

    private Model lift(MediaType mediaType, InputStream entityStream) {
        List<String> messages = new ArrayList<>();
        Iterator<LiftingHandler> it = liftingHandlerInstance.iterator();
        while (it.hasNext()) {
            LiftingHandler handler = it.next();
            ForMediaType handledMediaType = handler.getClass().getAnnotation(ForMediaType.class);
            if (!MediaType.valueOf(handledMediaType.value()).isCompatible(mediaType)) {
                continue;
            }
            try {
                return handler.lift(mediaType, entityStream);
            } catch (ResourcePlatformException ex) {
                messages.add(ex.getMessage());
            }
        }
        String message = "Could not lift content to RDF:\n" + String.join("\n", messages);
        Response response = Response.serverError().entity(message).build();
        throw new InternalServerErrorException(message, response);

    }

    private Model lift(MediaType mediaType, ResourceDescription presentation, InputStream entityStream) {
        if (presentation == null) {
            return lift(mediaType, entityStream);
        }
        if (mediaType == null) {
            String message = "No Content-Type is provided. Need one.";
            Response response = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(message).build();
            throw new InternalServerErrorException(message, response);
        }
        List<String> messages = new ArrayList<>();
        Iterator<LiftingHandler> it = liftingHandlerInstance.iterator();
        while (it.hasNext()) {
            LiftingHandler handler = it.next();
            ForMediaType handledMediaType = handler.getClass().getAnnotation(ForMediaType.class);
            if (handledMediaType != null && !MediaType.valueOf(handledMediaType.value()).isCompatible(mediaType)) {
                messages.add("Lifting handler " + handler.getClass().getSimpleName() + " cannot lift for media type \"" + mediaType + "\"");
                continue;
            }
            try {
                return handler.lift(mediaType, presentation, entityStream);
            } catch (ResourcePlatformException ex) {
                messages.add(ex.getMessage());
            }
        }
        String message = "Could not lower RDF content:\n" + String.join("\n", messages);
        Response response = Response.serverError().entity(message).build();
        throw new InternalServerErrorException(message, response);
    }

}
