package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * A {@link DatasetMetadataExporter} for the <a href="https://www.w3.org/TR/vocab-dcat/">Data Catalog
 * Vocabulary (DCAT)</a>
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 */
public class DCATDatasetMetadataExporter implements DatasetMetadataExporter {

	private final DCATDatasetMetadataExporterFactory factory;

	public DCATDatasetMetadataExporter(DCATDatasetMetadataExporterFactory factory) {
		this.factory = factory;
	}

	@Override
	public Model produceDatasetMetadata(Project project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException {
		Repository tempMetadataRepository = new SailRepository(new MemoryStore());
		tempMetadataRepository.initialize();
		
		ValueFactory valueFactory = conn.getValueFactory();
		try {
			try (RepositoryConnection metadataConnection = tempMetadataRepository.getConnection()) {
				/*DatasetMetadataExporterSettings extensionPointSettings = getExtensionPointProjectSettings(
						project);*/
				DCATDatasetMetadataExporterSettings pluginSettings = factory.getProjectSettings(project);

				Model metadataModel = new LinkedHashModel();
				metadataModel.setNamespace(FOAF.NS);
				metadataModel.setNamespace(DCTERMS.NS);
				metadataModel.setNamespace(DCAT.NS);
				metadataModel.setNamespace(XMLSchema.NS);
				metadataModel.setNamespace(SKOS.NS);
				
				//the dataset part (using the property and values regarding the dataset)
				
				IRI datasetIRI = valueFactory.createIRI(pluginSettings.dataset_iri);
				metadataModel.add(valueFactory.createStatement(datasetIRI, RDF.TYPE, DCAT.DATASET));

				String dataset_title = pluginSettings.dataset_title;
				if(dataset_title!=null && !dataset_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.TITLE, 
							generateLiteralWithLang(dataset_title, valueFactory)));
				}
				
				String dataset_description = pluginSettings.dataset_description;
				if(dataset_description!=null && !dataset_description.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.DESCRIPTION, 
							generateLiteralWithLang(dataset_description, valueFactory)));
				}
				
				String dataset_issued = pluginSettings.dataset_issued;
				if(dataset_issued!=null && !dataset_issued.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.ISSUED, 
							valueFactory.createLiteral(dataset_issued, XMLSchema.DATE)));
				}
				
				String dataset_modified = pluginSettings.dataset_modified;
				if(dataset_modified!=null && !dataset_modified.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.MODIFIED, 
							valueFactory.createLiteral(dataset_modified, XMLSchema.DATE)));
				}
				
				String dataset_identifier = pluginSettings.dataset_identifier;
				if(dataset_identifier!=null && !dataset_identifier.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.IDENTIFIER, 
							valueFactory.createLiteral(dataset_identifier)));
				}
				
				String dataset_keywords = pluginSettings.dataset_keywords;
				if(dataset_keywords!=null && !dataset_keywords.isEmpty()){
					String[] dataset_keywords_array = dataset_keywords.split(";");
					for(String dataset_keywords_single : dataset_keywords_array){
						metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.KEYWORD, 
								valueFactory.createLiteral(dataset_keywords_single)));
					}
				}
				
				String dataset_language = pluginSettings.dataset_language;
				if(dataset_language!=null && !dataset_language.isEmpty()){
					IRI asset_language_iri = valueFactory.createIRI(dataset_language);
					metadataModel.add(valueFactory.createStatement(asset_language_iri, RDF.TYPE, DCTERMS.LINGUISTIC_SYSTEM));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.LANGUAGE, 
							asset_language_iri));
				}
				
				
				String dataset_contact_point = pluginSettings.dataset_contactPoint;
				if(dataset_contact_point!=null && !dataset_contact_point.isEmpty()){
					IRI dataset_contact_point_iri = valueFactory.createIRI(dataset_contact_point);
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.CONTACT_POINT, 
							dataset_contact_point_iri));
					//TODO the dataset_contact_point is a VCARD, so deal with it
				}
				
				String dataset_temporal = pluginSettings.dataset_temporal;
				IRI dataset_temporal_iri = null;
				if(dataset_temporal!=null && !dataset_temporal.isEmpty()){
					dataset_temporal_iri = valueFactory.createIRI(dataset_temporal);
					metadataModel.add(valueFactory.createStatement(dataset_temporal_iri, RDF.TYPE, 
							DCTERMS.PERIOD_OF_TIME));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.TEMPORAL, 
							dataset_temporal_iri));
				}
				
				String dataset_temporal_startDate = pluginSettings.dataset_temporal_startDate;
				if(dataset_temporal_iri!=null && dataset_temporal_startDate!=null && !dataset_temporal_startDate.isEmpty()){
					metadataModel.add(valueFactory.createStatement(dataset_temporal_iri, valueFactory.createIRI("http://schema.org/startDate"), 
							valueFactory.createLiteral(dataset_temporal_startDate, XMLSchema.DATE)));
				}

				String dataset_temporal_endDate = pluginSettings.dataset_temporal_endDate;
				if(dataset_temporal_iri!=null && dataset_temporal_endDate!=null && !dataset_temporal_endDate.isEmpty()){
					metadataModel.add(valueFactory.createStatement(dataset_temporal_iri, valueFactory.createIRI("http://schema.org/endDate"), 
							valueFactory.createLiteral(dataset_temporal_endDate, XMLSchema.DATE)));
				}
				
				String dataset_spatial = pluginSettings.dataset_spatial;
				if(dataset_spatial!=null && !dataset_spatial.isEmpty()){
					IRI dataset_spatial_iri = valueFactory.createIRI(dataset_spatial);
					metadataModel.add(valueFactory.createStatement(dataset_spatial_iri, RDF.TYPE,
							DCTERMS.LOCATION));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.SPATIAL, 
							dataset_spatial_iri));
				}
				
				String dataset_accrual_periodicy = pluginSettings.dataset_accrualPeriodicy;
				if(dataset_accrual_periodicy!=null && !dataset_accrual_periodicy.isEmpty()){
					IRI dataset_accrual_periodicy_iri = valueFactory.createIRI(dataset_accrual_periodicy);
					metadataModel.add(valueFactory.createStatement(dataset_accrual_periodicy_iri, RDF.TYPE,
							DCTERMS.FREQUENCY));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.ACCRUAL_PERIODICITY, 
							dataset_accrual_periodicy_iri));
				}
				
				String dataset_landing_page = pluginSettings.dataset_landingPage;
				if(dataset_landing_page!=null && !dataset_landing_page.isEmpty()){
					IRI dataset_landing_page_iri = valueFactory.createIRI(dataset_landing_page);
					metadataModel.add(valueFactory.createStatement(dataset_landing_page_iri, RDF.TYPE, 
							FOAF.DOCUMENT));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.LANDING_PAGE, 
							dataset_landing_page_iri));
				}
				
				String dataset_theme = pluginSettings.dataset_theme;
				if(dataset_theme!=null && !dataset_theme.isEmpty()){
					IRI dataset_theme_iri = valueFactory.createIRI(dataset_theme);
					metadataModel.add(valueFactory.createStatement(dataset_theme_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.IDENTIFIER, 
							dataset_theme_iri));
				}
				
				String dataset_distribution = pluginSettings.dataset_distribution;
				IRI dataset_distribution_iri = null;
				if(dataset_distribution!=null && !dataset_distribution.isEmpty()){
					dataset_distribution_iri = valueFactory.createIRI(dataset_distribution);
					metadataModel.add(valueFactory.createStatement(dataset_distribution_iri, RDF.TYPE, DCAT.DISTRIBUTION));
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.HAS_DISTRIBUTION, 
							dataset_distribution_iri));
				}
				
				
				//the distribution part (using the property and values regarding the distribution)
				
				/*IRI distributionIRI = valueFactory.createIRI(pluginSettings.distribution_iri);
				metadataModel.add(valueFactory.createStatement(distributionIRI, RDF.TYPE, DCAT.DISTRIBUTION));
				metadataModel.add(valueFactory.createStatement(distributionIRI, DCAT.DISTRIBUTION, distributionIRI));*/
				IRI distributionIRI = dataset_distribution_iri;
				
				
				String distribution_title = pluginSettings.distribution_title;
				if(distribution_title!=null && !distribution_title.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.TITLE, 
							generateLiteralWithLang(distribution_title, valueFactory)));
				}
				
				String distribution_description = pluginSettings.distribution_description;
				if(distribution_description!=null && !distribution_description.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.DESCRIPTION, 
							generateLiteralWithLang(distribution_description, valueFactory)));
				}
				
				String distribution_issued = pluginSettings.distribution_issued;
				if(distribution_issued!=null && !distribution_issued.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.ISSUED, 
							valueFactory.createLiteral(distribution_issued, XMLSchema.DATE)));
				}
				
				String distribution_modified = pluginSettings.distribution_modified;
				if(distribution_modified!=null && !distribution_modified.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.MODIFIED, 
							valueFactory.createLiteral(distribution_modified, XMLSchema.DATE)));
				}
				
				String distribution_licence = pluginSettings.distribution_licence;
				IRI distribution_licence_iri = null;
				if(distribution_licence!=null && !distribution_licence.isEmpty()){
					distribution_licence_iri = valueFactory.createIRI(distribution_licence);
					metadataModel.add(valueFactory.createStatement(distribution_licence_iri, RDF.TYPE, DCTERMS.LICENSE_DOCUMENT));
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.LICENSE, 
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
				
				String distribution_rights = pluginSettings.distribution_rights;
				if(distribution_rights!=null && !distribution_rights.isEmpty()){
					IRI distribution_rights_iri = valueFactory.createIRI(distribution_rights);
					metadataModel.add(valueFactory.createStatement(distribution_rights_iri, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT));
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.RIGHTS, 
							distribution_rights_iri));
				}
				
				String distribution_access_url = pluginSettings.distribution_accessUrl;
				if(distribution_access_url!=null && !distribution_access_url.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCAT.ACCESS_URL, 
							valueFactory.createIRI(distribution_access_url)));
				}
				
				String distribution_download_url = pluginSettings.distribution_downloadUrl;
				if(distribution_download_url!=null && !distribution_download_url.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCAT.DOWNLOAD_URL, 
							valueFactory.createIRI(distribution_download_url)));
				}
				
				String distribution_media_type = pluginSettings.distribution_mediaType;
				if(distribution_media_type!=null && !distribution_media_type.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCAT.MEDIA_TYPE, 
							valueFactory.createIRI(distribution_media_type)));
				}
				
				String distribution_format = pluginSettings.distribution_format;
				if(distribution_format!=null && !distribution_format.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCTERMS.FORMAT, 
							valueFactory.createIRI(distribution_format)));
				}
				
				String distribution_byte_size = pluginSettings.distribution_byteSize;
				if(distribution_byte_size!=null && !distribution_byte_size.isEmpty()){
					metadataModel.add(valueFactory.createStatement(distributionIRI, DCAT.BYTE_SIZE, 
							valueFactory.createLiteral(distribution_byte_size, XMLSchema.DECIMAL)));
				}
				
				/*
				String  = pluginSettings.;
				if(!=null && !.isEmpty()){
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.IDENTIFIER, 
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
