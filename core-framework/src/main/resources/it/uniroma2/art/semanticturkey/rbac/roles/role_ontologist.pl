% ontologists can perform any action on resources, though they are not allowed to some advanced forms of editing (e.g. SPARQL)
% SPARQL is allowed in read-mode only

capability(rdf(resource),"CRUDV").
capability(rdf(resource,_),"CRUDV").

capability(rdf(sparql),"R").
capability(rdf,"R").

capability(sys(metadataRegistry), "R").