capability(rdf,"CRUDV").

capability(rbac,"CRUD").

% role e (role, capability) can only be given if project-local roles can be defined

capability(pm(project),"RUV").
capability(pm(project,_),"CRUD").

capability(pm(resourceMetadata,_),"CRUD").

capability(cform,"CRUD").

capability(um(user),"R").

capability(sys(metadataRegistry), "R").

capability(invokableReporter(reporter), "CRUD").
capability(invokableReporter(reporter,_), "CRUD").
capability(customService(service), "CRUD").
capability(customService(service,_), "CRUD").