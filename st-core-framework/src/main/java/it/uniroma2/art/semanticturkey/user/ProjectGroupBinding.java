package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.project.AbstractProject;

public class ProjectGroupBinding {
	
	private AbstractProject project;
	private UsersGroup group;
	private Collection<IRI> ownedSchemes;
	
	
	public ProjectGroupBinding(AbstractProject project, UsersGroup group) {
		this.project = project;
		this.group = group;
		this.ownedSchemes = new ArrayList<>();
	}
	
	public AbstractProject getProject() {
		return this.project;
	}
	
	public UsersGroup getGroup() {
		return this.group;
	}
	
	public Collection<IRI> getOwnedSchemes() {
		return this.ownedSchemes;
	}
	
	public void setSchemes(Collection<IRI> schemes) {
		this.ownedSchemes = schemes;
	}
	
	public void addScheme(IRI scheme) {
		if (!this.ownedSchemes.contains(scheme)) {
			this.ownedSchemes.add(scheme);
		}
	}
	
	public void removeScheme(IRI scheme) {
		this.ownedSchemes.remove(scheme);
	}
	
	
	@Override
	public String toString() {
		return "<" + this.project.getName() + "," + this.group.getIRI().stringValue() + ">:\n"
				+ "\tOwnedSchemes:" + this.ownedSchemes;
	}

}
