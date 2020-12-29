package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class VOIDLIMEDatasetMetadataExporterSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime.VOIDLIMEDatasetMetadataExporterSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String dataset_description_baseUri$description = keyBase + ".dataset_description_baseUri.description";
		public static final String dataset_description_baseUri$displayName = keyBase + ".dataset_description_baseUri.displayName";
		public static final String dataset_localName$description = keyBase + ".dataset_localName.description";
		public static final String dataset_localName$displayName = keyBase + ".dataset_localName.displayName";
		public static final String dataset_title$description = keyBase + ".dataset_title.description";
		public static final String dataset_title$displayName = keyBase + ".dataset_title.displayName";
		public static final String dataset_description$description = keyBase + ".dataset_description.description";
		public static final String dataset_description$displayName = keyBase + ".dataset_description.displayName";
		public static final String dataset_homePage$description = keyBase + ".dataset_homePage.description";
		public static final String dataset_homePage$displayName = keyBase + ".dataset_homePage.displayName";
		public static final String dataset_creators$description = keyBase + ".dataset_creators.description";
		public static final String dataset_creators$displayName = keyBase + ".dataset_creators.displayName";
		public static final String dataset_publisher$description = keyBase + ".dataset_publisher.description";
		public static final String dataset_publisher$displayName = keyBase + ".dataset_publisher.displayName";
		public static final String dataset_contributors$description = keyBase + ".dataset_contributors.description";
		public static final String dataset_contributors$displayName = keyBase + ".dataset_contributors.displayName";
		public static final String dataset_source$description = keyBase + ".dataset_source.description";
		public static final String dataset_source$displayName = keyBase + ".dataset_source.displayName";
		public static final String dataset_date$description = keyBase + ".dataset_date.description";
		public static final String dataset_date$displayName = keyBase + ".dataset_date.displayName";
		public static final String dataset_created$description = keyBase + ".dataset_created.description";
		public static final String dataset_created$displayName = keyBase + ".dataset_created.displayName";
		public static final String dataset_issued$description = keyBase + ".dataset_issued.description";
		public static final String dataset_issued$displayName = keyBase + ".dataset_issued.displayName";
		public static final String dataset_modified$description = keyBase + ".dataset_modified.description";
		public static final String dataset_modified$displayName = keyBase + ".dataset_modified.displayName";
		public static final String dataset_license$description = keyBase + ".dataset_license.description";
		public static final String dataset_license$displayName = keyBase + ".dataset_license.displayName";
		public static final String dataset_subjects$description = keyBase + ".dataset_subjects.description";
		public static final String dataset_subjects$displayName = keyBase + ".dataset_subjects.displayName";
		public static final String dataset_features$description = keyBase + ".dataset_features.description";
		public static final String dataset_features$displayName = keyBase + ".dataset_features.displayName";
		public static final String dataset_dataDumps$description = keyBase + ".dataset_dataDumps.description";
		public static final String dataset_dataDumps$displayName = keyBase + ".dataset_dataDumps.displayName";
		public static final String dataset_sparqlEndpoint$description = keyBase + ".dataset_sparqlEndpoint.description";
		public static final String dataset_sparqlEndpoint$displayName = keyBase + ".dataset_sparqlEndpoint.displayName";
		public static final String dataset_uriLookupEndpoint$description = keyBase + ".dataset_uriLookupEndpoint.description";
		public static final String dataset_uriLookupEndpoint$displayName = keyBase + ".dataset_uriLookupEndpoint.displayName";
		public static final String dataset_openSearchDescription$description = keyBase + ".dataset_openSearchDescription.description";
		public static final String dataset_openSearchDescription$displayName = keyBase + ".dataset_openSearchDescription.displayName";
		public static final String dataset_uriSpace$description = keyBase + ".dataset_uriSpace.description";
		public static final String dataset_uriSpace$displayName = keyBase + ".dataset_uriSpace.displayName";
		public static final String dataset_exampleResources$description = keyBase + ".dataset_exampleResources.description";
		public static final String dataset_exampleResources$displayName = keyBase + ".dataset_exampleResources.displayName";
		public static final String dataset_rootResources$description = keyBase + ".dataset_rootResources.description";
		public static final String dataset_rootResources$displayName = keyBase + ".dataset_rootResources.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.dataset_description_baseUri$description + "}", displayName = "{" + MessageKeys.dataset_description_baseUri$displayName + "}")
	@Required
	public String dataset_description_baseUri;

	@STProperty(description = "{" + MessageKeys.dataset_localName$description + "}", displayName = "{" + MessageKeys.dataset_localName$displayName + "}")
	@Required
	public String dataset_localName;

	// --- General dataset metadata --- //
	
	@STProperty(description = "{" + MessageKeys.dataset_title$description + "}", displayName = "{" + MessageKeys.dataset_title$displayName + "}")
	@Required
	public String dataset_title;
	
	@STProperty(description = "{" + MessageKeys.dataset_description$description + "}", displayName = "{" + MessageKeys.dataset_description$displayName + "}")
	@Required
	public String dataset_description;

	@STProperty(description = "{" + MessageKeys.dataset_homePage$description + "}", displayName = "{" + MessageKeys.dataset_homePage$displayName + "}")
	public String dataset_homePage;

	@STProperty(description = "{" + MessageKeys.dataset_creators$description + "}", displayName = "{" + MessageKeys.dataset_creators$displayName + "}")
	public String dataset_creators;

	@STProperty(description = "{" + MessageKeys.dataset_publisher$description + "}", displayName = "{" + MessageKeys.dataset_publisher$displayName + "}")
	public String dataset_publisher;

	@STProperty(description = "{" + MessageKeys.dataset_contributors$description + "}", displayName = "{" + MessageKeys.dataset_contributors$displayName + "}")
	public String dataset_contributors;

	@STProperty(description = "{" + MessageKeys.dataset_source$description + "}", displayName = "{" + MessageKeys.dataset_source$displayName + "}")
	public String dataset_source;

	@STProperty(description = "{" + MessageKeys.dataset_date$description + "}", displayName = "{" + MessageKeys.dataset_date$displayName + "}")
	public String dataset_date;
	
	@STProperty(description = "{" + MessageKeys.dataset_created$description + "}", displayName = "{" + MessageKeys.dataset_created$displayName + "}")
	public String dataset_created;

	@STProperty(description = "{" + MessageKeys.dataset_issued$description + "}", displayName = "{" + MessageKeys.dataset_issued$displayName + "}")
	public String dataset_issued;

	@STProperty(description = "{" + MessageKeys.dataset_modified$description + "}", displayName = "{" + MessageKeys.dataset_modified$displayName + "}")
	public String dataset_modified;

	@STProperty(description = "{" + MessageKeys.dataset_license$description + "}", displayName = "{" + MessageKeys.dataset_license$displayName + "}")
	public String dataset_license;

	@STProperty(description = "{" + MessageKeys.dataset_subjects$description + "}", displayName = "{" + MessageKeys.dataset_subjects$displayName + "}")
	public String dataset_subjects;
	
	@STProperty(description = "{" + MessageKeys.dataset_features$description + "}", displayName = "{" + MessageKeys.dataset_features$displayName + "}")
	public String dataset_features;

	// --- Access metadata --- //

	@STProperty(description = "{" + MessageKeys.dataset_dataDumps$description + "}", displayName = "{" + MessageKeys.dataset_dataDumps$displayName + "}")
	public String dataset_dataDumps;

	@STProperty(description = "{" + MessageKeys.dataset_sparqlEndpoint$description + "}", displayName = "{" + MessageKeys.dataset_sparqlEndpoint$displayName + "}")
	public String dataset_sparqlEndpoint;

	@STProperty(description = "{" + MessageKeys.dataset_uriLookupEndpoint$description + "}", displayName = "{" + MessageKeys.dataset_uriLookupEndpoint$displayName + "}")
	public String dataset_uriLookupEndpoint;
	
	@STProperty(description = "{" + MessageKeys.dataset_openSearchDescription$description + "}", displayName = "{" + MessageKeys.dataset_openSearchDescription$displayName + "}")
	public String dataset_openSearchDescription;

	// --- Structural metadata --- //
	@STProperty(description = "{" + MessageKeys.dataset_uriSpace$description + "}", displayName = "{" + MessageKeys.dataset_uriSpace$displayName + "}")
	public String dataset_uriSpace;

	@STProperty(description = "{" + MessageKeys.dataset_exampleResources$description + "}", displayName = "{" + MessageKeys.dataset_exampleResources$displayName + "}")
	public String dataset_exampleResources;

	@STProperty(description = "{" + MessageKeys.dataset_rootResources$description + "}", displayName = "{" + MessageKeys.dataset_rootResources$displayName + "}")
	public String dataset_rootResources;
}
