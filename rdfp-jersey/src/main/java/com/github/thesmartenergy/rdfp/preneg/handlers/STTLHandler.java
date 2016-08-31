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

import com.github.thesmartenergy.rdfp.preneg.LoweringHandler;
import com.github.thesmartenergy.rdfp.jersey.PresentationUtils;
import com.github.thesmartenergy.rdfp.BaseURI;
import com.github.thesmartenergy.rdfp.ResourcePlatformException;
import com.github.thesmartenergy.rdfp.resources.ResourceDescription;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.transform.Transformer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.stream.StreamManager;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class STTLHandler extends BaseHandler implements LoweringHandler {

    private static final Logger LOG = Logger.getLogger(STTLHandler.class.getSimpleName());

    public static final String CORESE_RULES_MEDIA_TYPE = "application/vnd.corese-rules";

    @Inject
    @BaseURI
    String base;

    @Inject
    PresentationUtils presentationUtils;
    
    @Override
    public void lower(Model model, OutputStream entityStream, MediaType mediaType) throws ResourcePlatformException {
        lower(model, entityStream, mediaType, null);
    }

    @Override
    public void lower(Model model, OutputStream entityStream, MediaType mediaType, ResourceDescription presentation) throws ResourcePlatformException {
        if (presentation == null) {
            throw new ResourcePlatformException("Lowering handler STTL cannot lower without a presentation description");
        }
        MediaType presentationMediaType = presentationUtils.presentationAcceptedMediaType(presentation);
        LOG.info(mediaType + " vs " + presentationMediaType);
        if (!presentationUtils.presentationAcceptedMediaType(presentation).isCompatible(mediaType)) {
            throw new ResourcePlatformException("Lowering handler STTL is asked to use presentatation <" + presentation.getNode() + "> that supports media type \"" + presentationMediaType + "\", but content media type is \"" + mediaType + "\"");
        }
        List<String> loweringRulesURIs = getLoweringRulesUris(presentation);
        if (loweringRulesURIs.isEmpty()) {
            throw new ResourcePlatformException("Lowering handler STTL could not find any lowering rule associated to presentation <" + presentation.getNode() + ">");
        }
        // MediaType.valueOf("application/vnd.corese-rules")

        List<String> errors = new ArrayList<>();
        for (String loweringRuleURI : loweringRulesURIs) {
            try {
                Graph g = Graph.create();
                Load ld = Load.create(g);
                StringWriter sb = new StringWriter();
                model.write(sb);
                ld.load(IOUtils.toInputStream(sb.toString()));
                String alt = StreamManager.get().getLocationMapper().getAltEntry(loweringRuleURI);
                if (alt != null) {
                    loweringRuleURI = alt;
                }
                Transformer t = Transformer.create(g, loweringRuleURI);
                entityStream.write(t.transform().getBytes());
                return;
            } catch (LoadException | IOException ex) {
                errors.add("Error while trying with rule <" + loweringRuleURI + ">:");
                errors.add(ex.getClass().getName() + ": " + ex.getMessage());
                errors.add(" at: " + ex.getStackTrace()[0].toString());
                Throwable cause = ex.getCause();
                while (cause != null) {
                    errors.add("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                    errors.add(" at: " + cause.getStackTrace()[0].toString());
                    cause = cause.getCause();
                }
            }
        }
        throw new ResourcePlatformException("Lowering handler STTL could not lower with representation <" + presentation.getNode() + ">. Errors are:\n" + String.join("\n", errors));
    }

}
