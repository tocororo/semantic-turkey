package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import it.uniroma2.art.maple.scenario.Dataset;
import it.uniroma2.art.maple.scenario.ScenarioDefinition;
import it.uniroma2.art.semanticturkey.alignment.AlignmentInitializationException;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.alignmentservices.RemoteAlignmentServiceConfiguration;
import it.uniroma2.art.semanticturkey.config.alignmentservices.RemoteAlignmentServicesStore;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.json.schema.ConversionException;
import it.uniroma2.art.semanticturkey.properties.json.schema.JsonSchemaConverter;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.AlignmentPlan;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.AlignmentServiceException;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.DatasetInfo;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.MatcherDTO;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.ReasonInfo;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.ServiceMetadataDTO;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.SettingsDTO;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.TaskDTO;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend.Matcher;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend.ServiceMetadata;
import it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend.Task;
import it.uniroma2.art.semanticturkey.settings.alignmentservices.RemoteAlignmentServiceProjectSettings;
import it.uniroma2.art.semanticturkey.settings.alignmentservices.RemoteAlignmentServiceProjectSettingsManager;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides services for interacting with remote alignment services.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class RemoteAlignmentServices extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(RemoteAlignmentServices.class);

	@Autowired
	private STMetadataRegistryBackend metadataRegistryBackend;
	@Autowired
	private it.uniroma2.art.semanticturkey.services.core.Alignment alignmentService;
	private RestTemplate restTemplate;

	public RemoteAlignmentServices() {
		restTemplate = new RestTemplate();
		for (HttpMessageConverter<?> msgConv : restTemplate.getMessageConverters()) {
			if (msgConv instanceof MappingJackson2HttpMessageConverter) {
				ObjectMapper objectMapper = ((MappingJackson2HttpMessageConverter) msgConv).getObjectMapper();
				objectMapper.registerModule(new JavaTimeModule());
			}
		}
	}

	/**
	 * Pings the alignment service to obtian its metadata, which may include <em>system-level settings</em>
	 * (schema). This operation can also be used to eagerly determine whether the configured alignment service
	 * is up and running.
	 * 
	 * @return
	 * @throws AlignmentServiceException
	 */
	@STServiceOperation
	public ServiceMetadataDTO getServiceMetadata() throws AlignmentServiceException {
		try {
			RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
			ServiceMetadata serviceMetadata = restTemplate.getForObject(endpoint.serverURL.toString(),
					ServiceMetadata.class);
			ServiceMetadataDTO serviceMetadataDTO = new ServiceMetadataDTO();
			serviceMetadataDTO.service = serviceMetadata.service;
			serviceMetadataDTO.version = serviceMetadata.version;
			serviceMetadataDTO.status = serviceMetadata.status;
			serviceMetadataDTO.specs = serviceMetadata.specs;
			serviceMetadataDTO.contact = serviceMetadata.contact;
			serviceMetadataDTO.documentation = serviceMetadata.documentation;
			ObjectNode originalSchema = serviceMetadata.settings;
			if (originalSchema != null) {
				SettingsDTO settingsDTO = new SettingsDTO();
				settingsDTO.originalSchema = originalSchema;

				try {
					settingsDTO.stProperties = new JsonSchemaConverter().convert(originalSchema);
				} catch (ConversionException e) {
					settingsDTO.conversionException = e.getClass() + ":" + e.getCause();
				}
				serviceMetadataDTO.settings = settingsDTO;
			}

			return serviceMetadataDTO;
		} catch (RestClientException e) {
			throw new AlignmentServiceException(e.getClass().getName() + ":" + e.getMessage(), e);
		}
	}

	/**
	 * Pings the alignment service to obtian its metadata, which may include <em>system-level settings</em>
	 * (schema). This operation can also be used to eagerly determine whether the configured alignment service
	 * is up and running.
	 * 
	 * @return
	 * @throws AlignmentServiceException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public List<MatcherDTO> searchMatchers(@JsonSerialized ScenarioDefinition scenarioDefinition)
			throws AlignmentServiceException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			HttpEntity<ScenarioDefinition> httpEntity = new HttpEntity<>(scenarioDefinition, headers);
			RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
			List<Matcher> matcher = restTemplate.exchange(endpoint.serverURL + "matchers/search",
					HttpMethod.POST, httpEntity, new ParameterizedTypeReference<List<Matcher>>() {
					}).getBody();
			return matcher.stream().map(m -> {
				MatcherDTO matcherDTO = new MatcherDTO();
				matcherDTO.id = m.id;
				matcherDTO.description = m.description;
				ObjectNode originalSchema = m.settings;
				if (originalSchema != null) {
					SettingsDTO settingsDTO = new SettingsDTO();
					settingsDTO.originalSchema = originalSchema;

					try {
						settingsDTO.stProperties = new JsonSchemaConverter().convert(originalSchema);
					} catch (ConversionException e) {
						settingsDTO.conversionException = e.getClass() + ":" + e.getCause();
					}
					matcherDTO.settings = settingsDTO;
				}

				return matcherDTO;
			}).collect(Collectors.toList());
		} catch (RestClientException e) {
			throw new AlignmentServiceException(e.getClass().getName() + ":" + e.getMessage(), e);
		}
	}

	/**
	 * Return the list of tasks managed by a remote alignment service. Tasks that terminated successfully have
	 * status {@code "Completed"} and an {@code "endTime"}. Currently, the service filters out any task that
	 * involves a closed project.
	 * 
	 * @param leftDataset
	 *            left dataset
	 * @param rightDataset
	 *            right dataset. A {@code null} values indicates that any dataset is fine
	 * @param allowReordering
	 *            left and right datasets may be matched in the reverse order
	 * @return
	 */
	@STServiceOperation
	public List<TaskDTO> listTasks(Project leftDataset, @Optional Project rightDataset,
			boolean allowReordering) {
		IRI leftDatasetIRI = metadataRegistryBackend.findDatasetForProject(leftDataset);

		if (leftDatasetIRI == null) {
			throw new IllegalArgumentException(
					"Unable to find metadata about project: " + leftDataset.getName());
		}

		IRI rightDatasetIRI;
		if (rightDataset != null) {
			rightDatasetIRI = metadataRegistryBackend.findDatasetForProject(rightDataset);
			if (rightDatasetIRI == null) {
				throw new IllegalArgumentException(
						"Unable to find metadata about project: " + rightDataset.getName());
			}
		} else {
			rightDatasetIRI = null;
		}

		RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
		ResponseEntity<List<Task>> response = restTemplate.exchange(endpoint.serverURL + "tasks",
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Task>>() {
				});

		return response.getBody().stream().filter(task -> { // filter by matching the provided
															// datasets
			if (Objects.equals(task.getLeftDataset(), leftDatasetIRI)
					&& (rightDatasetIRI == null || Objects.equals(task.getRightDataset(), rightDatasetIRI))) {
				return true;
			} else if (allowReordering) {
				return Objects.equals(task.getRightDataset(), leftDatasetIRI) && (rightDatasetIRI == null
						|| Objects.equals(task.getLeftDataset(), rightDatasetIRI));
			} else {
				return false;
			}
		}).flatMap(matchingStatus -> {
			Project leftProject = metadataRegistryBackend
					.findProjectForDataset(matchingStatus.getLeftDataset());
			if (leftProject == null) {
				return Stream.empty();
			}

			Project rightProject = metadataRegistryBackend
					.findProjectForDataset(matchingStatus.getRightDataset());
			if (rightProject == null) {
				return Stream.empty();
			}

			if (Stream.of(leftProject, rightProject)
					.anyMatch(proj -> !getProject().equals(proj) && !ProjectManager
							.checkAccessibility(getProject(), proj, AccessLevel.R, LockLevel.NO)
							.isAffirmative())) {
				return Stream.empty();
			}

			DatasetInfo leftDatasetInfo = DatasetInfo.valueOf(leftProject, matchingStatus.getLeftDataset());
			DatasetInfo rightDatasetInfo = DatasetInfo.valueOf(rightProject,
					matchingStatus.getRightDataset());

			TaskDTO taskDTO = new TaskDTO();
			taskDTO.setId(matchingStatus.getId());
			taskDTO.setLeftDataset(leftDatasetInfo);
			taskDTO.setRightDataset(rightDatasetInfo);
			taskDTO.setStatus(matchingStatus.getStatus());
			it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend.ReasonInfo backendReason = matchingStatus
					.getReason();
			if (backendReason != null) {
				ReasonInfo reasonInfo = new ReasonInfo();
				reasonInfo.setMessage(backendReason.getMessage());
				taskDTO.setReason(reasonInfo);
			}
			if ("running".equals(matchingStatus.getStatus())) {
				taskDTO.setProgress(matchingStatus.getProgress());
			}
			if (matchingStatus.getStartTime() != null) {
				taskDTO.setStartTime(Date.from(matchingStatus.getStartTime().toInstant()));
			}
			if (matchingStatus.getEndTime() != null) {
				taskDTO.setEndTime(Date.from(matchingStatus.getEndTime().toInstant()));
			}
			return Stream.of(taskDTO);
		}).collect(Collectors.toList());
	}

	@STServiceOperation
	public void downloadAlignment(HttpServletResponse oRes, String taskId) {
		RequestCallback requestCallback = request -> {
		};
		ResponseExtractor<Void> responseExtractor = response -> {
			oRes.setContentType("application/rdf+xml");
			oRes.setHeader("Content-Disposition", "attachment); filename=\"" + taskId + ".rdf\"");
			IOUtils.copy(response.getBody(), oRes.getOutputStream());
			return null;

		};
		RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
		restTemplate.execute(endpoint.serverURL + "tasks/{id}/alignment", HttpMethod.GET, requestCallback,
				responseExtractor, ImmutableMap.of("id", taskId));
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public JsonNode fetchAlignment(String taskId) throws IOException, AlignmentInitializationException {
		RequestCallback requestCallback = request -> {
		};

		File inputServerFile = File.createTempFile("alignment", taskId + ".rdf");
		ResponseExtractor<Void> responseExtractor = response -> {
			FileUtils.copyInputStreamToFile(response.getBody(), inputServerFile);
			return null;
		};
		RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
		restTemplate.execute(endpoint.serverURL + "tasks/{id}/alignment", HttpMethod.GET, requestCallback,
				responseExtractor, ImmutableMap.of("id", taskId));

		// creating model for loading alignment
		AlignmentModel alignModel = new AlignmentModel();
		alignModel.add(inputServerFile);

		Field connField;
		Project leftDatasetProject;
		Project rightDatasetProject;

		try {
			connField = alignModel.getClass().getDeclaredField("repoConnection");
			connField.setAccessible(true);

			RepositoryConnection conn = (RepositoryConnection) connField.get(alignModel);
			ValueFactory vf = conn.getValueFactory();

			// fixes onto1 and onto2
			// two problems: Genoma uses the dataset IRI, while the alignment validation uses the base URI.
			// Furthermore, Genoma represents them as strings, while the Alignment validation represents them
			// as IRIs

			Resource alignmentResource = Models
					.subject(QueryResults.asModel(conn.getStatements(null, RDF.TYPE, Alignment.ALIGNMENT)))
					.orElseThrow(() -> new RuntimeException("Unable to find the alignment resource"));

			IRI leftDatasetIRI = vf.createIRI(Models
					.object(QueryResults.asModel(conn.getStatements(alignmentResource,
							it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1, null)))
					.orElseThrow(() -> new RuntimeException("Missing onto1 inside alignment")).stringValue());
			IRI rightDatasetIRI = vf.createIRI(Models
					.object(QueryResults.asModel(conn.getStatements(alignmentResource,
							it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2, null)))
					.orElseThrow(() -> new RuntimeException("Missing onto2 inside alignment")).stringValue());

			leftDatasetProject = metadataRegistryBackend.findProjectForDataset(leftDatasetIRI);
			if (leftDatasetProject == null) {
				throw new RuntimeException("Unable to find the project associated with the left dataset");
			}

			rightDatasetProject = metadataRegistryBackend.findProjectForDataset(rightDatasetIRI);
			if (rightDatasetProject == null) {
				throw new RuntimeException("Unable to find the project associated with the right dataset");
			}

			conn.remove(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1, null);
			conn.remove(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2, null);

			conn.add(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1,
					vf.createIRI(leftDatasetProject.getBaseURI()));
			conn.add(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2,
					vf.createIRI(rightDatasetProject.getBaseURI()));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return alignmentService.loadAlignmentHelper(alignModel, leftDatasetProject, rightDatasetProject);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public String createTask(@JsonSerialized AlignmentPlan alignmentPlan)
			throws AlignmentServiceException {

		//// integrity checks

		ScenarioDefinition scenarioDefinition = alignmentPlan.getScenarioDefinition();

		// Left dataset has a SPARQL endpoint
		Dataset leftDataset = scenarioDefinition.getLeftDataset();
		if (!leftDataset.getSparqlEndpoint().isPresent()) {
			throw new AlignmentServiceException("Missing SPARQL endpoint for the left dataset");
		}

		// Right dataset has a SPARQL endpoint
		Dataset rightDataset = scenarioDefinition.getRightDataset();
		if (!rightDataset.getSparqlEndpoint().isPresent()) {
			throw new AlignmentServiceException("Missing SPARQL endpoint for the right dataset");
		}

		// Every support dataset has a SPARQL endpoint
		for (Dataset supportDataset : scenarioDefinition.getSupportDatasets()) {
			if (!supportDataset.getSparqlEndpoint().isPresent()) {
				throw new AlignmentServiceException("Missing SPARQL endpoint on the support dataset "
						+ NTriplesUtil.toNTriplesString(supportDataset.getId()));
			}
		}

		// There is at least a pairing of lexicalization sets

		/// End of integrity checks

		if (scenarioDefinition.getPairings().isEmpty()) {
			throw new AlignmentServiceException("No pairing of lexicalization set");
		}

		RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
		Task task = restTemplate.postForObject(endpoint.serverURL + "tasks", alignmentPlan, Task.class);
		return task.getId();
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteTask(String id) {
		RemoteAlignmentServiceConfiguration endpoint = getAlignmentServiceEndpoint();
		restTemplate.delete(endpoint.serverURL + "tasks/{id}", ImmutableMap.of("id", id));
	}

	/**
	 * Gets the configured remote alignment services.
	 * 
	 * @return
	 * @throws IOException
	 * @throws ConfigurationNotFoundException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAdmin() || @auth.isAuthorized('pm(project, alignmentService)', 'R')")
	@STServiceOperation
	public Map<String, RemoteAlignmentServiceConfiguration> getRemoteAlignmentServices()
			throws STPropertyAccessException, NoSuchConfigurationManager {
		RemoteAlignmentServicesStore cm = (RemoteAlignmentServicesStore) exptManager
				.getConfigurationManager(RemoteAlignmentServicesStore.class.getName());
		Map<String, RemoteAlignmentServiceConfiguration> configurations = new TreeMap<>();

		for (String id : cm.getSystemConfigurationIdentifiers()) {
			RemoteAlignmentServiceConfiguration config = cm.getSystemConfiguration(id);
			configurations.put(id, config);
		}

		return configurations;
	}

	/**
	 * Adds a configuration for a remote alignment service. Optionally, the configuration can be set as
	 * default (it is automatically set as default if it is the first).
	 * 
	 * @param id
	 * @param serverURL
	 * @param username
	 * @param password
	 * @param asDefault
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws NoSuchConfigurationManager
	 * @throws DuplicatedResourceException
	 */
	@PreAuthorize("@auth.isAdmin()")
	@STServiceOperation(method = RequestMethod.POST)
	public synchronized void addRemoteAlignmentService(String id, URL serverURL, @Optional String username,
			@Optional String password, @Optional(defaultValue = "false") boolean asDefault)
			throws IOException, WrongPropertiesException, STPropertyUpdateException,
			NoSuchConfigurationManager, DuplicatedResourceException {
		RemoteAlignmentServicesStore cm = (RemoteAlignmentServicesStore) exptManager
				.getConfigurationManager(RemoteAlignmentServicesStore.class.getName());
		Collection<String> existingIds = cm.getSystemConfigurationIdentifiers();
		if (existingIds.contains(id)) {
			throw new DuplicatedResourceException(
					"A configuration with this identifier already exists: " + id);
		}

		RemoteAlignmentServiceConfiguration config = new RemoteAlignmentServiceConfiguration();
		config.serverURL = serverURL;
		config.username = username;
		config.password = password;

		cm.storeSystemConfiguration(id, config);

		//set the new configuration as default if it is the only one or if it is explicitly asserted
		if (existingIds.size() == 0 || asDefault) {
			RemoteAlignmentServiceProjectSettings defaultSettings = new RemoteAlignmentServiceProjectSettings();
			defaultSettings.configID = id;
			STPropertiesManager.setProjectSettingsDefault(defaultSettings,
					RemoteAlignmentServiceProjectSettingsManager.class.getName(), false);
		}
	}

	@PreAuthorize("@auth.isAdmin()")
	@STServiceOperation(method = RequestMethod.POST)
	public synchronized void updateRemoteAlignmentService(String id, URL serverURL, @Optional String username,
			@Optional String password, @Optional boolean asDefault)
			throws IOException, WrongPropertiesException, STPropertyUpdateException,
			NoSuchConfigurationManager, STPropertyAccessException {
		RemoteAlignmentServicesStore cm = (RemoteAlignmentServicesStore) exptManager
				.getConfigurationManager(RemoteAlignmentServicesStore.class.getName());
		RemoteAlignmentServiceConfiguration config = cm.getSystemConfiguration(id);
		config.serverURL = serverURL;
		config.username = username;
		config.password = password;
		cm.storeSystemConfiguration(id, config);

		if (asDefault) {
			RemoteAlignmentServiceProjectSettings defaultSettings = new RemoteAlignmentServiceProjectSettings();
			defaultSettings.configID = id;
			STPropertiesManager.setProjectSettingsDefault(defaultSettings,
					RemoteAlignmentServiceProjectSettingsManager.class.getName(), false);
		}
	}

	@STServiceOperation
	public synchronized String getDefaultRemoteAlignmentServiceId() throws STPropertyAccessException {
		return STPropertiesManager.getProjectSettingDefault("configID",
				RemoteAlignmentServiceProjectSettingsManager.class.getName());
//		return STPropertiesManager.getProjectSettings(RemoteAlignmentServiceProjectSettings.class,
//				getProject(), RemoteAlignmentServiceProjectSettingsManager.class.getName(), false).configID;
	}

	/**
	 * Deletes the configuration of a remote alignment service. This operation can break existing references
	 * from projects.
	 * 
	 * @param id
	 * @throws ConfigurationNotFoundException
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAdmin()")
	@STServiceOperation(method = RequestMethod.POST)
	public synchronized void deleteRemoteAlignmentService(String id)
			throws ConfigurationNotFoundException, NoSuchConfigurationManager {
		RemoteAlignmentServicesStore cm = (RemoteAlignmentServicesStore) exptManager
				.getConfigurationManager(RemoteAlignmentServicesStore.class.getName());
		cm.deleteSystemConfiguration(id);
	}

	/**
	 * Associates the project to an alignment service configuration (given its <code>id</code>), possibly
	 * overriding an existing default service.
	 * 
	 * @param id
	 * @throws ConfigurationNotFoundException
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyUpdateException
	 */
	@PreAuthorize("@auth.isAuthorized('pm(project, alignmentService)', 'C')")
	@STServiceOperation(method = RequestMethod.POST)
	public synchronized void setAlignmentServiceForProject(String id)
			throws NoSuchSettingsManager, STPropertyUpdateException {
		RemoteAlignmentServiceProjectSettingsManager sm = (RemoteAlignmentServiceProjectSettingsManager) exptManager
				.getSettingsManager(RemoteAlignmentServiceProjectSettingsManager.class.getName());
		RemoteAlignmentServiceProjectSettings settings = new RemoteAlignmentServiceProjectSettings();
		settings.configID = id;
		sm.storeProjectSettings(getProject(), settings);
	}

	/**
	 * Returns the alignment service associated with the project. The service returns a pair consisting of a
	 * configuration reference and a boolean flag telling whether or not it is explicit. If no service is
	 * configured, the returned value is <code>null</code>.
	 * 
	 * @return
	 * @throws STPropertyAccessException
	 * @throws NoSuchSettingsManager
	 * 
	 */
	@PreAuthorize("@auth.isAuthorized('pm(project, alignmentService)', 'R')")
	@STServiceOperation
	public synchronized @Nullable Pair<String, Boolean> getAlignmentServiceForProject()
			throws STPropertyAccessException, NoSuchSettingsManager {
		RemoteAlignmentServiceProjectSettingsManager sm = (RemoteAlignmentServiceProjectSettingsManager) exptManager
				.getSettingsManager(RemoteAlignmentServiceProjectSettingsManager.class.getName());
		RemoteAlignmentServiceProjectSettings explicitSettings = sm.getProjectSettings(getProject(), true);

		if (explicitSettings.configID != null) {
			return ImmutablePair.of(explicitSettings.configID, true);
		}

		RemoteAlignmentServiceProjectSettings implicitSettings = sm.getProjectSettings(getProject(), false);

		if (implicitSettings.configID != null) {
			return ImmutablePair.of(implicitSettings.configID, false);
		}

		return null;
	}

	/**
	 * Disassociates the project from an alignment service configuration (given its <code>id</code>), leaving
	 * in place any default service defined by the administrator.
	 * 
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyUpdateException
	 */
	@PreAuthorize("@auth.isAuthorized('pm(project, alignmentService)', 'D')")
	@STServiceOperation(method = RequestMethod.POST)
	public synchronized void removeAlignmentServiceForProject()
			throws NoSuchSettingsManager, STPropertyUpdateException {
		RemoteAlignmentServiceProjectSettingsManager sm = (RemoteAlignmentServiceProjectSettingsManager) exptManager
				.getSettingsManager(RemoteAlignmentServiceProjectSettingsManager.class.getName());
		RemoteAlignmentServiceProjectSettings settings = new RemoteAlignmentServiceProjectSettings();
		settings.configID = null;
		sm.storeProjectSettings(getProject(), settings);
	}

	private RemoteAlignmentServiceConfiguration getAlignmentServiceEndpoint() {
		RemoteAlignmentServiceProjectSettingsManager sm;
		try {
			sm = (RemoteAlignmentServiceProjectSettingsManager) exptManager
					.getSettingsManager(RemoteAlignmentServiceProjectSettingsManager.class.getName());
		} catch (NoSuchSettingsManager e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
		RemoteAlignmentServiceProjectSettings settings;
		try {
			settings = sm.getProjectSettings(getProject());
		} catch (STPropertyAccessException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
		if (settings.configID == null) {
			throw new IllegalStateException("No alignement service configured");
		}

		RemoteAlignmentServicesStore cm;
		try {
			cm = (RemoteAlignmentServicesStore) exptManager
					.getConfigurationManager(RemoteAlignmentServicesStore.class.getName());
		} catch (NoSuchConfigurationManager e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
		try {
			return cm.getSystemConfiguration(settings.configID);
		} catch (STPropertyAccessException e) {
			throw new IllegalStateException(
					"Unable to retrieve the alignment service configuration: " + settings.configID, e);
		}
	}
}