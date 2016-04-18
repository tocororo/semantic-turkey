package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

/**
 * Validators associated with the constraint {@link LocallyDefined}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LocallyDefinedValidator implements
		ConstraintValidator<LocallyDefined, Resource> {
	
	@SuppressWarnings("unused")
	private LocallyDefined annotation;
	
	@Autowired
	private STServiceContext serviceContext;
	
	@Override
	public void initialize(LocallyDefined value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
		if (value == null) {
			return false;
		}

		RepositoryConnection repoConn = serviceContext.getRepositoryConnection();

		return repoConn.hasStatement(value, null, null, true);
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
