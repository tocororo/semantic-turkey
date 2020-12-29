package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link XLabelDereificationRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XLabelDereificationRDFTransformerFactory implements
		ConfigurableExtensionFactory<XLabelDereificationRDFTransformer, XLabelDereificationRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<XLabelDereificationRDFTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification.XLabelDereificationRDFTransformerFactory";
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
	public XLabelDereificationRDFTransformer createInstance(
			XLabelDereificationRDFTransformerConfiguration conf) {
		return new XLabelDereificationRDFTransformer(conf);
	}

	@Override
	public Collection<XLabelDereificationRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new XLabelDereificationRDFTransformerConfiguration());
	}

}
