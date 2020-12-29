package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.deleteproperty;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link DeletePropertyValueRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueRDFTransformerFactory implements
		ConfigurableExtensionFactory<DeletePropertyValueRDFTransformer, DeletePropertyValueRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<DeletePropertyValueRDFTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.deleteproperty.DeletePropertyValueRDFTransformerFactory";
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
	public DeletePropertyValueRDFTransformer createInstance(
			DeletePropertyValueRDFTransformerConfiguration conf) {
		return new DeletePropertyValueRDFTransformer(conf);
	}

	@Override
	public Collection<DeletePropertyValueRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new DeletePropertyValueRDFTransformerConfiguration());
	}

}
