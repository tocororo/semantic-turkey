package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.showvoc;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link ShowVocConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ShowVocConnectorFactory
		implements ConfigurableExtensionFactory<ShowVocConnector, ShowVocConnectorConfiguration>,
		PUScopedConfigurableComponent<ShowVocConnectorConfiguration> {

	@Autowired
	private ExtensionPointManager exptManager;

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.showvoc.ShowVocConnectorFactory";
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
	public ShowVocConnector createInstance(ShowVocConnectorConfiguration conf) {
		return new ShowVocConnector(conf, exptManager);
	}

	@Override
	public Collection<ShowVocConnectorConfiguration> getConfigurations() {
		return Arrays.asList(new ShowVocConnectorConfiguration());
	}

}
