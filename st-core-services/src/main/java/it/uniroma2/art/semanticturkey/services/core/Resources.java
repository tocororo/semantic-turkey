package it.uniroma2.art.semanticturkey.services.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
//import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

@STService
public class Resources extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Resources.class);

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'U')")
	public void updateTriple(@Subject @Modified Resource subject, IRI property, Value value, Value newValue) {
		logger.info("request to update a triple");
		RepositoryConnection repoConnection = getManagedConnection();

		String query = "DELETE DATA {								\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?value .		\n"
				+ "		}									\n"
				+ "};										\n"
				+ "INSERT DATA {							\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?newValue .	\n"
				+ "		}									\n" + "}";
		query = query.replace("?g", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		query = query.replace("?subject", NTriplesUtil.toNTriplesString(subject));
		query = query.replace("?property", NTriplesUtil.toNTriplesString(property));
		query = query.replace("?value", NTriplesUtil.toNTriplesString(value));
		query = query.replace("?newValue", NTriplesUtil.toNTriplesString(newValue));
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'D')")
	public void removeValue(@LocallyDefined @Modified @Subject Resource subject, @LocallyDefined IRI property,
			Value value) {
		getManagedConnection().remove(subject, property, value, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'C')")
	public void addValue(@LocallyDefined @Modified @Subject Resource subject, @LocallyDefined IRI property,
			Value value) {
		getManagedConnection().add(subject, property, value, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ')', 'U')")
	public void setDeprecated(@LocallyDefined @Modified @Subject IRI resource) {
		RepositoryConnection conn = getManagedConnection();
		Literal literalTrue = conn.getValueFactory().createLiteral("true", XMLSchema.BOOLEAN);
		conn.add(resource, OWL2Fragment.DEPRECATED, literalTrue, getWorkingGraph());
	}

	/**
	 * Return the description of a resource, including show and nature
	 * 
	 * @param resource
	 * @return
	 */
	@STServiceOperation
	@Read
	public AnnotatedValue<Resource> getResourceDescription(@LocallyDefined Resource resource) {
		QueryBuilder qb = createQueryBuilder(
			// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>						\n" +
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>							\n" +
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>									\n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>							\n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>							\n" +
			" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {				\n" +                                                                              
			"    ?resource ?p ?o .															\n" +
			generateNatureSPARQLWherePart("?resource") +
			" }																				\n" +
			" GROUP BY ?resource 															\n"
			// @formatter:on
		);
		qb.setBinding("resource", resource);
		qb.processRendering();
		qb.processQName();
		return qb.runQuery().iterator().next();
	}

}
