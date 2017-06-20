package it.uniroma2.art.semanticturkey.services.http;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.convert.ConversionService;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.services.InvalidContextException;
import it.uniroma2.art.semanticturkey.services.STRequest;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

public class STServiceHTTPContext implements STServiceContext, ApplicationListener<ContextRefreshedEvent> {

	private static final String HTTP_PARAM_PREFIX = "ctx_";
	private static final String HTTP_PARAM_PROJECT_CONSUMER = HTTP_PARAM_PREFIX + "consumer";
	private static final String HTTP_PARAM_PROJECT = HTTP_PARAM_PREFIX + "project";
	private static final String HTTP_PARAM_WGRAPH = HTTP_PARAM_PREFIX + "wgraph";
	private static final String HTTP_PARAM_RGRAPHS = HTTP_PARAM_PREFIX + "rgraphs";
	private static final String HTTP_PARAM_TOKEN = HTTP_PARAM_PREFIX + "token";
	private static final String HTTP_PARAM_VERSION = HTTP_PARAM_PREFIX + "version";

	private static final String HTTP_ARG_DEFAULT_GRAPH = "DEFAULT";
	private static final String HTTP_ARG_ANY_GRAPH = "ANY";

	protected static Logger logger = LoggerFactory.getLogger(STServiceHTTPContext.class);

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private ApplicationContext applicationContext;

	private ConversionService conversionService;

	private String extensionPathComponent = null;

	@Override
	public ProjectConsumer getProjectConsumer() {
		String consumerParameter = request.getParameter(HTTP_PARAM_PROJECT_CONSUMER);

		if (consumerParameter == null || consumerParameter.equals(ProjectConsumer.SYSTEM.getName())) {
			return ProjectConsumer.SYSTEM;
		} else {
			Project project = ProjectManager.getProject(consumerParameter);

			if (project == null) {
				throw new InvalidContextException(
						"the provided consumer is not an open project: " + consumerParameter);
			}

			return project;
		}

	}

	@Override
	public Project getProject() {
		String projectParameter = request.getParameter(HTTP_PARAM_PROJECT);
		Project project;

		if (projectParameter == null) {
			if (Config.isSingleProjectMode())
				project = ProjectManager.getCurrentProject();
			else
				throw new InvalidContextException(
						"either this context is meant to have no project information, or it has not been passed through the request");
		} else {
			project = ProjectManager.getProject(projectParameter);
		}

		logger.trace("project = " + project);

		if (project == null) {
			throw new InvalidContextException(
					"not an open project: " + (projectParameter == "null" ? "<current>" : projectParameter));
		}
		return project;
	}

	/**
	 * Returns the working graph. If none is indicated as a parameter of the request URL (see
	 * {@value #HTTP_PARAM_WGRAPH}), then it returns the base URI of the current project.
	 */
	@Override
	public Resource getWGraph() {
		String wgraphParameter = request.getParameter(HTTP_PARAM_WGRAPH);

		if (wgraphParameter == null) {
			wgraphParameter = "<"+getProject().getBaseURI()+">";
		}

		Resource wgraph = conversionService.convert(wgraphParameter, Resource.class);
		
		logger.trace("wgraph = " + wgraph);

		return wgraph;
	}

	@Override
	public Resource[] getRGraphs() {
		String rgraphsParameter = request.getParameter(HTTP_PARAM_RGRAPHS);

		Resource[] rgraphs;
		if (rgraphsParameter == null) {
			rgraphsParameter = HTTP_ARG_ANY_GRAPH;
			rgraphs = new Resource[0];
		} else{
			rgraphs = conversionService.convert(rgraphsParameter, Resource[].class);
		}

		logger.trace("rgraphs = " + Arrays.toString(rgraphs));

		return rgraphs;
	}

	@Deprecated
	@Override
	public Project getProject(int index) {
		String projectPar;
		if (index == 0) {
			projectPar = HTTP_PARAM_PROJECT;
		} else {
			projectPar = new StringBuilder(HTTP_PARAM_PROJECT).append("_").append(index).toString();
		}
		return ProjectManager.getProject(request.getParameter(projectPar.toString()));
	}

	@Override
	public String getExtensionPathComponent() {
		// Returns previously determined extension path component (i.e., groupId/artifactId)
		if (extensionPathComponent != null) {
			return extensionPathComponent;
		}

		// Otherwise, computes the extension path component by looking at the path of the resource
		// pom.properties added by Maven
		org.springframework.core.io.Resource[] resources;
		try {
			resources = applicationContext.getResources("classpath*:/META-INF/maven/**/pom.properties");
		} catch (IOException e) {
			throw new InvalidContextException(
					"An exception occurred while loading resources from the location classpath*:/META-INF/maven/**/pom.properties",
					e);
		}
		if (resources.length == 0) {
			throw new InvalidContextException(
					"No resource found at the location classpath*:/META-INF/maven/**/pom.properties");
		}

		org.springframework.core.io.Resource r = resources[0];

		try {
			// Path elements should be xxx, yyy, zzz, META-INF, maven, groupId, artifactId, pom.properties
			String[] pathElements = r.getURI().getPath().split("/");
			extensionPathComponent = pathElements[pathElements.length - 3] + "/"
					+ pathElements[pathElements.length - 2];
		} catch (IOException e) {
			throw new InvalidContextException(
					"Error while referencing a resource located at classpath*:/META-INF/maven/**/pom.properties",
					e);
		}

		return extensionPathComponent;
	}

	/*
	 * TODO: this method listens to the refresh of the application context, in oder to connect this HTTP
	 * context to the converters. This is a workaround for the circular dependency between (some) converters
	 * and the HTTP context. (non-Javadoc)
	 * 
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.
	 * ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.conversionService = event.getApplicationContext().getBean(ConversionService.class);
	}

	@Override
	public STRequest getRequest() {
		return new STHTTPRequest(request);
	}

	@Override
	public String getSessionToken() {
		String token = request.getParameter(HTTP_PARAM_TOKEN);
		if (token == null) {
			throw new InvalidContextException("The token has not been passed through the request");
		}
		return token;
	}
	
	@Override
	public String getVersion() {
		return request.getParameter(HTTP_PARAM_VERSION);
	}
	
	@Override
	public boolean hasContextParameter(String parameter) {
		return request.getParameter("ctx_" + parameter) != null;
	}
}
