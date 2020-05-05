package it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class RepositoryImplConfigurerExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return RepositoryImplConfigurer.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT;
	}

}
