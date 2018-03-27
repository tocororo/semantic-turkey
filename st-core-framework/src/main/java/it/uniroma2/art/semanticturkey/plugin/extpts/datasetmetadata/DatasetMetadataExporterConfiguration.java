package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;

/**
 * Base configuration class for {@link DatasetMetadataExporter}.
 *
 */
public class DatasetMetadataExporterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "Dataset Metadata Exporter";
	}

}
