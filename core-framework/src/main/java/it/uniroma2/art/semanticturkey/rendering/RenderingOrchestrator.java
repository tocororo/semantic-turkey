package it.uniroma2.art.semanticturkey.rendering;

import java.util.List;
import java.util.Map;

import it.uniroma2.art.semanticturkey.services.STServiceContext;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;

/**
 * The rendering orchestrator is the entry-point for clients willing to render a bunch of resources. The
 * rendering orchestrator:
 * <ul>
 * <li>
 * computes a default rendering: i) URIs are rendered as they are, unless it is possible to compress them as
 * qualified names, ii) bnodes are rendered as _:bnodeId</li>
 * <li>
 * wraps the {@link RenderingEngine} associated with the {@link ResourcePosition} of the subject</li>
 * <li>
 * is also an implementation of {@link RenderingEngine}, since the orchestrator is in charge of horizontal
 * rendering tasks, such as the generation of the Manchester serialization for class expressions.</li>
 */
public class RenderingOrchestrator implements RenderingEngine {

	private Project project;
	private ResourcePosition resourcePosition;
	private RenderingEngine baseRenderingEngine;

	public RenderingOrchestrator(Project project, ResourcePosition resourcePosition,
			RenderingEngine baseRenderingEngine) {
				this.project = project;
				this.resourcePosition = resourcePosition;
				this.baseRenderingEngine = baseRenderingEngine;
	}

	/**
	 * Builds an instance of {@link RenderingOrchestrator}
	 * 
	 * @return
	 */
	public static RenderingEngine buildInstance(Project project, ResourcePosition resourcePosition) {
		RenderingEngine baseRenderingEngine = getRenderingEngine(resourcePosition);
		return new RenderingOrchestrator(project, resourcePosition, baseRenderingEngine);
	}

	/**
	 * Determines the rendering engine to use based on the position of the subject resource
	 * 
	 * @param subjectPosition
	 * @return
	 */
	private static RenderingEngine getRenderingEngine(ResourcePosition subjectPosition) {
		if (subjectPosition instanceof LocalResourcePosition) {
			Project project = ((LocalResourcePosition) (subjectPosition)).getProject();
			return project.getRenderingEngine();
		} else if ((subjectPosition instanceof RemoteResourcePosition)) {
			DatasetMetadata meta = ((RemoteResourcePosition) subjectPosition).getDatasetMetadata();
			// return meta.getRenderingEngine();
			return new RDFSRenderingEngine(new RDFSRenderingEngineConfiguration());
		} else {
			return new RDFSRenderingEngine(new RDFSRenderingEngineConfiguration());
		}
	}

	@Override
	public GraphPattern getGraphPattern(STServiceContext context) {
		return baseRenderingEngine.getGraphPattern(context);
	}

	@Override
	public boolean introducesDuplicates() {
		return baseRenderingEngine.introducesDuplicates();
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return baseRenderingEngine.processBindings(context, resultTable);
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}
}
