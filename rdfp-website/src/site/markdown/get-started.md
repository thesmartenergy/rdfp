# Get Started

`rdfp-jersey-server` is an extension of Jersey that eases the development of RESTful services that produce and consume RDF. It hides the RDF Presentation description and RDF Presentation Negotiation parts, and lets the end developer focus on manipulating RDF Graphs as [Apache Jena](http://jena.apache.org/) models.

With `rdfp-jersey-server`, it become really easy to develop Jersey applications that use RDF Graph Presentation Negotiation over Jersey. 

`rdfp-jersey-server` uses Jena to represent RDF. The current version implements RDF lifting with [SPARQL-Generate](https://w3id.org/sparql-generate/), and RDF lowering with [STTP, aka SPARQL-Template](https://ns.inria.fr/sparql-template/).

RDF Graph validation using simple SPARQL-ASK queries is planed in the near future.


First, the Jersey Application needs to load utility classes in package `com.github.thesmartenergy.rdfp.jersey`:

```java
@ApplicationPath("example")
public class JerseyApp extends ResourceConfig {

    public JerseyApp() {
        packages("com.github.thesmartenergy.rdfp.jersey");
        ...
    }

    ...
} 
```

## Consuming RDF Presented Anyhow

`rdfp-jersey-server` takes care of using the RDF Presentation description to interpret the HTTP request as a RDF Graph when possible. This makes use of HTTP header fields `Content-Type` and `Content-Presentation`.

All the machinery is hidden to the end developer, that can directly use a parameter of type `Model` in any Jersey resource method:

```java
@POST
public Response doPost(@GraphDescription("https://w3id.org/rdfp/example/graph") Model model) {
    ...
}
```

The `@GraphDescription` annotation tells `rdfp-jersey-server` the graph description that the input RDF graph conforms to. If the client does not explicitly set parameter `Content-Presentation`, then `rdfp-jersey-server` dereferences this URI to find an applicable presentation to lift the input document to RDF.


## Negotiating RDF Presentation

`rdfp-jersey-server` takes care of using the RDF Presentation description to negotiate a RDF Presentation of the returned RDF Graph when possible, and to send this representation in the HTTP response. This makes use of the HTTP header fields `Accept`and `Accept-Presentation`.

All the machinery is hidden to the end developer, that can make the Jersey resource method directly return an entity of type `Model`:

```java
@GET
@GraphDescription("https://w3id.org/rdfp/example/graph")
public Model doGet() {
    Model model = ...
    return model;
}
```

The `@GraphDescription` annotation tells `rdfp-jersey-server` the graph description that the output RDF graph conforms to. If the client does not request a specific RDF presentation using parameter `Accept-Presentation`, then `rdfp-jersey-server` dereferences this URI to find an applicable presentation to lower the output RDF graph to a document.


## Exposition of RDF graphs, vocabularies, ontologies

Currently, the implementation also eases the exposition of RDF documents, and checks that the Semantic Web best practices are respected (Linked Data and/or Linked Vocabularies). These functionnalities will soon be part of a side project.


## Maven project

Binaries, sources and documentation for `rdfp-jersey-server` are available for download at [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Crdfp-jersey%22). To use it in your Maven project, add the following dependency declaration to your Maven project file ( `*.pom` file):
 
```xml
<dependency>
    <groupId>com.github.thesmartenergy</groupId>
    <artifactId>rdfp-jersey-server</artifactId>
    <version>${rdfp-jersey.version}</version>
</dependency>
```

The [javadoc](https://w3id.org/rdfp/apidocs/index.html) contains comprehensive documentations and examples, and the [sources of this website](https://github.com/thesmartenergy/rdfp/tree/master/rdfp-website) is a simple web project to get started with `rdfp-jersey-server`. 

