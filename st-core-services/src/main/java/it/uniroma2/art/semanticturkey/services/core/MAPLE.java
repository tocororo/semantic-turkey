package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.maple.orchestration.ProfilingException;
import it.uniroma2.art.maple.problem.MediationProblem;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
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
	private MetadataRegistryBackend metadataRegistryBackend;

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
		metadataRepository.initialize();
		try {
			try (SailRepositoryConnection metadataConnection = metadataRepository.getConnection()) {
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
	 * Profiles a mediation problem.
	 * 
	 * @param resourcePosition
	 * @return
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
	public MediationProblem profileMediationProblem(ResourcePosition resourcePosition)
			throws ProfilingException, ForbiddenProjectAccessException, RDFParseException,
			UnsupportedRDFormatException, IllegalStateException, IOException, STPropertyAccessException,
			NoSuchSettingsManager, InvalidConfigurationException {

		// The variables below will be initialized differently depending on the kind of target
		Pair<IRI, Model> targetDatasetDecription;

		if (resourcePosition instanceof LocalResourcePosition) { // local project
			targetDatasetDecription = getProjectMetadata(
					((LocalResourcePosition) resourcePosition).getProject());
		} else if (resourcePosition instanceof RemoteResourcePosition) { // remote dataset
			// Extracts metadata from the metadata registry
			it.uniroma2.art.semanticturkey.resources.DatasetMetadata targetDatasetMetadata = ((RemoteResourcePosition) resourcePosition)
					.getDatasetMetadata();

			try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
				GraphQuery graphQuery = conn.prepareGraphQuery(
						"PREFIX void: <http://rdfs.org/ns/void#> DESCRIBE ?y WHERE {?x void:subset* ?y}");
				graphQuery.setBinding("x", targetDatasetMetadata.getIdentity());
				Model targetDatasetProfile = QueryResults.asModel(graphQuery.evaluate());

				targetDatasetDecription = ImmutablePair.of(targetDatasetMetadata.getIdentity(),
						targetDatasetProfile);
			}

		} else {
			throw new IllegalArgumentException("Unsupported resource position");
		}

		Pair<IRI, Model> sourceDatasetDecription = getProjectMetadata(getProject());

		return mediationFramework.profileProblem(sourceDatasetDecription.getLeft(),
				sourceDatasetDecription.getRight(), targetDatasetDecription.getLeft(),
				targetDatasetDecription.getRight());
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
