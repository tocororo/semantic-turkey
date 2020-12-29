package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * Factory for the instantiation of {@link RDFSerializingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFSerializingExporterFactory
		implements ConfigurableExtensionFactory<RDFSerializingExporter, RDFSerializingExporterConfiguration>,
		PUScopedConfigurableComponent<RDFSerializingExporterConfiguration>, FormatCapabilityProvider {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporterFactory";
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
	public List<DataFormat> getFormats() {
		return RDF4JUtilities.getOutputFormats().stream().map(DataFormat::valueOf).collect(toList());
	}

	@Override
	public RDFSerializingExporter createInstance(RDFSerializingExporterConfiguration conf) {
		return new RDFSerializingExporter(conf);
	}

	@Override
	public Collection<RDFSerializingExporterConfiguration> getConfigurations() {
		return Arrays.asList(new RDFSerializingExporterConfiguration());
	}

}
