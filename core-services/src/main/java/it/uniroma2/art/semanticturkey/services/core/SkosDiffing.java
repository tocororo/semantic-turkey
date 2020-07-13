package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.ChangedLabel;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.ChangedResource;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.DiffResultStructure;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.LabelWithResAndLitForm;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.ResourceWithLexicalization;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@STService
public class SkosDiffing extends STServiceAdapter {

	private String serverHost = "http://localhost";
	private String serverPort = "7576";

	private final String SPARQL_ENDPOINT_1 = "sparqlEndpoint1";
	private final String SPARQL_ENDPOINT_2 = "sparqlEndpoint2";
	private final String LEXICALIZATION_TYPE_1 = "lexicalizationType1";
	private final String LEXICALIZATION_TYPE_2 = "lexicalizationType2";
	private final String LANG_LIST = "langList";

	private final String SKOSXL_LEX_MODE = "skosxl";
	private final String SKOS_LEX_MODE = "skos";

	private static final String TEXT_HTML = "text/html";
	private static final String APPLICATION_PDF = "application/pdf";
	private static final String APPLICATION_JSON = "application/json";



	@STServiceOperation(method = RequestMethod.POST)
	public String runDiffing(String leftProjectName, @Optional String leftVersionRepoId,
			String rightProjectName, @Optional String rightVersionRepoId, @Optional List<String> langList) throws IOException {

		Project leftProject = ProjectManager.getProject(leftProjectName);
		IRI leftSparqlEndpoint = getSparqlEndpoint(leftProject, leftVersionRepoId);
		System.out.println("leftSparqlEndpoint " + leftSparqlEndpoint);
		if (leftSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the left dataset");
		}
		IRI lexModelIRI = leftProject.getLexicalizationModel();
		String leftLexicalizationType;
		if(lexModelIRI.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			leftLexicalizationType = SKOSXL_LEX_MODE;
		} else if(lexModelIRI.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			leftLexicalizationType = SKOS_LEX_MODE;
		} else {
			throw new IllegalStateException("The only supported lexicalization models are SKOS and SKOSXL, "+lexModelIRI.getLocalName()+" is not supported");
		}

		Project rightProject = ProjectManager.getProject(rightProjectName);
		IRI rightSparqlEndpoint = getSparqlEndpoint(rightProject, rightVersionRepoId);
		System.out.println("rightSparqlEndpoint " + rightSparqlEndpoint);
		if (rightSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the right dataset");
		}
		lexModelIRI = leftProject.getLexicalizationModel();
		String rightLexicalizationType;
		if(lexModelIRI.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			rightLexicalizationType = SKOSXL_LEX_MODE;
		} else if(lexModelIRI.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			rightLexicalizationType = SKOS_LEX_MODE;
		} else {
			throw new IllegalStateException("The only supported lexicalization models are SKOS and SKOSXL, "+lexModelIRI.getLocalName()+" is not supported");
		}


		//prepare the HTTP POST
		String url = serverHost+serverPort+"/skosDiff/skosDiff/diff/executeDiffTask";
		HttpPost httpPost = new HttpPost(url);
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode objectNode = jsonFactory.objectNode();
		objectNode.set(SPARQL_ENDPOINT_1, jsonFactory.textNode(leftSparqlEndpoint.stringValue()));
		objectNode.set(LEXICALIZATION_TYPE_1, jsonFactory.textNode(rightLexicalizationType));
		objectNode.set(SPARQL_ENDPOINT_2, jsonFactory.textNode(rightSparqlEndpoint.stringValue()));
		objectNode.set(LEXICALIZATION_TYPE_2, jsonFactory.textNode(rightLexicalizationType));
		if(langList!=null && !langList.isEmpty()){
			ArrayNode arrayNode = jsonFactory.arrayNode();
			for (String lang : langList) {
				arrayNode.add(lang);
			}
			objectNode.set(LANG_LIST, arrayNode);
		}
		String jsonString = objectNode.toString();
		httpPost.setHeader("Accept", "application/json");
		org.apache.http.HttpEntity entity = new StringEntity(jsonString,
				ContentType.APPLICATION_JSON);
		httpPost.setEntity(entity);


		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse response = httpClient.execute(httpPost)) {
			return EntityUtils.toString(response.getEntity());
		}
	}

	@STServiceOperation(method = RequestMethod.GET)
	public String getAllTasksInfo() throws IOException {
		String url;
		url = serverHost+serverPort+"/skosDiff/skosDiff/diff/tasksInfo";

		//prepare and execute the HTTP GET
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Accept", "application/json");
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse response = httpClient.execute(httpGet)) {

			// Get HttpResponse Status
			System.out.println(response.getStatusLine().toString());

			HttpEntity entity = response.getEntity();
			Header headers = entity.getContentType();
			System.out.println(headers);

			if (entity != null) {
				// return it as a String
				String result = EntityUtils.toString(entity);
				return result;
			}
			return "";
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteTask(String taskId) throws IOException {
		String url = serverHost+serverPort+"/skosDiff/skosDiff/diff/"+taskId;
		HttpDelete httpDelete = new HttpDelete(url);
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse response = httpClient.execute(httpDelete)) {
		}
	}

	@STServiceOperation(method = RequestMethod.GET)
	public void getTaskResult(HttpServletResponse response, String taskId, ResultType resultType) throws IOException, ParserConfigurationException, TransformerException {
		String url = serverHost+serverPort+"http://localhost:7576/skosDiff/skosDiff/diff/"+taskId;


		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Accept", "application/json");
		DiffResultStructure diffResultStructure = null;
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse responseFromServer = httpClient.execute(httpGet)) {
			HttpEntity entity = responseFromServer.getEntity();
			//Header headers = entity.getContentType();

			if (entity != null) {
				// return it as a String
				String result = EntityUtils.toString(entity);
				ObjectMapper objectMapper = new ObjectMapper();
				diffResultStructure = objectMapper.readValue(result, DiffResultStructure.class);

			}
		}

		byte[] bytes = new byte[0];
		String contentType = null;
		if(diffResultStructure != null) {
			//tranform the response according to the desired resultType
			if (resultType.equals(ResultType.json)) {
				ObjectMapper obj = new ObjectMapper();
				String jsonString = obj.writeValueAsString(diffResultStructure);
				bytes = jsonString.getBytes();
				contentType = APPLICATION_JSON;
			} else if (resultType.equals(ResultType.html)) {
				String text = createResultInHTML(diffResultStructure);
				bytes = text.getBytes(StandardCharsets.UTF_8);
				contentType = TEXT_HTML;
			} else { // resultType.equals(ResultType.pdf
				String text = createResultInHTML(diffResultStructure);
				Document jsoupDocument = Jsoup.parse(text);
				org.w3c.dom.Document doc = W3CDom.convert(jsoupDocument);
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					PdfRendererBuilder builder = new PdfRendererBuilder();
					builder.useFastMode();
					builder.withW3cDocument(doc, "http://example.org/");
					builder.toStream(os);
					builder.run();

					bytes = os.toByteArray();
					contentType = APPLICATION_PDF;
				}
			}
		}

		response.setContentType(contentType);
		response.setContentLength(bytes.length);

		try (OutputStream os = response.getOutputStream()) {
			IOUtils.write(bytes, os);
		}
	}


	private String createResultInHTML(DiffResultStructure diffResultStructure) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		org.w3c.dom.Document document = documentBuilder.newDocument();
		Element htmlElem = document.createElement("html");
		document.appendChild(htmlElem);

		Element headElem = document.createElement("head");
		htmlElem.appendChild(headElem);
		Node titleElem = document.createElement("title");
		titleElem.setTextContent("Result for task: "+diffResultStructure.getTaskId());

		Element bodyElem = document.createElement("body");
		htmlElem.appendChild(bodyElem);

		Element infoElem = document.createElement("h1");
		infoElem.setTextContent("INFO:");
		bodyElem.appendChild(infoElem);
		Element sparqlLeft = document.createElement("div");
		sparqlLeft.setTextContent("LeftSPARQLEndpoint: "+diffResultStructure.getSparqlEndpoint1());
		infoElem.appendChild(sparqlLeft);
		Element lexlLeft = document.createElement("div");
		lexlLeft.setTextContent("LeftLexicalization: "+diffResultStructure.getLexicalization1());
		infoElem.appendChild(lexlLeft);
		Element rightlLeft = document.createElement("div");
		rightlLeft.setTextContent("RightSPARQLEndpoint: "+diffResultStructure.getSparqlEndpoint2());
		infoElem.appendChild(rightlLeft);
		Element lexRight = document.createElement("div");
		lexRight.setTextContent("RightLexicalization: "+diffResultStructure.getLexicalization2());
		infoElem.appendChild(lexRight);
		Element taskId = document.createElement("div");
		taskId.setTextContent("taskId: "+diffResultStructure.getTaskId());
		infoElem.appendChild(taskId);

		Element removedResourcesElem = document.createElement("h1");
		removedResourcesElem.setTextContent("REMOVED RESOURCES:");
		bodyElem.appendChild(removedResourcesElem);
		for(ResourceWithLexicalization resourceWithLexicalization : diffResultStructure.getRemovedResources()){
			Element removedResElem = document.createElement("div");
			removedResElem.setTextContent("Removed Resource:");
			removedResourcesElem.appendChild(removedResElem);
			Element resIriElem = document.createElement("div");
			resIriElem.setTextContent("resourceIRI: "+resourceWithLexicalization.getResourceIri());
			removedResElem.appendChild(resIriElem);
			Element resTypesElem = document.createElement("div");
			resTypesElem.setTextContent("Types:");
			removedResElem.appendChild(resTypesElem);
			for(String type : resourceWithLexicalization.getResourceTypeList()){
				Element resTypeElem = document.createElement("div");
				resTypeElem.setTextContent("type: "+type);
				resTypesElem.appendChild(resTypeElem);
			}
			Element lexTypesElem = document.createElement("div");
			lexTypesElem.setTextContent("Lexicalizations:");
			removedResElem.appendChild(lexTypesElem);
			for(String lexicalization : resourceWithLexicalization.getLexicalizationList()){
				Element lexicalizationElem = document.createElement("div");
				lexicalizationElem.setTextContent("lexicalization: "+lexicalization);
				lexTypesElem.appendChild(lexicalizationElem);
			}
		}

		Element addedResourcesElem = document.createElement("h1");
		addedResourcesElem.setTextContent("ADDED RESOURCES:");
		bodyElem.appendChild(addedResourcesElem);
		for(ResourceWithLexicalization resourceWithLexicalization : diffResultStructure.getAddedResources()){
			Element addedResElem = document.createElement("div");
			addedResElem.setTextContent("Added Resource:");
			addedResourcesElem.appendChild(addedResElem);
			Element resIriElem = document.createElement("div");
			resIriElem.setTextContent("resourceIRI: "+resourceWithLexicalization.getResourceIri());
			addedResElem.appendChild(resIriElem);
			Element resTypesElem = document.createElement("div");
			resTypesElem.setTextContent("Types:");
			addedResElem.appendChild(resTypesElem);
			for(String type : resourceWithLexicalization.getResourceTypeList()){
				Element resTypeElem = document.createElement("div");
				resTypeElem.setTextContent("type: "+type);
				resTypesElem.appendChild(resTypeElem);
			}
			Element lexTypesElem = document.createElement("div");
			lexTypesElem.setTextContent("Lexicalizations:");
			addedResElem.appendChild(lexTypesElem);
			for(String lexicalization : resourceWithLexicalization.getLexicalizationList()){
				Element lexicalizationElem = document.createElement("div");
				lexicalizationElem.setTextContent("lexicalization: "+lexicalization);
				lexTypesElem.appendChild(lexicalizationElem);
			}
		}

		Element changedResourcesElem = document.createElement("h1");
		changedResourcesElem.setTextContent("CHANGED RESOURCES:");
		bodyElem.appendChild(changedResourcesElem);
		for(ChangedResource changedResource : diffResultStructure.getChangedResources()){
			Element changedResElem = document.createElement("div");
			changedResElem.setTextContent("Changed Resource:");
			changedResourcesElem.appendChild(changedResElem);
			Element resourceIRIElem = document.createElement("div");
			resourceIRIElem.setTextContent("resourceIRI: "+changedResource.getResourceId());
			changedResElem.appendChild(resourceIRIElem);
			Element removedLexsElem = document.createElement("div");
			removedLexsElem.setTextContent("Removed Lexicalizations:");
			changedResElem.appendChild(removedLexsElem);
			for(String propIri : changedResource.getLexPropToRemovedLexicalizationListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Lexilization Property: "+propIri);
				removedLexsElem.appendChild(propElem);
				Element lexsElem = document.createElement("div");
				lexsElem.setTextContent("Lexicalizations:");
				propElem.appendChild(lexsElem);
				for(String removedLex : changedResource.getLexPropToRemovedLexicalizationListMap().get(propIri)){
					Element lexElem = document.createElement("div");
					lexElem.setTextContent("lexicalization: "+removedLex);
					lexsElem.appendChild(lexElem);
				}
			}
			Element addedLexsElem = document.createElement("div");
			addedLexsElem.setTextContent("Added Lexicalizations:");
			changedResElem.appendChild(addedLexsElem);
			for(String propIri : changedResource.getLexPropToAddedLexicalizationListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Lexilization Property: "+propIri);
				addedLexsElem.appendChild(propElem);
				Element lexsElem = document.createElement("div");
				lexsElem.setTextContent("Lexicalizations:");
				propElem.appendChild(lexsElem);
				for(String addedLex : changedResource.getLexPropToAddedLexicalizationListMap().get(propIri)){
					Element lexElem = document.createElement("div");
					lexElem.setTextContent("lexicalization: "+addedLex);
					lexsElem.appendChild(lexElem);
				}
			}
			Element removedNotesElem = document.createElement("div");
			removedNotesElem.setTextContent("Removed Notes:");
			changedResElem.appendChild(removedNotesElem);
			for(String propIri : changedResource.getNotePropToRemovedNoteValueListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Note Property: "+propIri);
				removedNotesElem.appendChild(propElem);
				Element notesElem = document.createElement("div");
				notesElem.setTextContent("Notes:");
				propElem.appendChild(notesElem);
				for(String removedNote : changedResource.getNotePropToRemovedNoteValueListMap().get(propIri)){
					Element noteElem = document.createElement("div");
					noteElem.setTextContent("note: "+removedNote);
					notesElem.appendChild(noteElem);
				}
			}
			Element addedNotesElem = document.createElement("div");
			addedNotesElem.setTextContent("Removed Notes:");
			changedResElem.appendChild(addedNotesElem);
			for(String propIri : changedResource.getNotePropToAddedNoteValueListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Note Property: "+propIri);
				addedNotesElem.appendChild(propElem);
				Element notesElem = document.createElement("div");
				notesElem.setTextContent("Notes:");
				propElem.appendChild(notesElem);
				for(String addedNote : changedResource.getNotePropToAddedNoteValueListMap().get(propIri)){
					Element noteElem = document.createElement("div");
					noteElem.setTextContent("note: "+addedNote);
					notesElem.appendChild(noteElem);
				}
			}
			Element removedPropElem = document.createElement("div");
			removedPropElem.setTextContent("Removed Properties Values:");
			changedResElem.appendChild(removedPropElem);
			for(String propIri : changedResource.getPropToRemovedValueListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Property: "+propIri);
				removedPropElem.appendChild(propElem);
				Element valuesElem = document.createElement("div");
				valuesElem.setTextContent("Values:");
				propElem.appendChild(valuesElem);
				for(String value : changedResource.getPropToRemovedValueListMap().get(propIri)){
					Element valueElem = document.createElement("div");
					valueElem.setTextContent("value: "+value);
					valuesElem.appendChild(valueElem);
				}
			}
			Element addedPropElem = document.createElement("div");
			addedPropElem.setTextContent("Added Properties Values:");
			changedResElem.appendChild(addedPropElem);
			for(String propIri : changedResource.getPropToAddedValueListMap().keySet()){
				Element propElem = document.createElement("div");
				propElem.setTextContent("Property: "+propIri);
				addedPropElem.appendChild(propElem);
				Element valuesElem = document.createElement("div");
				valuesElem.setTextContent("Values:");
				propElem.appendChild(valuesElem);
				for(String removedLex : changedResource.getPropToAddedValueListMap().get(propIri)){
					Element valueElem = document.createElement("div");
					valueElem.setTextContent("value: "+removedLex);
					valuesElem.appendChild(valueElem);
				}
			}
		}


		if(diffResultStructure.getLexicalization1().equals(SKOSXL_LEX_MODE) && diffResultStructure.getLexicalization2().equals(SKOSXL_LEX_MODE)) {
			Element removedLabelsElem = document.createElement("h1");
			removedLabelsElem.setTextContent("REMOVED LABELS:");
			bodyElem.appendChild(removedLabelsElem);
			for(LabelWithResAndLitForm labelWithResAndLitForm : diffResultStructure.getRemoveLabels()){
				Element removedLabelElem = document.createElement("div");
				removedLabelElem.setTextContent("Removed Label:");
				removedLabelsElem.appendChild(removedLabelElem);
				Element labelElem = document.createElement("div");
				labelElem.setTextContent("LabelIRI: "+labelWithResAndLitForm.getLabel());
				removedLabelElem.appendChild(labelElem);
				Element literalFormElem = document.createElement("div");
				literalFormElem.setTextContent("Literal Form: "+labelWithResAndLitForm.getLiteralForm());
				removedLabelElem.appendChild(literalFormElem);
				Element resourceElem = document.createElement("div");
				resourceElem.setTextContent("Resource: "+labelWithResAndLitForm.getResource());
				removedLabelElem.appendChild(resourceElem);
			}

			Element addedLabelsElem = document.createElement("h1");
			addedLabelsElem.setTextContent("ADDED LABELS:");
			bodyElem.appendChild(addedLabelsElem);
			for(LabelWithResAndLitForm labelWithResAndLitForm : diffResultStructure.getAddedLabels()){
				Element addedLabelElem = document.createElement("div");
				addedLabelElem.setTextContent("Added Label:");
				removedLabelsElem.appendChild(addedLabelElem);
				Element labelElem = document.createElement("div");
				labelElem.setTextContent("LabelIRI: "+labelWithResAndLitForm.getLabel());
				addedLabelElem.appendChild(labelElem);
				Element literalFormElem = document.createElement("div");
				literalFormElem.setTextContent("Literal Form: "+labelWithResAndLitForm.getLiteralForm());
				addedLabelElem.appendChild(literalFormElem);
				Element resourceElem = document.createElement("div");
				resourceElem.setTextContent("Resource: "+labelWithResAndLitForm.getResource());
				addedLabelElem.appendChild(resourceElem);
			}

			Element changedLabelsElem = document.createElement("h1");
			changedLabelsElem.setTextContent("CHANGED LABELS:");
			bodyElem.appendChild(changedLabelsElem);
			for(ChangedLabel changedLabel : diffResultStructure.getChangedLabels()){
				Element changedLabelElem = document.createElement("div");
				changedLabelElem.setTextContent("Changed Label:");
				changedLabelsElem.appendChild(changedLabelElem);
				Element labelIRIElem = document.createElement("div");
				labelIRIElem.setTextContent("LabelIRI: "+changedLabel.getLabel());
				changedLabelElem.appendChild(labelIRIElem);
				if(changedLabel.getLiteralForm() != null && !changedLabel.getLiteralForm().isEmpty()){
					Element literalFormElem = document.createElement("div");
					literalFormElem.setTextContent("Literal Form: "+changedLabel.getLiteralForm());
					changedLabelElem.appendChild(literalFormElem);
				} else {
					Element removedLiteralFormElem = document.createElement("div");
					removedLiteralFormElem.setTextContent("Removed Literal Form: "+changedLabel.getRemovedLiteralForm());
					changedLabelElem.appendChild(removedLiteralFormElem);
					Element addedLiteralFormElem = document.createElement("div");
					addedLiteralFormElem.setTextContent("Added Literal Form: "+changedLabel.getAddedLiteralForm());
					changedLabelElem.appendChild(addedLiteralFormElem);
				}
				if(changedLabel.getResource() != null && !changedLabel.getResource().isEmpty()){
					Element resourceElem = document.createElement("div");
					resourceElem.setTextContent("Resource: "+changedLabel.getResource());
					changedLabelElem.appendChild(resourceElem);
				} else {
					Element removedResourceElem = document.createElement("div");
					removedResourceElem.setTextContent("Removed Resource: "+changedLabel.getRemovedResouce());
					changedLabelElem.appendChild(removedResourceElem);
					Element addedResourceElem = document.createElement("div");
					addedResourceElem.setTextContent("Added Resource Form: "+changedLabel.getAddedResource());
					changedLabelElem.appendChild(addedResourceElem);
				}
				Element removedNotesElem = document.createElement("div");
				removedNotesElem.setTextContent("Removed Notes:");
				changedLabelElem.appendChild(removedNotesElem);
				for(String propIri : changedLabel.getNotePropToRemovedNoteValueListMap().keySet()){
					Element propElem = document.createElement("div");
					propElem.setTextContent("Note Property: "+propIri);
					removedNotesElem.appendChild(propElem);
					Element notesElem = document.createElement("div");
					notesElem.setTextContent("Notes:");
					propElem.appendChild(notesElem);
					for(String removedNote : changedLabel.getNotePropToRemovedNoteValueListMap().get(propIri)){
						Element noteElem = document.createElement("div");
						noteElem.setTextContent("note: "+removedNote);
						notesElem.appendChild(noteElem);
					}
				}
				Element addedNotesElem = document.createElement("div");
				addedNotesElem.setTextContent("Removed Notes:");
				changedLabelElem.appendChild(addedNotesElem);
				for(String propIri : changedLabel.getNotePropToAddedNoteValueListMap().keySet()){
					Element propElem = document.createElement("div");
					propElem.setTextContent("Note Property: "+propIri);
					addedNotesElem.appendChild(propElem);
					Element notesElem = document.createElement("div");
					notesElem.setTextContent("Notes:");
					propElem.appendChild(notesElem);
					for(String addedNote : changedLabel.getNotePropToAddedNoteValueListMap().get(propIri)){
						Element noteElem = document.createElement("div");
						noteElem.setTextContent("note: "+addedNote);
						notesElem.appendChild(noteElem);
					}
				}
				Element removedPropElem = document.createElement("div");
				removedPropElem.setTextContent("Removed Properties Values:");
				changedLabelElem.appendChild(removedPropElem);
				for(String propIri : changedLabel.getPropToRemovedValueListMap().keySet()){
					Element propElem = document.createElement("div");
					propElem.setTextContent("Property: "+propIri);
					removedPropElem.appendChild(propElem);
					Element valuesElem = document.createElement("div");
					valuesElem.setTextContent("Values:");
					propElem.appendChild(valuesElem);
					for(String value : changedLabel.getPropToRemovedValueListMap().get(propIri)){
						Element valueElem = document.createElement("div");
						valueElem.setTextContent("value: "+value);
						valuesElem.appendChild(valueElem);
					}
				}
				Element addedPropElem = document.createElement("div");
				addedPropElem.setTextContent("Added Properties Values:");
				changedLabelElem.appendChild(addedPropElem);
				for(String propIri : changedLabel.getPropToAddedValueListMap().keySet()){
					Element propElem = document.createElement("div");
					propElem.setTextContent("Property: "+propIri);
					addedPropElem.appendChild(propElem);
					Element valuesElem = document.createElement("div");
					valuesElem.setTextContent("Values:");
					propElem.appendChild(valuesElem);
					for(String removedLex : changedLabel.getPropToAddedValueListMap().get(propIri)){
						Element valueElem = document.createElement("div");
						valueElem.setTextContent("value: "+removedLex);
						valuesElem.appendChild(valueElem);
					}
				}
			}
		}

		DOMSource domSource = new DOMSource(document);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		return sw.toString();
	}

	private IRI getSparqlEndpoint(Project project, String versionId) {
		IRI sparqlEndpoint = null;
		if (versionId == null) {
			versionId = Project.CORE_REPOSITORY;
		}
		RepositoryImplConfig coreRepoImplConfig = STLocalRepositoryManager.getUnfoldedRepositoryImplConfig(
				project.getRepositoryManager().getRepositoryConfig(versionId));
		if (coreRepoImplConfig instanceof HTTPRepositoryConfig) {
			sparqlEndpoint = SimpleValueFactory.getInstance().createIRI(((HTTPRepositoryConfig) coreRepoImplConfig).getURL());
		}
		return sparqlEndpoint;
	}

	public enum ResultType{pdf, html, json}

}
