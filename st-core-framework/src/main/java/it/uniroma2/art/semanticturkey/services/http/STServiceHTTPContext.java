package it.uniroma2.art.semanticturkey.services.http;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

public class STServiceHTTPContext implements STServiceContext {
	
	@Autowired
	private ServletRequest request;
	
	@Override
	public Project<?> getProject() {
		System.out.println("Project parameter: " + request.getParameter("project"));
		if (request.getParameter("project") == null) {
			return ProjectManager.getCurrentProject();
		}
		return ProjectManager.getProject(request.getParameter("project"));
	}

	@Override
	public Project<?> getProject(int index) {
		String projectPar;
		if (index == 0) {
			projectPar = "project";
		} else {
			projectPar = new StringBuilder("project_").append(index).toString();
		}
		return ProjectManager.getProject(request.getParameter(projectPar.toString()));	
	}
	

}
