package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.AbstractProject;

public class ProjectUserBinding {
	
	private AbstractProject project;
	private STUser user;
	private Collection<Role> roles;
	
	public ProjectUserBinding(AbstractProject project, STUser user) {
		this.project = project;;
		this.user = user;
		this.roles = new ArrayList<>();
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

}
