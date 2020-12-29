package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link XNoteDereificationRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationRDFTransformerFactory implements
		ConfigurableExtensionFactory<XNoteDereificationRDFTransformer, XNoteDereificationRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<XNoteDereificationRDFTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification.XNoteDereificationRDFTransformerFactory";
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
	public XNoteDereificationRDFTransformer createInstance(
			XNoteDereificationRDFTransformerConfiguration conf) {
		return new XNoteDereificationRDFTransformer(conf);
	}

	@Override
	public Collection<XNoteDereificationRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new XNoteDereificationRDFTransformerConfiguration());
	}

}
