package it.uniroma2.art.semanticturkey.mdr.bindings.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Stream;

import it.uniroma2.art.semanticturkey.mdr.core.ConcreteDatasetSpecification;
import it.uniroma2.art.semanticturkey.mdr.core.Distribution;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.STMETADATAREGISTRY;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryCreationException;
import it.uniroma2.art.semanticturkey.mdr.core.impl.MetadataRegistryBackendImpl;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.settings.metadata.ProjectMetadataStore;
import it.uniroma2.art.semanticturkey.settings.metadata.StoredProjectMetadata;

/**
 * Implementation of {@link STMetadataRegistryBackend}.
 */
public class STMetadataRegistryBackendImpl extends MetadataRegistryBackendImpl
		implements STMetadataRegistryBackend {

	private static final Logger logger = LoggerFactory.getLogger(STMetadataRegistryBackendImpl.class);

	private ExtensionPointManager exptManager;

	public STMetadataRegistryBackendImpl(File baseDir, MediationFramework mediationFramework,
			ExtensionPointManager exptManager) throws MetadataRegistryCreationException {
		super(Config.getDataDir(), mediationFramework);
		this.exptManager = exptManager;
	}

	@Override
	public IRI findDatasetForProject(Project project) {
		return findDatasetForProject(project, false);
	}

	@Override
	public IRI findDatasetForProject(Project project, boolean useDistribution) {
		try (RepositoryConnection conn = getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI projectCtx = MetadataRegistryBackendImpl.computeProjectContext(project.getName(), vf);

			TupleQuery query = conn.prepareTupleQuery(
					//@formatter:off
					"PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
					"SELECT ?dataset WHERE {\n" +
					"  GRAPH " + RenderUtils.toSPARQL(projectCtx) + " {\n" +
					"    ?catalog dcat:record [ foaf:primaryTopic" + (useDistribution ? "/dcat:distribution" : "") + " ?dataset ] . \n" +
					"    FILTER(isIRI(?dataset))\n" +
					"  }\n" +
					"}\n" +
					"LIMIT 1\n"
					//@formatter:on
			);
			return QueryResults.stream(query.evaluate()).map(bs -> (IRI) bs.getValue("dataset")).findAny()
					.orElse(null);
		}
	}
	@Override
	public Project findProjectForDataset(IRI dataset) {
		return findProjectForDataset(dataset, false);
	}

	@Override
	public Project findProjectForDataset(IRI dataset, boolean allowSubset) {
		try (RepositoryConnection conn = getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(
			//@formatter:off
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
				"PREFIX void: <http://rdfs.org/ns/void#>\n" +
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"SELECT ?graph WHERE {\n" +
				"  GRAPH ?graph {\n" +
				"    ?dataset " + (allowSubset ? "^void:subset*/" : "") + "^dcat:distribution?/^foaf:primaryTopic/^dcat:record ?catalog . \n" +
				"  }\n" +
				"}\n" +
				"LIMIT 1\n"
				//@formatter:on
			);
			query.setBinding("dataset", dataset);

			return QueryResults.stream(query.evaluate()).map(bs -> (IRI) bs.getValue("graph"))
					.filter(IRI.class::isInstance).flatMap(ctx -> {
						try {
							return Stream.of(ProjectManager.getProject(((IRI) ctx).getLocalName(), true));
						} catch (ProjectAccessException | InvalidProjectNameException
								| ProjectInexistentException e) {
							return Stream.empty();
						}
					}).findAny().orElse(null);
		}
	}

	@Override
	public synchronized void registerProject(Project project) {
		try {
			StoredProjectMetadata settings = (StoredProjectMetadata) exptManager.getSettings(project,null,
					null , ProjectMetadataStore.class.getName(), Scope.PROJECT);

			if (!STPropertiesChecker.getModelConfigurationChecker(settings).isValid()) {
				settings = null;
			}

			try (RepositoryConnection metadataConn = getConnection()) {
				ValueFactory vf = metadataConn.getValueFactory();

				IRI projectCtx = computeProjectContext(project.getName(), vf);

				metadataConn.clear(projectCtx);

				if (settings != null) {

					IRI dataset = settings.datasetDescription.getFirst();
					Model datasetDescription = Rio.parse(
							new StringReader(settings.datasetDescription.getSecond()), "", RDFFormat.TURTLE);

					Distribution distributionSpecification = new Distribution(
						dataset,
						STMETADATAREGISTRY.PROJECT,
						Models.getPropertyIRI(datasetDescription, dataset, VOID.SPARQL_ENDPOINT).orElse(null),
						project.getName()
					);

					ConcreteDatasetSpecification datasetSpecification = new ConcreteDatasetSpecification(
							null,
							Models.getPropertyString(datasetDescription, dataset, VOID.URI_SPACE).orElse(null),
					Models.getPropertyLiteral(datasetDescription, dataset, DCTERMS.TITLE).orElse(null),
					Models.getPropertyLiteral(datasetDescription, dataset, DCTERMS.DESCRIPTION).orElse(null),
					null,
							distributionSpecification
							);


					createDataset(dataset.getLocalName(), datasetSpecification, null, null, projectCtx, false, false);

					metadataConn.add(datasetDescription, projectCtx);
				}
			}
		} catch (IllegalStateException | STPropertyAccessException | NoSuchSettingsManager | RDFParseException | UnsupportedRDFormatException | IOException | MetadataRegistryWritingException e) {
			logger.error("unable to register project '" + project.getName() + "'", e);
		}

		// there is no need to invoke saveFile() since we don't want to persist the metadata about projects
		// inside the metadata registry
	}

	@Override
	public synchronized void unregisterProject(Project project) {
		try (RepositoryConnection metadataConn = metadataRegistry.getConnection()) {
			ValueFactory vf = metadataConn.getValueFactory();

			IRI projectCtx = computeProjectContext(project.getName(), vf);
			metadataConn.clear(projectCtx);
		}

		// there is no need to invoke saveFile() since we don't want to persist the metadata about projects
		// inside the metadata registry

	}

}