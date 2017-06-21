package it.uniroma2.art.semanticturkey.services;

import org.eclipse.rdf4j.model.Resource;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;

/**
 * An interface describing contextual information associated with service invocation. Currently, the
 * implementation is in charge of interpreting the information explicitly available, for instance for the
 * purpose of inferring missing information (see
 * {@link it.uniroma2.art.semanticturkey.services.http.STServiceHTTPContext#getWGraph()} or
 * {@link it.uniroma2.art.semanticturkey.services.http.STServiceHTTPContext#getRGraphs()})
 *
 */
public interface STServiceContext {
	
	ProjectConsumer getProjectConsumer();
	
	Project getProject();

	Project getProject(int index);

	Resource getWGraph();

	Resource[] getRGraphs();

	String getExtensionPathComponent();

	STRequest getRequest();

	String getSessionToken();
	
	String getVersion();
	
	boolean hasContextParameter(String parameter);
}
