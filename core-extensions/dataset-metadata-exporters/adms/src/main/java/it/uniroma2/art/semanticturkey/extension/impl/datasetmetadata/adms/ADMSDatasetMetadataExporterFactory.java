package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.adms;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link ADMSDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ADMSDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<ADMSDatasetMetadataExporter>,
		ProjectSettingsManager<ADMSDatasetMetadataExporterSettings> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.adms.ADMSDatasetMetadataExporterFactory";
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
	public ADMSDatasetMetadataExporter createInstance() {
		return new ADMSDatasetMetadataExporter(this);
	}

}
