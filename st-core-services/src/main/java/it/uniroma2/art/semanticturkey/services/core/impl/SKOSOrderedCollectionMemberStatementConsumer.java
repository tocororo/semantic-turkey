package it.uniroma2.art.semanticturkey.services.core.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

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
		Queue<ARTResource> frontier = new LinkedList<>();
				
		Set<ARTStatement> relevantStatements = stmtCollector.getStatements(resource, SKOS.Res.MEMBERLIST, NodeFilters.ANY);
		frontier.addAll(relevantStatements.stream().map(s -> s.getObject().asResource()).collect(Collectors.toList()));
		
		while (!frontier.isEmpty()) {
			ARTResource top = frontier.poll();

			if (RDF.Res.NIL.equals(top))
				continue;

			if (alreadExpandedCollections.contains(top))
				continue;

			alreadExpandedCollections.add(top);

			List<ARTResource> firstElements = stmtCollector.getStatements(top, RDF.Res.FIRST, NodeFilters.ANY).stream()
					.map(ARTStatement::getObject).map(ARTNode::asResource).collect(Collectors.toList());

			for (ARTResource element : firstElements) {
				Set<ARTResource> graphs = new HashSet<>();
				STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(element,
						Objects.firstNonNull(resource2Role.get(element), RDFResourceRolesEnum.individual),
						currentProject && graphs.contains(workingGraph),
						Objects.firstNonNull(resource2Rendering.get(element), element.getNominalValue()));
				stRes.setInfo("graphs", Joiner.on(",").join(graphs));
				objects.add(stRes);
			}
			
			frontier.addAll(stmtCollector.getStatements(top, RDF.Res.REST, NodeFilters.ANY).stream()
					.map(ARTStatement::getObject).map(ARTNode::asResource).collect(Collectors.toList()));
		}
		
		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put("membersOrdered", new NodeListSection(objects));

		// Mark the matched statements as processed
		stmtCollector.markAllStatementsAsProcessed(relevantStatements);
		
		return result;
	}

}
