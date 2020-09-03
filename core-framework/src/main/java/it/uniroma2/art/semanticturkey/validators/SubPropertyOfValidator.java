package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link SubPropertyOf}
 * 
 * @author Tiziano Lorenzetti
 */
public class SubPropertyOfValidator implements ConstraintValidator<SubPropertyOf, Resource> {

	private SubPropertyOf annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(SubPropertyOf value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}
			
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			IRI superProp = vf.createIRI(this.annotation.superPropertyIRI());
			
			if (value.equals(superProp)) { //a prop is subPropOf itself
				return true; //avoid to perform the sparql query
			}
			
			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
				String query = "ASK { \n"
						+ NTriplesUtil.toNTriplesString(value) + " "
						+ NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* "
						+ NTriplesUtil.toNTriplesString(superProp) + " \n"
						+ " }";
				BooleanQuery bq = repoConn.prepareBooleanQuery(query);
				return bq.evaluate();
			}
				
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
