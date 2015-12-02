package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTBNode;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.model.syntax.manchester.ManchesterClassInterface;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.PrefixMapping;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.RDFSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;

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

	private static RenderingEngine instance;

	/**
	 * Gets the singleton instance of {@link RenderingOrchestrator}
	 * 
	 * @return
	 */
	public static synchronized RenderingEngine getInstance() {
		if (instance == null) {
			instance = new RenderingOrchestrator();
		}

		return instance;
	}

	@Override
	public Map<ARTResource, String> render(Project<?> project, ResourcePosition subjectPosition,
			ARTResource subject, OWLModel statements, Collection<ARTResource> resources,
			Collection<TupleBindings> bindings, String varPrefix) throws ModelAccessException,
			DataAccessException {

		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();

		Set<ARTResource> delegatedResources = new HashSet<ARTResource>();

		for (ARTResource res : resources) {
			boolean toBeRendered = true;

			if (res.isBlank()) {
				if ((statements.hasType(res, OWL.Res.CLASS, false, NodeFilters.ANY) || statements.hasType(
						res, RDFS.Res.CLASS, false, NodeFilters.ANY))
						&& !statements.hasType(res, OWL.Res.DATARANGE, false, NodeFilters.ANY)) {

					// Renders OWL class expressions using the Manchester syntax. Following common modeling
					// patterns,
					// it assumes that class expressions are represented as bnodes. Leveraging this assumption
					// the
					// serialization can be completely based on the statements describing the subject resource
					// (which are expanded through the closure of bnodes)

					ManchesterClassInterface anonCls = statements.getManchClassFromBNode(res.asBNode(),
							statements, NodeFilters.ANY, null);

					if (anonCls != null) {
						resource2rendering.put(res, anonCls.getManchExpr(project.getOntModel(), true, true));
						toBeRendered = false;
					}
				} else if (statements.hasType(res, RDF.Res.LIST, false, NodeFilters.ANY)
						|| Objects.equal(res, RDF.Res.NIL)) {
					resource2rendering.put(res, renderCollection(res, project, statements));
					toBeRendered = false;
				}
			}

			// this check is necessary, because there might be bnodes of type Class that we are unable to
			// express in Manchester syntax
			if (toBeRendered) {

				// Otherwise (uri or bnode that is not a class expressions)

				// 1. computes the default rendering.

				if (res.isBlank()) { // 1a. bnode as _:bnodeid
					resource2rendering.put(res, RDFNodeSerializer.toNT(res));
				} else { // 2a. uri as qname
					resource2rendering.put(res, project.getOntModel().getQName(res.getNominalValue()));
				}
				// 2. delegates URIs to the wrapped rendering engine. The default value is useful, if the
				// delegate does not render everything
				if (res.isURIResource()) {
					ARTURIResource uriResource = res.asURIResource();
					delegatedResources.add(uriResource);
				}
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

	private String renderCollection(ARTResource res, Project<?> project, OWLModel statements)
			throws ModelAccessException {
		PrefixMapping prefixMapping = project.getOntModel();

		StringBuilder sb = new StringBuilder();

		sb.append("[");

		for (ARTNode anItem : statements.getItems(res, false, NodeFilters.ANY)) {
			if (sb.length() != 1) {
				sb.append(", ");
			}

			if (anItem.isURIResource()) {
				String uriSpec = anItem.getNominalValue();
				String qname = prefixMapping.getQName(anItem.getNominalValue());

				if (!qname.equals(uriSpec)) {
					sb.append(qname);
					continue;
				}
			} else if (anItem.isBlank()) {
				ARTBNode aBnode = anItem.asBNode();

				if ((statements.hasType(aBnode, RDFS.Res.CLASS, false, NodeFilters.ANY) || statements
						.hasType(aBnode, OWL.Res.CLASS, false, NodeFilters.ANY))
						&& !statements.hasType(res, OWL.Res.DATARANGE, false, NodeFilters.ANY)) {
					ManchesterClassInterface anonCls = statements.getManchClassFromBNode(aBnode, statements,
							NodeFilters.ANY, null);

					if (anonCls != null) {
						sb.append(anonCls.getManchExpr(prefixMapping, true, false));
						continue;
					}

				} else if (statements.hasType(aBnode, RDF.Res.LIST, false, NodeFilters.ANY)
						|| Objects.equal(aBnode, RDF.Res.NIL)) {
					sb.append(renderCollection(aBnode, project, statements));
				}
			}

			sb.append(RDFNodeSerializer.toNT(anItem));
		}

		sb.append("]");

		return sb.toString();
	}

	/**
	 * Determines the rendering engine to use based on the position of the subject resource
	 * 
	 * @param subjectPosition
	 * @return
	 */
	private RenderingEngine getRenderingEngine(ResourcePosition subjectPosition) {
		if (subjectPosition instanceof LocalResourcePosition) {
			Project<?> project = ((LocalResourcePosition) (subjectPosition)).getProject();
			RDFModel ontModel = project.getOntModel();
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
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return getRenderingEngine(resourcePosition).getGraphPatternForDescribe(resourcePosition,
				resourceToBeRendered, varPrefix);
	}

}
