package it.uniroma2.art.semanticturkey.services.http;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

public class STServiceHTTPContext implements STServiceContext {
	
	@Autowired
	private ServletRequest request;
	
	public STServiceHTTPContext() {
		System.out.println("Hello, world!");
	}
	
	@Override
	public Project<?> getProject() {
		System.out.println("Project parameter: " + request.getParameter("project"));
		return ProjectManager.getCurrentProject();
	}

	@Override
	public Project<?> getProject(int index) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
