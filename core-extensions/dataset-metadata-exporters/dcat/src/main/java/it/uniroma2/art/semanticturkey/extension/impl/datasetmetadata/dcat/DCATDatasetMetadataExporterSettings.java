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

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcat.DCATDatasetMetadataExporterSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String dataset_iri$description = keyBase + ".dataset_iri.description";
		public static final String dataset_iri$displayName = keyBase + ".dataset_iri.displayName";
		public static final String dataset_title$description = keyBase + ".dataset_title.description";
		public static final String dataset_title$displayName = keyBase + ".dataset_title.displayName";
		public static final String dataset_description$description = keyBase + ".dataset_description.description";
		public static final String dataset_description$displayName = keyBase + ".dataset_description.displayName";
		public static final String dataset_issued$description = keyBase + ".dataset_issued.description";
		public static final String dataset_issued$displayName = keyBase + ".dataset_issued.displayName";
		public static final String dataset_modified$description = keyBase + ".dataset_modified.description";
		public static final String dataset_modified$displayName = keyBase + ".dataset_modified.displayName";
		public static final String dataset_identifier$description = keyBase + ".dataset_identifier.description";
		public static final String dataset_identifier$displayName = keyBase + ".dataset_identifier.displayName";
		public static final String dataset_keywords$description = keyBase + ".dataset_keywords.description";
		public static final String dataset_keywords$displayName = keyBase + ".dataset_keywords.displayName";
		public static final String dataset_language$description = keyBase + ".dataset_language.description";
		public static final String dataset_language$displayName = keyBase + ".dataset_language.displayName";
		public static final String dataset_contactPoint$description = keyBase + ".dataset_contactPoint.description";
		public static final String dataset_contactPoint$displayName = keyBase + ".dataset_contactPoint.displayName";
		public static final String dataset_temporal$description = keyBase + ".dataset_temporal.description";
		public static final String dataset_temporal$displayName = keyBase + ".dataset_temporal.displayName";
		public static final String dataset_temporal_startDate$description = keyBase + ".dataset_temporal_startDate.description";
		public static final String dataset_temporal_startDate$displayName = keyBase + ".dataset_temporal_startDate.displayName";
		public static final String dataset_temporal_endDate$description = keyBase + ".dataset_temporal_endDate.description";
		public static final String dataset_temporal_endDate$displayName = keyBase + ".dataset_temporal_endDate.displayName";
		public static final String dataset_spatial$description = keyBase + ".dataset_spatial.description";
		public static final String dataset_spatial$displayName = keyBase + ".dataset_spatial.displayName";
		public static final String dataset_accrualPeriodicy$description = keyBase + ".dataset_accrualPeriodicy.description";
		public static final String dataset_accrualPeriodicy$displayName = keyBase + ".dataset_accrualPeriodicy.displayName";
		public static final String dataset_landingPage$description = keyBase + ".dataset_landingPage.description";
		public static final String dataset_landingPage$displayName = keyBase + ".dataset_landingPage.displayName";
		public static final String dataset_theme$description = keyBase + ".dataset_theme.description";
		public static final String dataset_theme$displayName = keyBase + ".dataset_theme.displayName";
		public static final String dataset_distribution$description = keyBase + ".dataset_distribution.description";
		public static final String dataset_distribution$displayName = keyBase + ".dataset_distribution.displayName";
		public static final String distribution_title$description = keyBase + ".distribution_title.description";
		public static final String distribution_title$displayName = keyBase + ".distribution_title.displayName";
		public static final String distribution_description$description = keyBase + ".distribution_description.description";
		public static final String distribution_description$displayName = keyBase + ".distribution_description.displayName";
		public static final String distribution_issued$description = keyBase + ".distribution_issued.description";
		public static final String distribution_issued$displayName = keyBase + ".distribution_issued.displayName";
		public static final String distribution_modified$description = keyBase + ".distribution_modified.description";
		public static final String distribution_modified$displayName = keyBase + ".distribution_modified.displayName";
		public static final String distribution_licence$description = keyBase + ".distribution_licence.description";
		public static final String distribution_licence$displayName = keyBase + ".distribution_licence.displayName";
		public static final String distribution_licence_title$description = keyBase + ".distribution_licence_title.description";
		public static final String distribution_licence_title$displayName = keyBase + ".distribution_licence_title.displayName";
		public static final String distribution_licence_description$description = keyBase + ".distribution_licence_description.description";
		public static final String distribution_licence_description$displayName = keyBase + ".distribution_licence_description.displayName";
		public static final String distribution_licence_type$description = keyBase + ".distribution_licence_type.description";
		public static final String distribution_licence_type$displayName = keyBase + ".distribution_licence_type.displayName";
		public static final String distribution_rights$description = keyBase + ".distribution_rights.description";
		public static final String distribution_rights$displayName = keyBase + ".distribution_rights.displayName";
		public static final String distribution_accessUrl$description = keyBase + ".distribution_accessUrl.description";
		public static final String distribution_accessUrl$displayName = keyBase + ".distribution_accessUrl.displayName";
		public static final String distribution_downloadUrl$description = keyBase + ".distribution_downloadUrl.description";
		public static final String distribution_downloadUrl$displayName = keyBase + ".distribution_downloadUrl.displayName";
		public static final String distribution_mediaType$description = keyBase + ".distribution_mediaType.description";
		public static final String distribution_mediaType$displayName = keyBase + ".distribution_mediaType.displayName";
		public static final String distribution_format$description = keyBase + ".distribution_format.description";
		public static final String distribution_format$displayName = keyBase + ".distribution_format.displayName";
		public static final String distribution_byteSize$description = keyBase + ".distribution_byteSize.description";
		public static final String distribution_byteSize$displayName = keyBase + ".distribution_byteSize.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	//dcat:Dataset
	
	@STProperty(description = "{" + MessageKeys.dataset_iri$description + "}", displayName = "{" + MessageKeys.dataset_iri$displayName + "}")
	@Required
	public String dataset_iri;
	
	@STProperty(description = "{" + MessageKeys.dataset_title$description + "}", displayName = "{" + MessageKeys.dataset_title$displayName + "}")
	@Required
	public String dataset_title;
	
	@STProperty(description = "{" + MessageKeys.dataset_description$description + "}", displayName = "{" + MessageKeys.dataset_description$displayName + "}")
	public String dataset_description;
	
	@STProperty(description = "{" + MessageKeys.dataset_issued$description + "}", displayName = "{" + MessageKeys.dataset_issued$displayName + "}")
	public String dataset_issued;

	@STProperty(description = "{" + MessageKeys.dataset_modified$description + "}", displayName = "{" + MessageKeys.dataset_modified$displayName + "}")
	public String dataset_modified;

	@STProperty(description = "{" + MessageKeys.dataset_identifier$description + "}", displayName = "{" + MessageKeys.dataset_identifier$displayName + "}")
	public String dataset_identifier;

	@STProperty(description = "{" + MessageKeys.dataset_keywords$description + "}", displayName = "{" + MessageKeys.dataset_keywords$displayName + "}")
	public String dataset_keywords;

	@STProperty(description = "{" + MessageKeys.dataset_language$description + "}", displayName = "{" + MessageKeys.dataset_language$displayName + "}")
	public String dataset_language;

	@STProperty(description = "{" + MessageKeys.dataset_contactPoint$description + "}", displayName = "{" + MessageKeys.dataset_contactPoint$displayName + "}")
	public String dataset_contactPoint;

	@STProperty(description = "{" + MessageKeys.dataset_temporal$description + "}", displayName = "{" + MessageKeys.dataset_temporal$displayName + "}")
	public String dataset_temporal;

	@STProperty(description = "{" + MessageKeys.dataset_temporal_startDate$description + "}", displayName = "{" + MessageKeys.dataset_temporal_startDate$displayName + "}")
	public String dataset_temporal_startDate;

	@STProperty(description = "{" + MessageKeys.dataset_temporal_endDate$description + "}", displayName = "{" + MessageKeys.dataset_temporal_endDate$displayName + "}")
	public String dataset_temporal_endDate;

	
	@STProperty(description = "{" + MessageKeys.dataset_spatial$description + "}", displayName = "{" + MessageKeys.dataset_spatial$displayName + "}")
	public String dataset_spatial;

	@STProperty(description = "{" + MessageKeys.dataset_accrualPeriodicy$description + "}", displayName = "{" + MessageKeys.dataset_accrualPeriodicy$displayName + "}")
	public String dataset_accrualPeriodicy;

	@STProperty(description = "{" + MessageKeys.dataset_landingPage$description + "}", displayName = "{" + MessageKeys.dataset_landingPage$displayName + "}")
	public String dataset_landingPage;

	@STProperty(description = "{" + MessageKeys.dataset_theme$description + "}", displayName = "{" + MessageKeys.dataset_theme$displayName + "}")
	public String dataset_theme;
	
	@STProperty(description = "{" + MessageKeys.dataset_distribution$description + "}", displayName = "{" + MessageKeys.dataset_distribution$displayName + "}")
	@Required
	public String dataset_distribution;
	
	//dcat:Descrtiption
	
	/*@STProperty(description = "Distrubtion IRI")
	@Required
	public String distribution_iri;*/
	
	@STProperty(description = "{" + MessageKeys.distribution_title$description + "}", displayName = "{" + MessageKeys.distribution_title$displayName + "}")
	@Required
	public String distribution_title;
	
	@STProperty(description = "{" + MessageKeys.distribution_description$description + "}", displayName = "{" + MessageKeys.distribution_description$displayName + "}")
	public String distribution_description;
	
	@STProperty(description = "{" + MessageKeys.distribution_issued$description + "}", displayName = "{" + MessageKeys.distribution_issued$displayName + "}")
	public String distribution_issued;
	
	@STProperty(description = "{" + MessageKeys.distribution_modified$description + "}", displayName = "{" + MessageKeys.distribution_modified$displayName + "}")
	public String distribution_modified;
	
	@STProperty(description = "{" + MessageKeys.distribution_licence$description + "}", displayName = "{" + MessageKeys.distribution_licence$displayName + "}")
	public String distribution_licence;
	
	@STProperty(description = "{" + MessageKeys.distribution_licence_title$description + "}", displayName = "{" + MessageKeys.distribution_licence_title$displayName + "}")
	public String distribution_licence_title;

	@STProperty(description = "{" + MessageKeys.distribution_licence_description$description + "}", displayName = "{" + MessageKeys.distribution_licence_description$displayName + "}")
	public String distribution_licence_description;

	@STProperty(description = "{" + MessageKeys.distribution_licence_type$description + "}", displayName = "{" + MessageKeys.distribution_licence_type$displayName + "}")
	public String distribution_licence_type;
	
	@STProperty(description = "{" + MessageKeys.distribution_rights$description + "}", displayName = "{" + MessageKeys.distribution_rights$displayName + "}")
	public String distribution_rights;
	
	@STProperty(description = "{" + MessageKeys.distribution_accessUrl$description + "}", displayName = "{" + MessageKeys.distribution_accessUrl$displayName + "}")
	public String distribution_accessUrl;
	
	@STProperty(description = "{" + MessageKeys.distribution_downloadUrl$description + "}", displayName = "{" + MessageKeys.distribution_downloadUrl$displayName + "}")
	public String distribution_downloadUrl;
	
	@STProperty(description = "{" + MessageKeys.distribution_mediaType$description + "}", displayName = "{" + MessageKeys.distribution_mediaType$displayName + "}")
	public String distribution_mediaType;
	
	@STProperty(description = "{" + MessageKeys.distribution_format$description + "}", displayName = "{" + MessageKeys.distribution_format$displayName + "}")
	public String distribution_format;

	@STProperty(description = "{" + MessageKeys.distribution_byteSize$description + "}", displayName = "{" + MessageKeys.distribution_byteSize$displayName + "}")
	public String distribution_byteSize;
	
	
}
