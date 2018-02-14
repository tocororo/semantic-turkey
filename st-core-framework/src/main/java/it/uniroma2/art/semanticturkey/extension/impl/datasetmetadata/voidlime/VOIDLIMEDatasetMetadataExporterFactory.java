package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link VOIDLIMEDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VOIDLIMEDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<VOIDLIMEDatasetMetadataExporter>,
		ProjectSettingsManager<VOIDLIMEDatasetMetadataExporterSettings> {

	@Override
	public String getName() {
		return "VoID/LIME Dataset Metadata Exporter";
	}

	@Override
	public String getDescription() {
		return "Exports dataset metadata using the VoID and LIME vocabularies";
	}

	@Override
	public VOIDLIMEDatasetMetadataExporter createInstance() {
		return new VOIDLIMEDatasetMetadataExporter(this);
	}
}
