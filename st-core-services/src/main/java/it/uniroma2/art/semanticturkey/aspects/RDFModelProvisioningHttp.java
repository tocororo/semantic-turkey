package it.uniroma2.art.semanticturkey.aspects;

import it.uniroma2.art.semanticturkey.project.ProjectManager;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RDFModelProvisioningHttp {

	@Autowired
	public RDFModelProvisioning ref;
	
	@Around("it.uniroma2.art.semanticturkey.aspects.RDFModelProvisioning.getOWLModel()")
	public Object getOWLModel(ProceedingJoinPoint pjp) throws Throwable {
		return ProjectManager.getCurrentProject().getOWLModel();
	}
}
