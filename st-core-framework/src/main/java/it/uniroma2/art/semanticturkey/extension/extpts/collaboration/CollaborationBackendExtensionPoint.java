package it.uniroma2.art.semanticturkey.extension.extpts.collaboration;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class CollaborationBackendExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return CollaborationBackend.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT;
	}

}
