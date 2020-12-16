package it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;

public class DatasetMetadataExporterSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterSettings";

		public static final String shortName = keyBase + ".shortName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	/*@STProperty(description = "Dataset Title")
	@Required
	public String dataset_title;
	
	@STProperty(description = "Dataset Description")
	@Required
	public String dataset_description;
	*/
}
