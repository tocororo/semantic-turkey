package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 *
 */

public class ADMSDatasetMetadataExporterSettings extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "ADMS Dataset Metadata Exporter Settings";
	}

	//adms:Asset
	
	@STProperty(description = "Asset IRI")
	@Required
	public String asset_iri;
	
	@STProperty(description = "Asset Title")
	@Required
	public String asset_title;
	
	@STProperty(description = "Asset Description")
	@Required
	public String asset_description;
	
	@STProperty(description = "Asset SKOS AltLabel")
	public String asset_skos_label;

	@STProperty(description = "Asset Issued")
	public String asset_issued;

	@STProperty(description = "Asset Modified")
	@Required
	public String asset_modified;

	@STProperty(description = "Asset Keywords (comma separated)")
	public String asset_keywords;

	@STProperty(description = "Asset Version Info")
	public String asset_versionInfo;

	@STProperty(description = "Asset Version Notes")
	public String asset_versionsNotes;

	@STProperty(description = "Asset Theme")
	public String asset_theme;

	@STProperty(description = "Asset Spatial")
	@Required
	public String asset_spatial;

	@STProperty(description = "Asset Contact Point")
	@Required
	public String asset_contactPoint;

	@STProperty(description = "Asset Landing Pages")
	public String asset_landingPage;

	@STProperty(description = "Asset Landing Pages - Title")
	public String asset_landingPage_title;

	@STProperty(description = "Asset Described By")
	@Required
	public String asset_describedBy;

	@STProperty(description = "Asset Described By - Title")
	@Required
	public String asset_describedBy_title;

	@STProperty(description = "Asset Page")
	@Required
	public String asset_page;

	@STProperty(description = "Asset Page - Title")
	@Required
	public String asset_page_title;

	@STProperty(description = "Asset Identifier")
	@Required
	public String asset_identifier;

	@STProperty(description = "Asset Identifier - Notation (a rdfs:Literal which should be typed. )")
	@Required
	public String asset_identifier_notation;

	@STProperty(description = "Asset Identifier - creator")
	@Required
	public String asset_identifie_creator;

	@STProperty(description = "Asset Identifier - Scheme Agency")
	@Required
	public String asset_identifier_schemeAgency;

	@STProperty(description = "Asset Temporal")
	@Required
	public String asset_temporal;

	@STProperty(description = "Asset Temporal - Start Date")
	@Required
	public String asset_temporal_startDate;

	@STProperty(description = "Asset Temporal - End Date")
	@Required
	public String asset_temporal_endDate;

	@STProperty(description = "Asset Language (f an ISO 639-1 (two-letter) code is defined for language, "
			+ "then its corresponding IRI should be used; if no ISO 639-1 code is defined, then IRI corresponding "
			+ "to the ISO 639-2 (three-letter) code should be used. )")
	@Required
	public String asset_language;

	@STProperty(description = "Asset Statuts")
	@Required
	public String asset_status;

	@STProperty(description = "Asset Interoperability Level")
	@Required
	public String asset_interoperability_level;

	@STProperty(description = "Asset Type")
	@Required
	public String asset_type;

	@STProperty(description = "Asset Included Asset")
	@Required
	public String asset_includedAsset;

	@STProperty(description = "Asset sample")
	@Required
	public String asset_sample;

	@STProperty(description = "Asset traslation")
	@Required
	public String asset_traslation;

	@STProperty(description = "Asset Prev")
	@Required
	public String asset_prev;

	@STProperty(description = "Asset Last")
	@Required
	public String asset_last;

	@STProperty(description = "Asset Next")
	@Required
	public String asset_next;

	@STProperty(description = "Asset Relation")
	@Required
	public String asset_relation;

	@STProperty(description = "Asset Distribution")
	@Required
	public String asset_distribution;

	@STProperty(description = "Asset Publisher")
	@Required
	public String asset_publisher;

	@STProperty(description = "Asset Publisher - Name")
	@Required
	public String asset_publisher_name;

	@STProperty(description = "Asset Publisher - Type")
	@Required
	public String asset_publisher_type;

	
	//adms:AssertDistribution
	
	/*@STProperty(description = "Distribution IRI")
	@Required
	public String distribution_iri;*/
	
	@STProperty(description = "Distribution Issued")
	public String distribution_issued;

	@STProperty(description = "Distribution Modified")
	public String distribution_modified;

	@STProperty(description = "Distribution Title")
	public String distribution_title;

	@STProperty(description = "Distribution Description")
	public String distribution_description;

	@STProperty(description = "Distribution Access URL")
	public String distribution_accessUurl;

	@STProperty(description = "Distribution Download URL")
	public String distribution_downloadUrl;

	@STProperty(description = "Distribution Licence")
	public String distribution_licence;

	@STProperty(description = "Distribution Licence - Title")
	public String distribution_licence_title;

	@STProperty(description = "Distribution Licence - Description")
	public String distribution_licence_description;

	@STProperty(description = "Distribution Licence - type")
	public String distribution_licence_type;
	
	@STProperty(description = "Distribution Format")
	public String distribution_format;

	@STProperty(description = "Distribution Media type")
	public String distribution_media_type;
	
	
}
