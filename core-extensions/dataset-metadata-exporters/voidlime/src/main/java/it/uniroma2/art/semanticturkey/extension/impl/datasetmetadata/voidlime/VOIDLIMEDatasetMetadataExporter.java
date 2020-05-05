package it.uniroma2.art.semanticturkey.extension.impl.datasetmetadata.voidlime;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.lime.model.repo.LIMERepositoryConnectionWrapper;
import it.uniroma2.art.lime.model.repo.LIMERepositoryWrapper;
import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.profiler.LIMEProfiler;
import it.uniroma2.art.lime.profiler.ProfilerException;
import it.uniroma2.art.lime.profiler.ProfilerOptions;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * A {@link DatasetMetadataExporter} for the
 * <a href="https://www.w3.org/2016/05/ontolex/#metadata-lime">Linguistic Metadata vocabulary (LIME)</a> and
 * the <a href="https://www.w3.org/TR/void/">Vocabulary of Interlinked Datasets (VoID)</a>
 */
public class VOIDLIMEDatasetMetadataExporter implements DatasetMetadataExporter {

	private final VOIDLIMEDatasetMetadataExporterFactory factory;

	public VOIDLIMEDatasetMetadataExporter(VOIDLIMEDatasetMetadataExporterFactory factory) {
		this.factory = factory;
	}

	@Override
	public Model produceDatasetMetadata(Project project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException, STPropertyAccessException {
		LIMERepositoryWrapper tempMetadataRepository = new LIMERepositoryWrapper(
				new SailRepository(new MemoryStore()));
		tempMetadataRepository.initialize();
		try {
			try (LIMERepositoryConnectionWrapper metadataConnection = tempMetadataRepository
					.getConnection()) {
				// DatasetMetadataExporterSettings extensionPointSettings = getExtensionPointProjectSettings(
				// project);
				VOIDLIMEDatasetMetadataExporterSettings pluginSettings = factory.getProjectSettings(project);

				IRI datasetDescriptionBaseURI = SimpleValueFactory.getInstance()
						.createIRI(pluginSettings.dataset_description_baseUri);
				LIMEProfiler profiler = new LIMEProfiler(metadataConnection, datasetDescriptionBaseURI, conn,
						dataGraph);
				ProfilerOptions options = new ProfilerOptions();
				options.setMainDatasetName(pluginSettings.dataset_localName);

				try {
					profiler.profile(options);
				} catch (ProfilerException e) {
					throw new DatasetMetadataExporterException(e);

				}

				IRI datasetIRI = (IRI) metadataConnection.getMainDataset(false).get();

				if (pluginSettings.dataset_title != null) {
					Literal dataset_title = parseLiteral(pluginSettings.dataset_title);
					metadataConnection.add(datasetIRI, DCTERMS.TITLE, dataset_title);
					metadataConnection.add(datasetIRI, RDFS.LABEL, dataset_title);
				}

				if (pluginSettings.dataset_description != null) {
					Literal dataset_description = parseLiteral(pluginSettings.dataset_description);
					metadataConnection.add(datasetIRI, DCTERMS.DESCRIPTION, dataset_description);
				}

				if (pluginSettings.dataset_homePage != null) {
					metadataConnection.add(datasetIRI, FOAF.HOMEPAGE,
							metadataConnection.getValueFactory().createIRI(pluginSettings.dataset_homePage));
				}

				if (pluginSettings.dataset_homePage != null) {
					metadataConnection.add(datasetIRI, FOAF.HOMEPAGE,
							metadataConnection.getValueFactory().createIRI(pluginSettings.dataset_homePage));
				}

				if (pluginSettings.dataset_creators != null) {
					for (IRI creator : parseCommaSeparatedIRIs(pluginSettings.dataset_creators)) {
						metadataConnection.add(datasetIRI, DCTERMS.CREATOR, creator);
					}
				}

				if (pluginSettings.dataset_publisher != null) {
					metadataConnection.add(datasetIRI, DCTERMS.PUBLISHER,
							metadataConnection.getValueFactory().createIRI(pluginSettings.dataset_publisher));
				}

				if (pluginSettings.dataset_contributors != null) {
					for (IRI contributor : parseCommaSeparatedIRIs(pluginSettings.dataset_contributors)) {
						metadataConnection.add(datasetIRI, DCTERMS.CONTRIBUTOR, contributor);
					}
				}

				if (pluginSettings.dataset_source != null) {
					for (IRI source : parseCommaSeparatedIRIs(pluginSettings.dataset_source)) {
						metadataConnection.add(datasetIRI, DCTERMS.SOURCE, source);
					}
				}

				if (pluginSettings.dataset_date != null) {
					XMLGregorianCalendar dataset_date = DatatypeFactory.newInstance()
							.newXMLGregorianCalendar(pluginSettings.dataset_date);
					metadataConnection.add(datasetIRI, DCTERMS.DATE,
							metadataConnection.getValueFactory().createLiteral(dataset_date));
				}

				if (pluginSettings.dataset_created != null) {
					XMLGregorianCalendar dataset_created = DatatypeFactory.newInstance()
							.newXMLGregorianCalendar(pluginSettings.dataset_created);
					metadataConnection.add(datasetIRI, DCTERMS.CREATED,
							metadataConnection.getValueFactory().createLiteral(dataset_created));
				}

				if (pluginSettings.dataset_issued != null) {
					XMLGregorianCalendar dataset_issued = DatatypeFactory.newInstance()
							.newXMLGregorianCalendar(pluginSettings.dataset_issued);
					metadataConnection.add(datasetIRI, DCTERMS.ISSUED,
							metadataConnection.getValueFactory().createLiteral(dataset_issued));
				}

				if (pluginSettings.dataset_modified != null) {
					XMLGregorianCalendar dataset_modified = DatatypeFactory.newInstance()
							.newXMLGregorianCalendar(pluginSettings.dataset_modified);
					metadataConnection.add(datasetIRI, DCTERMS.MODIFIED,
							metadataConnection.getValueFactory().createLiteral(dataset_modified));
				}

				if (pluginSettings.dataset_license != null) {
					metadataConnection.add(datasetIRI, DCTERMS.LICENSE,
							metadataConnection.getValueFactory().createIRI(pluginSettings.dataset_license));
				}

				if (pluginSettings.dataset_subjects != null) {
					for (IRI subj : parseCommaSeparatedIRIs(pluginSettings.dataset_subjects)) {
						metadataConnection.add(datasetIRI, DCTERMS.SUBJECT, subj);
					}
				}

				if (pluginSettings.dataset_features != null) {
					for (IRI feat : parseCommaSeparatedIRIs(pluginSettings.dataset_features)) {
						metadataConnection.add(datasetIRI, VOID.FEATURE, feat);
					}
				}

				if (pluginSettings.dataset_dataDumps != null) {
					for (IRI dump : parseCommaSeparatedIRIs(pluginSettings.dataset_dataDumps)) {
						metadataConnection.add(datasetIRI, VOID.DATA_DUMP, dump);
					}
				}

				if (pluginSettings.dataset_sparqlEndpoint != null) {
					metadataConnection.add(datasetIRI, VOID.SPARQL_ENDPOINT, metadataConnection
							.getValueFactory().createIRI(pluginSettings.dataset_sparqlEndpoint));
				}

				if (pluginSettings.dataset_uriLookupEndpoint != null) {
					metadataConnection.add(datasetIRI, VOID.URI_LOOKUP_ENDPOINT, metadataConnection
							.getValueFactory().createIRI(pluginSettings.dataset_uriLookupEndpoint));
				}

				if (pluginSettings.dataset_openSearchDescription != null) {
					metadataConnection.add(datasetIRI, VOID.OPEN_SEARCH_DESCRIPTION, metadataConnection
							.getValueFactory().createIRI(pluginSettings.dataset_openSearchDescription));
				}

				if (pluginSettings.dataset_uriSpace != null) {
					metadataConnection.add(datasetIRI, VOID.URI_SPACE, metadataConnection.getValueFactory()
							.createLiteral(pluginSettings.dataset_uriSpace));
				}

				if (pluginSettings.dataset_exampleResources != null) {
					for (IRI ex : parseCommaSeparatedIRIs(pluginSettings.dataset_exampleResources)) {
						metadataConnection.add(datasetIRI, VOID.EXAMPLE_RESOURCE, ex);
					}
				}

				if (pluginSettings.dataset_rootResources != null) {
					for (IRI root : parseCommaSeparatedIRIs(pluginSettings.dataset_rootResources)) {
						metadataConnection.add(datasetIRI, VOID.ROOT_RESOURCE, root);
					}
				}

				Model metadataModel = new LinkedHashModel();
				metadataModel.setNamespace(RDFS.NS);
				metadataModel.setNamespace(XMLSchema.NS);
				metadataModel.setNamespace(VOID.NS);
				metadataModel.setNamespace(LIME.NS);
				metadataModel.setNamespace(FOAF.NS);
				metadataModel.setNamespace(DCTERMS.NS);

				StatementCollector stmtCollector = new StatementCollector(metadataModel);

				metadataConnection.export(stmtCollector);
				return metadataModel;
			} catch (DatatypeConfigurationException e) {
				throw new DatasetMetadataExporterException(e);
			}
		} finally {
			tempMetadataRepository.shutDown();
		}
	}

	protected static List<IRI> parseCommaSeparatedIRIs(String values) {
		return Arrays.stream(values.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.map(s -> SimpleValueFactory.getInstance().createIRI(s)).collect(Collectors.toList());
	}

	protected static Literal parseLiteral(String literal) {
		literal = literal.trim();

		if (literal.startsWith("\"")) {
			return NTriplesUtil.parseLiteral(literal, SimpleValueFactory.getInstance());
		} else {
			return SimpleValueFactory.getInstance().createLiteral(literal);
		}
	}
}
