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
package com.github.thesmartenergy.rdfp.resources;

import com.github.thesmartenergy.rdfp.BaseURI;
import com.github.thesmartenergy.rdfp.ResourcePlatformException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@Default
@Dependent
public class ResourceManager {

    private static final Logger LOG = Logger.getLogger(ResourceManager.class.getSimpleName());

    private static boolean initialized = false;

    private static final Map<String, Set<WebRepresentation>> RESOURCES = new HashMap<>();
    private static final Map<String, String> REDIRECTIONS = new HashMap<>();
    private static final LocationMapper LOC_MAPPER = new LocationMapper();

    private static String BASE;

    // parameter is the localPath
    private static final Set<String> CHECKED = new HashSet<>();

    private static void closeLocationMappingTransitively() {
        Iterator<String> it = LOC_MAPPER.listAltEntries();
        while (it.hasNext()) {
            String entry = it.next();
            String alt = LOC_MAPPER.getAltEntry(LOC_MAPPER.getAltEntry(entry));
            if (alt != null) {
                LOC_MAPPER.addAltEntry(entry, alt);
            }
        }
    }

    @Inject
    public ResourceManager(@BaseURI String base) {
        try {
            BASE = base;
            init();
        } catch (ResourcePlatformException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void init() throws ResourcePlatformException {
        if (initialized) {
            return;
        }
        initialized = true;
        Model model = ModelFactory.createDefaultModel();
        model.read(ResourceManager.class.getClassLoader().getResourceAsStream("resources/config.ttl"), BASE, "TTL");

        analyzeResources(model);
        analyzeRDFSources(model);

        closeLocationMappingTransitively();

        StreamManager.get().setLocationMapper(LOC_MAPPER);
    }

    private static String getResourcePath(RDFNode resource) throws ResourcePlatformException {
        if (!resource.isURIResource()) {
            throw new ResourcePlatformException("Resource must be a URI ");
        }
        String resourceUri = ((Resource) resource).getURI();
        if (!resourceUri.startsWith(BASE)) {
            throw new ResourcePlatformException("Resource URI " + resourceUri + " MUST start with " + BASE + ".");
        }
        if (resourceUri.contains("#")) {
            throw new ResourcePlatformException("Resource URI " + resourceUri + " MUST not contain a fragment identifier.");
        }
        String resourcePath = resourceUri.substring(BASE.length());
        if (REDIRECTIONS.containsKey(resourcePath)) {
            throw new ResourcePlatformException("Graph " + resourcePath + "is already defined as a defined resource in graph " + REDIRECTIONS.get(resourcePath));
        }
        return resourcePath;
    }

    private static void analyzeResources(final Model model) {
        model.listStatements(null, RDF.type, model.getResource("http://w3id.org/rdfp/Resource"))
                .forEachRemaining(new Consumer<Statement>() {
                    @Override
                    public void accept(Statement t) {
                        Resource r = t.getSubject();
                        try {
                            analyzeResource(r, model);
                        } catch (ResourcePlatformException ex) {
                            LOG.log(Level.WARNING, "error while analyzing resource " + r, ex);
                        }
                    }
                });
    }

    private static void analyzeResource(Resource resource, final Model model) throws ResourcePlatformException {
        final String resourcePath = getResourcePath(resource);
        final Set<String> aliases = getAliases(resource, model);

        model.listObjectsOfProperty(resource, model.getProperty("http://w3id.org/rdfp/presentedBy"))
                .forEachRemaining(new Consumer<RDFNode>() {
                    @Override
                    public void accept(RDFNode representation) {
                        if (!representation.isResource()) {
                            LOG.log(Level.WARNING, "representation " + representation + " must be a resource");
                        }
                        analyzeRepresentation(resourcePath, aliases, representation.asResource(), model);
                    }
                });
    }

    private static Set<String> getAliases(Resource resource, final Model model) {
        final Set<String> aliases = new HashSet<>();
        model.listStatements(resource, model.getProperty("http://w3id.org/rdfp/alias"), (Resource) null)
                .forEachRemaining(new Consumer<Statement>() {
                    @Override
                    public void accept(Statement t) {
                        try {
                            aliases.add(getResourcePath(t.getObject()));
                        } catch (ResourcePlatformException ex) {
                            LOG.log(Level.WARNING, "error while analyzing aliases: ", ex);
                        }
                    }
                });
        return aliases;
    }

    private static void analyzeRepresentation(String resourcePath, Set<String> aliases, Resource representation, Model model) {
        MediaType mediaType = null;
        try {
            mediaType = MediaType.valueOf(model.getProperty((Resource) representation, model.getProperty("http://w3id.org/rdfp/mediaType")).getObject().asLiteral().getLexicalForm());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "error while reading mediatype for representation " + representation, ex);
        }
        String localPath = getLocalPath(representation, model);
        String fileExtension = "." + FilenameUtils.getExtension(localPath);

        // create web representation
        WebRepresentation rep = new FileRepresentation(mediaType, localPath);

        // update indexes for:
        // the resourcePath
        // each of the aliases
        // them plus the file extension 
        registerRepresentation(resourcePath, fileExtension, rep);
        for (String alias : aliases) {
            registerRepresentation(alias, fileExtension, rep);
        }
    }

    private static String getLocalPath(Resource resource, Model model) {
        String localPath = null;
        try {
            localPath = "resources/" + model.getProperty((Resource) resource, model.getProperty("http://w3id.org/rdfp/fileName")).getObject().asLiteral().getLexicalForm();
            File f = new File(ResourceManager.class.getClassLoader().getResource(localPath).toURI());
            if (!f.exists()) {
                throw new Exception("file does not exist locally");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "error while reading local path for representation " + resource, ex);
        }
        return localPath;
    }

    private static void registerRepresentation(String resourcePath, String fileExtension, WebRepresentation rep) {
        registerRepresentation(resourcePath, rep);
        registerRepresentation(resourcePath + fileExtension, rep);
    }

    private static void registerRepresentation(String resourcePath, WebRepresentation rep) {
        if (!RESOURCES.containsKey(resourcePath)) {
            RESOURCES.put(resourcePath, new HashSet<WebRepresentation>());
        }
        RESOURCES.get(resourcePath).add(rep);
        LOC_MAPPER.addAltEntry(BASE + resourcePath, rep.getLocalURL());
    }

    private static void analyzeRDFSources(final Model model) {
        model.listStatements(null, RDF.type, model.getResource("http://w3id.org/rdfp/Graph"))
                .forEachRemaining(new Consumer<Statement>() {
                    @Override
                    public void accept(Statement t) {
                        Resource r = t.getSubject();
                        try {
                            analyzeRDFSource(r, model);
                        } catch (ResourcePlatformException ex) {
                            LOG.log(Level.WARNING, "error while analyzing resource " + r, ex);
                        }
                    }
                });
    }

    private static void analyzeRDFSource(Resource resource, Model model) throws ResourcePlatformException {
        final String resourcePath = getResourcePath(resource);
        final Set<String> aliases = getAliases(resource, model);
        final String localPath = getLocalPath(resource, model);

        // check quality
        if (!CHECKED.contains(localPath)) {
            LOG.info("Analysing graph: " + resourcePath + " - local path " + localPath);
            try {
                checkGraph(resourcePath, localPath);
                CHECKED.add(localPath);
            } catch (ResourcePlatformException ex) {
                LOG.log(Level.WARNING, "Quality check failed for graph: " + resourcePath + ". Aborting.", ex);
                CHECKED.add(localPath);
                return;
            }
        }

        // update indexes for:
        // the resourcePath
        // each of the aliases
        // them plus the file extension 
        WebRepresentation ttl = new RDFRepresentation(RDFRepresentation.TTL, localPath);
        registerRepresentation(resourcePath, ttl);
        registerRepresentation(resourcePath, ".ttl", ttl);
        for (String alias : aliases) {
            registerRepresentation(alias, ".ttl", ttl);
        }

        WebRepresentation rdfxml = new RDFRepresentation(RDFRepresentation.RDFXML, localPath);
        registerRepresentation(resourcePath, rdfxml);
        registerRepresentation(resourcePath, ".rdf", rdfxml);
        for (String alias : aliases) {
            registerRepresentation(alias, ".rdf", rdfxml);
        }
    }

    private static void checkGraph(String graphPath, String localPath) throws ResourcePlatformException {
        Model model;

        try {
            File f = new File(ResourceManager.class
                    .getClassLoader().getResource(localPath).toURI());
            model = ModelFactory.createDefaultModel().read(new FileInputStream(f), BASE, "TTL");
        } catch (Exception ex) {
            throw new ResourcePlatformException("Error while reading model", ex);
        }
        Resource document = model.getResource(BASE + graphPath);
        List<String> errors = new ArrayList<>();
        if (!model.contains(document, RDF.type, FOAF.Document)) {
            errors.add("Document " + document + "MUST be of type foaf:Document.");
        }
        try {
            checkRedirections(graphPath, model);
        } catch (ResourcePlatformException ex) {
            errors.add(ex.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new ResourcePlatformException(String.join("\n\t", errors.toArray(new String[]{})));
        }
    }

    private static void checkRedirections(String graphPath, Model model) throws ResourcePlatformException {
        List<String> errors = new ArrayList<>();
        StmtIterator it = model.listStatements(null, RDFS.isDefinedBy, (Resource) null);
        while (it.hasNext()) {
            Statement s = it.next();
            if (!s.getSubject().isURIResource() || !s.getObject().asResource().getURI().startsWith(BASE)) {
                errors.add("defined resource should be a URI and start with <" + BASE + ">. Got " + s.getSubject());
            }
            if (!s.getObject().isURIResource() || !s.getObject().asResource().getURI().equals(BASE + graphPath)) {
                errors.add("defining resource for " + s.getSubject() + " should be <" + BASE + graphPath + ">. Got " + s.getObject());
            }

            LOG.info(" defines resource: " + s.getSubject());

            String definedResource = s.getSubject().asResource().getURI().substring(BASE.length());
            if (definedResource.contains("#")) {
                definedResource = definedResource.substring(0, definedResource.indexOf("#"));
            }
            if (!definedResource.equals(graphPath)) {
                if (contains(graphPath)) {
                    errors.add("defined resource " + definedResource + " is already defined as a graph !");
                } else {
                    String n = REDIRECTIONS.put(definedResource, graphPath);
                    LOC_MAPPER.addAltEntry(BASE + definedResource, BASE + graphPath);
                    if (n == null) {
                        LOG.info(" new redirection: " + definedResource + " -> " + graphPath);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ResourcePlatformException(String.join("\n\t", errors.toArray(new String[]{})));
        }
    }

    public static boolean contains(String resourcePath) {
        return RESOURCES.containsKey(resourcePath);
    }

    public boolean containsRedirection(String resourcePath) {
        return REDIRECTIONS.containsKey(resourcePath);
    }

    public String getRedirection(String resourcePath) {
        return REDIRECTIONS.get(resourcePath);
    }

    public List<Variant> getVariants(String resourcePath) {
        final List<Variant> result = new ArrayList<>();
        RESOURCES.get(resourcePath).forEach(new Consumer<WebRepresentation>() {
            @Override
            public void accept(WebRepresentation t) {
                result.add(new Variant(t.mediaType, null, null));
            }
        });
        return result;
    }

    public WebRepresentation getRepresentation(String resourcePath, MediaType mediaType) {
        Set<WebRepresentation> reps = RESOURCES.get(resourcePath);
        if(reps == null) {
            return null;
        }
        for (WebRepresentation rep : reps) {
            if (rep.isCompatible(mediaType)) {
                return rep;
            }
        }
        return null;
    }

    public boolean containsResource(String resourcePath) {
        return RESOURCES.containsKey(resourcePath);
    }

    public String getByUri(String resourceUri, MediaType mediaType) throws ResourcePlatformException {
        if (resourceUri.startsWith(BASE)) {
            WebRepresentation rep = getRepresentation(resourceUri.substring(BASE.length()), mediaType);
            if (rep != null) {
                return rep.read();
            }
        } else {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(resourceUri).openConnection();
                conn.setUseCaches(true);
                conn.setInstanceFollowRedirects(true);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.addRequestProperty("Accept", mediaType.toString());
                if (conn.getResponseCode() == 200) {
                    return IOUtils.toString(conn.getInputStream());
                }
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "error while accessing " + resourceUri + " on the web with accept media type \"" + mediaType + "\": " + ex.getMessage());
            }
        }
        return null;
    }

}
