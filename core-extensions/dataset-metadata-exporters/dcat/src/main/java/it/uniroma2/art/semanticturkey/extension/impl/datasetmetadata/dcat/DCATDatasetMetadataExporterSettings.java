package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcat;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;


/**
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 *
 */

public class DCATDatasetMetadataExporterSettings implements Settings {

	@Override
	public String getShortName() {
		return "DCAT Dataset Metadata Exporter Settings";
	}

	//dcat:Dataset
	
	@STProperty(description = "Dataset IRI")
	@Required
	public String dataset_iri;
	
	@STProperty(description = "Dataset Title: A name given to the dataset.")
	@Required
	public String dataset_title;
	
	@STProperty(description = "Dataset Description: free-text account of the dataset.")
	public String dataset_description;
	
	@STProperty(description = "Dataset Issued: The date of listing the corresponding dataset in the catalog.")
	public String dataset_issued;

	@STProperty(description = "Dataset Modified")
	public String dataset_modified;

	@STProperty(description = "Dataset Identifier: A unique identifier of the dataset.")
	public String dataset_identifier;

	@STProperty(description = "Dataset keywords (comma separated): A keyword or tag describing the dataset.")
	public String dataset_keywords;

	@STProperty(description = "Dataset Language: The language of the dataset.")
	public String dataset_language;

	@STProperty(description = "Dataset Contact Point: 	Link a dataset to relevant contact information which is provided using VCard.")
	public String dataset_contactPoint;

	@STProperty(description = "Dataset Temporal: The temporal period that the dataset covers.")
	public String dataset_temporal;

	@STProperty(description = "Dataset Temporal - Start Date")
	public String dataset_temporal_startDate;

	@STProperty(description = "Dataset Temporal - End Date")
	public String dataset_temporal_endDate;

	
	@STProperty(description = "Dataset Spatial: Spatial coverage of the dataset.")
	public String dataset_spatial;

	@STProperty(description = "Dataset Accrual Periodicy: The frequency at which dataset is published.")
	public String dataset_accrualPeriodicy;

	@STProperty(description = "Dataset Landing Page")
	public String dataset_landingPage;

	@STProperty(description = "Dataset Theme(comma separated): The main category of the dataset. A dataset can have multiple themes.")
	public String dataset_theme;
	
	@STProperty(description = "Dataset Distribution: Connects a dataset to its available distributions.")
	@Required
	public String dataset_distribution;
	
	//dcat:Descrtiption
	
	/*@STProperty(description = "Distrubtion IRI")
	@Required
	public String distribution_iri;*/
	
	@STProperty(description = "Distribution Title: A name given to the distribution.")
	@Required
	public String distribution_title;
	
	@STProperty(description = "Distribution Description: free-text account of the distribution.")
	public String distribution_description;
	
	@STProperty(description = "Distribution Issued: Date of formal issuance (e.g., publication) of the distribution.")
	public String distribution_issued;
	
	@STProperty(description = "Distribution Modified: Most recent date on which the distribution was changed, updated or modified.")
	public String distribution_modified;
	
	@STProperty(description = "Distribution Licence: This links to the license document under which the distribution is made available.")
	public String distribution_licence;
	
	@STProperty(description = "Distribution Licence - Title")
	public String distribution_licence_title;

	@STProperty(description = "Distribution Licence - Description")
	public String distribution_licence_description;

	@STProperty(description = "Distribution Licence - type")
	public String distribution_licence_type;
	
	@STProperty(description = "Distribution Rights: Information about rights held in and over the distribution.")
	public String distribution_rights;
	
	@STProperty(description = "Distribution AccessURL: A landing page, feed, SPARQL endpoint or other type of resource that gives access to the distribution of the dataset")
	public String distribution_accessUrl;
	
	@STProperty(description = "Distribution DownloadURL: A file that contains the distribution of the dataset in a given format")
	public String distribution_downloadUrl;
	
	@STProperty(description = "Distribution MediaType: The media type of the distribution as defined by IANA.")
	public String distribution_mediaType;
	
	@STProperty(description = "Distribution Format: The file format of the distribution.")
	public String distribution_format;

	@STProperty(description = "Distribution Byte Size: The size of a distribution in bytes.")
	public String distribution_byteSize;
	
	
}
