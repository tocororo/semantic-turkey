package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.maple.problem.MatchingProblem;
import it.uniroma2.art.semanticturkey.alignment.AlignmentInitializationException;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.genoma.DatasetInfo;
import it.uniroma2.art.semanticturkey.services.core.genoma.GENOMAException;
import it.uniroma2.art.semanticturkey.services.core.genoma.Task;
import it.uniroma2.art.semanticturkey.services.core.genoma.backend.MatchingStatus;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;

/**
 * This class provides services for interacting with GENOMA.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class GENOMA extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(GENOMA.class);

	public static final String GENOMA_ENDPOINT = "http://localhost:8282/";

	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;
	@Autowired
	private it.uniroma2.art.semanticturkey.services.core.Alignment alignmentService;
	private RestTemplate restTemplate;

	public GENOMA() {
		restTemplate = new RestTemplate();
	}

	/**
	 * Return the list of tasks managed by GENOMA. Tasks that terminated successfully have status
	 * {@code "Completed"} and an {@code "endTime"}. Currently, the service filters out any task that involves
	 * a closed project.
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
	public List<Task> listTasks(Project leftDataset, @Optional Project rightDataset,
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

		ResponseEntity<List<MatchingStatus>> response = restTemplate.exchange(
				GENOMA_ENDPOINT + "getMatchingList", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<MatchingStatus>>() {
				});

		return response.getBody().stream().filter(matchingStatus -> { // filter by matching the provided
																		// datasets
			if (Objects.equals(matchingStatus.getOntology1(), leftDatasetIRI) && (rightDatasetIRI == null
					|| Objects.equals(matchingStatus.getOntology2(), rightDatasetIRI))) {
				return true;
			} else if (allowReordering) {
				return Objects.equals(matchingStatus.getOntology2(), leftDatasetIRI)
						&& (rightDatasetIRI == null
								|| Objects.equals(matchingStatus.getOntology1(), rightDatasetIRI));
			} else {
				return false;
			}
		}).flatMap(matchingStatus -> {
			Project leftProject = metadataRegistryBackend
					.findProjectForDataset(matchingStatus.getOntology1());
			if (leftProject == null) {
				return Stream.empty();
			}

			Project rightProject = metadataRegistryBackend
					.findProjectForDataset(matchingStatus.getOntology2());
			if (rightProject == null) {
				return Stream.empty();
			}

			if (Stream.of(leftProject, rightProject)
					.anyMatch(proj -> !getProject().equals(proj) && !ProjectManager
							.checkAccessibility(getProject(), proj, AccessLevel.R, LockLevel.NO)
							.isAffirmative())) {
				return Stream.empty();
			}

			DatasetInfo leftDatasetInfo = DatasetInfo.valueOf(leftProject, matchingStatus.getOntology1());
			DatasetInfo rightDatasetInfo = DatasetInfo.valueOf(rightProject, matchingStatus.getOntology2());

			Task task = new Task();
			task.setId(matchingStatus.getId());
			task.setLeftDataset(leftDatasetInfo);
			task.setRightDataset(rightDatasetInfo);
			task.setEngine(matchingStatus.getEngine());
			task.setStatus(matchingStatus.getStatus());
			task.setStartTime(matchingStatus.getStartTime());
			task.setEndTime(matchingStatus.getEndTime());
			return Stream.of(task);
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
		restTemplate.execute(GENOMA_ENDPOINT + "downloadFile/{taskId}.rdf?fileType=alignment", HttpMethod.GET,
				requestCallback, responseExtractor, ImmutableMap.of("taskId", taskId));
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
		restTemplate.execute(GENOMA_ENDPOINT + "downloadFile/{taskId}.rdf?fileType=alignment", HttpMethod.GET,
				requestCallback, responseExtractor, ImmutableMap.of("taskId", taskId));

		// creating model for loading alignment
		AlignmentModel alignModel = new AlignmentModel();
		alignModel.add(inputServerFile);

		Field connField;
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
							it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1, (Resource) null)))
					.orElseThrow(() -> new RuntimeException("Missing onto1 inside alignment")).stringValue());
			IRI rightDatasetIRI = vf.createIRI(Models
					.object(QueryResults.asModel(conn.getStatements(alignmentResource,
							it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2, (Resource) null)))
					.orElseThrow(() -> new RuntimeException("Missing onto2 inside alignment")).stringValue());

			Project leftDatasetProject = metadataRegistryBackend.findProjectForDataset(leftDatasetIRI);
			if (leftDatasetProject == null) {
				throw new RuntimeException("Unable to find the project associated with the left dataset");
			}

			Project rightDatasetProject = metadataRegistryBackend.findProjectForDataset(rightDatasetIRI);
			if (rightDatasetProject == null) {
				throw new RuntimeException("Unable to find the project associated with the right dataset");
			}

			conn.remove(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1,
					(Resource) null);
			conn.remove(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2,
					(Resource) null);

			conn.add(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO1,
					vf.createIRI(leftDatasetProject.getBaseURI()));
			conn.add(alignmentResource, it.uniroma2.art.semanticturkey.vocabulary.Alignment.ONTO2,
					vf.createIRI(rightDatasetProject.getBaseURI()));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return alignmentService.loadAlignmentHelper(alignModel);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public String createTask(@JsonSerialized MatchingProblem matchingProblem)
			throws IOException, GENOMAException {
		ObjectMapper objMapper = new ObjectMapper();
		String matchingProblemJson;
		try {
			matchingProblemJson = objMapper.writeValueAsString(matchingProblem);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e); // this should never happern
		}

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpPost request = new HttpPost(GENOMA_ENDPOINT + "runMatch");
			request.setEntity(new StringEntity(matchingProblemJson, ContentType.APPLICATION_JSON));
			try (CloseableHttpResponse httpReponse = httpClient.execute(request)) {
				StatusLine statusLine = httpReponse.getStatusLine();
				if ((statusLine.getStatusCode() / HttpStatus.SC_OK) != 1) {
					throw new IOException(statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				HttpEntity entity = httpReponse.getEntity();
				String responseString = IOUtils.toString(entity.getContent(),
						java.util.Optional.ofNullable(ContentType.get(entity).getCharset())
								.orElse(StandardCharsets.UTF_8).name());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode responseObject = mapper.readTree(new StringReader(responseString));
				JsonNode errorNode = responseObject.get("error");

				if (errorNode != null) {
					throw new GENOMAException(errorNode.textValue());
				}

				return responseObject.get("id").textValue();
			}

		}

	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteTask(String id) throws IOException, GENOMAException {
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpDelete request;
			try {
				request = new HttpDelete(
						new URIBuilder(GENOMA_ENDPOINT).setPath("delete").addParameter("id", id).build());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e); // should not happen
			}
			try (CloseableHttpResponse httpReponse = httpClient.execute(request)) {
				StatusLine statusLine = httpReponse.getStatusLine();
				if ((statusLine.getStatusCode() / HttpStatus.SC_OK) != 1) {
					throw new IOException(statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				HttpEntity entity = httpReponse.getEntity();
				String responseString = IOUtils.toString(entity.getContent(),
						java.util.Optional.ofNullable(ContentType.get(entity).getCharset())
								.orElse(StandardCharsets.UTF_8).name());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode responseObject = mapper.readTree(new StringReader(responseString));
				JsonNode errorNode = responseObject.get("error");

				if (errorNode != null) {
					throw new GENOMAException(errorNode.textValue());
				}
			}
		}
	}
}