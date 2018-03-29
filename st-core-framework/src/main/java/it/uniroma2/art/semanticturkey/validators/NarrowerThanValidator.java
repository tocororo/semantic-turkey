package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.NarrowerThan;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link SubClassOf}
 * 
 * @author Tiziano Lorenzetti
 * @authro Manuel Fiorelli
 */
public class NarrowerThanValidator implements ConstraintValidator<NarrowerThan, Resource> {

	private NarrowerThan annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(NarrowerThan value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}
			
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			IRI broaderConceptIRI = vf.createIRI(this.annotation.broaderConceptIRI());
			
//			if (value.equals(broaderConceptIRI)) { // assume that a concept is narrower of itself
//				return true; //avoid to perform the sparql query
//			}
			
			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
				String query = "ASK { \n"
						+ NTriplesUtil.toNTriplesString(value) + " " 
						+ "(" + NTriplesUtil.toNTriplesString(SKOS.BROADER) + "|^" + NTriplesUtil.toNTriplesString(SKOS.NARROWER) + ")+ "
						+ NTriplesUtil.toNTriplesString(broaderConceptIRI) + " \n"
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
