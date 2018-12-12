package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class MetadataRepositoryConnectorExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return MetadataRepositoryConnector.class;
	}

	@Override
	public Scope getScope() {
		return Scope.SYSTEM;
	}

}
