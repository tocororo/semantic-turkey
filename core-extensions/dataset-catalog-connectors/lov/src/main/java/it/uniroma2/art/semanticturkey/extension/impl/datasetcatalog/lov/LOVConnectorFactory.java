package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link LOVConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LOVConnectorFactory
		implements NonConfigurableExtensionFactory<LOVConnector> {

	@Override
	public String getName() {
		return "LOV Connector";
	}

	@Override
	public String getDescription() {
		return "A Metadata Repository connector for LOV (Linked Open Vocabularies)";
	}

	@Override
	public LOVConnector createInstance() {
		return new LOVConnector();
	}

}
