package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.HasType;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link HasType}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HasTypeValidator implements ConstraintValidator<HasType, Resource> {

	@Autowired
	private STServiceContext serviceContext;

	private IRI requiredType;
	private boolean allowSubClasses;

	@Override
	public void initialize(HasType value) {
		this.requiredType = SimpleValueFactory.getInstance().createIRI(value.value());
		this.allowSubClasses = value.allowSubClasses();
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		try (RepositoryConnection repoConn = RDF4JRepositoryUtils
				.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
			BooleanQuery query;
			if (allowSubClasses) {
				query = repoConn.prepareBooleanQuery(
						"ASK { ?t <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?type . ?resource a ?t .}");
			} else {
				query = repoConn.prepareBooleanQuery("ASK { ?resource a ?type}");
			}
			query.setBinding("resource", value);
			query.setBinding("type", requiredType);
			return query.evaluate();
		}
	}
}
