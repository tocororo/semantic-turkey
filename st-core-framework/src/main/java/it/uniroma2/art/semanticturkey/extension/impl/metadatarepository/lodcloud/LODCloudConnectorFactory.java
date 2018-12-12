package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lodcloud;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link LODCloudConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LODCloudConnectorFactory
		implements NonConfigurableExtensionFactory<LODCloudConnector> {

	@Override
	public String getName() {
		return "LOD Cloud Connector";
	}

	@Override
	public String getDescription() {
		return "A Metadata Repository connector for Linked Open Data Cloud";
	}

	@Override
	public LODCloudConnector createInstance() {
		return new LODCloudConnector();
	}

}
