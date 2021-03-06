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
package com.github.thesmartenergy.rdfp.preneg;

import com.github.thesmartenergy.rdfp.RDFPException;
import com.github.thesmartenergy.rdfp.ResourceDescription;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface LoweringHandler {

    /**
     * 
     * @param model
     * @param entityStream
     * @param mediaType
     * @param presentation 
     * @throws RDFPException 
     */
    void lower(Model model, OutputStream entityStream, MediaType mediaType, ResourceDescription presentation) throws RDFPException;

    /**
     * 
     * @param model
     * @param entityStream
     * @param mediaType
     * @throws RDFPException 
     */
    void lower(Model model, OutputStream entityStream, MediaType mediaType) throws RDFPException;

}
