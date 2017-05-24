package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.semanticturkey.plugin.AbstractPlugin;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.vocabulary.ADMSFragment;

/**
 * A {@link DatasetMetadataExporter} for the <a href="https://www.w3.org/TR/vocab-adms/">Asset Description
 * Metadata Schema (ADMS)</a>
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 *
 */
public class ADMSDatasetMetadataExporter extends
		AbstractPlugin<DatasetMetadataExporterSettings, ADMSDatasetMetadataExporterSettings, ADMSDatasetMetadataExporterFactory>
		implements DatasetMetadataExporter {

	public ADMSDatasetMetadataExporter(ADMSDatasetMetadataExporterFactory factory) {
		super(factory);
	}

	@Override
	public Model produceDatasetMetadata(Project<?> project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException {
		
		Repository tempMetadataRepository = new SailRepository(new MemoryStore());
		tempMetadataRepository.initialize();
		
		ValueFactory valueFactory = conn.getValueFactory();
		
		try {
			try (RepositoryConnection metadataConnection = tempMetadataRepository.getConnection()) {
				/*DatasetMetadataExporterSettings extensionPointSettings = getExtensionPointProjectSettings(
						project);*/
				ADMSDatasetMetadataExporterSettings pluginSettings = getClassLevelProjectSettings(
						project);

				Model metadataModel = new LinkedHashModel();
				metadataModel.setNamespace(FOAF.NS);
				metadataModel.setNamespace(DCTERMS.NS);
				metadataModel.setNamespace(DCAT.NS);
				metadataModel.setNamespace(XMLSchema.NS);
				metadataModel.setNamespace(SKOS.NS);
				metadataModel.setNamespace(ADMSFragment.NS); 
				metadataModel.setNamespace(new SimpleNamespace("wdrs", "http://www.w3.org/2007/05/powder-s#"));
				
				//the Asset part
				
				IRI assetIRI = valueFactory.createIRI(pluginSettings.asset_iri);
				metadataModel.add(valueFactory.createStatement(assetIRI, RDF.TYPE, ADMSFragment.ASSETCLASS));
				
				
				String asset_title = pluginSettings.asset_title;
				if(asset_title!=null && !asset_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.TITLE, 
							valueFactory.createLiteral(asset_title)));
				}
				
				String asset_description = pluginSettings.asset_description;
				if(asset_description!=null && !asset_description.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.DESCRIPTION, 
							generateLiteralWithLang(asset_description, valueFactory)));
				}

				String asset_skos_label = pluginSettings.asset_skos_label;
				if(asset_skos_label!=null && !asset_skos_label.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, SKOS.PREF_LABEL, 
							generateLiteralWithLang(asset_skos_label, valueFactory)));
				}

				String asset_issued = pluginSettings.asset_issued;
				if(asset_issued!=null && !asset_issued.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.ISSUED, 
							valueFactory.createLiteral(asset_issued, XMLSchema.DATE)));
				}

				String asset_modified = pluginSettings.asset_modified;
				if(asset_modified!=null && !asset_modified.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.MODIFIED, 
							valueFactory.createLiteral(asset_modified, XMLSchema.DATE)));
				}

				String asset_keywords = pluginSettings.asset_keywords;
				if(asset_keywords!=null && !asset_keywords.isEmpty()){
					String[] dataset_keywords_array = asset_keywords.split(";");
					for(String dataset_keywords_single : dataset_keywords_array){
						metadataModel.add(valueFactory.createStatement(assetIRI, DCAT.KEYWORD, 
								valueFactory.createLiteral(dataset_keywords_single)));
					}
					
				}

				String asset_versionInfo = pluginSettings.asset_versionInfo;
				if(asset_versionInfo!=null && !asset_versionInfo.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, OWL.VERSIONINFO,
							generateLiteralWithLang(asset_versionInfo, valueFactory)));
				}

				String asset_versionsNotes = pluginSettings.asset_versionsNotes;
				if(asset_versionsNotes!=null && !asset_versionsNotes.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.VERSIONNOTES , 
							generateLiteralWithLang(asset_versionsNotes, valueFactory)));
				}

				String asset_theme = pluginSettings.asset_theme;
				if(asset_theme!=null && !asset_theme.isEmpty()){
					IRI asset_theme_iri = valueFactory.createIRI(asset_theme);
					metadataModel.add(valueFactory.createStatement(asset_theme_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCAT.THEME , 
							valueFactory.createIRI(asset_theme)));
				}

				String asset_spatial = pluginSettings.asset_spatial;
				if(asset_spatial!=null && !asset_spatial.isEmpty()){
					IRI asset_spatial_iri = valueFactory.createIRI(asset_spatial);
					metadataModel.add(valueFactory.createStatement(asset_spatial_iri, RDF.TYPE, DCTERMS.LOCATION));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.SPATIAL , 
							asset_spatial_iri));
				}

				String asset_contactPoint = pluginSettings.asset_contactPoint;
				if(asset_contactPoint!=null && !asset_contactPoint.isEmpty()){
					IRI asset_contactPoint_iri = valueFactory.createIRI(asset_contactPoint);
					metadataModel.add(valueFactory.createStatement(assetIRI, DCAT.CONTACT_POINT, 
							asset_contactPoint_iri));
					//TODO the dataset_contact_point is a VCARD, so deal with it
				}

				String asset_landingPage = pluginSettings.asset_landingPage;
				IRI asset_landingPage_iri = null;
				if(asset_landingPage!=null && !asset_landingPage.isEmpty()){
					asset_landingPage_iri = valueFactory.createIRI(asset_landingPage);
					metadataModel.add(valueFactory.createStatement(asset_landingPage_iri, RDF.TYPE, FOAF.DOCUMENT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCAT.LANDING_PAGE, 
							asset_landingPage_iri));
				}

				String asset_landingPage_title = pluginSettings.asset_landingPage_title;
				if(asset_landingPage_iri != null && asset_landingPage_title!=null && !asset_landingPage_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(asset_landingPage_iri, DCTERMS.TITLE, 
							generateLiteralWithLang(asset_landingPage_title, valueFactory)));
				}

				
				String asset_describedBy = pluginSettings.asset_landingPage;
				IRI asset_describedBy_iri = null;
				if(asset_describedBy!=null && !asset_describedBy.isEmpty()){
					asset_describedBy_iri = valueFactory.createIRI(asset_describedBy);
					metadataModel.add(valueFactory.createStatement(asset_describedBy_iri, RDF.TYPE, FOAF.DOCUMENT));
					metadataModel.add(valueFactory.createStatement(assetIRI, valueFactory.createIRI("http://www.w3.org/2007/05/powder-s#describedBy"), 
							asset_describedBy_iri));
				}

				String asset_describedBy_title = pluginSettings.asset_landingPage_title;
				if(asset_describedBy_iri != null && asset_describedBy_title!=null && !asset_describedBy_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(asset_describedBy_iri, DCTERMS.TITLE, 
							generateLiteralWithLang(asset_describedBy_title, valueFactory)));
				}
				
				String asset_page = pluginSettings.asset_page;
				IRI asset_page_iri = null;
				if(asset_page!=null && !asset_page.isEmpty()){
					asset_page_iri = valueFactory.createIRI(asset_page);
					metadataModel.add(valueFactory.createStatement(asset_page_iri, RDF.TYPE, FOAF.DOCUMENT));
					metadataModel.add(valueFactory.createStatement(assetIRI, FOAF.PAGE, 
							asset_page_iri));
				}

				String asset_page_title = pluginSettings.asset_page_title;
				if(asset_page_iri != null && asset_page_title!=null && !asset_page_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(asset_page_iri, DCTERMS.TITLE, 
							generateLiteralWithLang(asset_page_title, valueFactory)));
				}
				
				String asset_identifier = pluginSettings.asset_identifier;
				IRI asset_identifier_iri = null;
				if(asset_identifier!=null && !asset_identifier.isEmpty()){
					asset_identifier_iri = valueFactory.createIRI(asset_identifier);
					metadataModel.add(valueFactory.createStatement(asset_identifier_iri, RDF.TYPE, 
							ADMSFragment.IDENTIFIERCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.IDENTIFIER, 
							asset_identifier_iri));
				}

				String asset_identifier_notation = pluginSettings.asset_identifier_notation;
				if(asset_identifier_iri != null && asset_identifier_notation!=null && !asset_identifier_notation.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, SKOS.NOTATION, 
							generateLiteralWithType(asset_identifier_notation, valueFactory)));
				}

				String asset_identifie_creator = pluginSettings.asset_identifie_creator;
				if(asset_identifier_iri != null && asset_identifie_creator!=null && !asset_identifie_creator.isEmpty()){
					IRI asset_identifie_creator_iri = valueFactory.createIRI(asset_identifie_creator);
					metadataModel.add(valueFactory.createStatement(asset_identifie_creator_iri, RDF.TYPE, FOAF.AGENT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.CREATOR, 
							asset_identifie_creator_iri));
				}

				String asset_identifier_schemeAgency = pluginSettings.asset_identifier_schemeAgency;
				if(asset_identifier_iri != null && asset_identifier_schemeAgency!=null && !asset_identifier_schemeAgency.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.SCHEMEAGENCY, 
							generateLiteralWithLang(asset_identifier_schemeAgency, valueFactory)));
				}

				String asset_temporal = pluginSettings.asset_temporal;
				IRI asset_temporal_iri = null;
				if(asset_temporal!=null && !asset_temporal.isEmpty()){
					asset_temporal_iri = valueFactory.createIRI(asset_temporal);
					metadataModel.add(valueFactory.createStatement(asset_temporal_iri, RDF.TYPE, DCTERMS.PERIOD_OF_TIME));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.TEMPORAL, 
							asset_temporal_iri));
				}

				String asset_temporal_startDate = pluginSettings.asset_temporal_startDate;
				if(asset_temporal_iri!=null && asset_temporal_startDate!=null && !asset_temporal_startDate.isEmpty()){
					metadataModel.add(valueFactory.createStatement(asset_temporal_iri, valueFactory.createIRI("http://schema.org/startDate"), 
							valueFactory.createLiteral(asset_temporal_startDate, XMLSchema.DATE)));
				}

				String asset_temporal_endDate = pluginSettings.asset_temporal_endDate;
				if(asset_temporal_iri!=null && asset_temporal_endDate!=null && !asset_temporal_endDate.isEmpty()){
					metadataModel.add(valueFactory.createStatement(asset_temporal_iri, valueFactory.createIRI("http://schema.org/endDate"), 
							valueFactory.createLiteral(asset_temporal_endDate, XMLSchema.DATE)));
				}

				String asset_language = pluginSettings.asset_language;
				if(asset_language!=null && !asset_language.isEmpty()){
					IRI asset_language_iri = valueFactory.createIRI(asset_language);
					metadataModel.add(valueFactory.createStatement(asset_language_iri, RDF.TYPE, DCTERMS.LINGUISTIC_SYSTEM));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.LANGUAGE, 
							asset_language_iri));
				}

				String asset_status = pluginSettings.asset_status;
				if(asset_status!=null && !asset_status.isEmpty()){
					IRI asset_status_iri = valueFactory.createIRI(asset_status);
					metadataModel.add(valueFactory.createStatement(asset_status_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.STATUS, 
							asset_status_iri));
				}
				
				String asset_interoperability_level = pluginSettings.asset_interoperability_level;
				if(asset_interoperability_level!=null && !asset_interoperability_level.isEmpty()){
					IRI asset_interoperability_level_iri = valueFactory.createIRI(asset_interoperability_level);
					metadataModel.add(valueFactory.createStatement(asset_interoperability_level_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.INTEROPERABILITYLEVEL, 
							asset_interoperability_level_iri));
				}
				
				String asset_type = pluginSettings.asset_type;
				if(asset_type!=null && !asset_type.isEmpty()){
					IRI asset_type_iri = valueFactory.createIRI(asset_type);
					metadataModel.add(valueFactory.createStatement(asset_type_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.TYPE, 
							asset_type_iri));
				}

				String asset_includeAsset = pluginSettings.asset_includedAsset;
				if(asset_includeAsset!=null && !asset_includeAsset.isEmpty()){
					IRI asset_includeAsset_iri = valueFactory.createIRI(asset_includeAsset);
					metadataModel.add(valueFactory.createStatement(asset_includeAsset_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.INCLUDEDASSET , 
							asset_includeAsset_iri));
				}

				String asset_sample = pluginSettings.asset_sample;
				if(asset_sample!=null && !asset_sample.isEmpty()){
					IRI asset_sample_iri = valueFactory.createIRI(asset_includeAsset);
					metadataModel.add(valueFactory.createStatement(asset_sample_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.SAMPLE, 
							asset_sample_iri));
				}
				
				String asset_traslation = pluginSettings.asset_traslation;
				if(asset_traslation!=null && !asset_traslation.isEmpty()){
					IRI asset_traslation_iri = valueFactory.createIRI(asset_traslation);
					metadataModel.add(valueFactory.createStatement(asset_traslation_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.TRANSLATION , 
							asset_traslation_iri));
				}
				
				String asset_prev = pluginSettings.asset_prev;
				if(asset_prev!=null && !asset_prev.isEmpty()){
					IRI asset_prev_iri = valueFactory.createIRI(asset_includeAsset);
					metadataModel.add(valueFactory.createStatement(asset_prev_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.PREV, 
							asset_prev_iri));
				}
				
				String asset_last = pluginSettings.asset_last;
				if(asset_last!=null && !asset_last.isEmpty()){
					IRI asset_last_iri = valueFactory.createIRI(asset_last);
					metadataModel.add(valueFactory.createStatement(asset_last_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.LAST, 
							asset_last_iri));
				}
				
				String asset_next = pluginSettings.asset_next;
				if(asset_next!=null && !asset_next.isEmpty()){
					IRI asset_next_iri = valueFactory.createIRI(asset_next);
					metadataModel.add(valueFactory.createStatement(asset_next_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, ADMSFragment.NEXT, 
							asset_next_iri));
				}
				String asset_relation = pluginSettings.asset_relation;
				if(asset_relation!=null && !asset_relation.isEmpty()){
					IRI asset_relation_iri = valueFactory.createIRI(asset_next);
					metadataModel.add(valueFactory.createStatement(asset_relation_iri, RDF.TYPE, ADMSFragment.ASSETCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.RELATION, 
							asset_relation_iri));
				}

				String asset_distribution = pluginSettings.asset_distribution;
				IRI asset_distribution_iri = null;
				if(asset_distribution!=null && !asset_distribution.isEmpty()){
					asset_distribution_iri = valueFactory.createIRI(asset_distribution);
					metadataModel.add(valueFactory.createStatement(asset_distribution_iri, RDF.TYPE, ADMSFragment.ASSETDISTRIBUTIONCLASS));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCAT.DISTRIBUTION, 
							asset_distribution_iri));
				}

				String asset_publisher = pluginSettings.asset_publisher;
				IRI asset_publisher_iri = null;
				if(asset_publisher!=null && !asset_publisher.isEmpty()){
					asset_publisher_iri = valueFactory.createIRI(asset_publisher);
					metadataModel.add(valueFactory.createStatement(asset_publisher_iri, RDF.TYPE, FOAF.AGENT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.PUBLISHER, 
							asset_publisher_iri));
				}

				String asset_publisher_name = pluginSettings.asset_publisher_name;
				if(asset_publisher_iri!=null && asset_publisher_name!=null && !asset_publisher_name.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, FOAF.NAME, 
							generateLiteralWithLang(asset_publisher_name, valueFactory)));
				}
				
				String asset_publisher_type = pluginSettings.asset_publisher_type;
				if(asset_publisher_iri!=null && asset_publisher_type!=null && !asset_publisher_type.isEmpty()){
					IRI asset_publisher_type_iri = valueFactory.createIRI(asset_publisher_type);
					metadataModel.add(valueFactory.createStatement(asset_publisher_type_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(assetIRI, DCTERMS.TYPE, 
							asset_publisher_type_iri));
				}
				
				//the AssetDistributionPart
				if(asset_distribution_iri!=null){
					
					String distribution_issued = pluginSettings.distribution_issued;
					if(distribution_issued!=null && !distribution_issued.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCTERMS.ISSUED, 
								valueFactory.createLiteral(distribution_issued, XMLSchema.DATE)));
					}
					
					String distribution_modified = pluginSettings.distribution_modified;
					if(distribution_modified!=null && !distribution_modified.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCTERMS.MODIFIED, 
								valueFactory.createLiteral(distribution_modified, XMLSchema.DATE)));
					}
					
					String distribution_title = pluginSettings.distribution_title;
					if(distribution_title!=null && !distribution_title.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCTERMS.TITLE, 
								generateLiteralWithLang(distribution_title, valueFactory)));
					}
					
					String distribution_description = pluginSettings.distribution_description;
					if(distribution_description!=null && !distribution_description.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCTERMS.DESCRIPTION, 
								generateLiteralWithLang(distribution_description, valueFactory)));
					}
					
					String distribution_accessUurl = pluginSettings.distribution_accessUurl;
					if(distribution_accessUurl!=null && !distribution_accessUurl.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCAT.ACCESS_URL, 
								valueFactory.createIRI(distribution_accessUurl)));
					}
					
					String distribution_downloadUrl = pluginSettings.distribution_downloadUrl;
					if(distribution_downloadUrl!=null && !distribution_downloadUrl.isEmpty()){
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCAT.DOWNLOAD_URL, 
								valueFactory.createIRI(distribution_downloadUrl)));
					}
					
					String distribution_licence = pluginSettings.distribution_licence;
					IRI distribution_licence_iri = null;
					if(distribution_licence!=null && !distribution_licence.isEmpty()){
						distribution_licence_iri = valueFactory.createIRI(distribution_licence);
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, RDF.TYPE, 
								DCTERMS.LICENSE_DOCUMENT));
						metadataModel.add(valueFactory.createStatement(asset_distribution_iri, DCTERMS.LICENSE, 
								distribution_licence_iri));
					}
					
					String distribution_licence_title = pluginSettings.distribution_licence_title;
					if(distribution_licence_iri !=null && distribution_licence_title!=null && !distribution_licence_title.isEmpty()){
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, DCTERMS.TITLE, 
								generateLiteralWithLang(distribution_licence_title, valueFactory)));
					}
					
					String distribution_licence_description = pluginSettings.distribution_licence_description;
					if(distribution_licence_iri !=null && distribution_licence_description!=null && !distribution_licence_description.isEmpty()){
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, DCTERMS.DESCRIPTION , 
								generateLiteralWithLang(distribution_licence_description, valueFactory)));
					}
					
					String distribution_licence_type = pluginSettings.distribution_licence_type;
					if(distribution_licence_iri !=null && distribution_licence_type!=null && !distribution_licence_type.isEmpty()){
						IRI distribution_licence_type_iri = valueFactory.createIRI(distribution_licence_type);
						metadataModel.add(valueFactory.createStatement(distribution_licence_type_iri, RDF.TYPE, SKOS.CONCEPT));
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, DCTERMS.TYPE, 
								distribution_licence_type_iri));
					}
					
					String distribution_format = pluginSettings.distribution_format;
					if(distribution_format!=null && !distribution_format.isEmpty()){
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, DCTERMS.FORMAT, 
								valueFactory.createIRI(distribution_format)));
					}
					
					String distribution_media_type = pluginSettings.distribution_media_type;
					if(distribution_media_type!=null && !distribution_media_type.isEmpty()){
						metadataModel.add(valueFactory.createStatement(distribution_licence_iri, DCAT.MEDIA_TYPE , 
								valueFactory.createIRI(distribution_media_type)));
					}
					
				}
				/*
				String  = pluginSettings.;
				if(!=null && !.isEmpty()){
					metadataModel.add(valueFactory.createStatement(assetIRI, , 
							valueFactory.createLiteral()));
				}
				*/
				
				StatementCollector stmtCollector = new StatementCollector(metadataModel);
				metadataConnection.export(stmtCollector);
				return metadataModel;
			} catch (STPropertyAccessException e) {
				throw new DatasetMetadataExporterException(e);
			}
		} finally {
			tempMetadataRepository.shutDown();
		}
	}
	
	public Literal generateLiteralWithLang(String inputLiteral, ValueFactory valueFactory){
		Literal literal;
		
		String[] inputSplit = inputLiteral.split("@");
		String label;
		if(inputSplit[0].startsWith("\"") && inputSplit[0].startsWith("\"")){
			label = inputSplit[0].substring(1, inputSplit[0].length()-1);
		} else{
			label = inputSplit[0];
		}
		
		if(inputSplit.length == 2){
			literal = valueFactory.createLiteral(label, inputSplit[1]);
		} else {
			literal = valueFactory.createLiteral(label);
		}
		
		return literal;
	}
	
	public Literal generateLiteralWithType(String inputLiteral, ValueFactory valueFactory){
		Literal literal;
		String[] inputSplit = inputLiteral.split("^^");
		literal = valueFactory.createLiteral(inputSplit[0], valueFactory.createIRI(inputSplit[1]));
		return literal;
	}

}
