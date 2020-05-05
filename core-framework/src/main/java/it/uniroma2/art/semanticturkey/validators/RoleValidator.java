package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link LocallyDefined}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RoleValidator implements ConstraintValidator<HasRole, Resource> {

	private HasRole annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(HasRole value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}

			RDFResourceRole requiredRole = annotation.value();

			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
				RDFResourceRole actualRole = RoleRecognitionOrchestrator.computeRole(value, repoConn);

				if (annotation.allowNarrowerRoles()) {
					return RDFResourceRole.subsumes(requiredRole, actualRole);
				} else {
					return actualRole == requiredRole;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
