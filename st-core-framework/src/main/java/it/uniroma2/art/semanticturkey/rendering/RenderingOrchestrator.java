package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The rendering orchestrator is the entry-point for clients willing to render a bunch of resources.
 */
public class RenderingOrchestrator implements RenderingEngine {

	private static RenderingEngine instance;
	
	public static synchronized RenderingEngine getInstance() {
		if (instance == null) {
			instance = new RenderingOrchestrator();
		}
		
		return instance;
	}
	@Override
	public Map<ARTResource, String> render(Project<?> project, ARTResource subject, Collection<ARTStatement> statements,
			ARTResource... resources) throws ModelAccessException, DataAccessException {
	
		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();
		
		Set<ARTResource> delegatedResources = new HashSet<ARTResource>();

		for (ARTResource res : resources) {
		
			resource2rendering.put(res, res.getNominalValue());

			if (res.isURIResource()) {
				ARTURIResource uriResource = res.asURIResource();
				
				if (uriResource.getNamespace().equals(project.getOWLModel().getDefaultNamespace())) {
					delegatedResources.add(uriResource);
				}
			}
		}
		
		if (!delegatedResources.isEmpty()) {
			RenderingEngine projectRenderingEngine = getRenderingEngineForProject(project);
			Map<ARTResource, String> renderingsFromDelegate = projectRenderingEngine.render(project, subject, statements, delegatedResources.toArray(new ARTResource[delegatedResources.size()]));
			
			resource2rendering.putAll(renderingsFromDelegate);
		}
		
		return resource2rendering;
	}
	
	private RenderingEngine getRenderingEngineForProject(Project<?> project) {
		return new RDFSRenderingEngine();
	}
	
}
