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
package com.github.thesmartenergy.rdfp.preneg.handlers;

import com.github.thesmartenergy.rdfp.ResourceDescription;
import com.github.thesmartenergy.rdfp.RDFP;
import com.github.thesmartenergy.rdfp.BaseURI;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
class BaseHandler {

    private static final Logger LOG = Logger.getLogger(BaseHandler.class.getSimpleName());

    @Inject
    @BaseURI
    String base;
    
    public List<String> getLiftingRulesUris(ResourceDescription presentation) {
        return getRulesUris(RuleType.LIFTING, presentation);
    }

    public List<String> getLoweringRulesUris(ResourceDescription presentation) {
        return getRulesUris(RuleType.LOWERING, presentation);
    }

    private List<String> getRulesUris(RuleType type, ResourceDescription presentation) {
        List<String> result = new ArrayList<>();
        NodeIterator nit = presentation.getModel().listObjectsOfProperty(
                presentation.getNode().asResource(),
                type.property);
        while (nit.hasNext()) {
            RDFNode n = nit.next();
            if (n.isURIResource()) {
                try {
                    result.add(n.asResource().getURI());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, null, ex);
                }
            } else {
                throw new UnsupportedOperationException("not implemented yet");
            }
        }
        return result;
    }

    private enum RuleType {
        LIFTING(RDFP.liftingRule),
        LOWERING(RDFP.loweringRule);

        private Property property;

        private RuleType(Property property) {
            this.property = property;
        }
    }

    protected String getRule(String url, String accept) throws IOException {
        URL resourceUrl, base, next;
        HttpURLConnection conn;
        String location;
        while (true) {
            resourceUrl = new URL(url);
            conn = (HttpURLConnection) resourceUrl.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
            conn.setRequestProperty("User-Agent", "RDFP");
            conn.addRequestProperty("Accept", accept);
            switch (conn.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    location = conn.getHeaderField("Location");
                    base = new URL(url);
                    next = new URL(base, location);  // Deal with relative URLs
                    url = next.toExternalForm();
                    continue;
            }
            break;
        }
        return IOUtils.toString(conn.getInputStream(), "UTF-8");
    }
}
