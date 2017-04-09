package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link LocallyDefined}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class NotLocallyDefinedValidator implements ConstraintValidator<NotLocallyDefined, Resource> {

	@SuppressWarnings("unused")
	private NotLocallyDefined annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(NotLocallyDefined value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}

			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(serviceContext.getProject().getRepository())) {
				return !repoConn.hasStatement(value, null, null, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
