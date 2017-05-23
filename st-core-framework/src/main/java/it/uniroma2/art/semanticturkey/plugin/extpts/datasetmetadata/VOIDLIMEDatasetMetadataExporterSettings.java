package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class VOIDLIMEDatasetMetadataExporterSettings extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "VOID/LIME Dataset Metadata Exporter Settings";
	}

	@STProperty(description = "Dataset Description base URI")
	@Required
	public String dataset_description_base_uri;
	
	@STProperty(description = "Dataset IRI")
	@Required
	public String dataset_iri;
	
	// TODO: add other properties and their descriptions!!!!
}