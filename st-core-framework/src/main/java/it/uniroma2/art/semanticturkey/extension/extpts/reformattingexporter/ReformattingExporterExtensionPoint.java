package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * The {@link ReformattingExporter} extension point.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ReformattingExporterExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return ReformattingExporter.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
