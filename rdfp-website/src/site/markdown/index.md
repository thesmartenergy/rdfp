# RDF Presentation and RDF Presentation Negociation

RDF graphs can have multiple representations, with different media types. Most of the data on the web is not represented as RDF. 

- We want to ease the adaptation of existing web services to Semantic Web formalisms;
- We want to enable lightweight server and/or clients to negociate lightweight representations for RDF documents;
- We want to ease the development of RESTful web applications that expose/consume RDF.


## RDF Presentation description 


This website exposes an ontology to describe RDF Graphs, and the different ways they may be presented as documents.

This ontology can be accessed at this URI: https://w3id.org/rdfp/ using content negociation (i.e., set HTTP Header field `Accept` to one of the RDF syntaxes media types, or directly in [turtle](.ttl), or in [RDF/XML](.rdf).

A RDF Graph description can be linked to:

- zero or more validation rules, that can be used to validate the RDF Graph.
- zero or more RDF Presentations, that define how such RDF Graphs can be represented as documents.

A RDF Presentation can be linked to:

- zero or more lowering rule, that was used to lower the RDF Graph to this representation, and/or
- zero or more lifting rule, that can be used to lift the representation to a RDF Graph, and/or
- zero or more validation rule, that can be used to validate the representation.


## RDF Presentation Negociation

RDF Presentation Negociation is a way for the client and the server to agree on a presentation to use for the request and/or response payloads.

As per the [W3C Architecture of the World Wide Web, Volume One](https://www.w3.org/TR/2004/REC-webarch-20041215/), presentation is related to representations, and not contents.
It would be best represented as a media type parameter. Yet, [RFC 2045, Multipurpose Internet Mail Extensions (MIME) Part One: Format of Internet Message Bodies](https://tools.ietf.org/html/rfc2045)  states:

```
There are NO globally-meaningful parameters that apply to all media types.  Truly global mechanisms are best addressed, in the MIME model, by the definition of additional Content-* header fields.
```

We hence use a new HTTP header: `Content-Presentation`, and its counterpart for presentation negociation: `Accept-Presentation`.

`Content-Presentation` defines the RDF Graph Presentation for the represented RDF Graph (an IRI). 
Lightweight clients can use this header to request a lightweight representation from the HTTP server.

`Accept-Presentation` can be in the HTTP request to define the presentation the client prefers.
Lightweight clients can use this header to request a lightweight representation from the HTTP server.


## Implementation of the RDF Presentation description and the RDF Presentation Negociation on top of Jersey

This implementation eases the development of RESTful applications that produce and consume RDF.

### Exposition of RDF graphs, vocabularies, ontologies

- It eases the exposition of RDF documents and checks that the Semantic Web best practices are respected (Linked Data and/or Linked Vocabularies).
- It eases the development of RESTful applications that consume and produce RDF, and takes care of all the content-negociation part, further implementing RDF Presentation Negociation.

### Consuming RDF presented anyhow

`rdfp-jersey` takes care of using the RDF Presentation description to interpret the HTTP request as a RDF Graph when possible. All the machinery is hidden to the end developper, that can directly use a parameter of type `Model` in the Jersey resource method:

```
@POST
public Response doPost(@GraphDescription("https://w3id.org/rdfp/example/graph") Model model) {
    ...
}
```

### Publishing RDF with RDF Presentation Negociation

`rdfp-jersey` takes care of using the RDF Presentation description and RDF Presentation Negociation to generate a representation of the returned RDF Graph when possible, and to send this representation in the HTTP response. All the machinery is hidden to the end developper, that can make the Jersey resource method directly return an entity of type `Model`:

```
@GET
@GraphDescription("https://w3id.org/rdfp/example/graph")
public Response doGet() {
    Model model = ...
    return Response.ok(model).build();
}
```

## Demonstration

Resource https://w3id.org/rdfp/example on this website demonstrates RDF Presentation Negociation, and the use of the `rdfp-jersey` implementation.

The resource exposes and consumes RDF Graphs that are described by https://w3id.org/rdfp/example/graph (negociate its representation with the server, or directly access the [turtle](https://w3id.org/rdfp/example/graph.ttl), or a [RDF/XML](https://w3id.org/rdfp/example/graph.rdf) document).

This graph description explicitly mentions two presentations (in addition to the well known turtle and RDF/XML presentations:

- one for media type `application/xml`;
- one for media type `application/json`.

Also, two other Graph Presentations can be played with:

- another for media type `application/xml`;
- one for media type `text/plain`.

In order to test the POST operation, examples of inputs can be found at URL https://w3id.org/rdfp/example/input. Negociate its representation with the server, or directly access:

-  https://w3id.org/rdfp/example/input.txt
-  https://w3id.org/rdfp/example/input.json
-  https://w3id.org/rdfp/example/input.xml
-  https://w3id.org/rdfp/example/input.ttl

