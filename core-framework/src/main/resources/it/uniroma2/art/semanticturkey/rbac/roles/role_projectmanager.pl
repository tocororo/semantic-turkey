capability(rdf,"CRUDV").

capability(rbac,"CRUDV").

% role e (role, capability) can only be given if project-local roles can be defined

capability(pm(project),"RUV").
capability(pm(project,_),"CRUDV").

capability(cform,"CRUDV").

capability(um(user),"R").

capability(sys(metadataRegistry), "R").

capability(invokableReporter(reporter), "CRUD").
capability(invokableReporter(reporter,_), "CRUD").
capability(customService(service), "CRUD").
capability(customService(service,_), "CRUD").