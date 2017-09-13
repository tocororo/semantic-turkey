capability(rdf(lexicalization), "CRUD").
capability(rdf, "R").

/* examples of capabilities that should be authorized */

%% any xLabel created
% auth(rdf(xLabel),"C").	

%% xLabel created for a given language			
% auth(rdf(xLabel,"it"),"C").		