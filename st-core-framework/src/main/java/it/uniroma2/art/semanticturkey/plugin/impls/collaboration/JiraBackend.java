package it.uniroma2.art.semanticturkey.plugin.impls.collaboration;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.exceptions.HTTPJiraException;
import it.uniroma2.art.semanticturkey.plugin.AbstractPlugin;
import it.uniroma2.art.semanticturkey.plugin.extpts.CollaborationBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * A {@link CollaborationBackend} for the <a href="https://jira.atlassian.com/">Atlassian Jira</a>
 *
 */
public class JiraBackend extends
		AbstractPlugin<STProperties, JiraBackendSettings, STProperties, JiraBackendPreferences, JiraBackendFactory>
		implements CollaborationBackend {

	private Project stProject;
	
	private static final String USER_AGENT = "Fake client"; 
	private final String COOKIES_HEADER = "Set-Cookie";
	private final String projectTypeKey = "software";
	private final String projectTemplateKey = "com.pyxis.greenhopper.jira:basic-software-development-template";
	
	private final String issueTypeId = "10002";
	
	public JiraBackend(JiraBackendFactory factory) {
		super(factory);
	}

	@Override
	public void bind2project(Project project) {
		this.stProject = project;
	}

	@Override
	public void createIssue(String resource, String summary) throws STPropertyAccessException, IOException, HTTPJiraException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(stProject);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(stProject,
				UsersManager.getLoggedUser());
		
		//first of all, do the login
		CookieManager cookieManager = login(projectPreferences.username, projectPreferences.password, 
				projectSettings.serverURL);
		
		//now create the issue

		String urlString = projectSettings.serverURL+"/rest/api/2/"+"issue";

		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		httpcon.setRequestMethod("POST");

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		
		String postJsonData = "{"
				+"\n\"fields\": {"
				//summary
				+"\n\"summary\":\""+summary+"\","
				//issuetype
				+"\n\"issuetype\": {"
				+"\n\"id\":\""+issueTypeId+"\","
				+"\n\"description\":\""+issueTypeId+"\""
				+"\n},"
				//project
				//+"\n\"project\": \""+newPrjKey+"\"," // use this or the other lines
				+"\n\"project\": {"
				//+"\n\"name\":\""+projectSettings.jiraPrjName+"\","
				+"\n\"id\":\""+projectSettings.jiraPrjId+"\""
				//+"\n\"description\":\""+newPrjId+"\""
				+"\n},"
				//reporter
				+"\n\"reporter\": {"
				+"\n\"name\":\""+projectPreferences.username+"\""
				+"\n},"
				//labels
				+"\n\"labels\": ["
				+"\n\""+resource+"\""
				+"\n]"
				//+"\n\"labels\":\"http://test.it/c_42\""
				+ "\n}"
				+ "\n}"; 
		
		// Add the cookie
		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			httpcon.setRequestProperty("Cookie",
					cookieManager.getCookieStore().getCookies().get(0).toString());
		}
		
		// Send post request
		httpcon.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();

		executeAndCheckError(httpcon);
		
	}

	@Override
	public void assignProject(String projectName, String projectKey, String projectId) 
			throws STPropertyAccessException, IOException, HTTPJiraException, STPropertyUpdateException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(stProject);
		if(projectId==null) {
			// since the projectId is not passed, ask Jira for the id associated to the project Key
			//first of all, do the login
			//the project ID is missing, so consult Jira to obtain such parameter
			JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(stProject,
					UsersManager.getLoggedUser());
			
			CookieManager cookieManager = login(projectPreferences.username, projectPreferences.password, 
					projectSettings.serverURL);
			
			String urlString = projectSettings.serverURL+"/rest/api/2/"+"project/"+projectKey;

			URL url = new URL(urlString);
			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

			// By default it is GET request
			httpcon.setRequestMethod("GET");

			// add request header
			httpcon.setRequestProperty("User-Agent", USER_AGENT);

			// Add the cookie
			if (cookieManager.getCookieStore().getCookies().size() > 0) {
				httpcon.setRequestProperty("Cookie",
						cookieManager.getCookieStore().getCookies().get(0).toString());
			}

			executeAndCheckError(httpcon);

			// Reading response from input Stream
			BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
			String output;
			StringBuffer response = new StringBuffer();

			while ((output = in.readLine()) != null) {
				response.append(output);
			}
			in.close();

			// analyze the json response to get the ID of the desired project
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(response.toString());
			projectId = rootNode.get("id").asText();
		}
		//now save all the information related to the Jira project
		projectSettings.jiraPrjId = projectId;
		projectSettings.jiraPrjKey = projectKey;
		projectSettings.jiraPrjName = projectName;
		storeClassLevelProjectSettings(stProject, projectSettings);
	}
		
	@Override
	public void createProject(String projectName, String projectKey) 
			throws STPropertyAccessException, JsonProcessingException, IOException, HTTPJiraException, 
			STPropertyUpdateException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(stProject);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(stProject,
				UsersManager.getLoggedUser());

		//first of all, do the login
		CookieManager cookieManager = login(projectPreferences.username, projectPreferences.password, 
				projectSettings.serverURL);
		
		//now create the project
		String urlString = projectSettings.serverURL+"/rest/api/2/" + "project";

		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		httpcon.setRequestMethod("POST");

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		String postJsonData = "{" + 
				"\n\"name\": \"" + projectName + "\"," + 
				"\n\"key\": \"" + projectKey + "\"," + 
				"\n\"lead\": \""+projectPreferences.username+"\"," +
				"\n\"projectTypeKey\": \""+projectTypeKey+"\"," +
				"\n\"description\": \"Example Project description\"," +
				"\n\"projectTemplateKey\":\""+projectTemplateKey+"\"" +
				"\n}";

		// Add the cookie
		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			httpcon.setRequestProperty("Cookie",
					cookieManager.getCookieStore().getCookies().get(0).toString());
		}

		// Send post request
		httpcon.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();


		executeAndCheckError(httpcon);

		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();
		
		//finally save all the info associated to the newly created project (take the id from the response)
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.toString());
		String projectId = rootNode.get("id").asText();
		projectSettings.jiraPrjId = projectId;
		projectSettings.jiraPrjKey = projectKey;
		projectSettings.jiraPrjName = projectName;
		storeClassLevelProjectSettings(stProject, projectSettings);
	}

	@Override
	public void assignResourceToIssue(String issueKey, IRI resource) throws STPropertyAccessException, IOException, HTTPJiraException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(stProject);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(stProject,
				UsersManager.getLoggedUser());

		//first of all, do the login
		CookieManager cookieManager = login(projectPreferences.username, projectPreferences.password, 
				projectSettings.serverURL);
		
		// now upda the labels of the desied issue with the IRI of the input Resorce
		String url = projectSettings.serverURL+"/rest/api/2/"+ "issue/"+issueKey;

		URL urlUpdate = new URL(url);
		HttpURLConnection httpconUpdate = (HttpURLConnection) urlUpdate.openConnection();

		// By default it is GET request
		httpconUpdate.setRequestMethod("PUT");

		// add request header
		httpconUpdate.setRequestProperty("User-Agent", USER_AGENT);
		httpconUpdate.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		String postJsonData = "{"
				+"\n\"update\":{"
				+"\n\"labels\": "
				+"\n[{\"add\":"+
				"\n\""+resource.stringValue()+"\""
				+ "\n}]"
				+ "\n}"	
				+ "\n}"; 
		
		// Add the cookie
		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			httpconUpdate.setRequestProperty("Cookie",
					cookieManager.getCookieStore().getCookies().get(0).toString());
		}

		// Send post request
		httpconUpdate.setDoOutput(true);
		DataOutputStream wrUpdate = new DataOutputStream(httpconUpdate.getOutputStream());
		wrUpdate.writeBytes(postJsonData);
		wrUpdate.flush();
		wrUpdate.close();

		executeAndCheckError(httpconUpdate);
		
	}

	@Override
	public JsonNode listIssuesAssignedToResource(IRI resource) throws STPropertyAccessException, IOException, 
			HTTPJiraException {
		if (stProject == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(stProject);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(stProject,
				UsersManager.getLoggedUser());
		
		//first of all, do the login
		CookieManager cookieManager = login(projectPreferences.username, projectPreferences.password, 
				projectSettings.serverURL);
		
		//now ask jira for all the issue asosciated to the desired Resource
		
		String urlString = projectSettings.serverURL+"/rest/api/2/"+"search";

		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		// By default it is GET request
		httpcon.setRequestMethod("POST");

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		String jqlString = "labels = \\\""+resource.stringValue()+"\\\" AND project ="
				+ "\\\""+projectSettings.jiraPrjId+"\\\"";
		
		String postJsonData = "{"
				+"\n\"jql\":\""+jqlString+"\","
				+"\n\"startAt\":0,"
				//+"\n\"maxResults\":100,"
				+"\n\"fields\": ["
				+"\n\"status\","
				+"\n\"labels\","
				+"\n\"summary\""
				+"\n]"
				+ "\n}"; 
		
		// Add the cookie
		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			httpcon.setRequestProperty("Cookie",
					cookieManager.getCookieStore().getCookies().get(0).toString());
		}

		// Send post request
		httpcon.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();

		executeAndCheckError(httpcon);

		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.toString());
		
		int issueNum = Integer.parseInt(rootNode.get("total").asText());
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode issueArrayResponse = jsonFactory.arrayNode();
		
		ArrayNode issuesArray = (ArrayNode) rootNode.get("issues");
		for(int i=0; i<issueNum; i++) {
			JsonNode issue = issuesArray.get(i);
			String issueId = issue.get("id").asText();
			String issueStatus = issue.get("fields").get("status").get("name").asText();
			String issueKey = issue.get("key").asText();
			String urlIssue = projectSettings.serverURL+"/browse/"+issueKey;
			/*ArrayNode labelsArray = (ArrayNode) issue.get("fields").get("labels");
			String labels = "";
			for(JsonNode labelNode : labelsArray) {
				labels += labelNode.asText()+",";
			}*/
			ObjectNode issueRedux = jsonFactory.objectNode();
			issueRedux.set("id", jsonFactory.textNode(issueId));
			issueRedux.set("key", jsonFactory.textNode(issueKey));
			issueRedux.set("status", jsonFactory.textNode(issueStatus));
			issueRedux.set("url", jsonFactory.textNode(urlIssue));
			issueArrayResponse.add(issueRedux);
		}
		
		return issueArrayResponse;
	}
	
	
	/*** PRVATE METHODS ***/
	private CookieManager login(String username, String password, String urlString) throws IOException, 
			HTTPJiraException{
		

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
		
	}
	
	private void executeAndCheckError(HttpURLConnection httpcon) throws IOException, HTTPJiraException {
		int respCode = httpcon.getResponseCode();
		InputStream errorStream = httpcon.getErrorStream();
		if (errorStream != null) {
			@SuppressWarnings("resource")
			Scanner s = new Scanner(errorStream).useDelimiter("\\A");
			String errorString = s.hasNext() ? s.next() : "";
			s.close();
			errorStream.close();
			throw new HTTPJiraException(respCode+" : "+errorString);
		}
	}

}
