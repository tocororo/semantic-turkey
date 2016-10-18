package it.uniroma2.art.semanticturkey.sparql;

import java.util.Map;

import com.google.common.collect.BiMap;

import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;

public class QueryBuildOutput {
	public TupleQueryShallowModel queryModel;
	public Map<QueryBuilderProcessor, BiMap<String, String>> mangled2processorSpecificVariableMapping;
}
