package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;


/**
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 *
 */

public class DCATAPDatasetMetadataExporterSettings implements Settings {

	@Override
	public String getShortName() {
		return "DCATAP Dataset Metadata Exporter Settings";
	}

	
	
	
	// Mandatory Classes
	//@STProperty(description = "Agent IRI")
	//@Required
	//public String agent_iri;
	
	@STProperty(description = "Catalogue IRI")
	@Required
	public String catalogue_iri;
	
	
	@STProperty(description = "Dataset IRI")
	@Required
	public String dataset_iri;
	
	
	//Recommended Classes 
	//@STProperty(description = "Category IRI")
	//public String category_iri;
	
	//@STProperty(description = "Category Scheme IRI")
	//public String categoryScheme_iri;
	
	//@STProperty(description = "Distribution IRI")
	//public String distribution_iri;
	
	//@STProperty(description = "Licence Document IRI")
	//public String licenceDocument_iri;

	
	//TODO check this
	//@STProperty(description = "Resource IRI")
	//@Required
	//public String resource_iri;
		
	
	
	//Optional Classes 
	//NOT IMPLEMENTED AT THE MOMENT
	
	
	//PROPERTIES
	
	//Agent: mandatory
	//@STProperty(description = "Agent name")
	//@Required
	//public String agent_name; 
	
	//Agent: recommended
	//@STProperty(description = "Agent type")
	//public String agent_type; 

	
	//Catalogue: mandatory
	//@STProperty(description = "Catalogue - dataset")
	//@Required
	//public String catalogue_dataset;
	
	@STProperty(description = "Catalogue - description")
	@Required
	public String catalogue_description;
	
	@STProperty(description = "Catalogue - publisher Agent IRI")
	@Required
	public String catalogue_publisherAgentIRI;
	
	@STProperty(description = "Catalogue - publisher Agent Name")
	@Required
	public String catalogue_publisherAgentName;
	
	@STProperty(description = "Catalogue - publisher Agent Type")
	public String catalogue_publisherAgentType;
	
	
	@STProperty(description = "Catalogue - title")
	@Required
	public String catalogue_title;
	
	//Catalogue: recomended
	@STProperty(description = "Catalogue - homepage")
	public String catalogue_homepage;
	
	@STProperty(description = "Catalogue - language")
	public String catalogue_language;
	
	@STProperty(description = "Catalogue - licence")
	public String catalogue_licence;
	
	@STProperty(description = "Catalogue - release date")
	public String catalogue_releaseDate;
	
	@STProperty(description = "Catalogue - themes")
	public String catalogue_themes;
	
	@STProperty(description = "Catalogue - modification date")
	public String catalogue_modificationDate;
	
	
	//Dataset: mandatory
	@STProperty(description = "Dataset - description")
	@Required
	public String dataset_description;
	
	@STProperty(description = "Dataset - title")
	@Required
	public String dataset_title;
	
		
	@STProperty(description = "Dataset - distribution IRI")
	@Required
	public String dataset_distributionIRI;  

	@STProperty(description = "Dataset - Distribution access URL")
	@Required
	public String dataset_distributionAccessUrl; 

	@STProperty(description = "Dataset - Distribution description")
	public String dataset_distributionDescription; // --> the range in Optional
	
	@STProperty(description = "Dataset - Distribution format")
	public String dataset_distributionFormat; 
	
	@STProperty(description = "Dataset - Distribution licence")
	public String dataset_distributionLicence; 

	//Dataset: recommended
	//@STProperty(description = "Dataset - contact point")
	//public String dataset_contactPoint; // --> the range in Optional
	
	
	@STProperty(description = "Dataset - keyword")
	public String dataset_keyword; 
	
	@STProperty(description = "Dataset - publisher Agent IRI")
	@Required
	public String dataset_publisherAgentIRI;
	
	@STProperty(description = "Dataset - publisher Agent Name")
	@Required
	public String dataset_publisherAgentName;
	
	@STProperty(description = "Dataset - publisher Agent Type")
	public String dataset_publisherAgentType;
	
	@STProperty(description = "Dataset theme")
	public String dataset_theme; // 

	
	//Category: mandatory
	@STProperty(description = "Category preferred label")
	@Required
	public String category_preferredLabel; 

	
	//Category Scheme: mandatory
	@STProperty(description = "Category Scheme title")
	@Required
	public String categoryScheme_title; 
	
	
	
	
	
}
