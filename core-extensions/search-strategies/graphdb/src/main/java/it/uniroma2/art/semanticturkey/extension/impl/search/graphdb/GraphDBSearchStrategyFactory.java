package it.uniroma2.art.semanticturkey.extension.impl.search.graphdb;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link GraphDBSearchStrategy}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphDBSearchStrategyFactory implements NonConfigurableExtensionFactory<GraphDBSearchStrategy> {

	@Override
	public String getName() {
		return "GraphDB Search Strategy";
	}

	@Override
	public String getDescription() {
		return "Uses fulltext capabilities of GraphDB repositories";

	}

	@Override
	public GraphDBSearchStrategy createInstance() {
		return new GraphDBSearchStrategy();
	}
}
