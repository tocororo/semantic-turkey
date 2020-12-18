package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * Use <a href="https://jira.atlassian.com/">Atlassian Jira</a> as a collaboration backend.
 * 
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 *
 */
public class JiraBackend implements CollaborationBackend {

	private Project stProject;
	
	private static final String USER_AGENT = "Fake client"; 
	//private final String COOKIES_HEADER = "Set-Cookie";
	private final String projectTypeKey = "software";
	private final String projectTemplateKey = "com.pyxis.greenhopper.jira:basic-software-development-template";
	
	//private final String issueTypeId = "10002";
	
	private final String[] issueNameArray = {"Task", "New Feature", "Improvement", "Bug"};

	private final JiraBackendFactory factory; 
	
	public JiraBackend(JiraBackendFactory factory) {
		this.factory = factory;
	}

	@Override
	public void bind2project(Project project) {
		this.stProject = project;
	}


	@Override 
	public void checkPrjConfiguration() throws STPropertyAccessException, IOException, CollaborationBackendException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//now check that there a JIRA project having such id and key
		boolean found = false;
		String prjId = projectSettings.jiraPrjId;
		String prjKey = projectSettings.jiraPrjKey;
		//get all the projects and see if one of them has the desired id and key
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"project";

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		
		ArrayNode prjFromJiraArray = (ArrayNode) objectMapper.readTree(response);
		Map<String, String> prjIdToPrjKeyMap = new HashMap<>();
		//add all the couple id-key to a map
		for(JsonNode prjFromJiraNode : prjFromJiraArray) {
			String currentPrjId = prjFromJiraNode.get("id").asText();
			String currentPrjKey = prjFromJiraNode.get("key").asText();
			prjIdToPrjKeyMap.put(currentPrjId, currentPrjKey);
		}
		if(prjIdToPrjKeyMap.containsKey(prjId) && prjIdToPrjKeyMap.get(prjId).equals(prjKey)) {
			found = true;
		}
		
		//if no project was found having the desired id-key, then throw and exception
		if(!found) {
			throw new CollaborationBackendException("Invalid Configuration: there is no Jira project having id:"+
					prjId+" and key:"+prjKey);
		}
		
	}
	
	@Override
	public JiraIssueCreationForm getCreateIssueForm() {
		return new JiraIssueCreationForm();
	}
	
	
	@Override
	public void createIssue(String resource, ObjectNode issueCreationForm) 
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		//first of all, check that there is a valid associated Jira Project
		checkPrjConfiguration();

		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//if String issueId is null, then get all the possible issueId for the desired project and select one 
		//according to a specific list
		String issueTypeId = "";
		/*if(issueId==null) {
			issueTypeId = getIssueIdToCreate(cookieManager);
		} else {
			issueTypeId = issueId;
		}*/
		issueTypeId = getIssueIdToCreate(projectPreferences.username, projectPreferences.password);
		
		//now create the issue
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"issue";

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/json;charset=UTF-8", projectPreferences);


		String summary = escapeString(issueCreationForm.get("summary").textValue());
		// @formatter:off
		String postJsonData = "{"
				+"\n\"fields\": {"
				//summary
				+"\n\"summary\":\""+summary+"\","
				//issuetype
				+"\n\"issuetype\": {"
				+"\n\"id\":\""+issueTypeId+"\","
				+"\n\"description\":\""+issueTypeId+"\""
				+"\n},"
				+"\n\"project\": {"
				//+"\n\"name\":\""+projectSettings.jiraPrjName+"\","
				+"\n\"id\":\""+projectSettings.jiraPrjId+"\""
				//+"\n\"description\":\""+newPrjId+"\""
				+"\n},";
				//reporter
				//+"\n\"reporter\": {"
				//+"\n\"name\":\""+projectPreferences.username+"\""
				//+"\n},";
		//assign to the one who created it
		/*postJsonData +=
				//assignee
				"\n\"assignee\": {"
				+"\n\"name\":\""+projectPreferences.username+"\""
				+"\n},";*/
		/*if(assignee!=null && assignee.length()!=0) {
			postJsonData +=
				//assignee
				"\n\"assignee\": {"
				+"\n\"name\":\""+assignee+"\""
				+"\n},";
		}*/
		if(issueCreationForm.get("description")!=null && 
				issueCreationForm.get("description").textValue().length() !=0) {
			String description = escapeString(issueCreationForm.get("description").textValue());
			postJsonData += 
				//description
				"\n\"description\":\""+description+"\",";
		}
		postJsonData +=
				//labels
				"\n\"labels\": ["
				+"\n\""+resource+"\""
				+"\n]"
				//+"\n\"labels\":\"http://test.it/c_42\""
				+ "\n}"
				+ "\n}"; 
		// @formatter:on
		
		// Send post request
		addPostToHttpURLConnection(httpcon, postJsonData);

		executeAndCheckError(httpcon);
		
	}
	
	/**
	 * In this implementation, projectJson is a json object with attributes key, id and name.
	 * Note that the attributes of this object, are the same of the projects json objects returned by 
	 * listProjects() 
	 */
	@Override
	public void assignProject(ObjectNode projectJson)
			throws STPropertyAccessException, IOException, CollaborationBackendException, STPropertyUpdateException {
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		//now save all the information related to the Jira project
		JsonNode idNode = projectJson.get("id");
		if (idNode == null || idNode instanceof NullNode) {
			throw new CollaborationBackendException("'id' attribute is missing in the project Json object.");
		}
		JsonNode keyNode = projectJson.get("key");
		if (keyNode == null || keyNode instanceof NullNode) {
			throw new CollaborationBackendException("'key' attribute is missing in the project Json object.");
		}
		JsonNode nameNode = projectJson.get("name");
		if (nameNode == null || nameNode instanceof NullNode) {
			throw new CollaborationBackendException("'name' attribute is missing in the project Json object.");
		}
		projectSettings.jiraPrjId = idNode.textValue();
		projectSettings.jiraPrjKey = keyNode.textValue();
		//projectSettings.jiraPrjName = nameNode.textValue();
		factory.storeProjectSettings(stProject, projectSettings);
	}
	
	@Override
	public void createProject(ObjectNode projectJson) 
			throws STPropertyAccessException, JsonProcessingException, IOException, CollaborationBackendException, 
			STPropertyUpdateException {
		
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}

		JsonNode keyNode = projectJson.get("key");
		if (keyNode == null || keyNode instanceof NullNode) {
			throw new CollaborationBackendException("'key' attribute is missing in the project Json object.");
		}
		JsonNode nameNode = projectJson.get("name");
		if (nameNode == null || nameNode instanceof NullNode) {
			throw new CollaborationBackendException("'name' attribute is missing in the project Json object.");
		}

		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//now create the project
		String urlString = projectSettings.serverURL+"/rest/api/2/" + "project";

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.POST, urlString, "application/json;charset=UTF-8",
				projectPreferences);

		String projectName = nameNode.textValue();
		String projectKey = keyNode.textValue();

		String postJsonData = "{" + 
				"\n\"name\": \"" + projectName + "\"," + 
				"\n\"key\": \"" + projectKey + "\"," + 
				"\n\"lead\": \""+projectPreferences.username+"\"," +
				"\n\"projectTypeKey\": \""+projectTypeKey+"\"," +
				"\n\"description\": \"Example Project description\"," +
				"\n\"projectTemplateKey\":\""+projectTemplateKey+"\"" +
				"\n}";

		addPostToHttpURLConnection(httpcon, postJsonData);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		//finally save all the info associated to the newly created project (take the id from the response)
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response);
		String projectId = rootNode.get("id").asText();
		projectSettings.jiraPrjId = projectId;
		projectSettings.jiraPrjKey = projectKey;
		//projectSettings.jiraPrjName = projectName;
		factory.storeProjectSettings(stProject, projectSettings);
	}

	@Override
	public void assignResourceToIssue(String issueKey, IRI resource) throws STPropertyAccessException, IOException, CollaborationBackendException {
		//first of all, check that there is a valid associated Jira Project
		checkPrjConfiguration();
				
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		// now update the labels of the desired issue with the IRI of the input Resource
		String url = projectSettings.serverURL+"/rest/api/2/"+ "issue/"+issueKey;

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.PUT, url, "application/json;charset=UTF-8",
				projectPreferences);

		String postJsonData = "{"
				+"\n\"update\":{"
				+"\n\"labels\": "
				+"\n[{\"add\":"+
				"\n\""+resource.stringValue()+"\""
				+ "\n}]"
				+ "\n}"	
				+ "\n}";

		addPostToHttpURLConnection(httpcon, postJsonData);

		executeAndCheckError(httpcon);
	}

	@Override
	public JsonNode listIssuesAssignedToResource(IRI resource) throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		//first of all, check that there is a valid associated Jira Project
		checkPrjConfiguration();
		
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//now ask jira for all the issue asosciated to the desired Resource
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"search";

		URL url = new URL(urlString);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.POST, urlString, "application/json;charset=UTF-8",
				projectPreferences);

		String jqlString = "labels = \\\""+resource.stringValue()+"\\\" AND project ="
				+ "\\\""+projectSettings.jiraPrjId+"\\\"";
		
		String postJsonData = "{"
				+"\n\"jql\":\""+jqlString+"\","
				+"\n\"startAt\":0"
				//+"\n\"maxResults\":100,"
				/*+"\n\"fields\": ["
				+"\n\"status\","
				+"\n\"labels\","
				+"\n\"summary\""
				+"\n]"*/
				+ "\n}";

		addPostToHttpURLConnection(httpcon, postJsonData);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response);
		
		int issueNum = Integer.parseInt(rootNode.get("total").asText());
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode issueArrayResponse = jsonFactory.arrayNode();
		
		ArrayNode issuesArray = (ArrayNode) rootNode.get("issues");
		for(int i=0; i<issueNum; i++) {
			JsonNode issue = issuesArray.get(i);
			ObjectNode issueRedux = parseIssue(issue, projectSettings);
			issueArrayResponse.add(issueRedux);
		}
		
		return issueArrayResponse;
	}
	
	
	@Override
	public JsonNode listIssues(int pageOffset) throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		int maxResult = 100;
		int startAt = pageOffset*maxResult;
		
		//first of all, check that there is a valid associated Jira Project
		checkPrjConfiguration();
		
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//now ask jira for all the issue in the desired project
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"search";

		URL url = new URL(urlString);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.POST, urlString, "application/json;charset=UTF-8",
				projectPreferences);

		String jqlString = "project =\\\""+projectSettings.jiraPrjId+"\\\"";
		
		String postJsonData = "{"
				+"\n\"jql\":\""+jqlString+"\","
				+"\n\"startAt\":"+startAt+","
				+"\n\"maxResults\":"+maxResult+""
				/*+"\n\"fields\": ["
				+"\n\"status\","
				+"\n\"labels\","
				+"\n\"summary\""
				+"\n]"*/
				+ "\n}";


		addPostToHttpURLConnection(httpcon, postJsonData);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response);
		
		int total = Integer.parseInt(rootNode.get("total").asText());
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		
		ArrayNode issuesArray = (ArrayNode) rootNode.get("issues");
		//Map<String, List<ObjectNode>> resToIssuesMap = new HashMap<>();
		List<ObjectNode> issueNodeList = new ArrayList<>();
		List<String> issueIdList = new ArrayList<>();
		
		for(int i=0; i<issuesArray.size(); i++) {
			JsonNode issue = issuesArray.get(i);
			if(issue==null) {
				//it is not a real issue, so just skip it
				continue;
			}
			ObjectNode issueRedux = parseIssue(issue, projectSettings);
			String issueId = issueRedux.get("id").asText();
			if(!issueIdList.contains(issueId)) {
				issueIdList.add(issueId);
				issueNodeList.add(issueRedux);
			}
		}
		//now construct the response using the issue contained in the issueNodeList 
		ArrayNode arrayNode = jsonFactory.arrayNode();
		for(ObjectNode issueRedux : issueNodeList) {
			arrayNode.add(issueRedux);
		}
		
		int start = pageOffset*maxResult;
		int end = start + arrayNode.size() ;
		String more = (end<total) ? "true" : "false";
		int numPagesTotal = total/maxResult;
		if(total%maxResult != 0) {
			++numPagesTotal;
		}
		
		ObjectNode objectNode = jsonFactory.objectNode();
		objectNode.set("more", jsonFactory.textNode(more));
		objectNode.set("numIssues", jsonFactory.textNode(total+""));
		objectNode.set("numPagesTotal", jsonFactory.textNode(numPagesTotal+""));
		
		objectNode.set("issues",arrayNode);
		
		
		return objectNode;
	}

	/*
	@Override
	public JsonNode listUsers() throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		//first of all, check that there is a valid associated Jira Project
		checkPrjConfiguration();
		
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//now ask JIRA for all the issue associated to the desired Resource
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"user/assignable/search?";

		urlString += "project="+projectSettings.jiraPrjKey;

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString, "application/json;charset=UTF-8",
				projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode userArray = (ArrayNode) objectMapper.readTree(response);
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayResponse = jsonFactory.arrayNode();
		
		for(int i=0; i<userArray.size(); i++) {
			JsonNode user = userArray.get(i);
			ObjectNode issueRedux = parseUser(user);
			userArrayResponse.add(issueRedux);
		}
		
		return userArrayResponse;
	}
	*/
	
	
	@Override
	public JsonNode listProjects() throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		JiraBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		
		//now ask jira for all the project
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"project";

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString, "application/json;charset=UTF-8",
				projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		
		ArrayNode prjFromJiraArray = (ArrayNode) objectMapper.readTree(response);
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ObjectNode respNode = jsonFactory.objectNode();
		
		ArrayNode headerArray = jsonFactory.arrayNode();
		respNode.set("headers", headerArray);
		headerArray.add("key");
		headerArray.add("name");
		
		ArrayNode prjArray = jsonFactory.arrayNode();
		respNode.set("projects", prjArray);
		for(JsonNode prjFromJiraNode : prjFromJiraArray) {
			String prjId = prjFromJiraNode.get("id").asText();
			String prjKey = prjFromJiraNode.get("key").asText();
			String prjName = prjFromJiraNode.get("name").asText();
			ObjectNode projectRedux = jsonFactory.objectNode();
			projectRedux.set("id", jsonFactory.textNode(prjId));
			projectRedux.set("key", jsonFactory.textNode(prjKey));
			projectRedux.set("name", jsonFactory.textNode(prjName));
			prjArray.add(projectRedux);
		}
		
		return respNode;
	}
	
	@Override
	public boolean isProjectLinked() throws STPropertyAccessException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		//return projectSettings.jiraPrjName != null && projectSettings.jiraPrjKey != null 
		//		&& projectSettings.jiraPrjId!=null;
		return projectSettings.jiraPrjKey != null && projectSettings.jiraPrjId!=null;
	}

	
	/*** PRVATE METHODS ***/
	private String generateEncodeBase64String(String username, String password) {
		String userAndPass = username+":"+password;
		String encodedUserAndPass = Base64.encodeBase64String(userAndPass.getBytes());
		return encodedUserAndPass;
	}
	
	
	/*private CookieManager login(String username, String password, String urlString) throws IOException, 
			CollaborationBackendException{

		String url = urlString+"/rest/auth/1/session";
		URL obj = new URL(url);
		HttpURLConnection httpcon = (HttpURLConnection) obj.openConnection();

		// Setting basic post request
		httpcon.setRequestMethod("POST");
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		String postJsonData = "{ \"username\":\"" + username + "\" , \"password\":\"" + password + "\"}";

		// Send post request
		httpcon.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();

		executeAndCheckError(httpcon);

		// Save the cookie
		List<String> cookiesHeader = httpcon.getHeaderFields().get(COOKIES_HEADER);
		CookieManager msCookieManager = new java.net.CookieManager();
		if (cookiesHeader != null) {
			for (String cookie : cookiesHeader) {
				msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
			}
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		return msCookieManager;
	}*/
	
	private void executeAndCheckError(HttpURLConnection httpcon) throws IOException, CollaborationBackendException {
		int respCode = httpcon.getResponseCode();
		InputStream errorStream = httpcon.getErrorStream();
		if (errorStream != null) {
			@SuppressWarnings("resource")
			Scanner s = new Scanner(errorStream).useDelimiter("\\A");
			String errorString = s.hasNext() ? s.next() : "";
			s.close();
			errorStream.close();
			throw new CollaborationBackendException(respCode+" : "+errorString);
		}
	}
	
	private ObjectNode parseIssue(JsonNode issue, JiraBackendProjectSettings projectSettings) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		String issueId = issue.get("id").asText();
		String summary ="";
		if(issue.get("fields")!=null && issue.get("fields").get("status") !=null) {
			summary = issue.get("fields").get("summary").asText();
		}
		String issueStatus="";
		String issueStatusId="";
		if(issue.get("fields")!=null && issue.get("fields").get("status") !=null &&
				issue.get("fields").get("status").get("name")!=null) {
			issueStatus=issue.get("fields").get("status").get("name").asText();
			issueStatusId = issue.get("fields").get("status").get("id").asText();
		};
		String category="";
		if(issue.get("fields") != null && issue.get("fields").get("status") != null &&
				issue.get("fields").get("status").get("statusCategory") != null && 
				issue.get("fields").get("status").get("statusCategory").get("name") != null) {
			category = issue.get("fields").get("status").get("statusCategory").get("name").asText();
		};
		String issueKey = issue.get("key").asText();
		String optionalSlash = "";
		if(!projectSettings.serverURL.endsWith("/")) {
			optionalSlash = "/";
		}
		String urlIssue = projectSettings.serverURL+optionalSlash+"browse/"+issueKey;
		//check the labels, which is an array. Consider only those labels not having a whitespace and than can
		// be converted as an IRI
		ArrayNode tempLabelsArray = (ArrayNode) issue.get("fields").get("labels");
		ArrayNode labelsArray = jsonFactory.arrayNode();
		for(JsonNode label : tempLabelsArray) {
			String iriOrNot = label.asText();
			if(!iriOrNot.contains(" ")) {
				try {
					SimpleValueFactory.getInstance().createIRI(iriOrNot);
					labelsArray.add(iriOrNot);
				} catch (IllegalArgumentException e) {
					//do nothing
				}
			}
		}
		
		String resolution="";
		if(issue.get("fields").get("resolution") != null && 
				issue.get("fields").get("resolution").get("name")!=null) {
			resolution = issue.get("fields").get("resolution").get("name").asText();
		}
		/*ArrayNode labelsArray = (ArrayNode) issue.get("fields").get("labels");
		String labels = "";
		for(JsonNode labelNode : labelsArray) {
			labels += labelNode.asText()+",";
		}*/
		ObjectNode issueRedux = jsonFactory.objectNode();
		issueRedux.set("id", jsonFactory.textNode(issueId));
		issueRedux.set("summary", jsonFactory.textNode(summary));
		issueRedux.set("key", jsonFactory.textNode(issueKey));
		issueRedux.set("status", jsonFactory.textNode(issueStatus));
		issueRedux.set("statusId", jsonFactory.textNode(issueStatusId));
		issueRedux.set("url", jsonFactory.textNode(urlIssue));
		issueRedux.set("labels", labelsArray);
		issueRedux.set("resolution", jsonFactory.textNode(resolution));
		issueRedux.set("category", jsonFactory.textNode(category));
		return issueRedux;
	}
	
	private ObjectNode parseUser(JsonNode user){
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		String userKey = user.get("key").asText();
		String userName = user.get("name").asText();
		String userEmail = user.get("emailAddress").asText();
		String displayName = user.get("displayName").asText();
		
		ObjectNode userRedux = jsonFactory.objectNode();
		userRedux.set("key", jsonFactory.textNode(userKey));
		userRedux.set("name", jsonFactory.textNode(userName));
		userRedux.set("email", jsonFactory.textNode(userEmail));
		userRedux.set("dispalyName", jsonFactory.textNode(displayName));
		return userRedux;
	}
	
	private String getIssueIdToCreate(String username, String password) 
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		String issueId = "";
		
		String urlString = factory.getProjectSettings(stProject).serverURL+"/rest/api/2/"+"issue/createmeta;"
				+ "?expand=projects.issuetypes.fields"
				+ "&projectKeys="+factory.getProjectSettings(stProject).jiraPrjKey;
		
		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		// By default it is GET request
		httpcon.setRequestMethod("GET");

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Authorization", "Basic " + 
				generateEncodeBase64String(username, password));

		executeAndCheckError(httpcon);
		
		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();
		
		// analyze the json response and print all the value inside it
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.toString());
		
		Map<String, String> issueNameToIdMap = new HashMap<>();
		
		ArrayNode projectArrayNode = (ArrayNode) rootNode.get("projects");
		ArrayNode issueTypesArrayNode = (ArrayNode) projectArrayNode.get(0).get("issuetypes");
		for(int i=0; i<issueTypesArrayNode.size(); ++i) {
			JsonNode issueNode = issueTypesArrayNode.get(i);
			String id = issueNode.get("id").textValue();
			String name = issueNode.get("name").textValue().toLowerCase();
			issueNameToIdMap.put(name, id);
		}
		
		//now consult the array containing the desired issue type and get the first id to the first matching name
		boolean foundId = false;
		for(String name : issueNameArray) {
			if(issueNameToIdMap.containsKey(name.toLowerCase())) {
				issueId = issueNameToIdMap.get(name.toLowerCase());
				//a possible id was found, so exit the for
				foundId = true;
				break;
			}
		}
		if(!foundId) {
			//no candidate id was found, so just take one element from the map 
			issueId = issueNameToIdMap.values().iterator().next();
		}
			
		
		return issueId;
	}
	
	private String escapeString(String text) {
		String escaptedText = text.replace("\\", "\\\\");
		escaptedText = escaptedText.replace("\"", "\\\"");
		escaptedText = escaptedText.replace("\n", "\\n");
		escaptedText = escaptedText.replace("\r", "\\r");
		escaptedText = escaptedText.replace("\t", "\\t");
		return escaptedText;
	}


	private HttpURLConnection prepareHttpURLConnection(ConnType connType, String urlString,
			String contentType, JiraBackendPUSettings projectPreferences) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		httpcon.setRequestMethod(connType.name());

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", contentType);
		httpcon.setRequestProperty("Authorization", "Basic " +
				generateEncodeBase64String(projectPreferences.username, projectPreferences.password));

		return  httpcon;
	}

	public void addPostToHttpURLConnection(HttpURLConnection httpcon, String postJsonData)
			throws IOException {
		httpcon.setDoOutput(true);

		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();
	}

	private String readRsponse(HttpURLConnection httpcon) throws IOException {
		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		return response.toString();
	}

	public enum ConnType {
		GET, POST, PUT
	}
}
