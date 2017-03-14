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

import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.RDFPException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.vocabulary.RDFS;

/**
 * A representations map is generated from a turtle document. It is parsed by
 * the dependency to be used by jersey filters and resources.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class RepresentationsMap {

    private static final Logger LOG = Logger.getLogger(RepresentationsMap.class.getSimpleName());

    private static RepresentationsMap INSTANCE = null;

    static RepresentationsMap get(String base) {
        if (base == null) {
            throw new NullPointerException();
        }
        if (INSTANCE == null) {
            INSTANCE = new RepresentationsMap(base);
        }
        if (!INSTANCE.base.equals(base)) {
            throw new IllegalArgumentException();
        }
        return INSTANCE;
    }

    private final String base;

    /**
     * value rdfs:isDefinedBy key
     */
    private final Map<String, String> redirections = new HashMap<>();

    /**
     * value rdfp:alias key
     */
    private final Map<String, String> aliases = new HashMap<>();

    /**
     * key rdfp:representedBy one of values
     */
    private final Map<String, Set<Representation>> multiRepresentations = new HashMap<>();

    /**
     * key rdfp:mediaType some media type
     */
    private final Map<String, Representation> representations = new HashMap<>();

    private Model conf = null;

    private LocationMapper loc;

    private RepresentationsMap(String base) {
        this.base = base;
        System.out.println("Constructing representations map...");

        loc = StreamManager.get().getLocationMapper();

        final String confpath = "_rdfp/config.ttl";
        try {
            conf = RDFDataMgr.loadModel(RepresentationsMap.class.getClassLoader().getResource(confpath).toURI().toString());
        } catch (Exception ex) {
            LOG.warning("Configuration file '" + confpath + "'. RDFP will be disabled.");
            return;
        }

        try {
            readRedirections();
        } catch (RDFPException ex) {
            throw new RuntimeException(ex);
        }

        try {
            readAliases();
        } catch (RDFPException ex) {
            throw new RuntimeException(ex);
        }

        try {
            readRepresentations();
        } catch (RDFPException ex) {
            throw new RuntimeException(ex);
        }

        try {
            readMultiRepresentations();
        } catch (RDFPException ex) {
            throw new RuntimeException(ex);
        }

        // close alts. 
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<String> it = loc.listAltEntries();
            Set<String> entries = new HashSet<>();
            while (it.hasNext()) {
                entries.add(it.next());
            }
            for (String entry : entries) {
                String target = loc.getAltEntry(entry);
                if (entry.equals(target)) {
                    loc.removeAltEntry(entry);
                    continue;
                }
                String targetTarget = loc.getAltEntry(target);
                if (target.equals(targetTarget)) {
                    loc.removeAltEntry(target);
                    continue;
                }
                if (targetTarget != null) {
                    loc.removeAltEntry(entry);
                    loc.addAltEntry(entry, targetTarget);
                    changed = true;
                }
                System.out.println("");
            }
        }
        System.out.println("Jena StreamManager FileLocator contains:");
        Iterator<String> it = loc.listAltEntries();
        while (it.hasNext()) {
            String entry = it.next();
            System.out.println("Entry " + entry + " --> " + loc.getAltEntry(entry));
        }
    }

    public Map<String, String> getRedirections() {
        return redirections;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public Map<String, Set<Representation>> getMultiRepresentations() {
        return multiRepresentations;
    }

    public Map<String, Representation> getRepresentations() {
        return representations;
    }

    private void readRedirections() throws RDFPException {
        StmtIterator sit = conf.listStatements(null, RDFS.isDefinedBy, (RDFNode) null);
        while (sit.hasNext()) {
            Statement s = sit.next();
            final Resource subject = s.getSubject();
            final RDFNode object = s.getObject();
            if (!subject.isURIResource()) {
                throw new RDFPException("resource " + subject + " should be a URI resource");
            }
            if (!object.isURIResource()) {
                throw new RDFPException("resource " + object + " should be a URI resource");
            }
            try {
                String source = getLocalPath(subject.getURI());
                String target = getLocalPath(object.asResource().getURI());
                redirections.put(source, target);
                loc.addAltEntry(subject.getURI(), object.asResource().getURI());
                System.out.println("redirection " + subject.getURI() + " -> " + object.asResource().getURI());
            } catch (RDFPException ex) {
                throw new RDFPException(ex.getMessage());
            }
        }

    }

    private void readRepresentations() throws RDFPException {
        StmtIterator sit = conf.listStatements(null, RDFP.mediaType, (RDFNode) null);
        while (sit.hasNext()) {
            Statement s = sit.next();
            final Resource subject = s.getSubject();
            final RDFNode object = s.getObject();
            if (!subject.isURIResource()) {
                throw new RDFPException("resource " + subject + " should be a URI resource");
            }
            if (!object.isLiteral()) {
                throw new RDFPException("resource " + object + " should be a literal");
            }
            try {
                String localPath = getLocalPath(subject.getURI());
                MediaType mt = MediaType.valueOf(object.asLiteral().getLexicalForm());
                Representation representation = new Representation(mt, localPath);
                representations.put(localPath, representation);
                try {
                    String localUrl = RepresentationsMap.class.getClassLoader().getResource(representation.getLocalPath()).toString();
                    loc.addAltEntry(base + localPath, localUrl);
                    System.out.println("new RDF representation " + base + localPath + " -> " + localUrl);
                } catch (Exception ex) {
                }
            } catch (Exception ex) {
                throw new RDFPException(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }

    private void readMultiRepresentations() throws RDFPException {
        StmtIterator sit = conf.listStatements(null, RDFP.representedBy, (RDFNode) null);
        while (sit.hasNext()) {
            Statement s = sit.next();
            final Resource subject = s.getSubject();
            final RDFNode object = s.getObject();
            if (!subject.isURIResource()) {
                throw new RDFPException("resource " + subject + " should be a URI resource");
            }
            if (!object.isURIResource()) {
                throw new RDFPException("resource " + object + " should be a URI resource");
            }
            try {
                String localPath = getLocalPath(subject.getURI());
                String objectLocalPath = getLocalPath(object.asResource().getURI());
                Representation representation = representations.get(objectLocalPath);
                if (representation == null) {
                    throw new RDFPException("representation should not be null for " + objectLocalPath);
                }
                Set<Representation> set = multiRepresentations.get(localPath);
                if (set == null) {
                    set = new HashSet<>();
                    multiRepresentations.put(localPath, set);
                }
                set.add(representation);
                loc.addAltEntry(subject.getURI(), object.asResource().getURI());
                System.out.println("new multi-representation " + subject.getURI() + " -> " + object.asResource().getURI());
            } catch (Exception ex) {
                throw new RDFPException(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }

    private void readAliases() throws RDFPException {
        StmtIterator sit = conf.listStatements(null, RDFP.alias, (RDFNode) null);
        while (sit.hasNext()) {
            Statement s = sit.next();

            final Resource subject = s.getSubject();
            final RDFNode object = s.getObject();
            if (!subject.isURIResource()) {
                throw new RDFPException("resource " + subject + " should be a URI resource");
            }
            if (!object.isURIResource()) {
                throw new RDFPException("resource " + object + " should be a URI resource");
            }
            try {
                String localPath = getLocalPath(subject.getURI());
                String aliasLocalPath = getLocalPath(object.asResource().getURI());
                aliases.put(aliasLocalPath, localPath);
                loc.addAltEntry(object.asResource().getURI(), subject.getURI());
                System.out.println("new alias " + object.asResource().getURI() + " -> " + subject.getURI());
            } catch (Exception ex) {
                throw new RDFPException(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }

    private String getLocalPath(String path) throws RDFPException {
        if (!path.startsWith(base)) {
            throw new RDFPException("path must start with " + base + ". got " + path);
        }
        return path.substring(base.length());
    }

}
