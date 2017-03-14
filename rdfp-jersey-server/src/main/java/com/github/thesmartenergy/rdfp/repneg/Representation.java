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

import javax.ws.rs.core.MediaType;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class Representation {

    private final String resourcePath;
    private final MediaType mediaType;

    public Representation(MediaType mediaType, String resourcePath) {
        if (mediaType == null) {
            throw new IllegalArgumentException();
        }
        if(resourcePath == null) {
            throw new IllegalArgumentException();
        }
        this.mediaType = mediaType;
        this.resourcePath = resourcePath;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isCompatible(MediaType mediaType) {
        return this.mediaType.isCompatible(mediaType);
    }

    public String getResourcePath() {
        return resourcePath;
    }
 
    public String getFileName() {
        return resourcePath.replace("/", "-");
    }

    String getLocalPath() {
        return "_rdfp/" + resourcePath;
    }
    
}