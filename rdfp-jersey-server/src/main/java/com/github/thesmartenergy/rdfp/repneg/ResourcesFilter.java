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
package com.github.thesmartenergy.rdfp.repneg;

import com.github.thesmartenergy.rdfp.BaseURI;
import com.github.thesmartenergy.rdfp.DevelopmentBaseURI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

/**
 * This class filters the calls to resources, and dispatches to the endpoint
 * that exposes the ontology that defines the resource.
 *
 * @author maxime.lefrancois
 */
@WebFilter(urlPatterns = {"/*"})
public class ResourcesFilter implements Filter {

    @Inject
    @DevelopmentBaseURI
    String DEV_BASE;

    @Inject
    @BaseURI
    String BASE; 

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RepresentationsMap map = RepresentationsMap.get(BASE);
        HttpServletRequest req = ((HttpServletRequest) request);
        HttpServletResponse res = ((HttpServletResponse) response);
        String contextPath = req.getContextPath() + "/";
        String requestURI = req.getRequestURI();
        if (requestURI.startsWith(contextPath)) { 
            String resourcePath = requestURI.substring(contextPath.length());
            req.setAttribute("base", DEV_BASE);

            if (req.getMethod().equals("GET") && map.getRedirections().containsKey(resourcePath)) {
                String redirection = DEV_BASE + (DEV_BASE.endsWith("/") ? "" : "/") + map.getRedirections().get(resourcePath);
                res.setHeader("Location", redirection);
                res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                res.flushBuffer();
                return;
            }

            final List<MediaType> available = new ArrayList<>();

            if (map.getAliases().containsKey(resourcePath)) {
                req.setAttribute("setCanonicalPath", true);
                resourcePath = map.getAliases().get(resourcePath);
            }

            Representation representation;

            representation = map.getRepresentations().get(resourcePath);
            if (representation != null) {
                available.add(representation.getMediaType());
                Conneg conneg = new Conneg(req.getHeader("Accept"));
                if (conneg.isAcceptable(representation.getMediaType())) {
                    req.setAttribute("representation", representation);
                    String newURI = "/_rdfp/resource/" + resourcePath;
                    req.getRequestDispatcher(newURI).forward(req, res);
                    return;
                }
            }

            Set<Representation> offeredRepresentations = map.getMultiRepresentations().get(resourcePath); 
            if (offeredRepresentations != null) {
                for (Representation r : offeredRepresentations) {
                    available.add(r.getMediaType());
                }
                Conneg conneg = new Conneg(req.getHeader("Accept"));
                representation = conneg.getPreferredRepresentation(offeredRepresentations);
                if (representation != null) {
                    req.setAttribute("representation", representation);
                    String newURI = "/_rdfp/resource/" + resourcePath;
                    req.getRequestDispatcher(newURI).forward(req, res);
                    return;
                }
            }

            if (!available.isEmpty()) {
                res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Not Acceptable. Acceptable media types are: " + available);
                res.flushBuffer();
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}