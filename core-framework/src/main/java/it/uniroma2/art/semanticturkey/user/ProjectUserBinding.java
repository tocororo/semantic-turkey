package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.AbstractProject;

public class ProjectUserBinding {
	
	private AbstractProject project;
	private STUser user;
	private Collection<Role> roles;
	private Collection<String> languages;
	private UsersGroup group;
	private boolean groupLimitation;
	
	public ProjectUserBinding(AbstractProject project, STUser user) {
		this.project = project;;
		this.user = user;
		this.roles = new ArrayList<>();
		this.languages = new ArrayList<>();
		// set false as default, eventually it will changed when retrieving data from the bindings.ttl files
		this.groupLimitation = false;
	}
	
	public AbstractProject getProject() {
		return this.project;
	}
	
	public STUser getUser() {
		return this.user;
	}
	
	public Collection<Role> getRoles() {
		return this.roles;
	}
	
	public void setRoles(Collection<Role> roles) {
		this.roles = roles;
	}
	
	public void addRole(Role role) {
		if (!this.roles.contains(role)) {
			this.roles.add(role);
		}
	}
	
	public void removeRole(Role role) {
		this.roles.remove(role);
	}
	
	public Collection<String> getLanguages() {
		return this.languages;
	}
	
	public void setLanguages(Collection<String> languages) {
		this.languages = languages;
	}
	
	public void addLanguage(String language) {
		if (!this.languages.contains(language)) {
			this.languages.add(language);
		}
	}
	
	public void removeLanguage(String language) {
		this.languages.remove(language);
	}
	
	public void assignGroup(UsersGroup group) {
		this.group = group;
	}
	
	public void removeGroup() {
		this.group = null;
	}
	
	public UsersGroup getGroup() {
		return this.group;
	}
	
	public void setSubjectToGroupLimitations(boolean groupLimitation) {
		this.groupLimitation = groupLimitation;
	}
	
	public boolean isSubjectToGroupLimitations() {
		return groupLimitation;
	}
	
	@Override
	public String toString() {
		Collection<String> rolesAsStringList = new ArrayList<>();
		for (Role r: this.roles) {
			rolesAsStringList.add(r.getName());
		}
		String asString = "<" + this.project.getName() + "," + this.user.getIRI().stringValue() + ">:\n"
				+ "\tRoles:" + this.roles
				+ "\tLanguages:" + this.languages;
		if (this.group != null) {
			asString += "\tGroup:" + this.group.getShortName();
		}
		return asString;
	}

}
