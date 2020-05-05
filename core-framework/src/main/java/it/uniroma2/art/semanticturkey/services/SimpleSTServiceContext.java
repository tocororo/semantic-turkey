package it.uniroma2.art.semanticturkey.services;

import org.eclipse.rdf4j.model.Resource;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;

public class SimpleSTServiceContext implements STServiceContext{

	Project project;
	
	public SimpleSTServiceContext(Project project){
		this.project = project;
	}
	
	@Override
	public ProjectConsumer getProjectConsumer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public Project getProject(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource getWGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource[] getRGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExtensionPathComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STRequest getRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSessionToken() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLanguages() {
		return null;
	}

	@Override
	public boolean hasContextParameter(String parameter) {
		return false;
	}
	
	@Override
	public String getContextParameter(String string) {
		// TODO Auto-generated method stub
		return null;
	}

}
