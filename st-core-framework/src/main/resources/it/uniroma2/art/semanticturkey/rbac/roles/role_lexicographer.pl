capability(rdf(lexicalization), "CRUD").

/* examples of capabilities that should be authorized */

%% any xLabel created
% auth(rdf(xLabel),"C").	

%% xLabel created for a given language			
% auth(rdf(xLabel,"it"),"C").		