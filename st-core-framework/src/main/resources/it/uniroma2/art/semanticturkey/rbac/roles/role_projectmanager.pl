capability(rdf,"CRUDV").

capability(rbac,"CRUDV").

% role e (role, capability) can only be given if project-local roles can be defined

capability(pm(project),"RUV").
capability(pm(project,_),"CRUDV").

capability(cform,"CRUDV").