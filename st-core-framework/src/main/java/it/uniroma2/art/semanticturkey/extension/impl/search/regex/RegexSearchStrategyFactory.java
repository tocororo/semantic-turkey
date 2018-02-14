package it.uniroma2.art.semanticturkey.extension.impl.search.regex;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link GraphDBSearchStrategy}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RegexSearchStrategyFactory implements NonConfigurableExtensionFactory<RegexSearchStrategy> {

	@Override
	public String getName() {
		return "Regex Search Strategy";
	}

	@Override
	public String getDescription() {
		return "Uses regular expressions over SPARQL to implement search capabilities";
	}

	@Override
	public RegexSearchStrategy createInstance() {
		return new RegexSearchStrategy();
	}
}
