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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class Conneg {

    final TreeSet<MT> acceptedMTs = new TreeSet<>();

    public Conneg(String accept) {
        String[] acceptedMediaTypes = accept.split(",");
        for (String acceptedMediaType : acceptedMediaTypes) {
            acceptedMTs.add(new MT(acceptedMediaType));
        }
    }

    public boolean isAcceptable(MediaType offeredMediaType) {
        Iterator<MT> mtit = acceptedMTs.descendingIterator();
        while (mtit.hasNext()) {
            MT mt = mtit.next();
            if (mt.mt.isCompatible(offeredMediaType)) {
                return true; 
            }
        }
        return false;
    }

    public Representation getPreferredRepresentation(Set<Representation> offeredRepresentations) {
        Iterator<MT> mtit = acceptedMTs.descendingIterator();
        while (mtit.hasNext()) {
            MT mt = mtit.next();
            Iterator<Representation> orit = offeredRepresentations.iterator();
            while (orit.hasNext()) {
                Representation or = orit.next();
                if (mt.mt.isCompatible(or.getMediaType())) {
                    return or;
                }
            }
        }
        return null;
    }

    private class MT implements Comparable<MT> {

        private final Double q;
        private final MediaType mt;

        private MT(String mt) {
            this.mt = MediaType.valueOf(mt);
            String q = this.mt.getParameters().get("q");
            this.q = q == null ? 1.0 : Double.parseDouble(q);
        }

        @Override
        public int compareTo(MT o) {
            int c = q.compareTo(o.q);
            if (c != 0) {
                return c;
            }
            c = mt.getType().compareTo(o.mt.getType());
            if (c != 0) {
                return c;
            }
            c = mt.getSubtype().compareTo(o.mt.getSubtype());
            if (c != 0) {
                return c;
            }
            return 0;
        }

    }

}