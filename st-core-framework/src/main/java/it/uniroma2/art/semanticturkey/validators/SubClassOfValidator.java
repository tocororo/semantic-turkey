package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Validators associated with the constraint {@link SubClassOf}
 * 
 * @author Tiziano Lorenzetti
 */
public class SubClassOfValidator implements ConstraintValidator<SubClassOf, Resource> {

	private SubClassOf annotation;

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public void initialize(SubClassOf value) {
		this.annotation = value;
	}

	@Override
	public boolean isValid(Resource value, ConstraintValidatorContext context) {
		try {
			if (value == null) {
				return true;
			}
			
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			IRI superClass = vf.createIRI(this.annotation.superClassIRI());
			
			if (value.equals(superClass)) { //a class is subClassOf itself
				return true;
			}
			
			try (RepositoryConnection repoConn = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(serviceContext))) {
				String query = "ASK { \n"
						+ NTriplesUtil.toNTriplesString(value) + " " 
						+ NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + " "
						+ NTriplesUtil.toNTriplesString(superClass) + " \n"
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
