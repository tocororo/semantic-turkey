package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.dcatap;

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
 * A {@link DatasetMetadataExporter} for the
 * <a href="https://joinup.ec.europa.eu/asset/dcat_application_profile/asset_release/dcat-ap-v11"> Data
 * Catalog Vocabulary (DCAT)</a> (<a href="https://joinup.ec.europa.eu/system/files/project/dcat-ap_.1.bmp">
 * bmp</a>)
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati </a>
 */
public class DCATAPDatasetMetadataExporter implements DatasetMetadataExporter {

	private final DCATAPDatasetMetadataExporterFactory factory;

	public DCATAPDatasetMetadataExporter(DCATAPDatasetMetadataExporterFactory factory) {
		this.factory = factory;
	}

	@Override
	public Model produceDatasetMetadata(Project project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException {
		Repository tempMetadataRepository = new SailRepository(new MemoryStore());
		tempMetadataRepository.init();

		ValueFactory valueFactory = conn.getValueFactory();
		try {
			try (RepositoryConnection metadataConnection = tempMetadataRepository.getConnection()) {
				DCATAPDatasetMetadataExporterSettings pluginSettings = factory.getProjectSettings(project);

				Model metadataModel = new LinkedHashModel();
				metadataModel.setNamespace(FOAF.NS);
				metadataModel.setNamespace(DCTERMS.NS);
				metadataModel.setNamespace(DCAT.NS);
				metadataModel.setNamespace(XMLSchema.NS);
				metadataModel.setNamespace(SKOS.NS);

				// First create the mandatory and recommended classes

				IRI catalogueIRI = valueFactory.createIRI(pluginSettings.catalogue_iri);
				metadataModel.add(valueFactory.createStatement(catalogueIRI, RDF.TYPE, DCAT.CATALOG));

				IRI datasetIRI = valueFactory.createIRI(pluginSettings.dataset_iri);
				metadataModel.add(valueFactory.createStatement(datasetIRI, RDF.TYPE, DCAT.DATASET));

				// now add the property

				/* CATALOGUE */
				metadataModel.add(valueFactory.createStatement(catalogueIRI, DCAT.HAS_DATASET, datasetIRI));

				String catalogue_description = pluginSettings.catalogue_description;
				metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.DESCRIPTION,
						generateLiteralWithLang(catalogue_description, valueFactory)));

				// Agent for catalogue
				IRI catalogueAgentIRI = valueFactory.createIRI(pluginSettings.catalogue_publisherAgentIRI);
				metadataModel.add(valueFactory.createStatement(catalogueAgentIRI, RDF.TYPE, FOAF.AGENT));
				String catalogue_publisherAgentName = pluginSettings.catalogue_publisherAgentName;
				metadataModel.add(valueFactory.createStatement(catalogueAgentIRI, FOAF.NAME,
						generateLiteralWithLang(catalogue_publisherAgentName, valueFactory)));
				String catalogue_publisherAgentType = pluginSettings.catalogue_publisherAgentType;
				if (catalogue_publisherAgentType != null && !catalogue_publisherAgentType.isEmpty()) {
					IRI agent_type_iri = valueFactory.createIRI(catalogue_publisherAgentType);
					metadataModel.add(valueFactory.createStatement(agent_type_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(
							valueFactory.createStatement(catalogueAgentIRI, DCTERMS.TYPE, agent_type_iri));
				}

				metadataModel.add(
						valueFactory.createStatement(catalogueIRI, DCTERMS.PUBLISHER, catalogueAgentIRI));

				String catalogue_title = pluginSettings.catalogue_title;
				metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.TITLE,
						generateLiteralWithLang(catalogue_title, valueFactory)));

				String catalogue_homepage = pluginSettings.catalogue_homepage;
				if (catalogue_homepage != null && !catalogue_homepage.isEmpty()) {
					IRI catalogue_homepage_iri = valueFactory.createIRI(catalogue_homepage);
					metadataModel.add(
							valueFactory.createStatement(catalogue_homepage_iri, RDF.TYPE, FOAF.DOCUMENT));
					metadataModel.add(
							valueFactory.createStatement(catalogueIRI, DCTERMS.TYPE, catalogue_homepage_iri));
				}

				String catalogue_language = pluginSettings.catalogue_language;
				if (catalogue_language != null && !catalogue_language.isEmpty()) {
					IRI catalogue_language_iri = valueFactory.createIRI(catalogue_language);
					metadataModel.add(valueFactory.createStatement(catalogue_language_iri, RDF.TYPE,
							DCTERMS.LINGUISTIC_SYSTEM));
					metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.LANGUAGE,
							catalogue_language_iri));
				}

				String catalogue_licence = pluginSettings.catalogue_licence;
				if (catalogue_licence != null && !catalogue_licence.isEmpty()) {
					IRI catalogue_licence_iri = valueFactory.createIRI(catalogue_licence);
					metadataModel.add(
							valueFactory.createStatement(catalogue_licence_iri, RDF.TYPE, DCTERMS.LICENSE));
					metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.LANGUAGE,
							catalogue_licence_iri));
				}

				String catalogue_releaseDate = pluginSettings.catalogue_releaseDate;
				if (catalogue_releaseDate != null && !catalogue_releaseDate.isEmpty()) {
					metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.ISSUED,
							valueFactory.createLiteral(catalogue_releaseDate, XMLSchema.DATE)));
				}

				String catalogue_themes = pluginSettings.catalogue_themes;
				if (catalogue_language != null && !catalogue_language.isEmpty()) {
					IRI catalogue_themes_iri = valueFactory.createIRI(catalogue_themes);
					metadataModel.add(valueFactory.createStatement(catalogue_themes_iri, RDF.TYPE,
							SKOS.CONCEPT_SCHEME));
					metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.LANGUAGE,
							catalogue_themes_iri));
				}

				String catalogue_modificationDate = pluginSettings.catalogue_modificationDate;
				if (catalogue_modificationDate != null && !catalogue_modificationDate.isEmpty()) {
					metadataModel.add(valueFactory.createStatement(catalogueIRI, DCTERMS.ISSUED,
							valueFactory.createLiteral(catalogue_modificationDate, XMLSchema.DATE)));
				}

				/* DATASET */
				String dataset_description = pluginSettings.dataset_description;
				metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.DESCRIPTION,
						generateLiteralWithLang(dataset_description, valueFactory)));

				String dataset_title = pluginSettings.dataset_title;
				metadataModel.add(valueFactory.createStatement(datasetIRI, DCTERMS.DESCRIPTION,
						generateLiteralWithLang(dataset_title, valueFactory)));

				// distribution for dataset
				IRI datasetDistributionIRI = valueFactory.createIRI(pluginSettings.dataset_distributionIRI);
				metadataModel.add(
						valueFactory.createStatement(datasetDistributionIRI, RDF.TYPE, DCAT.DISTRIBUTION));
				metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.HAS_DISTRIBUTION,
						datasetDistributionIRI));
				IRI dataset_distributionAccessUrl = valueFactory
						.createIRI(pluginSettings.dataset_distributionAccessUrl);
				metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.ACCESS_URL,
						dataset_distributionAccessUrl));
				String dataset_distributionDescription = pluginSettings.dataset_distributionDescription;
				if (dataset_distributionDescription != null && !dataset_distributionDescription.isEmpty()) {
					metadataModel
							.add(valueFactory.createStatement(datasetDistributionIRI, DCTERMS.DESCRIPTION,
									generateLiteralWithLang(dataset_distributionDescription, valueFactory)));
				}
				String dataset_distributionFormat = pluginSettings.dataset_distributionFormat;
				if (dataset_distributionFormat != null && !dataset_distributionFormat.isEmpty()) {
					IRI dataset_distributionFormat_iri = valueFactory.createIRI(dataset_distributionFormat);
					metadataModel.add(valueFactory.createStatement(dataset_distributionFormat_iri, RDF.TYPE,
							DCTERMS.MEDIA_TYPE_OR_EXTENT));
					metadataModel.add(valueFactory.createStatement(datasetDistributionIRI, DCTERMS.FORMAT,
							dataset_distributionFormat_iri));
				}
				String dataset_distributionLicence = pluginSettings.dataset_distributionLicence;
				if (dataset_distributionLicence != null && !dataset_distributionLicence.isEmpty()) {
					IRI dataset_distributionLicence_iri = valueFactory.createIRI(dataset_distributionLicence);
					metadataModel.add(valueFactory.createStatement(dataset_distributionLicence_iri, RDF.TYPE,
							DCTERMS.LICENSE_DOCUMENT));
					metadataModel.add(valueFactory.createStatement(datasetDistributionIRI, DCTERMS.LICENSE,
							dataset_distributionLicence_iri));
				}

				String dataset_keyword = pluginSettings.dataset_keyword;
				if (dataset_keyword != null && !dataset_keyword.isEmpty()) {
					metadataModel.add(valueFactory.createStatement(datasetIRI, DCAT.KEYWORD,
							generateLiteralWithLang(dataset_keyword, valueFactory)));
				}

				// Agent for dataset
				IRI dataset_publisherAgentIRI = valueFactory
						.createIRI(pluginSettings.dataset_publisherAgentIRI);
				metadataModel
						.add(valueFactory.createStatement(dataset_publisherAgentIRI, RDF.TYPE, FOAF.AGENT));
				String dataset_publisherAgentName = pluginSettings.catalogue_publisherAgentName;
				metadataModel.add(valueFactory.createStatement(catalogueAgentIRI, FOAF.NAME,
						generateLiteralWithLang(dataset_publisherAgentName, valueFactory)));
				String dataset_publisherAgentType = pluginSettings.catalogue_publisherAgentType;
				if (dataset_publisherAgentType != null && !dataset_publisherAgentType.isEmpty()) {
					IRI agent_type_iri = valueFactory.createIRI(dataset_publisherAgentType);
					metadataModel.add(valueFactory.createStatement(agent_type_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel.add(
							valueFactory.createStatement(catalogueAgentIRI, DCTERMS.TYPE, agent_type_iri));
				}

				String dataset_theme = pluginSettings.dataset_theme;
				if (dataset_theme != null && !dataset_theme.isEmpty()) {
					IRI dataset_theme_iri = valueFactory.createIRI(dataset_theme);
					metadataModel
							.add(valueFactory.createStatement(dataset_theme_iri, RDF.TYPE, SKOS.CONCEPT));
					metadataModel
							.add(valueFactory.createStatement(datasetIRI, DCAT.THEME, dataset_theme_iri));
				}

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

	public Literal generateLiteralWithLang(String inputLiteral, ValueFactory valueFactory) {
		Literal literal;

		String[] inputSplit = inputLiteral.split("@");
		String label;
		if (inputSplit[0].startsWith("\"") && inputSplit[0].startsWith("\"")) {
			label = inputSplit[0].substring(1, inputSplit[0].length() - 1);
		} else {
			label = inputSplit[0];
		}

		if (inputSplit.length == 2) {
			literal = valueFactory.createLiteral(label, inputSplit[1]);
		} else {
			literal = valueFactory.createLiteral(label);
		}

		return literal;
	}

	public Literal generateLiteralWithType(String inputLiteral, ValueFactory valueFactory) {
		Literal literal;
		String[] inputSplit = inputLiteral.split("^^");
		literal = valueFactory.createLiteral(inputSplit[0], valueFactory.createIRI(inputSplit[1]));
		return literal;
	}

}
