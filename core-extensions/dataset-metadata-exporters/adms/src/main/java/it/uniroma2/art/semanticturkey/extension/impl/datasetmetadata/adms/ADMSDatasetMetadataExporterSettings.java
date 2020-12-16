package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.adms;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 *
 */

public class ADMSDatasetMetadataExporterSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.adms.ADMSDatasetMetadataExporterSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String asset_iri$description = keyBase + ".asset_iri.description";
		public static final String asset_iri$displayName = keyBase + ".asset_iri.displayName";
		public static final String asset_title$description = keyBase + ".asset_title.description";
		public static final String asset_title$displayName = keyBase + ".asset_title.displayName";
		public static final String asset_description$description = keyBase + ".asset_description.description";
		public static final String asset_description$displayName = keyBase + ".asset_description.displayName";
		public static final String asset_skos_label$description = keyBase + ".asset_skos_label.description";
		public static final String asset_skos_label$displayName = keyBase + ".asset_skos_label.displayName";
		public static final String asset_issued$description = keyBase + ".asset_issued.description";
		public static final String asset_issued$displayName = keyBase + ".asset_issued.displayName";
		public static final String asset_modified$description = keyBase + ".asset_modified.description";
		public static final String asset_modified$displayName = keyBase + ".asset_modified.displayName";
		public static final String asset_keywords$description = keyBase + ".asset_keywords.description";
		public static final String asset_keywords$displayName = keyBase + ".asset_keywords.displayName";
		public static final String asset_versionInfo$description = keyBase + ".asset_versionInfo.description";
		public static final String asset_versionInfo$displayName = keyBase + ".asset_versionInfo.displayName";
		public static final String asset_versionsNotes$description = keyBase + ".asset_versionsNotes.description";
		public static final String asset_versionsNotes$displayName = keyBase + ".asset_versionsNotes.displayName";
		public static final String asset_theme$description = keyBase + ".asset_theme.description";
		public static final String asset_theme$displayName = keyBase + ".asset_theme.displayName";
		public static final String asset_spatial$description = keyBase + ".asset_spatial.description";
		public static final String asset_spatial$displayName = keyBase + ".asset_spatial.displayName";
		public static final String asset_contactPoint$description = keyBase + ".asset_contactPoint.description";
		public static final String asset_contactPoint$displayName = keyBase + ".asset_contactPoint.displayName";
		public static final String asset_landingPage$description = keyBase + ".asset_landingPage.description";
		public static final String asset_landingPage$displayName = keyBase + ".asset_landingPage.displayName";
		public static final String asset_landingPage_title$description = keyBase + ".asset_landingPage_title.description";
		public static final String asset_landingPage_title$displayName = keyBase + ".asset_landingPage_title.displayName";
		public static final String asset_describedBy$description = keyBase + ".asset_describedBy.description";
		public static final String asset_describedBy$displayName = keyBase + ".asset_describedBy.displayName";
		public static final String asset_describedBy_title$description = keyBase + ".asset_describedBy_title.description";
		public static final String asset_describedBy_title$displayName = keyBase + ".asset_describedBy_title.displayName";
		public static final String asset_page$description = keyBase + ".asset_page.description";
		public static final String asset_page$displayName = keyBase + ".asset_page.displayName";
		public static final String asset_page_title$description = keyBase + ".asset_page_title.description";
		public static final String asset_page_title$displayName = keyBase + ".asset_page_title.displayName";
		public static final String asset_identifier$description = keyBase + ".asset_identifier.description";
		public static final String asset_identifier$displayName = keyBase + ".asset_identifier.displayName";
		public static final String asset_identifier_notation$description = keyBase + ".asset_identifier_notation.description";
		public static final String asset_identifier_notation$displayName = keyBase + ".asset_identifier_notation.displayName";
		public static final String asset_identifie_creator$description = keyBase + ".asset_identifie_creator.description";
		public static final String asset_identifie_creator$displayName = keyBase + ".asset_identifie_creator.displayName";
		public static final String asset_identifier_schemeAgency$description = keyBase + ".asset_identifier_schemeAgency.description";
		public static final String asset_identifier_schemeAgency$displayName = keyBase + ".asset_identifier_schemeAgency.displayName";
		public static final String asset_temporal$description = keyBase + ".asset_temporal.description";
		public static final String asset_temporal$displayName = keyBase + ".asset_temporal.displayName";
		public static final String asset_temporal_startDate$description = keyBase + ".asset_temporal_startDate.description";
		public static final String asset_temporal_startDate$displayName = keyBase + ".asset_temporal_startDate.displayName";
		public static final String asset_temporal_endDate$description = keyBase + ".asset_temporal_endDate.description";
		public static final String asset_temporal_endDate$displayName = keyBase + ".asset_temporal_endDate.displayName";
		public static final String asset_language$description = keyBase + ".asset_language.description";
		public static final String asset_language$displayName = keyBase + ".asset_language.displayName";
		public static final String asset_status$description = keyBase + ".asset_status.description";
		public static final String asset_status$displayName = keyBase + ".asset_status.displayName";
		public static final String asset_interoperability_level$description = keyBase + ".asset_interoperability_level.description";
		public static final String asset_interoperability_level$displayName = keyBase + ".asset_interoperability_level.displayName";
		public static final String asset_type$description = keyBase + ".asset_type.description";
		public static final String asset_type$displayName = keyBase + ".asset_type.displayName";
		public static final String asset_includedAsset$description = keyBase + ".asset_includedAsset.description";
		public static final String asset_includedAsset$displayName = keyBase + ".asset_includedAsset.displayName";
		public static final String asset_sample$description = keyBase + ".asset_sample.description";
		public static final String asset_sample$displayName = keyBase + ".asset_sample.displayName";
		public static final String asset_traslation$description = keyBase + ".asset_traslation.description";
		public static final String asset_traslation$displayName = keyBase + ".asset_traslation.displayName";
		public static final String asset_prev$description = keyBase + ".asset_prev.description";
		public static final String asset_prev$displayName = keyBase + ".asset_prev.displayName";
		public static final String asset_last$description = keyBase + ".asset_last.description";
		public static final String asset_last$displayName = keyBase + ".asset_last.displayName";
		public static final String asset_next$description = keyBase + ".asset_next.description";
		public static final String asset_next$displayName = keyBase + ".asset_next.displayName";
		public static final String asset_relation$description = keyBase + ".asset_relation.description";
		public static final String asset_relation$displayName = keyBase + ".asset_relation.displayName";
		public static final String asset_distribution$description = keyBase + ".asset_distribution.description";
		public static final String asset_distribution$displayName = keyBase + ".asset_distribution.displayName";
		public static final String asset_publisher$description = keyBase + ".asset_publisher.description";
		public static final String asset_publisher$displayName = keyBase + ".asset_publisher.displayName";
		public static final String asset_publisher_name$description = keyBase + ".asset_publisher_name.description";
		public static final String asset_publisher_name$displayName = keyBase + ".asset_publisher_name.displayName";
		public static final String asset_publisher_type$description = keyBase + ".asset_publisher_type.description";
		public static final String asset_publisher_type$displayName = keyBase + ".asset_publisher_type.displayName";
		public static final String distribution_issued$description = keyBase + ".distribution_issued.description";
		public static final String distribution_issued$displayName = keyBase + ".distribution_issued.displayName";
		public static final String distribution_modified$description = keyBase + ".distribution_modified.description";
		public static final String distribution_modified$displayName = keyBase + ".distribution_modified.displayName";
		public static final String distribution_title$description = keyBase + ".distribution_title.description";
		public static final String distribution_title$displayName = keyBase + ".distribution_title.displayName";
		public static final String distribution_description$description = keyBase + ".distribution_description.description";
		public static final String distribution_description$displayName = keyBase + ".distribution_description.displayName";
		public static final String distribution_accessUurl$description = keyBase + ".distribution_accessUurl.description";
		public static final String distribution_accessUurl$displayName = keyBase + ".distribution_accessUurl.displayName";
		public static final String distribution_downloadUrl$description = keyBase + ".distribution_downloadUrl.description";
		public static final String distribution_downloadUrl$displayName = keyBase + ".distribution_downloadUrl.displayName";
		public static final String distribution_licence$description = keyBase + ".distribution_licence.description";
		public static final String distribution_licence$displayName = keyBase + ".distribution_licence.displayName";
		public static final String distribution_licence_title$description = keyBase + ".distribution_licence_title.description";
		public static final String distribution_licence_title$displayName = keyBase + ".distribution_licence_title.displayName";
		public static final String distribution_licence_description$description = keyBase + ".distribution_licence_description.description";
		public static final String distribution_licence_description$displayName = keyBase + ".distribution_licence_description.displayName";
		public static final String distribution_licence_type$description = keyBase + ".distribution_licence_type.description";
		public static final String distribution_licence_type$displayName = keyBase + ".distribution_licence_type.displayName";
		public static final String distribution_format$description = keyBase + ".distribution_format.description";
		public static final String distribution_format$displayName = keyBase + ".distribution_format.displayName";
		public static final String distribution_media_type$description = keyBase + ".distribution_media_type.description";
		public static final String distribution_media_type$displayName = keyBase + ".distribution_media_type.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	//adms:Asset
	
	@STProperty(description = "{" + MessageKeys.asset_iri$description + "}")
	@Required
	public String asset_iri;
	
	@STProperty(description = "{" + MessageKeys.asset_title$description + "}")
	@Required
	public String asset_title;
	
	@STProperty(description = "{" + MessageKeys.asset_description$description + "}")
	@Required
	public String asset_description;
	
	@STProperty(description = "{" + MessageKeys.asset_skos_label$description + "}")
	public String asset_skos_label;

	@STProperty(description = "{" + MessageKeys.asset_issued$description + "}")
	public String asset_issued;

	@STProperty(description = "{" + MessageKeys.asset_modified$description + "}")
	public String asset_modified;

	@STProperty(description = "{" + MessageKeys.asset_keywords$description + "}")
	public String asset_keywords;

	@STProperty(description = "{" + MessageKeys.asset_versionInfo$description + "}")
	public String asset_versionInfo;

	@STProperty(description = "{" + MessageKeys.asset_versionsNotes$description + "}")
	public String asset_versionsNotes;

	@STProperty(description = "{" + MessageKeys.asset_theme$description + "}")
	public String asset_theme;

	@STProperty(description = "{" + MessageKeys.asset_spatial$description + "}")
	public String asset_spatial;

	@STProperty(description = "{" + MessageKeys.asset_contactPoint$description + "}")
	public String asset_contactPoint;

	@STProperty(description = "{" + MessageKeys.asset_landingPage$description + "}")
	public String asset_landingPage;

	@STProperty(description = "{" + MessageKeys.asset_landingPage_title$description + "}")
	public String asset_landingPage_title;

	@STProperty(description = "{" + MessageKeys.asset_describedBy$description + "}")
	public String asset_describedBy;

	@STProperty(description = "{" + MessageKeys.asset_describedBy_title$description + "}")
	@Required
	public String asset_describedBy_title;

	@STProperty(description = "{" + MessageKeys.asset_page$description + "}")
	public String asset_page;

	@STProperty(description = "{" + MessageKeys.asset_page_title$description + "}")
	public String asset_page_title;

	@STProperty(description = "{" + MessageKeys.asset_identifier$description + "}")
	public String asset_identifier;

	@STProperty(description = "{" + MessageKeys.asset_identifier_notation$description + "}")
	public String asset_identifier_notation;

	@STProperty(description = "{" + MessageKeys.asset_identifie_creator$description + "}")
	public String asset_identifie_creator;

	@STProperty(description = "{" + MessageKeys.asset_identifier_schemeAgency$description + "}")
	public String asset_identifier_schemeAgency;

	@STProperty(description = "{" + MessageKeys.asset_temporal$description + "}")
	public String asset_temporal;

	@STProperty(description = "{" + MessageKeys.asset_temporal_startDate$description + "}")
	public String asset_temporal_startDate;

	@STProperty(description = "{" + MessageKeys.asset_temporal_endDate$description + "}")
	public String asset_temporal_endDate;

	@STProperty(description = "{" + MessageKeys.asset_language$description + "}")
	public String asset_language;

	@STProperty(description = "{" + MessageKeys.asset_status$description + "}")
	public String asset_status;

	@STProperty(description = "{" + MessageKeys.asset_interoperability_level$description + "}")
	public String asset_interoperability_level;

	@STProperty(description = "{" + MessageKeys.asset_type$description + "}")
	public String asset_type;

	@STProperty(description = "{" + MessageKeys.asset_includedAsset$description + "}")
	public String asset_includedAsset;

	@STProperty(description = "{" + MessageKeys.asset_sample$description + "}")
	public String asset_sample;

	@STProperty(description = "{" + MessageKeys.asset_traslation$description + "}")
	public String asset_traslation;

	@STProperty(description = "{" + MessageKeys.asset_prev$description + "}")
	public String asset_prev;

	@STProperty(description = "{" + MessageKeys.asset_last$description + "}")
	public String asset_last;

	@STProperty(description = "{" + MessageKeys.asset_next$description + "}")
	public String asset_next;

	@STProperty(description = "{" + MessageKeys.asset_relation$description + "}")
	public String asset_relation;

	@STProperty(description = "{" + MessageKeys.asset_distribution$description + "}")
	public String asset_distribution;

	@STProperty(description = "{" + MessageKeys.asset_publisher$description + "}")
	public String asset_publisher;

	@STProperty(description = "{" + MessageKeys.asset_publisher_name$description + "}")
	public String asset_publisher_name;

	@STProperty(description = "{" + MessageKeys.asset_publisher_type$description + "}")
	public String asset_publisher_type;

	
	//adms:AssetDistribution
	
	/*@STProperty(description = "Distribution IRI")
	@Required
	public String distribution_iri;*/
	
	@STProperty(description = "{" + MessageKeys.distribution_issued$description + "}")
	public String distribution_issued;

	@STProperty(description = "{" + MessageKeys.distribution_modified$description + "}")
	public String distribution_modified;

	@STProperty(description = "{" + MessageKeys.distribution_title$description + "}")
	public String distribution_title;

	@STProperty(description = "{" + MessageKeys.distribution_description$description + "}")
	public String distribution_description;

	@STProperty(description = "{" + MessageKeys.distribution_accessUurl$description + "}")
	public String distribution_accessUurl;

	@STProperty(description = "{" + MessageKeys.distribution_downloadUrl$description + "}")
	public String distribution_downloadUrl;

	@STProperty(description = "{" + MessageKeys.distribution_licence$description + "}")
	public String distribution_licence;

	@STProperty(description = "{" + MessageKeys.distribution_licence_title$description + "}")
	public String distribution_licence_title;

	@STProperty(description = "{" + MessageKeys.distribution_licence_description$description + "}")
	public String distribution_licence_description;

	@STProperty(description = "{" + MessageKeys.distribution_licence_type$description + "}")
	public String distribution_licence_type;
	
	@STProperty(description = "{" + MessageKeys.distribution_format$description + "}")
	public String distribution_format;

	@STProperty(description = "{" + MessageKeys.distribution_media_type$description + "}")
	public String distribution_media_type;
	
	
}
