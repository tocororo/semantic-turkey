package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.adms;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link ADMSDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ADMSDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<ADMSDatasetMetadataExporter>,
		ProjectSettingsManager<ADMSDatasetMetadataExporterSettings> {

	@Override
	public String getName() {
		return "ADMS Dataset Metadata Exporter";
	}

	@Override
	public String getDescription() {
		return "Exports dataset metadata using the ADMS vocabulary";
	}

	@Override
	public ADMSDatasetMetadataExporter createInstance() {
		return new ADMSDatasetMetadataExporter(this);
	}

}
