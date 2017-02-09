package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;

/**
 * This class provides services for manipulating individuals.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Individuals extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Individuals.class);

	/**
	 * Returns the (explicit) named types of the given individual <code>individual</code>. 
	 * 
	 * @param superClass
	 * @param numInst
	 * @return
	 */
	@STServiceOperation
	@Read
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
};