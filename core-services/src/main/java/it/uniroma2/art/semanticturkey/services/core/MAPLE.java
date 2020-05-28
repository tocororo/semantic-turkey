package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.profiler.LIMEProfiler;
import it.uniroma2.art.lime.profiler.ProfilerException;
import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.maple.orchestration.ProfilerOptions;
import it.uniroma2.art.maple.orchestration.ProfilingException;
import it.uniroma2.art.maple.problem.RefinableTaskReport;
import it.uniroma2.art.maple.problem.ResourceLexicalizationSet;
import it.uniroma2.art.maple.problem.SingleResourceMatchingProblem;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.metadata.ProjectMetadataStore;
import it.uniroma2.art.semanticturkey.settings.metadata.StoredProjectMetadata;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides access to the capabilities of <a href="http://art.uniroma2.it/maple/">MAPLE</a>
 * (Mapping Architecture based on Linguistic Evidences).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class MAPLE extends STServiceAdapter {

	@Autowired
	private MediationFramework mediationFramework;

	@Autowired
	private STMetadataRegistryBackend metadataRegistryBackend;

	/**
	 * Profiles the current project and stores its LIME metadata (as a settings using the
	 * {@link ProjectMetadataStore}).
	 * 
	 * @throws ProfilerException
	 * @throws NoSuchSettingsManager
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public void profileProject() throws ProfilerException, NoSuchSettingsManager, IllegalStateException,
			STPropertyAccessException, STPropertyUpdateException {
		SailRepository metadataRepository = new SailRepository(new MemoryStore());
		metadataRepository.init();
		try {
			try (SailRepositoryConnection metadataConnection = metadataRepository.getConnection()) {
				ValueFactory vf = metadataConnection.getValueFactory();
				IRI metadataBaseURI = metadataConnection.getValueFactory()
						.createIRI("http://example.org/" + UUID.randomUUID().toString() + "/void.ttl");

				metadataConnection.setNamespace("", metadataBaseURI.stringValue() + "#");
				metadataConnection.setNamespace(VOID.PREFIX, VOID.NAMESPACE);
				metadataConnection.setNamespace(LIME.PREFIX, LIME.NAMESPACE);
				metadataConnection.setNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
				metadataConnection.setNamespace(XMLSchema.PREFIX, XMLSchema.NAMESPACE);

				LIMEProfiler profiler = new LIMEProfiler(metadataConnection, metadataBaseURI,
						getManagedConnection(), (IRI) getWorkingGraph());
				profiler.profile();

				IRI datasetIRI = Models
						.objectIRI(QueryResults.asModel(
								metadataConnection.getStatements(metadataBaseURI, FOAF.PRIMARY_TOPIC, null)))
						.orElseThrow(() -> new ProfilerException(
								"Could not identigy the main dataset inside the metadata"));

				Project project = getProject();

				metadataConnection.add(datasetIRI, VOID.URI_SPACE,
						vf.createLiteral(project.getDefaultNamespace()));

				RepositoryImplConfig coreRepoImplConfig = STLocalRepositoryManager
						.getUnfoldedRepositoryImplConfig(
								project.getRepositoryManager().getRepositoryConfig(Project.CORE_REPOSITORY));
				if (coreRepoImplConfig instanceof HTTPRepositoryConfig) {
					IRI sparqlEndpoint = SimpleValueFactory.getInstance()
							.createIRI(((HTTPRepositoryConfig) coreRepoImplConfig).getURL());
					metadataConnection.add(datasetIRI, VOID.SPARQL_ENDPOINT, sparqlEndpoint);
				}

				metadataConnection.add(datasetIRI, DCTERMS.TITLE, vf.createLiteral(project.getName()));

				String description = project.getDescription();
				if (StringUtils.isNoneBlank(description)) {
					metadataConnection.add(datasetIRI, DCTERMS.DESCRIPTION, vf.createLiteral(description));
				}

				StringWriter stringWriter = new StringWriter();
				RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, stringWriter);
				metadataConnection.export(rdfWriter);

				SettingsManager settingsManager = exptManager
						.getSettingsManager(ProjectMetadataStore.class.getName());
				StoredProjectMetadata settings = (StoredProjectMetadata) settingsManager
						.getSettings(getProject(), UsersManager.getLoggedUser(), Scope.PROJECT);
				settings.datasetDescription = new it.uniroma2.art.semanticturkey.properties.Pair<>(datasetIRI,
						stringWriter.toString().replace("\r\n", "\n").replace("\t", "  "));
				settingsManager.storeSettings(getProject(), UsersManager.getLoggedUser(), Scope.PROJECT,
						settings);

				// updates the metadata stored in the metadata registry
				metadataRegistryBackend.registerProject(project);
			}
		} finally {
			metadataRepository.shutDown();
		}
	}

	/**
	 * Determines whether LIME metadata for the current project are available (via the settings manager
	 * {@link ProjectMetadataStore})
	 * 
	 * @return
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws IllegalStateException
	 */
	@STServiceOperation
	public Boolean checkProjectMetadataAvailability()
			throws NoSuchSettingsManager, IllegalStateException, STPropertyAccessException {
		SettingsManager settingsManager = exptManager
				.getSettingsManager(ProjectMetadataStore.class.getName());
		StoredProjectMetadata settings = (StoredProjectMetadata) settingsManager.getSettings(getProject(),
				UsersManager.getLoggedUser(), Scope.PROJECT);
		return STPropertiesChecker.getModelConfigurationChecker(settings).isValid();
	}

	/**
	 * Profiles a matching problem between two datasets described in the metadata registry.
	 * 
	 * @param sourceDataset
	 * @param targetDataset
	 * @param options
	 * @return
	 * @throws ProfilingException
	 */
	@STServiceOperation
	public RefinableTaskReport profileMatchingProblem(IRI sourceDataset, IRI targetDataset,
			@JsonSerialized @Optional(defaultValue = "{}") ProfilerOptions options)
			throws ProfilingException {

		try (RepositoryConnection metadataConn = metadataRegistryBackend.getConnection()) {
			return mediationFramework.profileProblem(metadataConn, sourceDataset, targetDataset, options);
		}
	}

	/**
	 * Profiles a matching problem between two datasets associated with local projects.
	 * 
	 * @param sourceDataset
	 * @param targetDataset
	 * @return
	 * @throws ProfilingException
	 */
	@STServiceOperation
	public RefinableTaskReport profileMatchingProblemBetweenProjects(Project leftDataset,
			Project rightDataset, @JsonSerialized @Optional(defaultValue = "{}") ProfilerOptions options)
			throws ProfilingException {

		IRI leftDatasetIRI = metadataRegistryBackend.findDatasetForProject(leftDataset);
		IRI rightDatasetIRI = metadataRegistryBackend.findDatasetForProject(rightDataset);

		try (RepositoryConnection metadataConn = metadataRegistryBackend.getConnection()) {
			return mediationFramework.profileProblem(metadataConn, leftDatasetIRI, rightDatasetIRI);
		}
	}

	/**
	 * Profiles the problem of matching the provided resource in the current project against the provided
	 * resource position (i.e. another local project or remote dataset).
	 * 
	 * @param sourceResource
	 * @param targetPosition
	 * @return
	 * @throws AssessmentException
	 * @throws IllegalArgumentException
	 * @throws RepositoryException
	 * @throws ProfilingException
	 * @throws ForbiddenProjectAccessException
	 * @throws RDFParseException
	 * @throws UnsupportedRDFormatException
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws STPropertyAccessException
	 * @throws NoSuchSettingsManager
	 * @throws InvalidConfigurationException
	 */
	@STServiceOperation
	@Read
	public SingleResourceMatchingProblem profileSingleResourceMatchProblem(IRI sourceResource,
			ResourcePosition targetPosition) throws AssessmentException, RDFParseException,
			RepositoryException, UnsupportedRDFormatException, IllegalStateException,
			IllegalArgumentException, IOException, STPropertyAccessException, NoSuchSettingsManager,
			ForbiddenProjectAccessException, InvalidConfigurationException, ProfilingException {

		Pair<IRI, Model> targetDatasetDescription = getDatasetDescriptionFromResourcePosition(targetPosition);

		List<ResourceLexicalizationSet> resourceLexicalizationSets = mediationFramework
				.discoverLexicalizationSetsForResource(getManagedConnection(), sourceResource);
		return mediationFramework.profileSingleResourceMatchingProblem(sourceResource,
				resourceLexicalizationSets, targetDatasetDescription.getKey(),
				targetDatasetDescription.getValue());
	}

	protected Pair<IRI, Model> getDatasetDescriptionFromResourcePosition(ResourcePosition targetPosition)
			throws RDFParseException, UnsupportedRDFormatException, IOException, IllegalStateException,
			STPropertyAccessException, NoSuchSettingsManager, ForbiddenProjectAccessException,
			InvalidConfigurationException, RepositoryException, IllegalArgumentException {
		// The variables below will be initialized differently depending on the kind of target
		Pair<IRI, Model> targetDatasetDecription;

		if (targetPosition instanceof LocalResourcePosition) { // local project
			targetDatasetDecription = getProjectMetadata(
					((LocalResourcePosition) targetPosition).getProject());
		} else if (targetPosition instanceof RemoteResourcePosition) { // remote dataset
			// Extracts metadata from the metadata registry
			it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata targetDatasetMetadata = ((RemoteResourcePosition) targetPosition)
					.getDatasetMetadata();

			try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
				Model targetDatasetProfile = metadataRegistryBackend
						.extractProfile(targetDatasetMetadata.getIdentity());

				targetDatasetDecription = ImmutablePair.of(targetDatasetMetadata.getIdentity(),
						targetDatasetProfile);
			}

		} else {
			throw new IllegalArgumentException("Unsupported resource position");
		}
		return targetDatasetDecription;
	}

	protected Pair<IRI, Model> getProjectMetadata(Project project) throws RDFParseException,
			UnsupportedRDFormatException, IOException, IllegalStateException, STPropertyAccessException,
			NoSuchSettingsManager, ForbiddenProjectAccessException, InvalidConfigurationException {
		// Checks accessibility in case of another local project
		if (!project.equals(getProject())) {
			AccessResponse accessResponse = ProjectManager.checkAccessibility(getProject(), project,
					AccessLevel.R, LockLevel.NO);
			if (!accessResponse.isAffirmative()) {
				throw new ForbiddenProjectAccessException(accessResponse.getMsg());
			}
		}
		StoredProjectMetadata storedProjectMetadata = (StoredProjectMetadata) exptManager.getSettings(project,
				UsersManager.getLoggedUser(), ProjectMetadataStore.class.getName(), Scope.PROJECT);
		STPropertiesChecker settingsManager = STPropertiesChecker
				.getModelConfigurationChecker(storedProjectMetadata);
		if (!settingsManager.isValid()) {
			throw new InvalidConfigurationException(
					"Project: " + project.getName() + ", Error: " + settingsManager.getErrorMessage());
		}

		return Pair.of(storedProjectMetadata.datasetDescription.getFirst(),
				Rio.parse(new StringReader(storedProjectMetadata.datasetDescription.getSecond()), "",
						RDFFormat.TURTLE));

	}
}
