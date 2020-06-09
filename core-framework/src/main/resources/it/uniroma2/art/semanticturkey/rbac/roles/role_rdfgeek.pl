% rdf geeks can perform any action at the level of RDF content, with no restrictions (even SPARQL updates)

capability(rdf,"CRUDV").
capability(sys(metadataRegistry), "R").

capability(invokableReporter(reporter), "CRUD").
capability(invokableReporter(reporter,_), "CRUD").
capability(customService(service), "CRUD").
capability(customService(service,_), "CRUD").