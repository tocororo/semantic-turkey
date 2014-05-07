package it.uniroma2.art.semanticturkey.services.http;

import java.util.Arrays;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.convert.ConversionService;

public class STServiceHTTPContext implements STServiceContext, ApplicationListener<ContextRefreshedEvent> {

	private static final String HTTP_PARAM_PREFIX = "ctx_";
	private static final String HTTP_PARAM_PROJECT = HTTP_PARAM_PREFIX + "project";
	private static final String HTTP_PARAM_WGRAPH = HTTP_PARAM_PREFIX + "wgraph";
	private static final String HTTP_PARAM_RGRAPHS = HTTP_PARAM_PREFIX + "rgraphs";

	private static final String HTTP_ARG_DEFAULT_GRAPH = "DEFAULT";
	private static final String HTTP_ARG_ANY_GRAPH = "ANY";

	@Autowired
	private ServletRequest request;

	private ConversionService conversionService;

	@Override
	public Project<?> getProject() {
		String projectParameter = request.getParameter(HTTP_PARAM_PROJECT);
		Project<?> project;

		if (projectParameter == null) {
			project = ProjectManager.getCurrentProject();
		} else {
			project = ProjectManager.getProject(projectParameter);
		}

		System.out.println("project = " + project);
		System.out.flush();

		return project;
	}

	@Override
	public ARTResource getWGraph() {
		String wgraphParameter = request.getParameter(HTTP_PARAM_WGRAPH);

		if (wgraphParameter == null) {
			wgraphParameter = HTTP_ARG_DEFAULT_GRAPH;
		}

		ARTResource wgraph = conversionService.convert(wgraphParameter, ARTResource.class);

		System.out.println("wgraph = " + wgraph);
		System.out.flush();

		return wgraph;
	}

	@Override
	public ARTResource[] getRGraphs() {
		String rgraphsParameter = request.getParameter(HTTP_PARAM_RGRAPHS);

		if (rgraphsParameter == null) {
			rgraphsParameter = HTTP_ARG_ANY_GRAPH;
		}

		ARTResource[] rgraphs = conversionService.convert(rgraphsParameter, ARTResource[].class);

		System.out.println("rgraphs = " + Arrays.toString(rgraphs) );
		System.out.flush();

		return rgraphs;
	}

	@Deprecated
	@Override
	public Project<?> getProject(int index) {
		String projectPar;
		if (index == 0) {
			projectPar = HTTP_PARAM_PROJECT;
		} else {
			projectPar = new StringBuilder(HTTP_PARAM_PROJECT).append("_").append(index).toString();
		}
		return ProjectManager.getProject(request.getParameter(projectPar.toString()));
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

}
