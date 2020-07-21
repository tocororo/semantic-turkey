package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.core.type.TypeReference;
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
import it.uniroma2.art.semanticturkey.services.core.skosdiff.DatasetInfo;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.DiffResultStructure;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.LabelWithResAndLitForm;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.ResourceWithLexicalization;
import it.uniroma2.art.semanticturkey.services.core.skosdiff.TaskInfo;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@STService
public class SkosDiffing extends STServiceAdapter {

	private final String serverHost = "http://localhost";
	private final String serverPort = "7576";

	private final String SPARQL_ENDPOINT_1 = "sparqlEndpoint1";
	private final String SPARQL_ENDPOINT_2 = "sparqlEndpoint2";
	private final String LEXICALIZATION_TYPE_1 = "lexicalizationType1";
	private final String LEXICALIZATION_TYPE_2 = "lexicalizationType2";
	private final String PROJECT_NAME_1 = "projectName1";
	private final String PROJECT_NAME_2 = "projectName2";
	private final String VERSION_REPO_ID_1 = "versionRepoId1";
	private final String VERSION_REPO_ID_2 = "versionRepoId2";

	private final String LANG_LIST = "langList";

	//private final String SKOSXL_LEX_MODE = "skosxl";
	//private final String SKOS_LEX_MODE = "skos";

	private static final String TEXT_HTML = "text/html";
	private static final String APPLICATION_PDF = "application/pdf";
	private static final String APPLICATION_JSON = "application/json";



	@STServiceOperation(method = RequestMethod.POST)
	public String runDiffing(String leftProjectName, @Optional String leftVersionRepoId,
			String rightProjectName, @Optional String rightVersionRepoId, @Optional List<String> langList) throws IOException {

		Project leftProject = ProjectManager.getProject(leftProjectName);
		IRI leftSparqlEndpoint = getSparqlEndpoint(leftProject, leftVersionRepoId);
		if (leftSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the left dataset");
		}
		IRI lexModelIRI = leftProject.getLexicalizationModel();
		//String leftLexicalizationType;
		if(!lexModelIRI.equals(Project.SKOSXL_LEXICALIZATION_MODEL) && !lexModelIRI.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			throw new IllegalStateException("The only supported lexicalization models are SKOS and SKOSXL, "+lexModelIRI.getLocalName()+" is not supported");
		}

		Project rightProject = ProjectManager.getProject(rightProjectName);
		IRI rightSparqlEndpoint = getSparqlEndpoint(rightProject, rightVersionRepoId);
		if (rightSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the right dataset");
		}
		lexModelIRI = leftProject.getLexicalizationModel();
		//String rightLexicalizationType;

		if(!lexModelIRI.equals(Project.SKOSXL_LEXICALIZATION_MODEL) && !lexModelIRI.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			throw new IllegalStateException("The only supported lexicalization models are SKOS and SKOSXL, "+lexModelIRI.getLocalName()+" is not supported");
		}


		//prepare the HTTP POST
		String url = serverHost+":"+serverPort+"/skosDiff/skosDiff/diff/executeDiffTask";
		HttpPost httpPost = new HttpPost(url);
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode objectNode = jsonFactory.objectNode();

		objectNode.set(SPARQL_ENDPOINT_1, jsonFactory.textNode(leftSparqlEndpoint.stringValue()));
		objectNode.set(LEXICALIZATION_TYPE_1, jsonFactory.textNode(leftProject.getLexicalizationModel().stringValue()));
		objectNode.set(PROJECT_NAME_1, jsonFactory.textNode(leftProjectName));
		if(leftVersionRepoId==null){
			objectNode.set(VERSION_REPO_ID_1, jsonFactory.textNode(""));
		} else {
			objectNode.set(VERSION_REPO_ID_1, jsonFactory.textNode(leftVersionRepoId));
		}

		objectNode.set(SPARQL_ENDPOINT_2, jsonFactory.textNode(rightSparqlEndpoint.stringValue()));
		objectNode.set(LEXICALIZATION_TYPE_2, jsonFactory.textNode(leftProject.getLexicalizationModel().stringValue()));
		objectNode.set(PROJECT_NAME_2, jsonFactory.textNode(rightProjectName));
		if(rightVersionRepoId==null){
			objectNode.set(VERSION_REPO_ID_2, jsonFactory.textNode(""));
		} else {
			objectNode.set(VERSION_REPO_ID_2, jsonFactory.textNode(rightVersionRepoId));
		}

		ArrayNode arrayNode = jsonFactory.arrayNode();
		if(langList!=null && !langList.isEmpty()){
			for (String lang : langList) {
				arrayNode.add(lang);
			}

		}
		objectNode.set(LANG_LIST, arrayNode);
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
	public List<TaskInfo> getAllTasksInfo(@Optional String projectName) throws IOException {

		List<TaskInfo> tasks = new ArrayList<>();

		String url;
		url = serverHost+":"+serverPort+"/skosDiff/skosDiff/diff/tasksInfo";
		if(projectName!=null && !projectName.isEmpty()){
			url += "?projectName="+projectName;
		}

		//prepare and execute the HTTP GET
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Accept", "application/json");
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse response = httpClient.execute(httpGet)) {

			// Get HttpResponse Status
			HttpEntity entity = response.getEntity();
			Header headers = entity.getContentType();
			if (entity != null) {
				// return it as a String
				String result = EntityUtils.toString(entity);
				tasks = new ObjectMapper().readValue(result, new TypeReference<List<TaskInfo>>(){});
			}
		}
		return tasks;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteTask(String taskId) throws IOException {
		String url = serverHost+":"+serverPort+"/skosDiff/skosDiff/diff/"+taskId;
		HttpDelete httpDelete = new HttpDelete(url);
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			 CloseableHttpResponse response = httpClient.execute(httpDelete)) {
		}
	}

	@STServiceOperation(method = RequestMethod.GET)
	public void getTaskResult(HttpServletResponse response, String taskId, ResultType resultType) throws IOException, ParserConfigurationException, TransformerException {
		String url = serverHost+":"+serverPort+"/skosDiff/skosDiff/diff/"+taskId;


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

		//==== START head ====
		Element headElem = document.createElement("head");
		htmlElem.appendChild(headElem);

		//title
		Node titleElem = document.createElement("title");
		titleElem.setTextContent("Result for task: "+diffResultStructure.getTaskId());
		headElem.appendChild(titleElem);

		//style
		Node styleElem = document.createElement("style");
		styleElem.setTextContent("body { font-size: 16px; } " +
				".section { margin-bottom: 30px; } " +
				".modifiedResSection { margin-bottom: 16px; } " +
				".modifiedResHeader { font-size: 18px; margin-bottom: 6px; } " +
				"ul { margin-top: 4px; margin-bottom: 4px; padding-inline-start: 26px; } " +
				"label { font-weight: bold; margin-right: 4px; } "
		);
		headElem.appendChild(styleElem);

		//==== END head ====

		//==== START body ====

		Element bodyElem = document.createElement("body");
		htmlElem.appendChild(bodyElem);

		//== START task info

		appendTextualElement(document, bodyElem, "h1", "Task info");

		//task id
		Element taskIdSection = document.createElement("div");
		bodyElem.appendChild(taskIdSection);
		taskIdSection.setAttribute("class", "section");

		appendTextualElement(document, taskIdSection, "label", "taskId:");

		appendTextualElement(document, taskIdSection, "span", diffResultStructure.getTaskId());

		//left dataset
		appendDatasetSection(document, bodyElem, diffResultStructure.getLeftDataset(), "Left dataset:");

		//right dataset
		appendDatasetSection(document, bodyElem, diffResultStructure.getRightDataset(), "Right dataset:");

		//== END task info

		//== START diffing results

		appendTextualElement(document, bodyElem, "h1", "Diffing results");

		//removed resources
		appendResourcesWithLexicalizationSection(document, bodyElem, diffResultStructure.getRemovedResources(), "Removed resources");

		//added resources
		appendResourcesWithLexicalizationSection(document, bodyElem, diffResultStructure.getAddedResources(), "Added resources");

		//changed resources
		Element changedResourcesSection = document.createElement("div");
		bodyElem.appendChild(changedResourcesSection);
		changedResourcesSection.setAttribute("class", "section");

		appendTextualElement(document, changedResourcesSection, "h2", "Changed resources");

		for (ChangedResource changedResource : diffResultStructure.getChangedResources()) {

			Element changedResBlock = document.createElement("div");
			changedResBlock.setAttribute("class", "modifiedResSection");
			changedResourcesSection.appendChild(changedResBlock);

			appendModifiedEntityHeader(document, changedResBlock, "Resource:", changedResource.getResourceId());

			Element changedResUl = document.createElement("ul");
			changedResBlock.appendChild(changedResUl);

			//removed lexicalizations
			appendUpdatedPropValues(document, changedResUl, changedResource.getLexPropToRemovedLexicalizationListMap(), "Removed lexicalizations:");

			//added lexicalizations
			appendUpdatedPropValues(document, changedResUl, changedResource.getLexPropToAddedLexicalizationListMap(), "Added lexicalizations:");

			//removed notes
			appendUpdatedPropValues(document, changedResUl, changedResource.getNotePropToRemovedNoteValueListMap(), "Removed notes:");

			//added notes
			appendUpdatedPropValues(document, changedResUl, changedResource.getNotePropToAddedNoteValueListMap(), "Added notes:");

			//removed notes
			appendUpdatedPropValues(document, changedResUl, changedResource.getPropToRemovedValueListMap(), "Removed property-values:");

			//added notes
			appendUpdatedPropValues(document, changedResUl, changedResource.getPropToAddedValueListMap(), "Added property-values:");

		}

		//labels changes
		if (diffResultStructure.getLeftDataset().getLexicalizationIRI().equals(Project.SKOSXL_LEXICALIZATION_MODEL.stringValue()) &&
				diffResultStructure.getRightDataset().getLexicalizationIRI().equals(Project.SKOSXL_LEXICALIZATION_MODEL.stringValue())) {

			//removed xlabels
			appendXLabelSection(document, bodyElem, diffResultStructure.getRemoveLabels(), "Removed SKOS-XL labels");

			//added xlabels
			appendXLabelSection(document, bodyElem, diffResultStructure.getAddedLabels(), "Added SKOS-XL labels");

			Element changedXlabelsSection = document.createElement("div");
			bodyElem.appendChild(changedXlabelsSection);
			changedXlabelsSection.setAttribute("class", "section");

			appendTextualElement(document, changedXlabelsSection, "h2", "Changed SKOS-XL labels");

			for(ChangedLabel changedLabel : diffResultStructure.getChangedLabels()){

				Element changedXlabelBlock = document.createElement("div");
				changedXlabelBlock.setAttribute("class", "modifiedResSection");
				changedXlabelsSection.appendChild(changedXlabelBlock);

				appendModifiedEntityHeader(document, changedXlabelBlock, "XLabel:", changedLabel.getLabel());

				Element changedXlabelUl = document.createElement("ul");
				changedXlabelBlock.appendChild(changedXlabelUl);

				//literal form
				if (changedLabel.getLiteralForm() != null && !changedLabel.getLiteralForm().isEmpty()) {
					appendPlainListItem(document, changedXlabelUl, "Literal Form:", changedLabel.getLiteralForm());
				} else { //nested ul with removed and added lit form
					Element litFormLi = document.createElement("li");
					changedXlabelUl.appendChild(litFormLi);

					appendTextualElement(document, litFormLi, "label", "Literal Form:");

					Element changedLitFormUl = document.createElement("ul");
					litFormLi.appendChild(changedLitFormUl);

					//removed
					appendPlainListItem(document, changedLitFormUl, "Removed:", changedLabel.getRemovedLiteralForm());
					//added
					appendPlainListItem(document, changedLitFormUl, "Added:", changedLabel.getAddedLiteralForm());
				}

				//lexicalized resource
				if (changedLabel.getResource() != null && !changedLabel.getResource().isEmpty()) {
					appendPlainListItem(document, changedXlabelUl, "Lexicalized Resource:", changedLabel.getResource());
				} else { //nested ul with removed and added resource
					Element lexicalizedResLi = document.createElement("li");
					changedXlabelUl.appendChild(lexicalizedResLi);

					appendTextualElement(document, lexicalizedResLi, "label", "Lexicalized Resource:");

					Element changedLexResUl = document.createElement("ul");
					lexicalizedResLi.appendChild(changedLexResUl);

					//removed
					appendPlainListItem(document, changedLexResUl, "Removed:", changedLabel.getRemovedResouce());
					//added
					appendPlainListItem(document, changedLexResUl, "Added:", changedLabel.getAddedResource());
				}

				//removed notes
				appendUpdatedPropValues(document, changedXlabelUl, changedLabel.getNotePropToRemovedNoteValueListMap(), "Removed notes:");
				//added notes
				appendUpdatedPropValues(document, changedXlabelUl, changedLabel.getNotePropToAddedNoteValueListMap(), "Added notes:");
				//removed prop-values
				appendUpdatedPropValues(document, changedXlabelUl, changedLabel.getPropToRemovedValueListMap(), "Removed property-values:");
				//added prop-values
				appendUpdatedPropValues(document, changedXlabelUl, changedLabel.getPropToAddedValueListMap(), "Added property-values:");

			}
		}

		//== END diffing results

		DOMSource domSource = new DOMSource(document);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		return sw.toString();
	}

	private void appendDatasetSection(org.w3c.dom.Document document, Element parentElem, DatasetInfo dataset, String title) {
		Element datasetSection = document.createElement("div");
		parentElem.appendChild(datasetSection);
		datasetSection.setAttribute("class", "section");

		appendTextualElement(document, datasetSection, "label", title);

		Element datasetUl = document.createElement("ul");
		datasetSection.appendChild(datasetUl);

		//project name
		appendPlainListItem(document, datasetUl, "Project Name:", dataset.getProjectName());

		//version repo id
		String versRepoId = dataset.getVersionRepoId();
		if (versRepoId != null || !versRepoId.equals("")) {
			appendPlainListItem(document, datasetUl, "Version Repository ID:", versRepoId);
		}

		//sparql endpoint
		appendPlainListItem(document, datasetUl, "SPARQL endpoint:", dataset.getSparqlEndpoint());

		//lexicalization model
		appendPlainListItem(document, datasetUl, "Lexicalization model:", dataset.getLexicalizationIRI());
	}

	private void appendResourcesWithLexicalizationSection(org.w3c.dom.Document document,
			Element bodyElem, List<ResourceWithLexicalization> resourcesWithLexicalization, String title) {

		Element removedResSection = document.createElement("div");
		bodyElem.appendChild(removedResSection);
		removedResSection.setAttribute("class", "section");

		appendTextualElement(document, removedResSection, "h2", title);

		for (ResourceWithLexicalization resourceWithLexicalization : resourcesWithLexicalization) {

			Element removedResBlock = document.createElement("div");
			removedResBlock.setAttribute("class", "modifiedResSection");
			removedResSection.appendChild(removedResBlock);

			appendModifiedEntityHeader(document, removedResBlock, "Resource:", resourceWithLexicalization.getResourceIri());

			Element lexicalizedResUl = document.createElement("ul");
			removedResBlock.appendChild(lexicalizedResUl);

			//types
			Element typesLi = document.createElement("li");
			lexicalizedResUl.appendChild(typesLi);

			appendTextualElement(document, typesLi, "label", "Types:");

			Element typesValueUl = document.createElement("ul");
			typesLi.appendChild(typesValueUl);

			for (String type : resourceWithLexicalization.getResourceTypeList()) {
				Element typeValueLi = document.createElement("li");
				typesValueUl.appendChild(typeValueLi);
				typeValueLi.setTextContent(type);
			}

			//lexicalizations
			Element lexicalizationsLi = document.createElement("li");
			lexicalizedResUl.appendChild(lexicalizationsLi);

			appendTextualElement(document, lexicalizationsLi, "label", "Lexicalizations:");

			Element lexicalizationsValueUl = document.createElement("ul");
			lexicalizationsLi.appendChild(lexicalizationsValueUl);

			for(String lexicalization : resourceWithLexicalization.getLexicalizationList()){
				Element lexicalizationValueLi = document.createElement("li");
				lexicalizationsValueUl.appendChild(lexicalizationValueLi);
				lexicalizationValueLi.setTextContent(lexicalization);
			}
		}
	}

	private void appendXLabelSection(org.w3c.dom.Document document, Element bodyElem,
			List<LabelWithResAndLitForm> labels, String title) {

		Element xlabelSection = document.createElement("div");
		bodyElem.appendChild(xlabelSection);
		xlabelSection.setAttribute("class", "section");

		appendTextualElement(document, xlabelSection, "h2", title);

		for (LabelWithResAndLitForm labelWithResAndLitForm : labels) {

			Element changedLexBlock = document.createElement("div");
			changedLexBlock.setAttribute("class", "modifiedResSection");
			xlabelSection.appendChild(changedLexBlock);

			appendModifiedEntityHeader(document, changedLexBlock, "XLabel:", labelWithResAndLitForm.getLabel());

			Element lexicalizationUl = document.createElement("ul");
			changedLexBlock.appendChild(lexicalizationUl);

			//lit form
			appendPlainListItem(document, lexicalizationUl, "Literal form:", labelWithResAndLitForm.getLiteralForm());
			//lexicalized resource
			appendPlainListItem(document, lexicalizationUl, "Lexicalized resource:", labelWithResAndLitForm.getResource());
		}
	}

	private void appendUpdatedPropValues(org.w3c.dom.Document document,
			Element parentUl, Map<String, List<String>> updatedProValuesMap, String itemLabel) {

		Element changedPropValuesLi = document.createElement("li");
		parentUl.appendChild(changedPropValuesLi);

		appendTextualElement(document, changedPropValuesLi, "label", itemLabel);

		Element changedPropValuesUl = document.createElement("ul");
		changedPropValuesLi.appendChild(changedPropValuesUl);

		for (String propIri : updatedProValuesMap.keySet()) {

			appendPlainListItem(document, changedPropValuesUl, "Property:", propIri);

			Element changedValuesLi = document.createElement("li");
			changedPropValuesUl.appendChild(changedValuesLi);

			appendTextualElement(document, changedValuesLi, "label", "Values:");

			Element changedValuesUl = document.createElement("ul");
			changedValuesLi.appendChild(changedValuesUl);
			for (String value : updatedProValuesMap.get(propIri)) {
				Element changedValueLi = document.createElement("li");
				changedValueLi.setTextContent(value);
				changedValuesUl.appendChild(changedValueLi);
			}
		}
	}

	//Utilities for the creation of html elements

	private void appendModifiedEntityHeader(org.w3c.dom.Document document, Element parentEl, String label, String value) {
		Element changedEntityHeader = document.createElement("div");
		changedEntityHeader.setAttribute("class", "modifiedResHeader");
		parentEl.appendChild(changedEntityHeader);
		appendTextualElement(document, changedEntityHeader, "label", label);
		appendTextualElement(document, changedEntityHeader, "span", value);
	}

	/**
	 * Create and append a list item with "label: value" to the given ul element
	 * @param document
	 * @param parentUl
	 * @param label
	 * @param value
	 */
	private void appendPlainListItem(org.w3c.dom.Document document, Element parentUl, String label, String value) {
		Element li = document.createElement("li");
		parentUl.appendChild(li);
		appendTextualElement(document, li, "label", label);
		appendTextualElement(document, li, "span", value);
	}

	/**
	 * Append a textual element (h1, h2, ..., p) with the given text content
	 * @param document
	 * @param parentEl
	 * @param elementTag
	 * @param textContent
	 */
	private void appendTextualElement(org.w3c.dom.Document document, Element parentEl, String elementTag, String textContent) {
		Element el = document.createElement(elementTag);
		parentEl.appendChild(el);
		el.setTextContent(textContent);
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
