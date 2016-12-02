package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProjectUserBinding {
	
	private String projectName;
	private String userEmail;
	private Collection<String> rolesName;
	
	public ProjectUserBinding(String projectName, String userEmail) {
		this.projectName = projectName;
		this.userEmail = userEmail;
		this.rolesName = new ArrayList<>();
	}
	
	public String getProjectName() {
		return this.projectName;
	}
	
	public String getUserEmail() {
		return this.userEmail;
	}
	
	public Collection<String> getRolesName() {
		return this.rolesName;
	}
	
	public void setRoles(Collection<String> rolesName) {
		this.rolesName = rolesName;
	}
	
	public void addRole(String roleName) {
		if (!this.rolesName.contains(roleName)) {
			this.rolesName.add(roleName);
		}
	}
	
	public void removeRole(String roleName) {
		this.rolesName.remove(roleName);
	}

	public JSONObject getAsJSONObject() throws JSONException {
		JSONObject bindingJson = new JSONObject();
		bindingJson.put("userEmail", userEmail);
		bindingJson.put("projectName", projectName);
		JSONArray rolesJson = new JSONArray(rolesName);
		bindingJson.put("roles", rolesJson);
		return bindingJson;
	}
}
