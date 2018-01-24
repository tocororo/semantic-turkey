package it.uniroma2.art.semanticturkey.extension.extpts.urigen;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class URIGeneratorExtensionPoint implements ExtensionPoint {

	@Override
	public Scope getScope() {
		return Scope.PROJECT;
	}
	
	
}
