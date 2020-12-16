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
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap.DCATAPDatasetMetadataExporterSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String catalogue_iri$description = keyBase + ".catalogue_iri.description";
		public static final String catalogue_iri$displayName = keyBase + ".catalogue_iri.displayName";
		public static final String dataset_iri$description = keyBase + ".dataset_iri.description";
		public static final String dataset_iri$displayName = keyBase + ".dataset_iri.displayName";
		public static final String catalogue_description$description = keyBase + ".catalogue_description.description";
		public static final String catalogue_description$displayName = keyBase + ".catalogue_description.displayName";
		public static final String catalogue_publisherAgentIRI$description = keyBase + ".catalogue_publisherAgentIRI.description";
		public static final String catalogue_publisherAgentIRI$displayName = keyBase + ".catalogue_publisherAgentIRI.displayName";
		public static final String catalogue_publisherAgentName$description = keyBase + ".catalogue_publisherAgentName.description";
		public static final String catalogue_publisherAgentName$displayName = keyBase + ".catalogue_publisherAgentName.displayName";
		public static final String catalogue_publisherAgentType$description = keyBase + ".catalogue_publisherAgentType.description";
		public static final String catalogue_publisherAgentType$displayName = keyBase + ".catalogue_publisherAgentType.displayName";
		public static final String catalogue_title$description = keyBase + ".catalogue_title.description";
		public static final String catalogue_title$displayName = keyBase + ".catalogue_title.displayName";
		public static final String catalogue_homepage$description = keyBase + ".catalogue_homepage.description";
		public static final String catalogue_homepage$displayName = keyBase + ".catalogue_homepage.displayName";
		public static final String catalogue_language$description = keyBase + ".catalogue_language.description";
		public static final String catalogue_language$displayName = keyBase + ".catalogue_language.displayName";
		public static final String catalogue_licence$description = keyBase + ".catalogue_licence.description";
		public static final String catalogue_licence$displayName = keyBase + ".catalogue_licence.displayName";
		public static final String catalogue_releaseDate$description = keyBase + ".catalogue_releaseDate.description";
		public static final String catalogue_releaseDate$displayName = keyBase + ".catalogue_releaseDate.displayName";
		public static final String catalogue_themes$description = keyBase + ".catalogue_themes.description";
		public static final String catalogue_themes$displayName = keyBase + ".catalogue_themes.displayName";
		public static final String catalogue_modificationDate$description = keyBase + ".catalogue_modificationDate.description";
		public static final String catalogue_modificationDate$displayName = keyBase + ".catalogue_modificationDate.displayName";
		public static final String dataset_description$description = keyBase + ".dataset_description.description";
		public static final String dataset_description$displayName = keyBase + ".dataset_description.displayName";
		public static final String dataset_title$description = keyBase + ".dataset_title.description";
		public static final String dataset_title$displayName = keyBase + ".dataset_title.displayName";
		public static final String dataset_distributionIRI$description = keyBase + ".dataset_distributionIRI.description";
		public static final String dataset_distributionIRI$displayName = keyBase + ".dataset_distributionIRI.displayName";
		public static final String dataset_distributionAccessUrl$description = keyBase + ".dataset_distributionAccessUrl.description";
		public static final String dataset_distributionAccessUrl$displayName = keyBase + ".dataset_distributionAccessUrl.displayName";
		public static final String dataset_distributionDescription$description = keyBase + ".dataset_distributionDescription.description";
		public static final String dataset_distributionDescription$displayName = keyBase + ".dataset_distributionDescription.displayName";
		public static final String dataset_distributionFormat$description = keyBase + ".dataset_distributionFormat.description";
		public static final String dataset_distributionFormat$displayName = keyBase + ".dataset_distributionFormat.displayName";
		public static final String dataset_distributionLicence$description = keyBase + ".dataset_distributionLicence.description";
		public static final String dataset_distributionLicence$displayName = keyBase + ".dataset_distributionLicence.displayName";
		public static final String dataset_keyword$description = keyBase + ".dataset_keyword.description";
		public static final String dataset_keyword$displayName = keyBase + ".dataset_keyword.displayName";
		public static final String dataset_publisherAgentIRI$description = keyBase + ".dataset_publisherAgentIRI.description";
		public static final String dataset_publisherAgentIRI$displayName = keyBase + ".dataset_publisherAgentIRI.displayName";
		public static final String dataset_publisherAgentName$description = keyBase + ".dataset_publisherAgentName.description";
		public static final String dataset_publisherAgentName$displayName = keyBase + ".dataset_publisherAgentName.displayName";
		public static final String dataset_publisherAgentType$description = keyBase + ".dataset_publisherAgentType.description";
		public static final String dataset_publisherAgentType$displayName = keyBase + ".dataset_publisherAgentType.displayName";
		public static final String dataset_theme$description = keyBase + ".dataset_theme.description";
		public static final String dataset_theme$displayName = keyBase + ".dataset_theme.displayName";
		public static final String category_preferredLabel$description = keyBase + ".category_preferredLabel.description";
		public static final String category_preferredLabel$displayName = keyBase + ".category_preferredLabel.displayName";
		public static final String categoryScheme_title$description = keyBase + ".categoryScheme_title.description";
		public static final String categoryScheme_title$displayName = keyBase + ".categoryScheme_title.displayName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	
	
	
	// Mandatory Classes
	//@STProperty(description = "Agent IRI")
	//@Required
	//public String agent_iri;
	
	@STProperty(description = "{" + MessageKeys.catalogue_iri$description + "}")
	@Required
	public String catalogue_iri;
	
	
	@STProperty(description = "{" + MessageKeys.dataset_iri$description + "}")
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
	
	@STProperty(description = "{" + MessageKeys.catalogue_description$description + "}")
	@Required
	public String catalogue_description;
	
	@STProperty(description = "{" + MessageKeys.catalogue_publisherAgentIRI$description + "}")
	@Required
	public String catalogue_publisherAgentIRI;
	
	@STProperty(description = "{" + MessageKeys.catalogue_publisherAgentName$description + "}")
	@Required
	public String catalogue_publisherAgentName;
	
	@STProperty(description = "{" + MessageKeys.catalogue_publisherAgentType$description + "}")
	public String catalogue_publisherAgentType;
	
	
	@STProperty(description = "{" + MessageKeys.catalogue_title$description + "}")
	@Required
	public String catalogue_title;
	
	//Catalogue: recomended
	@STProperty(description = "{" + MessageKeys.catalogue_homepage$description + "}")
	public String catalogue_homepage;
	
	@STProperty(description = "{" + MessageKeys.catalogue_language$description + "}")
	public String catalogue_language;
	
	@STProperty(description = "{" + MessageKeys.catalogue_licence$description + "}")
	public String catalogue_licence;
	
	@STProperty(description = "{" + MessageKeys.catalogue_releaseDate$description + "}")
	public String catalogue_releaseDate;
	
	@STProperty(description = "{" + MessageKeys.catalogue_themes$description + "}")
	public String catalogue_themes;
	
	@STProperty(description = "{" + MessageKeys.catalogue_modificationDate$description + "}")
	public String catalogue_modificationDate;
	
	
	//Dataset: mandatory
	@STProperty(description = "{" + MessageKeys.dataset_description$description + "}")
	@Required
	public String dataset_description;
	
	@STProperty(description = "{" + MessageKeys.dataset_title$description + "}")
	@Required
	public String dataset_title;
	
		
	@STProperty(description = "{" + MessageKeys.dataset_distributionIRI$description + "}")
	@Required
	public String dataset_distributionIRI;  

	@STProperty(description = "{" + MessageKeys.dataset_distributionAccessUrl$description + "}")
	@Required
	public String dataset_distributionAccessUrl; 

	@STProperty(description = "{" + MessageKeys.dataset_distributionDescription$description + "}")
	public String dataset_distributionDescription; // --> the range in Optional
	
	@STProperty(description = "{" + MessageKeys.dataset_distributionFormat$description + "}")
	public String dataset_distributionFormat; 
	
	@STProperty(description = "{" + MessageKeys.dataset_distributionLicence$description + "}")
	public String dataset_distributionLicence; 

	//Dataset: recommended
	//@STProperty(description = "Dataset - contact point")
	//public String dataset_contactPoint; // --> the range in Optional
	
	
	@STProperty(description = "{" + MessageKeys.dataset_keyword$description + "}")
	public String dataset_keyword; 
	
	@STProperty(description = "{" + MessageKeys.dataset_publisherAgentIRI$description + "}")
	@Required
	public String dataset_publisherAgentIRI;
	
	@STProperty(description = "{" + MessageKeys.dataset_publisherAgentName$description + "}")
	@Required
	public String dataset_publisherAgentName;
	
	@STProperty(description = "{" + MessageKeys.dataset_publisherAgentType$description + "}")
	public String dataset_publisherAgentType;
	
	@STProperty(description = "{" + MessageKeys.dataset_theme$description + "}")
	public String dataset_theme; // 

	
	//Category: mandatory
	@STProperty(description = "{" + MessageKeys.category_preferredLabel$description + "}")
	@Required
	public String category_preferredLabel; 

	
	//Category Scheme: mandatory
	@STProperty(description = "{" + MessageKeys.categoryScheme_title$description + "}")
	@Required
	public String categoryScheme_title; 
	
	
	
	
	
}
