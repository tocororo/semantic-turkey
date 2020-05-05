package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class VOIDLIMEDatasetMetadataExporterSettings implements Settings {

	@Override
	public String getShortName() {
		return "VoID/LIME Dataset Metadata Exporter Settings";
	}

	@STProperty(description = "Dataset Description base URI")
	@Required
	public String dataset_description_baseUri;

	@STProperty(description = "Dataset Local Name")
	@Required
	public String dataset_localName;

	// --- General dataset metadata --- //
	
	@STProperty(description = "The name of the dataset")
	@Required
	public String dataset_title;
	
	@STProperty(description = "A textual description of the dataset")
	@Required
	public String dataset_description;

	@STProperty(description = "Dataset Home Page")
	public String dataset_homePage;

	@STProperty(description = "An entity, such as a person, organisation, or service, that is primarily responsible for creating the dataset (comma-separated IRIs)")
	public String dataset_creators;

	@STProperty(description = "An entity, such as a person, organisation, or service, that is responsible for making the dataset available (it should be an IRI)")
	public String dataset_publisher;

	@STProperty(description = "An entity, such as a person, organisation, or service, that is responsible for making contributions to the dataset (comma-separated IRIs)")
	public String dataset_contributors;

	@STProperty(description = "A related resource from which the dataset is derived (comma-separated IRIs)")
	public String dataset_source;

	@STProperty(description = "A point or period of time associated with an event in the life-cycle of the resource. The value should be formatted and data-typed as an xsd:date.")
	public String dataset_date;
	
	@STProperty(description = "Date of creation of the dataset. The value should be formatted and data-typed as an xsd:date.")
	public String dataset_created;

	@STProperty(description = "Date of formal issuance (e.g., publication) of the dataset. The value should be formatted and datatyped as an xsd:date.")
	public String dataset_issued;

	@STProperty(description = "Date on which the dataset was changed. The value should be formatted and datatyped as an xsd:date.")
	public String dataset_modified;

	@STProperty(description = "Dataset License (it should be an IRI)")
	public String dataset_license;

	@STProperty(description = "Dataset Subjects (comma-separated IRIs)")
	public String dataset_subjects;
	
	@STProperty(description = "A technical features of a dataset (comma-separated IRIs)")
	public String dataset_features;

	// --- Access metadata --- //

	@STProperty(description = "Dataset Data Dump (comma-separated IRIs)")
	public String dataset_dataDumps;

	@STProperty(description = "Dataset SPARQL Endpoint (it should be an IRI)")
	public String dataset_sparqlEndpoint;

	@STProperty(description = "A protocol endpoint for simple URI lookups for a void:Dataset (it should be an IRI)")
	public String dataset_uriLookupEndpoint;
	
	@STProperty(description = "An OpenSearch description document for a free-text search service over a void:Dataset (it should be an IRI)")
	public String dataset_openSearchDescription;

	// --- Structural metadata --- //
	@STProperty(description = "A URI that is a common string prefix of all the entity URIs in a void:Datset")
	public String dataset_uriSpace;

	@STProperty(description = "An example entity that is representative for the entities described in a void:Dataset (comma-separated IRIs)")
	public String dataset_exampleResources;

	@STProperty(description = "A top concept or entry point for a void:Dataset that is structured in a tree-like fashion. (comma-separated IRIs)")
	public String dataset_rootResources;
}
