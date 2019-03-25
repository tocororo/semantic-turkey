package it.uniroma2.art.semanticturkey.sparql;

import java.util.Map;

import com.google.common.collect.BiMap;

import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;

/**
 * The result of applying {@link QueryBuilderProcessor} objects to a {@link TupleQueryShallowModel}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class QueryBuildOutput {
	/**
	 * Enriched query
	 */
	public TupleQueryShallowModel queryModel;

	/**
	 * Variable substitution mapping (from original to mangled variables)
	 */
	public Map<QueryBuilderProcessor, BiMap<String, String>> variableSubstitutionMapping;
}
