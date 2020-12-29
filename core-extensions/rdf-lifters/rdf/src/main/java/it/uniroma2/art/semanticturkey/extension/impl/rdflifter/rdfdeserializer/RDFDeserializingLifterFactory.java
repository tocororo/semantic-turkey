package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer;

import static java.util.stream.Collectors.toList;

import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * Factory for the instantiation of {@link RDFDeserializingLifter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFDeserializingLifterFactory
		implements NonConfigurableExtensionFactory<RDFDeserializingLifter>, FormatCapabilityProvider {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer.RDFDeserializingLifterFactory";
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
	public RDFDeserializingLifter createInstance() {
		return new RDFDeserializingLifter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return RDF4JUtilities.getInputFormats().stream().map(DataFormat::valueOf).collect(toList());
	}

}
