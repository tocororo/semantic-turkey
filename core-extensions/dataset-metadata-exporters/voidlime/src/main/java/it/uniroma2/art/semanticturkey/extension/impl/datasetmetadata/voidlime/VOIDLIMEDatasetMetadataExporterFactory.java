package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link VOIDLIMEDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VOIDLIMEDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<VOIDLIMEDatasetMetadataExporter>,
		ProjectSettingsManager<VOIDLIMEDatasetMetadataExporterSettings> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime.VOIDLIMEDatasetMetadataExporterFactory";
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
	public VOIDLIMEDatasetMetadataExporter createInstance() {
		return new VOIDLIMEDatasetMetadataExporter(this);
	}
}
