package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcat;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link DCATDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DCATDatasetMetadataExporterFactory
		implements NonConfigurableExtensionFactory<DCATDatasetMetadataExporter>, ProjectSettingsManager<DCATDatasetMetadataExporterSettings> {

	@Override
	public String getName() {
		return "DCAT Dataset Metadata Exporter";
	}

	@Override
	public String getDescription() {
		return "Exports dataset metadata using the DCAT-AP vocabulary";
	}

	@Override
	public DCATDatasetMetadataExporter createInstance() {
		return new DCATDatasetMetadataExporter(this);
	}
}
