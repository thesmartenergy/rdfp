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

import com.github.thesmartenergy.rdfp.preneg.LoweringHandler;
import com.github.thesmartenergy.rdfp.preneg.ForMediaType;
import com.github.thesmartenergy.rdfp.resources.ResourceDescription;
import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.ResourcePlatformException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
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
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Provider
public class ModelWriter implements MessageBodyWriter<Model> {

    private static final Logger LOG = Logger.getLogger(ModelWriter.class.getSimpleName());

    @Inject
    @Any
    private Instance<LoweringHandler> loweringHandlerInstance;

    @Inject
    PresentationUtils presentationUtils;

    @Override
    public boolean isWriteable(Class<?> type, java.lang.reflect.Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Model.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Model t, Class<?> type, java.lang.reflect.Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(Model model,
            Class<?> type,
            java.lang.reflect.Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException {
        // first forget about media type "presentation" parameter
        // if there is header content-presentation, then use this.
        String presentationURI = (String) httpHeaders.getFirst(RDFP.CONTENT_PRESENTATION);
        if (presentationURI != null) {
            LOG.log(Level.INFO, "use content-presentation " + presentationURI);
            // use this presentation
            ResourceDescription presentation = (ResourceDescription) httpHeaders.getFirst("presentation-description");
            httpHeaders.remove("presentation-description");
            if (presentation == null) {
                LOG.log(Level.SEVERE, "presentation description should be set in the headers.");
                String message = "Presentation description should have been set in the headers.";
                Response res = Response.serverError().entity(message).build();
                throw new InternalServerErrorException(message, res);
            }
            lower(model, entityStream, mediaType, presentation);
            return;
        }
        ResourceDescription graphType = (ResourceDescription) httpHeaders.getFirst("graph-description");
        httpHeaders.remove("graph-description");
        if (graphType == null) {
            // use only presentation-agnostic lowerers
            LOG.log(Level.INFO, "presentation-agnostic lowerers");
            lower(model, entityStream, mediaType);
            return;
        }
        LOG.log(Level.INFO, "use graph description");
        // use first presentation that declares the given mediaType
        List<String> messages = new ArrayList<>();
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
                    lower(model, entityStream, mediaType, presentation);
                    if(presentation.getNode().isURIResource()) {
                        httpHeaders.add(RDFP.CONTENT_PRESENTATION, presentation.getNode().asResource().getURI());                    
                    }
                    return;
                } catch (Exception ex) {
                    String message = "Error while lowering with presentation " + n + ": " + ex.getMessage();
                    messages.add(message);
                    LOG.log(Level.INFO, message);
                }
            }
        }
        try {
            lower(model, entityStream, mediaType);
        } catch (Exception ex) {
            String message = "Error while lifting with no presentation: " + ex.getMessage();
            messages.add(message);
            LOG.log(Level.INFO, message);
        }
    }

    private void lower(Model model, OutputStream entityStream, MediaType mediaType) {
        List<String> messages = new ArrayList<>();
        Iterator<LoweringHandler> it = loweringHandlerInstance.iterator();
        while (it.hasNext()) {
            LoweringHandler handler = it.next();
            ForMediaType handledMediaType = handler.getClass().getAnnotation(ForMediaType.class);
            if (handledMediaType != null && !MediaType.valueOf(handledMediaType.value()).isCompatible(mediaType)) {
                continue;
            }
            try {
                handler.lower(model, entityStream, mediaType);
                return;
            } catch (ResourcePlatformException ex) {
                messages.add("Error while trying with handler: " + handler.getClass().getSimpleName());
                messages.add(ex.getMessage());
            }
        }
        String message = "Could not lower RDF content:\n" + String.join("\n", messages);
        Response response = Response.serverError().entity(message).build();
        throw new InternalServerErrorException(message, response);

    }

    private void lower(Model model, OutputStream entityStream, MediaType mediaType, ResourceDescription presentation) {
        if (presentation == null) {
            lower(model, entityStream, mediaType);
            return;
        }
        List<String> messages = new ArrayList<>();
        Iterator<LoweringHandler> it = loweringHandlerInstance.iterator();
        while (it.hasNext()) {
            LoweringHandler handler = it.next();
            ForMediaType handledMediaType = handler.getClass().getAnnotation(ForMediaType.class);
            if (handledMediaType != null && !MediaType.valueOf(handledMediaType.value()).isCompatible(mediaType)) {
                messages.add("Lowering handler " + handler.getClass().getSimpleName() + " cannot lower for media type \"" + mediaType + "\"");
                continue;
            }
            try {
                handler.lower(model, entityStream, mediaType, presentation);
                return;
            } catch (ResourcePlatformException ex) {
//                messages.add("Error while trying with handler: " + handler.getClass().getSimpleName());
                messages.add(ex.getMessage());
            }
        }
        String message = "Could not lower RDF content:\n" + String.join("\n", messages);
        Response response = Response.serverError().entity(message).build();
        throw new InternalServerErrorException(response);
    }

}
