package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link DCATAPDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class DCATAPDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<DCATAPDatasetMetadataExporter>,
		ProjectSettingsManager<DCATAPDatasetMetadataExporterSettings> {

	@Override
	public String getName() {
		return "DCAT-AP Dataset Metadata Exporter";
	}

	@Override
	public String getDescription() {
		return "Exports dataset metadata using the DCAT-AP vocabulary";
	}

	@Override
	public DCATAPDatasetMetadataExporter createInstance() {
		return new DCATAPDatasetMetadataExporter(this);
	}
}
