package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ADMSDatasetMetadataExporterSettings extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "ADMS Dataset Metadata Exporter Settings";
	}

	@STProperty(description = "Dataset Title")
	@Required
	public String dataset_title;
	
	@STProperty(description = "Dataset Description")
	@Required
	public String dataset_description;
	
	// TODO: add other properties and their descriptions!!!!
}
