package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
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

		String query = "DELETE  {							\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?value .		\n"
				+ "		}									\n"
				+ "}										\n"
				+ "INSERT  {							\n"
				+ "		GRAPH ?g {							\n"
				+ "			?subject ?property ?newValue .	\n"
				+ "		}									\n" 
				+ "}										\n"
				+ "WHERE{									\n"
				+ "BIND(?g_input AS ?g )					\n"
				+ "BIND(?subject_input AS ?subject )		\n"
				+ "BIND(?property_input AS ?property )		\n"
				+ "BIND(?value_input AS ?value )			\n"
				+ "BIND(?newValue_input AS ?newValue )		\n"
				+ "}";
		
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("g_input", getWorkingGraph());
		update.setBinding("subject_input", subject);
		update.setBinding("property_input", property);
		update.setBinding("value_input", value);
		update.setBinding("newValue_input", newValue);
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
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
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

	
	/**
	 * Return the description of a list of resources, including show and nature
	 * 
	 * @param resource
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public Collection <AnnotatedValue<Resource>> getResourcesInfo( IRI[] resources) {
		
		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +                                      
				" prefix owl: <http://www.w3.org/2002/07/owl#>							\n" +                                      
				" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {		\n" +
				"     VALUES(?resource) {");
		sb.append(Arrays.stream(resources).map(iri -> "(" + RenderUtils.toSPARQL(iri) + ")").collect(joining()));
		sb.append("}													 				\n" +
				generateNatureSPARQLWherePart("?resource") +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
		
	}
}
