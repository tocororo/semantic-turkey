package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link UpdatePropertyValueRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueRDFTranformerFactory implements
		ConfigurableExtensionFactory<UpdatePropertyValueRDFTransformer, UpdatePropertyValueRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<UpdatePropertyValueRDFTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty.UpdatePropertyValueRDFTranformerFactory";
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
	public UpdatePropertyValueRDFTransformer createInstance(
			UpdatePropertyValueRDFTransformerConfiguration conf) {
		return new UpdatePropertyValueRDFTransformer(conf);
	}

	@Override
	public Collection<UpdatePropertyValueRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new UpdatePropertyValueRDFTransformerConfiguration());
	}

}
