package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.spreadsheetdeserializer;

import java.util.Arrays;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Factory for the instantiation of {@link SpreadsheetDeserializingLifter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SpreadsheetDeserializingLifterFactory
		implements NonConfigurableExtensionFactory<SpreadsheetDeserializingLifter>, FormatCapabilityProvider {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdflifter.spreadsheetdeserializer.SpreadsheetDeserializingLifterFactory";
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
	public SpreadsheetDeserializingLifter createInstance() {
		return new SpreadsheetDeserializingLifter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return Arrays.asList(new DataFormat("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"));
	}

}
