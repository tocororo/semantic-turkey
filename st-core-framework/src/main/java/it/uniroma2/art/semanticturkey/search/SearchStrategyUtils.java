package it.uniroma2.art.semanticturkey.search;

import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.impls.search.GraphDBSearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.impls.search.RegexSearchStrategy;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;

public abstract class SearchStrategyUtils {
	public static SearchStrategy instantiateSearchStrategy(SearchStrategies searchStrategy) {
		switch (searchStrategy) {
		case REGEX:
			return new RegexSearchStrategy();
		case GRAPH_DB:
			return new GraphDBSearchStrategy();
		default:
			throw new IllegalStateException("Unsupported search stategy: " + searchStrategy);
		}

	}
}
