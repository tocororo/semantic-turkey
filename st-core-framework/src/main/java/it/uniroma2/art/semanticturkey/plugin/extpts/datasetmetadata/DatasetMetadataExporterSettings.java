package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class DatasetMetadataExporterSettings extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "Common settings for dataset metadata exporter";
	}

	@STProperty(description = "Dataset Title")
	@Required
	public String dataset_title;
	
	@STProperty(description = "Dataset Description")
	@Required
	public String dataset_description;

}
