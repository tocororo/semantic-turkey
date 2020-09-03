package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collection;

/**
 * This class provides services for manipulating individuals.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Individuals extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(Individuals.class);

	/**
	 * Returns the (explicit) named types of the given individual <code>individual</code>. 
	 * 
	 * @param individual
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#individual)+ ')', 'R')")
	public Collection<AnnotatedValue<Resource>> getNamedTypes(@LocallyDefined Resource individual) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" SELECT ?resource WHERE {                                                     \n" +                                                                              
				" 	?individual a ?resource .                                                  \n" +
				" 	FILTER(isIRI(?resource))                                                   \n" +
				" }                                                                            \n" +
				" GROUP BY ?resource                                                           \n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("individual", individual);
		return qb.runQuery();
	}
	
	/**
	 * Adds a type to an individual
	 * @param individual
	 * @param type
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#individual)+ ')', 'U')")
	public void addType(@LocallyDefined @Modified Resource individual,
			@LocallyDefined Resource type) {
		String query =
				//@formatter:off
				"INSERT DATA {					\n" +
				"	GRAPH %graph% {				\n" +
				"		%individual% a %type% .	\n" + 
				"	}							\n" +
				"}";
				//@formatter:on
		query = query.replace("%graph%", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		query = query.replace("%individual%", NTriplesUtil.toNTriplesString(individual));
		query = query.replace("%type%", NTriplesUtil.toNTriplesString(type));
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Removes a type to an individual
	 * @param individual
	 * @param type
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#individual)+ ')', 'D')")
	public void removeType(@LocallyDefined @Modified Resource individual, Resource type) {
		String query =
				// @formatter:off
				"DELETE WHERE {					\n" +
				"	GRAPH %graph% {				\n" +
				"		%individual% a %type% .	\n" + 
				"	}							\n" +
				"}";
				// @formatter:on
		query = query.replace("%graph%", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		query = query.replace("%individual%", NTriplesUtil.toNTriplesString(individual));
		query = query.replace("%type%", NTriplesUtil.toNTriplesString(type));
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(query);
		update.execute();
	}
	
}