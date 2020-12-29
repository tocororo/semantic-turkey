package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link DCATAPDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class DCATAPDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<DCATAPDatasetMetadataExporter>,
		ProjectSettingsManager<DCATAPDatasetMetadataExporterSettings> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap.DCATAPDatasetMetadataExporterFactory";
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
	public DCATAPDatasetMetadataExporter createInstance() {
		return new DCATAPDatasetMetadataExporter(this);
	}
}
