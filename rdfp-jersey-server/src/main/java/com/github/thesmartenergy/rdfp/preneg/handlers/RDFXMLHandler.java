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

import com.github.thesmartenergy.rdfp.preneg.ForMediaType;
import com.github.thesmartenergy.rdfp.ResourceDescription;
import com.github.thesmartenergy.rdfp.preneg.LiftingHandler;
import com.github.thesmartenergy.rdfp.preneg.LoweringHandler;
import com.github.thesmartenergy.rdfp.BaseURI;
import com.github.thesmartenergy.rdfp.RDFPException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
@ForMediaType("application/rdf+xml")
public class RDFXMLHandler implements LiftingHandler, LoweringHandler {

    public final String MEDIA_TYPE = "application/rdf+xml";

    @Inject
    @BaseURI
    String base;

    @Override
    public void lower(Model model, OutputStream entityStream, MediaType mediaType) throws RDFPException {
        lower(model, entityStream, mediaType, null);
    }

    @Override
    public void lower(Model model, OutputStream entityStream, MediaType mediaType, ResourceDescription presentation) throws RDFPException {
        if (presentation != null) {
            throw new RDFPException("Lowering handler RDFXMLHandler can only lower RDF content with no specified presentation");
        }
        if (!mediaType.isCompatible(MediaType.valueOf(MEDIA_TYPE))) {
            throw new RDFPException("Lowering handler RDFXMLHandler can only lower RDF content to a representation with MediaType " + MEDIA_TYPE);
        }
        model.write(entityStream, "RDF/XML", base);
    }

    @Override
    public Model lift(MediaType mediaType, ResourceDescription presentation, InputStream entityStream) throws RDFPException {
        if (!mediaType.isCompatible(MediaType.valueOf(MEDIA_TYPE))) {
            throw new RDFPException("Lifting handler RDFXMLHandler can only lift representations with MediaType " + MEDIA_TYPE);
        }
        Model model = ModelFactory.createDefaultModel();
        try {
            model.read(entityStream, base, "RDF/XML");
        } catch (Exception ex) {
            throw new RDFPException(ex);
        }
        return model;
    }

    @Override
    public Model lift(MediaType mediaType, InputStream entityStream) throws RDFPException {
        return lift(mediaType, null, entityStream);
    }

}
