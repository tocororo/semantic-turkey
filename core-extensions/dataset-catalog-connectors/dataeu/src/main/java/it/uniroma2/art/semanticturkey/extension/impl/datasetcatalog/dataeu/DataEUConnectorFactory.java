package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link DataEUConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DataEUConnectorFactory
		implements NonConfigurableExtensionFactory<DataEUConnector> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.DataEUConnectorFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	@Override
	public String getName() {
		return STMessageSource.getMessage(MessageKeys.name);
	}

	@Override
	public String getDescription() {
		return STMessageSource.getMessage(MessageKeys.description);
	}

	@Override
	public DataEUConnector createInstance() {
		return new DataEUConnector();
	}

}
