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

import com.github.thesmartenergy.rdfp.ResourcePlatformException;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FileRepresentation extends WebRepresentation {

    private final String localPath;
    
    public FileRepresentation(MediaType mediaType, String localPath) {
        super(mediaType);
        if(localPath == null) {
            throw new IllegalArgumentException();
        }
        this.localPath = localPath;
    }

    @Override
    public String read() throws ResourcePlatformException {
        try {
            return IOUtils.toString(FileRepresentation.class.getClassLoader().getResourceAsStream(localPath));
        } catch (IOException ex) {
            throw new ResourcePlatformException(ex);
        }
    }

    @Override
    public String getFileName() {
        return localPath.substring("resources/".length());
    }

    @Override
    public String getLocalURL() {
        return ResourceManager.class.getClassLoader().getResource(localPath).toString();
    }
    
    

}
