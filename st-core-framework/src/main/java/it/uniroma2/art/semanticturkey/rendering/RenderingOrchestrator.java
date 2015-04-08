package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The rendering orchestrator is the entry-point for clients willing to render a bunch of resources.
 * 
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
	public Map<ARTResource, String> render(Project<?> project, ResourcePosition subjectPosition,
			ARTResource subject, Collection<ARTStatement> statements, Collection<ARTResource> resources,
			Collection<TupleBindings> bindings, String varPrefix) throws ModelAccessException, DataAccessException {

		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();

		Set<ARTResource> delegatedResources = new HashSet<ARTResource>();

		for (ARTResource res : resources) {

			resource2rendering.put(res, res.getNominalValue());

			if (res.isURIResource()) {
				ARTURIResource uriResource = res.asURIResource();

				delegatedResources.add(uriResource);
			}
		}

		if (!delegatedResources.isEmpty()) {
			RenderingEngine projectRenderingEngine = getRenderingEngine(subjectPosition);
			Map<ARTResource, String> renderingsFromDelegate = projectRenderingEngine.render(project,
					subjectPosition, subject, statements, delegatedResources, bindings, varPrefix);

			resource2rendering.putAll(renderingsFromDelegate);
		}

		return resource2rendering;
	}

	private RenderingEngine getRenderingEngine(ResourcePosition subjectPosition) {
		if (subjectPosition instanceof LocalResourcePosition) {
			Project<?> project = ((LocalResourcePosition) (subjectPosition)).getProject();
			RDFModel ontModel = project.getOntModel();
			
			if (ontModel instanceof SKOSXLModel) {
				return new SKOSXLRenderingEngine();
			} else if (ontModel instanceof SKOSModel) {
				return new SKOSRenderingEngine();
			} else {
				return new RDFSRenderingEngine();
			}
			
		} else if ((subjectPosition instanceof RemoteResourcePosition)) {
			DatasetMetadata meta = ((RemoteResourcePosition) subjectPosition).getDatasetMetadata();
			// return meta.getRenderingEngine();
			return new RDFSRenderingEngine();
		} else {
			return new RDFSRenderingEngine();
		}
	}

	@Override
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return getRenderingEngine(resourcePosition).getGraphPatternForDescribe(resourcePosition, resourceToBeRendered, varPrefix);
	}

}
