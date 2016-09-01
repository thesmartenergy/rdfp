# RDF Presentation and RDF Presentation Negociation

An RDF graph can be presented in several ways, using different media types. Examples of RDF media types include `application/rdf+xml`, `text/turtle`, `application/json+ld`.

Today, most of the content consumed/produced/published, on the Web is not presented in RDF. 

In the Web of Things, HTTP servers and clients would rather exchange lightweight documents, potentially binary. 
Currently, most existing RDF presentations generically apply to any RDF graph, at the cost of being heavy text-based documents.
Yet, lightweight HTTP servers/clients could be better satisfied with consuming/producing/publishing lightweight documents, may its structure be application-specific.

On the other hand, various formalisms have been developed:
- to lift documents to RDF. Examples include [RML mappings](http://rml.io), [XSPARQL](http://xsparql.deri.org/), [SPARQL-Generate](http://w3id.org/sparql-generate/);
- to lower RDF to documents. Examples include [XSPARQL](http://xsparql.deri.org/), [STTL, aka SPARQL-Template](https://ns.inria.fr/sparql-template/);
- to validate RDF graphs. Examples include simple [SPIN](http://spinrdf.org/), [ShEx](http://shexspec.github.io), [SHACL](https://www.w3.org/TR/shacl/). 

For a given range of RDF Graphs and a specific media types, a RDF Presentation is a combination, of lifting, lowering, and validation rules. Together, these rules enable to coherently interpret a representation as RDF (lift), validate the RDF Graph, and generate back the representation from the RDF graph (lower).

While passing any kind of document, potentially lightweight, an HTTP server/client may refer to the specific RDF Presentation that is used. This potentially enables the HTTP client/server to lift the document to RDF, and to validate it.

Similarly, while requesting for a RDF graph, an HTTP server/client may inform the client/server what representation it prefers. Then, the client/server can validate the RDF graph, then lower it into a document.

## What this project contains

This project contains the sources for the RDF Presentation project:

`rdfp-website` contains the sources of the project website at https://w3id.org/rdfp

`rdfp-jersey-server` is an extension of Jersey that eases the development of RESTful services that produce and consume RDF. It hides the RDF Presentation description and RDF Presentation Negociation parts, and let the end developer focus on manipulating RDF Graphs as [Apache Jena](http://jena.apache.org/) models.

Binaries, sources and documentation for `rdfp-jersey-server` are available for download at [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Crdfp-jersey%22). To use it in your Maven project, add the following dependency declaration to your Maven project file ( `*.pom` file):
 
```xml
<dependency>
    <groupId>com.github.thesmartenergy</groupId>
    <artifactId>rdfp-jersey-server</artifactId>
    <version>${rdfp-jersey.version}</version>
</dependency>
```

# Contact

maxime.lefrancois.86@gmail.com

http://maxime-lefrancois.info/

