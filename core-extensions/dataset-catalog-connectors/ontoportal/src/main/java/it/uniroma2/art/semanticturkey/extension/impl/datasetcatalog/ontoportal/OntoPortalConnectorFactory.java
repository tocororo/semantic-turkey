package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link OntoPortalConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntoPortalConnectorFactory implements ExtensionFactory<OntoPortalConnector>, ConfigurableExtensionFactory<OntoPortalConnector, AbstractOntoPortalConnectorConfiguration>,
		PUScopedConfigurableComponent<AbstractOntoPortalConnectorConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.OntoPortalConnectorFactory";
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
	public OntoPortalConnector createInstance(AbstractOntoPortalConnectorConfiguration conf) {
		return new OntoPortalConnector(conf);
	}

	@Override
	public Collection<AbstractOntoPortalConnectorConfiguration> getConfigurations() {
		return Arrays.asList(new OntoPortalConnectorConfiguration(), new EcoPortalConnectorConfiguration());
	}

}
