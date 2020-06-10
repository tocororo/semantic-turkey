package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.maple.scenario.Dataset;
import it.uniroma2.art.maple.scenario.ScenarioDefinition;
import it.uniroma2.art.semanticturkey.alignment.AlignmentInitializationException;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
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
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;

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
			ServiceMetadata serviceMetadata = restTemplate.getForObject(getAlignmentServiceEndpoint(),
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
			List<Matcher> matcher = restTemplate.exchange(getAlignmentServiceEndpoint() + "matchers/search",
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

		ResponseEntity<List<Task>> response = restTemplate.exchange(getAlignmentServiceEndpoint() + "tasks",
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
		restTemplate.execute(getAlignmentServiceEndpoint() + "tasks/{id}/alignment", HttpMethod.GET,
				requestCallback, responseExtractor, ImmutableMap.of("id", taskId));
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
		restTemplate.execute(getAlignmentServiceEndpoint() + "tasks/{id}/alignment", HttpMethod.GET,
				requestCallback, responseExtractor, ImmutableMap.of("id", taskId));

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
			throws IOException, AlignmentServiceException {

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

		Task task = restTemplate.postForObject(getAlignmentServiceEndpoint() + "tasks", alignmentPlan,
				Task.class);
		return task.getId();
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteTask(String id) throws IOException, AlignmentServiceException {
		restTemplate.delete(getAlignmentServiceEndpoint() + "tasks/{id}", ImmutableMap.of("id", id));
	}

	private String getAlignmentServiceEndpoint() {
		String alignmentPort = null;
		try {
			alignmentPort = STPropertiesManager
					.getSystemSetting(STPropertiesManager.SETTING_ALIGNMENT_REMOTE_PORT);
		} catch (STPropertyAccessException e) {
			e.printStackTrace();
		}
		if (alignmentPort == null) {
			alignmentPort = "7575";
		}
		return "http://localhost:" + alignmentPort + "/";
	}
}