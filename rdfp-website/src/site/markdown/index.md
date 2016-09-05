# RDF Presentation and RDF Presentation Negociation

An RDF graph can be presented in several ways, using different media types. Examples of RDF media types include `application/rdf+xml`, `text/turtle`, `application/json+ld`.

Today, most of the content consumed/produced/published, on the Web is not presented in RDF. 

In the Web of Things, HTTP servers and clients would rather exchange lightweight documents, potentially binary. 
Currently, most existing RDF Presentations generically apply to any RDF graph, at the cost of being heavy text-based documents.
Yet, lightweight HTTP servers/clients could be better satisfied with consuming/producing/publishing lightweight documents, may its structure be application-specific.

On the other hand, various formalisms have been developed:

- to lift documents to RDF. Examples include [RML mappings](http://rml.io), [XSPARQL](http://xsparql.deri.org/), [SPARQL-Generate](http://w3id.org/sparql-generate/);
- to lower RDF to documents. Examples include [XSPARQL](http://xsparql.deri.org/), [STTL, aka SPARQL-Template](https://ns.inria.fr/sparql-template/);
- to validate RDF graphs. Examples include simple [SPIN](http://spinrdf.org/), [ShEx](http://shexspec.github.io), [SHACL](https://www.w3.org/TR/shacl/). 

For a given range of RDF graphs and a specific media types, an RDF Presentation is a combination of lifting, lowering, and validation rules. With these rules, one can coherently interpret a representation as RDF (lift), validate the RDF graph, and generate back the representation from the RDF graph (lower).

While sending any kind of document, potentially lightweight, an HTTP server/client may refer to the specific RDF Presentation that is used. Then, the HTTP client/server can lift the document to RDF, and validate it.

Similarly, while requesting for an RDF graph, an HTTP server/client may inform the client/server what representation it prefers. Then, the client/server can validate the RDF graph, then lower it into a document.

## RDF Presentation description 

Following the Linked Data principles, RDF Presentations are given uniform identifiers (URIs), and an RDF description of these presentations can be retrieved at their URI.

The RDFP vocabulary can be used to describe RDF Presentations and the range of RDF graphs they apply to. It can be accessed at this URI: https://w3id.org/rdfp/ using content negociation (i.e., set HTTP Header field `Accept` to one of the RDF syntaxes media types, or access it directly in [turtle](index.ttl), or in [RDF/XML](index.rdf).

For example, the RDF Presentation identified by https://w3id.org/rdfp/example/graph/xml is described at its URI by:

```
@prefix rdfp: <https://w3id.org/rdfp/>.
@base <https://w3id.org/rdfp/>.

<example/graph/xml> a rdfp:Presentation ;
  rdfp:mediaType "application/xml" ; 
  rdfp:liftingRule <example/graph/xml/liftingRule> ;
  rdfp:loweringRule <example/graph/xml/loweringRule> ;
  rdfs:isDefinedBy <example/description> .
```

A full example RDF graph that uses this vocabulary can be found at URI https://w3id.org/rdfp/example/description. Use content negociation, or access it directly in [turtle](https://w3id.org/rdfp/example/description.ttl), or in [RDF/XML](https://w3id.org/rdfp/example/description.rdf).


## Refering to an RDF Presentation

The RDF Presentation qualifies the representation type. Following the general architecture principles defined in [W3C Architecture of the World Wide Web, Volume One](https://www.w3.org/TR/2004/REC-webarch-20041215/), we keep orthogonal the identification and representation concepts. Arguably, the representation type (the media type) should be annotated with a link to the RDF Presentation used. 

Although new media types could have a parameter that refers to its presentation, such as: `application/seas;p="https://w3id.org/rdfp/example/graph/xml"`. 
This link cannot be set by a global media type parameter, as per [RFC 2045, Multipurpose Internet Mail Extensions (MIME) Part One: Format of Internet Message Bodies](https://tools.ietf.org/html/rfc2045):

```
There are NO globally-meaningful parameters that apply to all media types.  Truly global mechanisms are best addressed, in the MIME model, by the definition of additional Content-* header fields.
```

We hence introduce HTTP header field `Content-Presentation` for this purpose. The value of this field is any absolute URI, that identifies the RDF Presentation of the represented RDF graph. 

Using this header properly, any existing server can be adapted to behave "as if" it was producing RDF: the client simply needs to dereference the presentation link, and use the associated lifting rule to interpret the retrieved document as RDF.

Equally important, a lightweight client/server can send lightweight binary messages, while still letting its server/client the chance to interpret the message body as RDF.


## RDF Presentation Negociation

RDF Presentation Negociation is a way for the client to state its presentation preferences for the response message body. 

To achieve this, we introduce HTTP header field `Accept-Presentation`.  The value of this field is any (absolute) URI, that identifies the RDF Presentation the clients would like the server to use to encode the response RDF graph. 

Using this header properly, a lightweight client can request a compliant server to encode its responses in a specific format, hence transferring all the computation cost on the server.


## Implementation over Jersey

`rdfp-jersey-server` is an extension of Jersey that eases the development of RESTful services that produce and consume RDF. It hides the RDF Presentation description and RDF Presentation Negociation parts, and let the end developer focus on manipulating RDF graphs as [Apache Jena](http://jena.apache.org/) models.

### Exposition of RDF graphs, vocabularies, ontologies

Currently, the implementation also eases the exposition of RDF documents, and checks that the Semantic Web best practices are respected (Linked Data and/or Linked Vocabularies). These functionnalities will soon be part of a side project.

