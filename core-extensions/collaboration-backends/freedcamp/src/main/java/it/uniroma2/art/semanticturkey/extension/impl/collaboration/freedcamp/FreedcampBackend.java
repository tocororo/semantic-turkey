package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.node.NullNode;
import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Use <a href="https://freedcamp.com/">Freedcamp</a> as a collaboration backend.
 * 
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 *
 */
public class FreedcampBackend implements CollaborationBackend {

	private Project stProject;
	
	private static final String USER_AGENT = "Fake client";
	private static final String X_APP_VERSION= "vocbench_001";
	private static final String F_USE_CACHE = "f_use_cache=1";

	private static final String LIST_FOR_VB3_RESOURCES = "List for VB3 resources";

	private static final String GROUP_FOR_VB3_RESOURCES = "Group for VB3 resources";

	private static boolean apiInURL = false;

	private final FreedcampBackendFactory factory;
	
	public FreedcampBackend(FreedcampBackendFactory factory) {
		this.factory = factory;
	}

	@Override
	public void bind2project(Project project) {
		this.stProject = project;
	}


	@Override 
	public void checkPrjConfiguration() throws STPropertyAccessException, IOException, CollaborationBackendException {
		if (stProject == null) {
			throw new NullPointerException("Freedcamp Backend not bound to a project");
		}

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//if the current Project is null, do not perform any check on the project id, so just return
		if(projectSettings.freedcampPrjId == null || projectSettings.freedcampPrjId.isEmpty()){
			return;
		}


		//check that there is a Freedcamp Project with such id and that thare is a list, in this project
		//  with the specified list_id is present
		// then such group should belong to the selected project.
		// If no group_id is specified, then check if there is a list, in that project having the
		// name {LIST_FOR_VB3_RESOURCES}, in case there is, take its id, if not, create a new list
		// having name {LIST_FOR_VB3_RESOURCES}
		String prjId = projectSettings.freedcampPrjId;
		String listId = projectSettings.freedcampTaskListId;

		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/projects?"+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon, true);


		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode freedCampResponseNode = objectMapper.readTree(response);

		ArrayNode prjFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("projects");

		boolean projectFound = false;
		for(JsonNode prjFromFreedcampNode : prjFromFreedcampArray) {
			String currentPrjId = prjFromFreedcampNode.get("project_id").asText();
			if(currentPrjId.equals(prjId)) {
				projectFound = true;
			}
		}
		//if no project was found having the desired id-key, then throw and exception
		if(!projectFound) {
			throw new CollaborationBackendException("Invalid Configuration: there is no Freedcamp project having id:"+
					prjId);
		}

		//the project was found, now check that the list is part of the project
		//get all the list in the desired project
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/lists/2?project_id="+prjId+setSecureOrNot(projectPreferences);

		httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		executeAndCheckError(httpcon);

		response = readRsponse(httpcon, true);
		freedCampResponseNode = objectMapper.readTree(response);
		ArrayNode listFromFreedcampArray = (ArrayNode) freedCampResponseNode.get("data").get("lists");
		boolean listFound = false;
		for(JsonNode listFormFreedcampNode : listFromFreedcampArray){
			String currentId = listFormFreedcampNode.get("id").asText();
			if(currentId.equals(listId)){
				listFound = true;
			}
		}
		//if no project was found having the desired id-key, then throw and exception
		if(!listFound) {
			throw new CollaborationBackendException("Invalid Configuration: in project "+prjId+" there is no task list with id:"+
					listId);
		}

	}
	
	@Override
	public FreedcampIssueCreationForm getCreateIssueForm() {
		return new FreedcampIssueCreationForm();
	}
	
	
	@Override
	public void createIssue(String resource, ObjectNode issueCreationForm) 
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		//first of all, check that there is a valid associated Freedcamp Project
		checkPrjConfiguration();


		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		String projId = projectSettings.freedcampPrjId;
		String listId = projectSettings.freedcampTaskListId;

		//get the mapping of tags_name-tags_id
		Map<String, String > resourceToTagsidMap = new HashMap<>();
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tags?"+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		// Send post request
		executeAndCheckError(httpcon);

		String responseTags = readRsponse(httpcon, true);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(responseTags);
		ArrayNode tagsFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("tags");
		for(JsonNode tagNode : tagsFromFreedcampArray){
			String tagId = tagNode.get("id").asText();
			String tagTitle = tagNode.get("title").asText();
			resourceToTagsidMap.put(tagTitle, tagId);
		}
		//check if there already a tag having as name the resource URI
		String tagId = null;
		if(resourceToTagsidMap.containsKey(resource)){
			tagId = resourceToTagsidMap.get(resource);
		}

		//now create the task
		String title = escapeString(issueCreationForm.get("title").textValue());
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tasks?"+F_USE_CACHE+setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/x-www-form-urlencoded", projectPreferences);
		Map<String, String> nameValueMap = new HashMap<>();
		nameValueMap.put("project_id", projId);
		nameValueMap.put("title", title);
		nameValueMap.put("task_group_id", listId);

		//if an existing tag was found, use its id in existingTagResourceList, otherwise use the resource name in newTagResourceList
		List<String> newTagResourceList = new ArrayList<>();
		List<String> existingTagResourceList = new ArrayList<>();
		if(tagId!=null){
			existingTagResourceList.add(tagId);
		} else {
			//newTagResourceList.add("_"+resource); //OLD temporary solution, the _ will be removed when fixed in the freedcamp API server
			newTagResourceList.add(resource);
		}
		String postJsonData = preparePostData(nameValueMap, newTagResourceList, existingTagResourceList);

		addPostToHttpURLConnection(httpcon, postJsonData);

		// Send post request
		executeAndCheckError(httpcon);

	}
	
	/**
	 * In this implementation, projectJson is a json object with id, id and name.
	 * Note that the attributes of this object, are the same of the projects json objects returned by 
	 * listProjects()
	 * Then it automatically assign a task_list (either an existing one, if there is a list with name {LIST_FOR_VB3_RESOURCES})
	 * or it creates a new list with name {LIST_FOR_VB3_RESOURCES}
	 */
	@Override
	public void assignProject(ObjectNode projectJson)
			throws STPropertyAccessException, IOException, CollaborationBackendException, STPropertyUpdateException {
		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());
		//now save all the information related to the Freedcamp project
		JsonNode idNode = projectJson.get("id");
		if (idNode == null || idNode instanceof NullNode) {
			throw new CollaborationBackendException("'id' attribute is missing in the project Json object.");
		}
		JsonNode nameNode = projectJson.get("name");
		if (nameNode == null || nameNode instanceof NullNode) {
			throw new CollaborationBackendException("'name' attribute is missing in the project Json object.");
		}
		String prjId = idNode.textValue();
		projectSettings.freedcampPrjId = prjId;

		//now check if the specified project has a list with name {LIST_FOR_VB3_RESOURCES}, if so,
		// assign such list to the configuration, otherwise, create a new list with name {LIST_FOR_VB3_RESOURCES}
		// and then assign such list
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/lists/2?project_id="+prjId+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon, true);
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(response);
		//analyze the response and construct a map title-id
		Map<String, String> titleToIdMap = new HashMap<>();
		ArrayNode listFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("lists");
		for(JsonNode listFromFreedcampNode : listFromFreedcampArray) {
			String id = listFromFreedcampNode.get("id").asText();
			String title = listFromFreedcampNode.get("title").asText();
			titleToIdMap.put(title, id);
		}
		//check if there is a list with name {LIST_FOR_VB3_RESOURCES}
		String taskListId = null;
		if(titleToIdMap.containsKey(LIST_FOR_VB3_RESOURCES)){
			taskListId = titleToIdMap.get(LIST_FOR_VB3_RESOURCES);
		} else {
			//the list was not found, so create it
			urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/lists/2?"+setSecureOrNot(projectPreferences);
			httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
					"application/x-www-form-urlencoded", projectPreferences);

			Map<String, String> nameValueMap = new HashMap<>();
			nameValueMap.put("project_id", prjId);
			nameValueMap.put("title", LIST_FOR_VB3_RESOURCES);
			String postJsonData = preparePostData(nameValueMap);

			addPostToHttpURLConnection(httpcon, postJsonData);

			// Send post request
			executeAndCheckError(httpcon);

			response = readRsponse(httpcon, true);

			//get the id of the newly created list
			freedCampResponseNode = objectMapper.readTree(response);
			listFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("lists");
			taskListId = listFromFreedcampArray.get(0).get("id").asText();

		}
		projectSettings.freedcampTaskListId = taskListId;

		//store the project id and the task_list id
		factory.storeProjectSettings(stProject, projectSettings);

	}
	
	@Override
	public void createProject(ObjectNode projectJson) 
			throws STPropertyAccessException, JsonProcessingException, IOException, CollaborationBackendException, 
			STPropertyUpdateException {

		if (stProject == null) {
			throw new NullPointerException("Freedcamp Backend not bound to a project");
		}

		JsonNode nameNode = projectJson.get("name");
		if (nameNode == null || nameNode instanceof NullNode) {
			throw new CollaborationBackendException("'name' attribute is missing in the project Json object.");
		}
		String projectName = nameNode.textValue();
		if(projectName==null || projectName.isEmpty()){
			throw new CollaborationBackendException(" the Project Name cannot be empty.");
		}

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//create the project
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/projects?"+setSecureOrNot(projectPreferences);
		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/x-www-form-urlencoded", projectPreferences);
		Map<String, String> nameValueMap = new HashMap<>();
		nameValueMap.put("project_name", projectName);
		nameValueMap.put("title", projectName);
		nameValueMap.put("group_name", GROUP_FOR_VB3_RESOURCES);
		String postJsonData = preparePostData(nameValueMap);
		addPostToHttpURLConnection(httpcon, postJsonData);
		// Send post request
		executeAndCheckError(httpcon);
		String responseProject = readRsponse(httpcon, true);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(responseProject);
		ArrayNode listFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("projects");
		String projectId = listFromFreedcampArray.get(0).get("project_id").asText();

		//create the task list
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/lists/2?"+setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/x-www-form-urlencoded", projectPreferences);

		nameValueMap = new HashMap<>();
		nameValueMap.put("project_id", projectId);
		nameValueMap.put("title", LIST_FOR_VB3_RESOURCES);
		postJsonData = preparePostData(nameValueMap);

		addPostToHttpURLConnection(httpcon, postJsonData);

		// Send post request
		executeAndCheckError(httpcon);

		String responseList = readRsponse(httpcon, true);

		//get the id of the newly created list
		freedCampResponseNode = objectMapper.readTree(responseList);
		listFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("lists");
		String taskListId = listFromFreedcampArray.get(0).get("id").asText();

		//save the new projectId and taskListId
		projectSettings.freedcampPrjId = projectId;
		projectSettings.freedcampTaskListId = taskListId;
		factory.storeProjectSettings(stProject, projectSettings);
	}

	@Override
	public void assignResourceToIssue(String taskId, IRI resource) throws STPropertyAccessException, IOException, CollaborationBackendException {

		//first of all, check that there is a valid associated Freedcamp Project
		checkPrjConfiguration();

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//get the mapping of tags_name-tags_id
		Map<String, String > resourceToTagsidMap = new HashMap<>();
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tags?"+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		// Send post request
		executeAndCheckError(httpcon);

		String responseTags = readRsponse(httpcon, true);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(responseTags);
		ArrayNode tagsFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("tags");
		for(JsonNode tagNode : tagsFromFreedcampArray){
			String tagId = tagNode.get("id").asText();
			String tagTitle = tagNode.get("title").asText();
			resourceToTagsidMap.put(tagTitle, tagId);
		}
		//check if there already a tag having as name the resource URI
		String tagId = null;
		if(resourceToTagsidMap.containsKey(resource.stringValue())){
			tagId = resourceToTagsidMap.get(resource.stringValue());
		}

		//get the tags already associated to the task having id taskId
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tasks/"+taskId+"?"+ F_USE_CACHE +
				setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);
		// Send post request
		executeAndCheckError(httpcon);
		String responseTask = readRsponse(httpcon, true);
		freedCampResponseNode = objectMapper.readTree(responseTask);
		ArrayNode tasksArray = (ArrayNode)freedCampResponseNode.get("data").get("tasks");
		if(tasksArray.size()==0){
			throw new NullPointerException("There is no task with id "+taskId);
		} else if(tasksArray.size()>1){
			throw new NullPointerException("There are multiple tasks with id "+taskId+" which should not be possible");
		}
		JsonNode taskNode = tasksArray.get(0);
		ArrayNode tagsArray = (ArrayNode) taskNode.get("tags");
		List<String> alreadyAssociatedTagsList = new ArrayList<>();
		for(JsonNode tagNode : tagsArray ){
			alreadyAssociatedTagsList.add(tagNode.asText());
		}

		//check that the task does now already have the tags you want to add
		if(tagId!=null && alreadyAssociatedTagsList.contains(tagId)){
			//the tags is already present, so just return;
			return;
		}

		//now add the new tag to the task (maintaining the already existing tags)
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/item_tags/2/"+
				taskId+"/?"+ F_USE_CACHE +setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/x-www-form-urlencoded", projectPreferences);

		Map<String, String> nameValueMap = new HashMap<>();
		List<String> newTagResourceList = new ArrayList<>();
		List<String> existingTagResourceList = new ArrayList<>();

		//add the already associated tags
		for(String alreadyAssocaitedTagId : alreadyAssociatedTagsList){
			existingTagResourceList.add(alreadyAssocaitedTagId);
		}
		//add the new tag or name
		if(tagId!=null){
			existingTagResourceList.add(tagId);
		} else {
			//newTagResourceList.add("_"+resource.stringValue()); //OLD temporary solution, the _ will be removed when fixed in the freedcamp API server
			newTagResourceList.add(resource.stringValue());
		}
		String postJsonData = preparePostData(nameValueMap, newTagResourceList, existingTagResourceList);

		addPostToHttpURLConnection(httpcon, postJsonData);

		// Send post request
		executeAndCheckError(httpcon);
	}

	@Override
	public void removeResourceFromIssue(String taskId, IRI resource) throws STPropertyAccessException, IOException, CollaborationBackendException {
		//first of all, check that there is a valid associated Freedcamp Project
		checkPrjConfiguration();

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//get the mapping of tags_name-tags_id (the tags_id in this icas is a List, since there could be different tag with the same resourceIRI, even
		// if this should never happen)
		Map<String, List<String>> resourceToListTagsidMap = new HashMap<>();
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tags?"+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		// Send post request
		executeAndCheckError(httpcon);

		String responseTags = readRsponse(httpcon, true);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(responseTags);
		ArrayNode tagsFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("tags");
		for(JsonNode tagNode : tagsFromFreedcampArray){
			String tagId = tagNode.get("id").asText();
			String tagTitle = tagNode.get("title").asText();
			if(!resourceToListTagsidMap.containsKey(tagTitle)) {
				resourceToListTagsidMap.put(tagTitle, new ArrayList<>());
			}
			resourceToListTagsidMap.get(tagTitle).add(tagId);
		}
		//check if there already a tag having as name the resource URI
		List<String> tagIdList = new ArrayList<>();
		if(resourceToListTagsidMap.containsKey(resource.stringValue())){
			tagIdList = resourceToListTagsidMap.get(resource.stringValue());
		}
		//if there is no tag with the desired resource URI (the one that the user want to remove from the task) then just exit,
		// since is not a resource with
		if(tagIdList == null || tagIdList.isEmpty()){
			return;
		}

		//get the tags already associated to the task having id taskId
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tasks/"+taskId+"?"+F_USE_CACHE+
				setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);
		// Send post request
		executeAndCheckError(httpcon);
		String responseTask = readRsponse(httpcon, true);
		freedCampResponseNode = objectMapper.readTree(responseTask);
		ArrayNode tasksArray = (ArrayNode)freedCampResponseNode.get("data").get("tasks");
		if(tasksArray.size()==0){
			throw new NullPointerException("There is no task with id "+taskId);
		} else if(tasksArray.size()>1){
			throw new NullPointerException("There are multiple tasks with id "+taskId+" which should not be possible");
		}
		JsonNode taskNode = tasksArray.get(0);
		ArrayNode tagsArray = (ArrayNode) taskNode.get("tags");
		List<String> alreadyAssociatedTagsList = new ArrayList<>();
		for(JsonNode tagNode : tagsArray ){
			alreadyAssociatedTagsList.add(tagNode.asText());
		}

		//remove from alreadyAssociatedTagsList the values from  tagIdList
		List<String> tagsToMaintainList = new ArrayList<>();
		for(String tagId : alreadyAssociatedTagsList){
			if(!tagIdList.contains(tagId)){
				tagsToMaintainList.add(tagId);
			}
		}

		//now add to the task all the tags that were already present (except the one that has been removed)
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/item_tags/2/"+
				taskId+"/?"+ F_USE_CACHE +setSecureOrNot(projectPreferences);
		httpcon = prepareHttpURLConnection(ConnType.POST, urlString,
				"application/x-www-form-urlencoded", projectPreferences);

		Map<String, String> nameValueMap = new HashMap<>();
		List<String> newTagResourceList = new ArrayList<>();
		List<String> existingTagResourceList = new ArrayList<>();

		//add the already associated tags
		for(String tagsToMaintain : tagsToMaintainList){
			existingTagResourceList.add(tagsToMaintain);
		}
		String postJsonData = preparePostData(nameValueMap, newTagResourceList, existingTagResourceList);

		addPostToHttpURLConnection(httpcon, postJsonData);

		// Send post request
		executeAndCheckError(httpcon);
	}

	@Override
	public JsonNode listIssuesAssignedToResource(IRI resource) throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		// first of all, check that there is a valid associated Freedcamp Project
		// checkPrjConfiguration(); // commented to avoid to many connections to Freedcamp server

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		String projId = projectSettings.freedcampPrjId;
		String listId = projectSettings.freedcampTaskListId;

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode taskArrayResponse = jsonFactory.arrayNode();

		//get the tag_id having the resource uri as name (if any)
		Map<String, String > resourceToTagsidMap = new HashMap<>();
		Map<String, String> tagsidToResourceMap = new HashMap<>();
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tags?"+ F_USE_CACHE +setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		// Send post request
		executeAndCheckError(httpcon);

		String responseTags = readRsponse(httpcon, true);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode freedCampResponseNode = objectMapper.readTree(responseTags);
		ArrayNode tagsFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("tags");
		for(JsonNode tagNode : tagsFromFreedcampArray){
			String tagId = tagNode.get("id").asText();
			String tagTitle = tagNode.get("title").asText();
			resourceToTagsidMap.put(tagTitle, tagId);
			tagsidToResourceMap.put(tagId, tagTitle);
		}
		//check if there already a tag having as name the resource URI
		String tagId = null;
		if(resourceToTagsidMap.containsKey(resource.stringValue())){
			tagId = resourceToTagsidMap.get(resource.stringValue());
		}
		if(tagId == null){
			return taskArrayResponse;
		}

		//now get all task having the desired tagId
		urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/tasks?project_id="+projId+
				"&filter={\"tag_id\":[\""+tagId+"\"]}&f_fiu=1&f_json=1&"+F_USE_CACHE+"&f_include_tags=1"+setSecureOrNot(projectPreferences);

		httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		// Send post request
		executeAndCheckError(httpcon);

		String responseTasks = readRsponse(httpcon, true);

		//process the results
		objectMapper = new ObjectMapper();
		freedCampResponseNode = objectMapper.readTree(responseTasks);
		ArrayNode taskFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("tasks");
		//List<ObjectNode> taskReduxNodeList = new ArrayList<>();
		int count = 0;
		for(JsonNode taskFromFreedcampNode : taskFromFreedcampArray){
			/*if(!taskFromFreedcampNode.get("task_group_id").asText().equals(listId)){
				//the task belong to a different task_list, so do not consider it
				continue;
			}*/
			ObjectNode taskRedux = parseTask(taskFromFreedcampNode, tagsidToResourceMap);
			taskArrayResponse.add(taskRedux);
			++count;
		}

		

		return taskArrayResponse;
	}
	
	
	@Override
	public JsonNode listIssues(int pageOffset) throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		int maxResult = 100;
		int startAt = pageOffset*maxResult;
		//TODO check how to do the limit + offset
		String more="false";
		int total=0;
		int numPagesTotal=0;
		
		//first of all, check that there is a valid associated Freedcamp Project
		// checkPrjConfiguration(); // commented to avoid to many connections to Freedcamp server

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		String projId = projectSettings.freedcampPrjId;
		String listId = projectSettings.freedcampTaskListId;

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		List<ObjectNode> taskReduxNodeList = new ArrayList<>();

		if(projectSettings.freedcampPrjId != null && !projectSettings.freedcampPrjId.isEmpty()) {
			//get the mapping of tags_id-tags_name
			Map<String, String> tagsidToResourceMap = new HashMap<>();
			String urlString = normalizeServerUrl(projectSettings.serverURL) + "api/v1/tags?" + setSecureOrNot(projectPreferences);

			HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
					"application/json;charset=UTF-8", projectPreferences);

			// Send post request
			executeAndCheckError(httpcon);

			String responseTags = readRsponse(httpcon, true);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode freedCampResponseNode = objectMapper.readTree(responseTags);
			ArrayNode tagsFromFreedcampArray = (ArrayNode) freedCampResponseNode.get("data").get("tags");
			for (JsonNode tagNode : tagsFromFreedcampArray) {
				String tagId = tagNode.get("id").asText();
				String tagTitle = tagNode.get("title").asText();
				tagsidToResourceMap.put(tagId, tagTitle);
			}

			//get the list of tasks
			urlString = normalizeServerUrl(projectSettings.serverURL) + "api/v1/tasks/?project_id="
					+ projId + "&limit=" + maxResult + "&offset=" + startAt + "&f_include_tags=1" + setSecureOrNot(projectPreferences);

			httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
					"application/json;charset=UTF-8", projectPreferences);

			executeAndCheckError(httpcon);

			String responseTasks = readRsponse(httpcon, true);


			//process the results
			objectMapper = new ObjectMapper();
			freedCampResponseNode = objectMapper.readTree(responseTasks);
			ArrayNode taskFromFreedcampArray = (ArrayNode) freedCampResponseNode.get("data").get("tasks");
			int count = 0;
			for (JsonNode taskFromFreedcampNode : taskFromFreedcampArray) {
			/*if(!taskFromFreedcampNode.get("task_group_id").asText().equals(listId)){
				//the task belong to a different task_list, so do not consider it
				continue;
			}*/
				ObjectNode taskRedux = parseTask(taskFromFreedcampNode, tagsidToResourceMap);
				taskReduxNodeList.add(taskRedux);
				++count;
			}

			JsonNode metaNode = freedCampResponseNode.get("data").get("meta");
			more = metaNode.get("has_more").asText();
			total = metaNode.get("total_count") != null ? Integer.parseInt(metaNode.get("total_count").asText()) : count;
			numPagesTotal = (total / maxResult);
			if (total % maxResult != 0) {
				++numPagesTotal;
			}
		}

			//now construct the response using the issue contained in the issueNodeList
			ArrayNode arrayNode = jsonFactory.arrayNode();
			for (ObjectNode issueRedux : taskReduxNodeList) {
				arrayNode.add(issueRedux);
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
		//first of all, check that there is a valid associated Freedcamp Project
		checkPrjConfiguration();

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode userArrayResponse = jsonFactory.arrayNode();
		

		return userArrayResponse;
	}
	*/
	
	
	@Override
	public JsonNode listProjects() throws STPropertyAccessException, IOException, 
			CollaborationBackendException {
		if (stProject == null) {
			throw new NullPointerException("Freedcamp Backend not bound to a project");
		}

		FreedcampBackendProjectSettings projectSettings = factory.getProjectSettings(stProject);
		FreedcampBackendPUSettings projectPreferences = factory.getProjectSettings(stProject,
				UsersManager.getLoggedUser());

		//now ask freedcamp for all the projects
		String urlString = normalizeServerUrl(projectSettings.serverURL)+"api/v1/projects?"+setSecureOrNot(projectPreferences);

		HttpURLConnection httpcon = prepareHttpURLConnection(ConnType.GET, urlString,
				"application/json;charset=UTF-8", projectPreferences);

		executeAndCheckError(httpcon);

		String response = readRsponse(httpcon, true);


		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode freedCampResponseNode = objectMapper.readTree(response);

		ArrayNode prjFromFreedcampArray = (ArrayNode)freedCampResponseNode.get("data").get("projects");

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		ObjectNode respNode = jsonFactory.objectNode();

		ArrayNode headerArray = jsonFactory.arrayNode();
		respNode.set("headers", headerArray);
		headerArray.add("name");

		ArrayNode prjArray = jsonFactory.arrayNode();
		respNode.set("projects", prjArray);
		for(JsonNode prjFromFreedcampNode : prjFromFreedcampArray) {
			String prjId = prjFromFreedcampNode.get("project_id").asText();
			String prjName = prjFromFreedcampNode.get("project_name").asText();
			ObjectNode projectRedux = jsonFactory.objectNode();
			projectRedux.set("id", jsonFactory.textNode(prjId));
			projectRedux.set("name", jsonFactory.textNode(prjName));
			prjArray.add(projectRedux);
		}

		return respNode;
	}
	
	@Override
	public boolean isProjectLinked() throws STPropertyAccessException {
		if (stProject == null) {
			throw new NullPointerException("Freedcamp Backend not bound to a project");
		}
		return true;
	}

	
	/*** PRVATE METHODS ***/

	private String normalizeServerUrl(String serverUrl){
		if(serverUrl.endsWith("/")) {
			return serverUrl;
		}
		return  serverUrl+"/";
	}
	

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

	private String escapeString(String text) {
		String escaptedText = text.replace("\\", "\\\\");
		escaptedText = escaptedText.replace("\"", "\\\"");
		escaptedText = escaptedText.replace("\n", "\\n");
		escaptedText = escaptedText.replace("\r", "\\r");
		escaptedText = escaptedText.replace("\t", "\\t");
		return escaptedText;
	}

	private String setSecureOrNot(FreedcampBackendPUSettings projectPreferences)
			throws CollaborationBackendException {
		boolean useSecureKey = true;
		if(projectPreferences.apiSecret==null || projectPreferences.apiSecret.isEmpty()){
			useSecureKey = false;
		}
		if(!useSecureKey){
			return "";
		}
		String timestamp = getTimeStamp();
		String hash = null;
		try {
			hash = generateHash(timestamp, projectPreferences.apiKey, projectPreferences.apiSecret);
		} catch (NoSuchAlgorithmException e) {
			throw new CollaborationBackendException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new CollaborationBackendException(e.getMessage());
		} catch (InvalidKeyException e) {
			throw new CollaborationBackendException(e.getMessage());
		}

		String returnValue = "&timestamp="+timestamp+"&hash="+hash;

		if(apiInURL){
			returnValue+="&api_key=8c294926bf2b3b15609156be98ba2c270908cc7e";
		}

		return returnValue;
	}

	private HttpURLConnection prepareHttpURLConnection(ConnType connType, String urlString,
			String contentType, FreedcampBackendPUSettings projectPreferences) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();

		httpcon.setRequestMethod(connType.name());

		// add request header
		httpcon.setRequestProperty("User-Agent", USER_AGENT);
		httpcon.setRequestProperty("Content-Type", contentType);
		if(!apiInURL) {
			httpcon.setRequestProperty("X-API-KEY", projectPreferences.apiKey);
		}
		httpcon.setRequestProperty("X-APP-VERSION", X_APP_VERSION);
		return  httpcon;
	}

	private String preparePostData(Map<String, String> nameValueMap){
		return preparePostData(nameValueMap, null, null);
	}

	private String preparePostData(Map<String, String> nameValueMap, List<String> tagNameList, List<String> tagIdList){
		String postJsonData = "";
		postJsonData += "data= {";
		for(String key : nameValueMap.keySet()){
			postJsonData += "\n\""+key+"\":\""+nameValueMap.get(key)+"\",";
		}
		// if tagList is not null and not empty, add the custom field
		postJsonData +=  "\n\"tags\":[";
		if((tagNameList != null && !tagNameList.isEmpty()) || (tagIdList != null && !tagIdList.isEmpty())){
			assert tagNameList != null;
			for(String tagName : tagNameList){
				postJsonData += "\""+tagName+"\",";
				//postJsonData += ""+tag+",";
			}
			for(String tagId : tagIdList){
				//postJsonData += "\""+tagId+"\",";
				postJsonData += ""+tagId+",";
			}
			//remove the last ,
			postJsonData = postJsonData.substring(0, postJsonData.length()-1);

		}
		postJsonData += "]"+
				"\n}";
		return  postJsonData;
	}

	private void addPostToHttpURLConnection(HttpURLConnection httpcon, String postJsonData)
			throws IOException {
		httpcon.setDoOutput(true);

		DataOutputStream wr = new DataOutputStream(httpcon.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();
	}

	private String readRsponse(HttpURLConnection httpcon, boolean disconnect) throws IOException {
		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		httpcon.disconnect();

		return response.toString();
	}

	public enum ConnType {
		GET, POST
	}

	private String getTimeStamp(){
		String timestamp = System.currentTimeMillis()+"";
		return timestamp;
	}

	private String generateHash(String timestamp, String api_key, String secret) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		String apiKeyAndTimeStamp = api_key +timestamp;

		return hmacDigest(apiKeyAndTimeStamp, secret, "HmacSHA1");
	}

	private String hmacDigest(String msg, String keyString, String algo) throws UnsupportedEncodingException,
			NoSuchAlgorithmException, InvalidKeyException {
		String digest = null;
		SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
		Mac mac = Mac.getInstance(algo);
		mac.init(key);

		byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

		StringBuffer hash = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				hash.append('0');
			}
			hash.append(hex);
		}
		digest = hash.toString();
		return digest;
	}

	private ObjectNode parseTask(JsonNode taskNode, Map<String, String> tagsidToResourceMap){
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		String taskId = taskNode.get("id").asText();
		String taskTitle = taskNode.get("title").asText();
		String taskStatus = taskNode.get("status_title").asText();
		String taskStatusId = taskNode.get("status").asText();
		String taskUrl = taskNode.get("url").asText();

		ArrayNode labelsArray = jsonFactory.arrayNode();
		ArrayNode tagsArray = (ArrayNode) taskNode.get("tags");
		for(JsonNode tagNode : tagsArray){
			String tagId = tagNode.asText();
			if(tagsidToResourceMap.containsKey(tagId)){
				String tagName = tagsidToResourceMap.get(tagId);
				if(!tagName.contains(" ")) {
					try {
						SimpleValueFactory.getInstance().createIRI(tagName);
						labelsArray.add(tagName);
					} catch (IllegalArgumentException e) {
						//do nothing
					}
				}
			}
		}
		ObjectNode taskRedux = jsonFactory.objectNode();
		taskRedux.set("id", jsonFactory.textNode(taskId));
		taskRedux.set("summary", jsonFactory.textNode(taskTitle));
		taskRedux.set("key", jsonFactory.textNode(taskId));
		taskRedux.set("status", jsonFactory.textNode(taskStatus));
		taskRedux.set("statusId", jsonFactory.textNode(taskStatusId));
		taskRedux.set("url", jsonFactory.textNode(taskUrl));
		taskRedux.set("labels", labelsArray);
		taskRedux.set("resolution", jsonFactory.textNode(""));
		taskRedux.set("category", jsonFactory.textNode(taskStatus));

		return taskRedux;
	}

}
