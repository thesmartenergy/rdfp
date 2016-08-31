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

import com.github.thesmartenergy.rdfp.resources.ResourceManager;
import com.github.thesmartenergy.rdfp.resources.WebRepresentation;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

/**
 * @author maxime.lefrancois
 */
@Path("resource")
public class Resources {

    static final Logger LOG = Logger.getLogger(Resources.class.getSimpleName());

    // WARNING: the glassfish container cannot do dependency injection for Jersey resources when they are in a third party library !!!
    
    @Inject
    HttpServletRequest req;

    @GET
    @Path("{resourcePath: .*}")
    public Response get(@Context Request request, @PathParam("resourcePath") String resourcePath) {
        ResourceManager resourceManager = (ResourceManager) req.getAttribute("resourceManager");
        if (resourceManager == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
//        System.out.println("resourcePath " + resourcePath);
        try {
            if (!resourceManager.contains(resourcePath)) {
                String msg = "Did not find resource " + resourcePath + " locally";
                LOG.log(Level.SEVERE, msg);
                return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
            }
            Variant variant = request.selectVariant(resourceManager.getVariants(resourcePath));
            if (variant == null) {
                String msg = "Did not find an acceptable representation for resource " + resourcePath;
                LOG.log(Level.SEVERE, msg);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            MediaType mediaType = variant.getMediaType();
            WebRepresentation rep = resourceManager.getRepresentation(resourcePath, mediaType);
            return Response.ok(rep.read(), variant.getMediaType().getType() + "/" + variant.getMediaType().getSubtype())
                    .header("Content-Disposition", "filename= " + rep.getFileName() + ";")
                    .build();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
