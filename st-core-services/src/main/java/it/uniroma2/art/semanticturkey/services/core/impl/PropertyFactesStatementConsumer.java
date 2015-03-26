package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

public class PropertyFactesStatementConsumer implements StatementConsumer {

	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(
			Project<?> project, ARTResource resource, ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, String> xLabel2LiteralForm)
			throws ModelAccessException {
		boolean symmetric = false;
		boolean symmetricExplicit = false;

		boolean functional = false;
		boolean functionalExplicit = false;

		boolean inverseFunctional = false;
		boolean inverseFunctionalExplicit = false;

		boolean transitive = false;
		boolean transitiveExplicit = false;

		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.SYMMETRICPROPERTY, NodeFilters.ANY)) {
			symmetric = true;
			symmetricExplicit = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.SYMMETRICPROPERTY).contains(StatementCollector.INFERENCE_GRAPH);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.FUNCTIONALPROPERTY, NodeFilters.ANY)) {
			functional = true;
			functionalExplicit = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.FUNCTIONALPROPERTY).contains(StatementCollector.INFERENCE_GRAPH);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.INVERSEFUNCTIONALPROPERTY, NodeFilters.ANY)) {
			inverseFunctional = true;
			inverseFunctionalExplicit = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.INVERSEFUNCTIONALPROPERTY).contains(StatementCollector.INFERENCE_GRAPH);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.TRANSITIVEPROPERTY, NodeFilters.ANY)) {
			transitive = true;
			transitiveExplicit = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.TRANSITIVEPROPERTY).contains(StatementCollector.INFERENCE_GRAPH);
		}


		Set<ARTStatement> inverseOfStmts = stmtCollector.getStatements(resource, OWL.Res.INVERSEOF,
				NodeFilters.ANY);

		List<STRDFNode> inverseOf = new ArrayList<STRDFNode>();

		for (ARTStatement stmt : inverseOfStmts) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
			STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
					resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
					resource2Rendering.get(stmt.getObject()));
			stRes.setInfo("graphs", Joiner.on(",").join(graphs));
			inverseOf.add(stRes);
		}

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result .put("facets", new PropertyFacets(symmetric, symmetricExplicit, functional,
				functionalExplicit, inverseFunctional, inverseFunctionalExplicit, transitive,
				transitiveExplicit, inverseOf));

		// Remove the inverse of statements from the resource view
		inverseOfStmts.clear();
		
		return result;
	}

}
