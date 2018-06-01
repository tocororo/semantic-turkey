package it.uniroma2.art.semanticturkey.extension.extpts.rdflifter;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * The {@link RDFLifter} extension point.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFLifterExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return RDFLifter.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
