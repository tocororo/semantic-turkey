package it.uniroma2.art.semanticturkey.extension.extpts.search;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class SearchStrategyExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return SearchStrategy.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}


}
