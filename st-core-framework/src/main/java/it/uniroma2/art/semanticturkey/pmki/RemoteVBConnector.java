package it.uniroma2.art.semanticturkey.pmki;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.Role;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class RemoteVBConnector {

	private ObjectMapper mapper;

	private CloseableHttpClient httpClient;
	private String stHost;
	private String adminEmail;
	private String adminPwd;
	private String vbUrl;

	public RemoteVBConnector() throws IOException, STPropertyAccessException {
		mapper = new ObjectMapper();
		httpClient = HttpClientBuilder.create().build();

		String vbConfValue = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_VB_CONFIG_FOR_PMKI);
		if (vbConfValue == null) {
			throw new IllegalStateException("No configuration found for connecting to a remote VocBench instance");
		}

		JsonNode confJson = mapper.readTree(vbConfValue);
		stHost = confJson.get("stHost").textValue();
		if (!stHost.endsWith("/")) {
			stHost += "/";
		}
		adminEmail = confJson.get("adminEmail").textValue();
		adminPwd = confJson.get("adminPassword").textValue();
		vbUrl = confJson.get("vbUrl").textValue();
	}

	public String getVocbenchUrl() {
		return this.vbUrl;
	}

	public ObjectNode loginAdmin() throws IOException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Auth/login";
		HttpPost httpPost = new HttpPost(requestUrl);
		//Request parameters and other properties
		List<NameValuePair> params = new ArrayList<>(2);
		params.add(new BasicNameValuePair("email", adminEmail));
		params.add(new BasicNameValuePair("password", adminPwd));
		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		//Execute and get the response
		CloseableHttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}

	public ObjectNode createProject(String projectName, String baseURI, IRI model, IRI lexicalizationModel, PluginSpecification coreRepoSailConfigurerSpecification) throws IOException, URISyntaxException {
		ObjectNode remoteConfigurationJson;

		ObjectNode respJson = getRemoteAccessConfigurations();
		System.out.println(respJson);
		JsonNode remoteConfigsJson = respJson.findValue("remote_configs");
		if (remoteConfigsJson.isTextual()) {
			String remoteConfigsValue = remoteConfigsJson.asText();
			ArrayNode configsJson = (ArrayNode) mapper.readTree(remoteConfigsValue);
			System.out.println(configsJson);
			remoteConfigurationJson = (ObjectNode) configsJson.get(0);
		} else {
			throw new IllegalStateException("Remote VocBench has not been configured with a'Remote Access configurations'");
		}

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		remoteConfigurationJson.set("@type", jsonFactory.textNode("CreateRemote"));

		String coreRepoConfigSpec = mapper.writeValueAsString(coreRepoSailConfigurerSpecification);

		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Projects/createProject";
		HttpPost httpPost = new HttpPost(requestUrl);
		//Request parameters and other properties
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("consumer", ProjectConsumer.SYSTEM.getName()));
		params.add(new BasicNameValuePair("projectName", projectName));
		params.add(new BasicNameValuePair("baseURI", baseURI));
		params.add(new BasicNameValuePair("model", NTriplesUtil.toNTriplesString(model)));
		params.add(new BasicNameValuePair("lexicalizationModel", NTriplesUtil.toNTriplesString(lexicalizationModel)));
		params.add(new BasicNameValuePair("historyEnabled", "false"));
		params.add(new BasicNameValuePair("validationEnabled", "false"));
		params.add(new BasicNameValuePair("repositoryAccess", remoteConfigurationJson.toString()));
		params.add(new BasicNameValuePair("coreRepoID", projectName+"_core"));
		params.add(new BasicNameValuePair("coreRepoSailConfigurerSpecification", coreRepoConfigSpec));
		params.add(new BasicNameValuePair("supportRepoID", projectName+"_support")); //this will not be used (no H or V), but it still mandatory

		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}

	public ObjectNode registerUser(String email, String password, String givenName, String familyName, String organization) throws IOException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Users/registerUser";
		HttpPost httpPost = new HttpPost(requestUrl);

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("email", email));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("givenName", givenName));
		params.add(new BasicNameValuePair("familyName", familyName));
		params.add(new BasicNameValuePair("affiliation", organization));
		params.add(new BasicNameValuePair("sendNotification", "false"));

		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}

	public ObjectNode enableUser(String email) throws IOException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Users/enableUser";
		HttpPost httpPost = new HttpPost(requestUrl);

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("email", email));
		params.add(new BasicNameValuePair("enabled", "true"));
		params.add(new BasicNameValuePair("sendNotification", "false"));

		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}

	public ObjectNode addRolesToUser(String projectName, String email, List<Role> roles) throws IOException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Administration/addRolesToUser";
		HttpPost httpPost = new HttpPost(requestUrl);

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("projectName", projectName));
		params.add(new BasicNameValuePair("email", email));
		List<String> rolesName = roles.stream().map(Role::getName).collect(Collectors.toList());
		params.add(new BasicNameValuePair("roles", String.join(",", rolesName)));

		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}

	public ObjectNode getProjectInfo(String projectName) throws IOException, URISyntaxException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Projects/getProjectInfo";
		URIBuilder builder = new URIBuilder(requestUrl);
		builder.setParameter("consumer", ProjectConsumer.SYSTEM.getName());
		builder.setParameter("projectName", projectName);
		HttpGet httpGet = new HttpGet(builder.build());
		httpGet.addHeader(HttpHeaders.ACCEPT, "application/json");
		//Execute and get the response
		HttpResponse response = httpClient.execute(httpGet);
		return processResponse(response);
	}

	public ObjectNode loadRDF(String projectName, String baseURI, File file, String format,
			PluginSpecification rdfLifterSpec, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws URISyntaxException, IOException {

		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/InputOutput/loadRDF";
		URIBuilder uriBuilder = new URIBuilder(requestUrl);
		uriBuilder.setParameter("ctx_project", projectName);
		HttpPost httpPost = new HttpPost(uriBuilder.build());

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
		builder.addPart("inputFile", fileBody);
		builder.addPart("baseURI", new StringBody(baseURI, ContentType.MULTIPART_FORM_DATA));
		builder.addPart("transformationPipeline", new StringBody("[]", ContentType.MULTIPART_FORM_DATA));
		builder.addPart("rdfLifterSpec", new StringBody(mapper.writeValueAsString(rdfLifterSpec), ContentType.MULTIPART_FORM_DATA));
		builder.addPart("format", new StringBody(format, ContentType.MULTIPART_FORM_DATA));
		builder.addPart("transitiveImportAllowance", new StringBody(transitiveImportAllowance.name(), ContentType.MULTIPART_FORM_DATA));

		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		HttpResponse response = httpClient.execute(httpPost);
		return processResponse(response);
	}


	private ObjectNode getRemoteAccessConfigurations() throws URISyntaxException, IOException {
		String requestUrl = stHost + "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/PreferencesSettings/getSystemSettings";
		URIBuilder builder = new URIBuilder(requestUrl);
		builder.setParameter("properties", "remote_configs");
		System.out.println("getRemoteAccessConfigurations " + builder.build());
		HttpGet httpGet = new HttpGet(builder.build());
		httpGet.addHeader(HttpHeaders.ACCEPT, "application/json");
		//Execute and get the response
		System.out.println(httpGet.getURI());

		CloseableHttpResponse response = httpClient.execute(httpGet);
		return processResponse(response);
	}

	private ObjectNode processResponse(HttpResponse response) throws IOException {
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
			HttpEntity entity = response.getEntity();
			String contentType = entity.getContentType().getValue();
			if (contentType.contains("application/json")) {
				String responseAsString = EntityUtils.toString(entity);
				ObjectNode respJson = (ObjectNode) new ObjectMapper().readTree(responseAsString);
				JsonNode stresponseJson = respJson.get("stresponse");
				if (stresponseJson != null) {
					JsonNode exceptionJson = stresponseJson.get("exception");
					JsonNode msgJson = stresponseJson.get("msg");
					if (exceptionJson != null && msgJson != null) {
						throw new IOException("Remote SemanticTurkey Error: " + msgJson.textValue());
					}
				}
				return respJson;
			} else {
				throw new IllegalStateException("Unexpected SemanticTurkey response content type: " + contentType);
			}
		} else {
			throw new IOException("HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
		}
	}

}
