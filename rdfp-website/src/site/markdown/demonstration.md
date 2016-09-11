# Demonstration

Resource https://w3id.org/rdfp/example on this website demonstrates RDF Presentation Negotiation, and the use of the `rdfp-jersey-server` implementation.

The resource exposes and consumes RDF Graphs that are described by https://w3id.org/rdfp/example/graph (negotiate its representation with the server, or directly access the [turtle](https://w3id.org/rdfp/example/description.ttl), or a [RDF/XML](https://w3id.org/rdfp/example/description.rdf) document).

This graph description explicitly mentions two presentations (in addition to the well known turtle and RDF/XML presentations:

- one for media type `application/xml`;
- one for media type `application/json`.

Also, two other RDF Presentations can be played with:

- another for media type `application/xml`;
- one for media type `text/plain`.


## Testing POST operation


In order to test the POST operation, examples of inputs can be found at URL https://w3id.org/rdfp/example/input. Negotiate its representation with the server, or directly access:
-  https://w3id.org/rdfp/example/input.txt
-  https://w3id.org/rdfp/example/input.json
-  https://w3id.org/rdfp/example/input.xml
-  https://w3id.org/rdfp/example/input.ttl


Examples of the requests you can test include:

```
POST /rdfp/example
Content-Type: text/turtle

-- the content of https://w3id.org/rdfp/example/input.ttl in the body -- 


POST /rdfp/example
Content-Type: application/xml

-- the content of https://w3id.org/rdfp/example/input.xml in the body -- 


POST /rdfp/example
Content-Type: application/json

-- the content of https://w3id.org/rdfp/example/input.json in the body -- 


POST /rdfp/example
Content-Type: text/plain
Content-Presentation: https://w3id.org/rdfp/presentation3

-- the content of https://w3id.org/rdfp/example/input.txt in the body -- 
```

Examples of requests that trigger errors include:

```
POST /rdfp/example
Content-Type: foo/bar

-- any content in the body  -- 


POST/rdfp/example
Accept: text/plain
Accept-Presentation: https://w3id.org/rdfp/presentation2

-- any content in the body -- 
```


## Testing GET operation

Examples of the requests you can test include:

```
GET /rdfp/example
Accept: text/turtle

GET /rdfp/example
Accept: application/rdf+xml

GET /rdfp/example
Accept: application/json

GET /rdfp/example
Accept: application/xml

GET /rdfp/example
Accept: application/xml
Accept-Presentation: https://w3id.org/rdfp/presentation2
```

Examples of requests that trigger errors include:

```
GET /rdfp/example
Accept: foo/bar

GET /rdfp/example
Accept: text/plain
Accept-Presentation: https://w3id.org/rdfp/presentation2

GET /rdfp/example
Accept: text/plain
Accept-Presentation: https://w3id.org/rdfp/presentation3
```