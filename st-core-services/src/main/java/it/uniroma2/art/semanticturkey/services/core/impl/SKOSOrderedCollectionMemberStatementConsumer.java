package it.uniroma2.art.semanticturkey.services.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;

public class SKOSOrderedCollectionMemberStatementConsumer implements StatementConsumer {

	public SKOSOrderedCollectionMemberStatementConsumer() {
	}

	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(Project<?> project,
			ARTResource resource, ResourcePosition resourcePosition, ARTResource workingGraph,
			RDFResourceRolesEnum resourceRole, StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role, Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, ARTLiteral> xLabel2LiteralForm) throws ModelAccessException {
		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		}

		ArrayList<STRDFNode> objects = new ArrayList<STRDFNode>();

		Set<ARTResource> alreadExpandedCollections = new HashSet<>();

		Set<ARTStatement> relevantStatements = stmtCollector.getStatements(resource, SKOS.Res.MEMBERLIST,
				NodeFilters.ANY);

		Queue<NodeContext> frontier2 = new LinkedList<>();

		for (ARTStatement stmt : relevantStatements) {
			ARTNode listAsNode = stmt.getObject();
			if (!listAsNode.isResource())
				continue;

			ARTResource listResource = listAsNode.asResource();
			frontier2.add(new NodeContext(listResource, new HashSet<>(stmtCollector.getGraphsFor(stmt))));
		}

		while (!frontier2.isEmpty()) {
			NodeContext topContext = frontier2.poll();
			ARTResource top = topContext.getResource();
			Set<ARTResource> cumulativeGraphs = topContext.getCumulativeGraphs();

			if (RDF.Res.NIL.equals(top))
				continue;

			if (alreadExpandedCollections.contains(top))
				continue;

			alreadExpandedCollections.add(top);

			Map<ARTResource, Set<ARTResource>> firstElement2graphs = new HashMap<>();

			for (ARTStatement firstStmt : stmtCollector.getStatements(top, RDF.Res.FIRST, NodeFilters.ANY)) {
				ARTNode firstElement = firstStmt.getObject();

				if (!firstElement.isResource())
					continue;

				firstElement2graphs.put(firstElement.asResource(),
						new HashSet<>(stmtCollector.getGraphsFor(firstStmt)));
			}

			for (Entry<ARTResource, Set<ARTResource>> firstElementAndGraphs : firstElement2graphs
					.entrySet()) {
				ARTResource firstElement = firstElementAndGraphs.getKey();

				Set<ARTResource> graphs = new HashSet<>();
				graphs.addAll(firstElementAndGraphs.getValue());
				graphs.addAll(cumulativeGraphs);

				STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(firstElement.asResource(),
						Objects.firstNonNull(resource2Role.get(firstElement),
								RDFResourceRolesEnum.individual),
						currentProject && graphs.contains(workingGraph),
						Objects.firstNonNull(resource2Rendering.get(firstElement),
								firstElement.getNominalValue()));
				stRes.setInfo("graphs", Joiner.on(",").join(graphs));
				stRes.setInfo("index", "" + (topContext.getIndex() + 1));
				objects.add(stRes);
			}

			Set<ARTStatement> nextStmtSet = stmtCollector.getStatements(top, RDF.Res.REST, NodeFilters.ANY);
			boolean requireCloning = nextStmtSet.size() > 1;

			for (ARTStatement nextStmt : nextStmtSet) {
				ARTNode nextElement = nextStmt.getObject();

				if (!nextElement.isResource())
					continue;

				frontier2.add(topContext.nextContext(nextElement.asResource(),
						stmtCollector.getGraphsFor(nextStmt), requireCloning));
			}
		}

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put("membersOrdered", new NodeListSection(objects));

		// Mark the matched statements as processed
		stmtCollector.markAllStatementsAsProcessed(relevantStatements);

		return result;
	}

}

class NodeContext {
	private ARTResource resource;
	private Set<ARTResource> cumulativeGraphs;
	private int index;

	public NodeContext(ARTResource resource, Set<ARTResource> cumulativeGraphs) {
		this(resource, cumulativeGraphs, 0);
	}

	public NodeContext(ARTResource resource, Set<ARTResource> cumulativeGraphs, int index) {
		this.resource = resource;
		this.cumulativeGraphs = new HashSet<>(cumulativeGraphs);
		this.index = index;
	}

	public NodeContext nextContext(ARTResource resource, Set<ARTResource> newGraphs,
			boolean requireCloning) {
		NodeContext nc = this;

		if (requireCloning) {
			nc = new NodeContext(resource, new HashSet<>(cumulativeGraphs), index);
		}

		nc.index++;
		nc.resource = resource;
		nc.cumulativeGraphs.addAll(newGraphs);
		
		return nc;
	}

	public int getIndex() {
		return index;
	}

	public ARTResource getResource() {
		return resource;
	}

	public Set<ARTResource> getCumulativeGraphs() {
		return cumulativeGraphs;
	}
}
