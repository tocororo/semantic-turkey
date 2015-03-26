package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

public class TypesStatementConsumer implements StatementConsumer {

	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(
			Project<?> project, ARTResource resource, ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, String> xLabel2LiteralForm)
			throws ModelAccessException {
		Set<ARTStatement> typeStmts = stmtCollector.getStatements(resource,
				RDF.Res.TYPE, NodeFilters.ANY);

		ArrayList<STRDFNode> types = new ArrayList<STRDFNode>();

		for (ARTStatement stmt : typeStmts) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
			STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt
					.getObject().asResource(), resource2Role.get(stmt
					.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
					resource2Rendering.get(stmt.getObject()));
			stRes.setInfo("graphs", Joiner.on(",").join(graphs));
			types.add(stRes);
		}

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put("types", new NodeListSection(types));

		// Remove the typing statements from the resource view
		typeStmts.clear();

		return result;
	}

}
