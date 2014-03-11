package it.uniroma2.art.semanticturkey.validators;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

public class ExistingValidator implements
		ConstraintValidator<Existing, ARTResource> {
	
	private Existing annotation;
	
	@Autowired
	private STServiceContext serviceContext;
	
	@Override
	public void initialize(Existing value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(ARTResource value, ConstraintValidatorContext context) {
		int projectIndex = annotation.projectIndex();
		Project<?> project = serviceContext.getProject(projectIndex);
		
		
		System.out.println("service context = " + serviceContext + " projectIndex = " + projectIndex + " project = " + project);
		
		ARTResource test;
		try {
			if (value.isBlank()) {
				test = project.getOntModel()
						.retrieveBNode(value.asBNode().getID());
			} else {
				test = project.getOntModel()
						.retrieveURIResource(value.asURIResource().getURI());
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return (test != null);
	}
}
