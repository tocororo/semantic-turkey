package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link EUODPConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class EUODPConnectorFactory
		implements NonConfigurableExtensionFactory<EUODPConnector> {

	@Override
	public String getName() {
		return "European Union Open Data Portal";
	}

	@Override
	public String getDescription() {
		return "A connector for the European Union Open Data Portal (EU ODP)";
	}

	@Override
	public EUODPConnector createInstance() {
		return new EUODPConnector();
	}

}
