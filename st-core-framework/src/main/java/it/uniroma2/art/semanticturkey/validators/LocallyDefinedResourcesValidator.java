package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link LocallyDefinedResources}
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class LocallyDefinedResourcesValidator implements ConstraintValidator<LocallyDefinedResources, 
		Iterable<? extends Resource>> {

	@SuppressWarnings("unused")
	private LocallyDefinedResources annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(LocallyDefinedResources value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Iterable<? extends Resource> value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}

			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
				for(Resource resource : value){
					boolean found = repoConn.hasStatement(resource, null, null, true);
					if(!found){
						return false;
					}
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
