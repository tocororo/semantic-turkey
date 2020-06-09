capability(rdf(lexicalization), "CRUD").
capability(rdf(notes), "CRUD").
capability(rdf, "R").
capability(sys(metadataRegistry), "R").

capability(invokableReporter(reporter), "R").
capability(customService(service), "R").

/* examples of capabilities that should be authorized */

%% any xLabel created
% auth(rdf(xLabel),"C").	

%% xLabel created for a given language			
% auth(rdf(xLabel,"it"),"C").