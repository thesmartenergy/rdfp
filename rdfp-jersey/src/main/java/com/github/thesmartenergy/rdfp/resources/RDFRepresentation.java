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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class RDFRepresentation extends WebRepresentation {

    private String localPath;
    
    static final MediaType TTL = MediaType.valueOf("text/turtle");
    static final MediaType RDFXML = MediaType.valueOf("application/rdf+xml");
    
    @Inject
    @BaseURI
    private String BASE;

    public RDFRepresentation(MediaType mediaType, String localPath) {
        super(mediaType);
        if(!mediaType.isCompatible(TTL) && !mediaType.isCompatible(RDFXML)) {
            throw new IllegalArgumentException();
        }
        if(localPath == null) {
            throw new IllegalArgumentException();
        }
        this.localPath = localPath;
    }

    @Override
    public String read() throws ResourcePlatformException {
        try {
            InputStream in = FileRepresentation.class.getClassLoader().getResourceAsStream(localPath);
            if (mediaType.isCompatible(TTL)) {
                return IOUtils.toString(in);
            } else if (mediaType.isCompatible(RDFXML)) {
                Model model = ModelFactory.createDefaultModel();
                model.read(in, BASE, "TTL");
                StringWriter sw = new StringWriter();
                model.write(sw);
                return sw.toString();
            }
            throw new ResourcePlatformException();
        } catch (IOException ex) {
            throw new ResourcePlatformException(ex);
        }
    }
    

    @Override
    public String getFileName() {
        if (mediaType.isCompatible(TTL)) {
            return localPath.substring("resources/".length());
        } else if (mediaType.isCompatible(RDFXML)) {
            return localPath.substring("resources/".length(), localPath.length() - 4) + ".rdf";
        }
        return null;
    }
    
    @Override
    public String getLocalURL() {
        return ResourceManager.class.getClassLoader().getResource(localPath).toString();
    }

}
