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
import com.github.thesmartenergy.rdfp.preneg.LiftingHandler;
import com.github.thesmartenergy.rdfp.jersey.PresentationUtils;
import com.github.thesmartenergy.rdfp.BaseURI;
import com.github.thesmartenergy.rdfp.RDFPException;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateHandler extends BaseHandler implements LiftingHandler {

    private static final Logger LOG = Logger.getLogger(SPARQLGenerateHandler.class.getSimpleName());

    final String base;

    final PresentationUtils presentationUtils;

    @Inject
    public SPARQLGenerateHandler(@BaseURI String base, PresentationUtils presentationUtils) {
        this.base = base;
        this.presentationUtils = presentationUtils;
    }

    @Override
    public Model lift(MediaType mediaType, ResourceDescription presentation, InputStream entityStream) throws RDFPException {
        if (presentation == null) {
            throw new RDFPException("Lifting handler SPARQL Generate cannot lift without a presentation description");
        }
        MediaType presentationMediaType = presentationUtils.presentationAcceptedMediaType(presentation);
        if (!presentationUtils.presentationAcceptedMediaType(presentation).isCompatible(mediaType)) {
            throw new RDFPException("Lifting handler SPARQL Generate is asked to use presentatation <" + presentation.getNode() + "> that supports media type \"" + presentationMediaType + "\", but content media type is \"" + mediaType + "\"");
        }
        List<String> liftingRulesURIs = getLiftingRulesUris(presentation);
        if (liftingRulesURIs.isEmpty()) {
            throw new RDFPException("Lifting handler SPARQL Generate could not find any lifting rule associated to presentation <" + presentation.getNode() + ">");
        }
        List<String> errors = new ArrayList<>();
        for (String liftingRuleURI : liftingRulesURIs) {
            try {

                String accept = SPARQLGenerate.MEDIA_TYPE + ";q=1.0,*/*;q=0.1";
                String liftingRule = getRule(liftingRuleURI, accept);
                if (liftingRule == null) {
                    throw new RDFPException("No SPARQL Generate rule was found at " + liftingRuleURI);
                }

                SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create(liftingRule, SPARQLGenerate.SYNTAX);
                PlanFactory factory = new PlanFactory();
                RootPlan plan = factory.create(query);
                QuerySolutionMap initialBinding = new QuerySolutionMap();
                Model initialModel = ModelFactory.createDefaultModel();
                initialBinding.add("message", initialModel.createLiteral(IOUtils.toString(entityStream)));
                plan.exec(initialBinding, initialModel);
                return initialModel;
            } catch (Exception ex) {
                errors.add("Error while trying with rule <" + liftingRuleURI + ">:");
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
        throw new RDFPException("Lifting handler SPARQL Generate could not lower with representation <" + presentation.getNode() + ">. Errors are:\n" + String.join("\n", errors));
    }

    @Override
    public Model lift(MediaType mediaType, InputStream entityStream) throws RDFPException {
        return lift(mediaType, null, entityStream);
    }

}
