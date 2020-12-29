package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.zthesdeserializer;

import java.util.Arrays;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Factory for the instantiation of {@link ZthesDeserializingLifter}.
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class ZthesDeserializingLifterFactory
		implements NonConfigurableExtensionFactory<ZthesDeserializingLifter>, FormatCapabilityProvider {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdflifter.zthesdeserializer.ZthesDeserializingLifterFactory";
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
	public ZthesDeserializingLifter createInstance() {
		return new ZthesDeserializingLifter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return Arrays.asList(new DataFormat("XML", "application/xml", "xml"));
	}

}
