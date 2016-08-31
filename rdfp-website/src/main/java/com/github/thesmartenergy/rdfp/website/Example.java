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
package com.github.thesmartenergy.rdfp.website;

import com.github.thesmartenergy.rdfp.BaseURI;
import java.io.StringWriter;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import com.github.thesmartenergy.rdfp.preneg.GraphDescription;

/**
 *
 * @author maxime.lefrancois
 */
@Path("")
public class Example {

    private static final Logger LOG = Logger.getLogger(Example.class.getSimpleName());

    @Inject
    @BaseURI
    String BASE;

    @POST
    public Response doPost(@GraphDescription("https://w3id.org/rdfp/example/graph") Model model) {
        StringWriter sw = new StringWriter();
        model.write(sw, "TTL");
        return Response.ok(sw.toString(), "text/turtle").build();
    }

    @GET
    @GraphDescription("https://w3id.org/rdfp/example/graph")
    public Response doGet() {
        Model model = RDFDataMgr.loadModel(Example.class.getClassLoader().getResource("resources/input-example.ttl").toString());
        return Response.ok(model).build();
    }


}
