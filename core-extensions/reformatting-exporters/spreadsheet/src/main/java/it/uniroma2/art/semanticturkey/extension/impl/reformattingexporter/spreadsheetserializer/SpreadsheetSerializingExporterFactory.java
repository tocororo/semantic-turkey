package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Factory for the instantiation of {@link SpreadsheetSerializingExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SpreadsheetSerializingExporterFactory
		implements ConfigurableExtensionFactory<SpreadsheetSerializingExporter, SpreadsheetSerializingExporterConfiguration>,
		PUScopedConfigurableComponent<SpreadsheetSerializingExporterConfiguration>, FormatCapabilityProvider {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.SpreadsheetSerializingExporterFactory";
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
		return Arrays.asList(new DataFormat("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"));
	}

	@Override
	public SpreadsheetSerializingExporter createInstance(SpreadsheetSerializingExporterConfiguration conf) {
		return new SpreadsheetSerializingExporter(conf);
	}

	@Override
	public Collection<SpreadsheetSerializingExporterConfiguration> getConfigurations() {
		return Arrays.asList(new SpreadsheetSerializingExporterConfiguration());
	}

}
