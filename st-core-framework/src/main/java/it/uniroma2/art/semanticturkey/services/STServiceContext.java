package it.uniroma2.art.semanticturkey.services;

import org.openrdf.repository.RepositoryConnection;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.semanticturkey.project.Project;

public interface STServiceContext {
	Project<?> getProject();
	Project<?> getProject(int index);
	ARTResource getWGraph();
	ARTResource[] getRGraphs();
	String getExtensionPathComponent();
	STRequest getRequest();
	String getSessionToken();
	RepositoryConnection getRepositoryConnection();
}
