package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
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
			Map<ARTResource, ARTLiteral> xLabel2LiteralForm)
			throws ModelAccessException {
		boolean symmetric = false;
		boolean symmetricExplicit = true;

		boolean functional = false;
		boolean functionalExplicit = true;

		boolean inverseFunctional = false;
		boolean inverseFunctionalExplicit = true;

		boolean transitive = false;
		boolean transitiveExplicit = true;

		Set<ARTStatement> stmts;
		Set<ARTResource> graphs;
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.SYMMETRICPROPERTY, NodeFilters.ANY)) {
			symmetric = true;
			stmts = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.SYMMETRICPROPERTY);
			graphs = stmtCollector.getGraphsFor(VocabUtilities.nodeFactory.createStatement(resource, RDF.Res.TYPE, OWL.Res.SYMMETRICPROPERTY));
			symmetricExplicit = graphs.contains(NodeFilters.MAINGRAPH);
			// Mark statements as processed
			stmtCollector.markAllStatementsAsProcessed(stmts);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.FUNCTIONALPROPERTY, NodeFilters.ANY)) {
			functional = true;
			stmts = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.FUNCTIONALPROPERTY);
			graphs = stmtCollector.getGraphsFor(VocabUtilities.nodeFactory.createStatement(resource, RDF.Res.TYPE, OWL.Res.FUNCTIONALPROPERTY));
			functionalExplicit = graphs.contains(NodeFilters.MAINGRAPH);
			// Mark statements as processed
			stmtCollector.markAllStatementsAsProcessed(stmts);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.INVERSEFUNCTIONALPROPERTY, NodeFilters.ANY)) {
			inverseFunctional = true;
			stmts = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.INVERSEFUNCTIONALPROPERTY);
			graphs = stmtCollector.getGraphsFor(VocabUtilities.nodeFactory.createStatement(resource, RDF.Res.TYPE, OWL.Res.INVERSEFUNCTIONALPROPERTY));
			inverseFunctionalExplicit = graphs.contains(NodeFilters.MAINGRAPH);
			// Mark statements as processed
			stmtCollector.markAllStatementsAsProcessed(stmts);
		}
		
		if (stmtCollector.hasStatement(resource, RDF.Res.TYPE, OWL.Res.TRANSITIVEPROPERTY, NodeFilters.ANY)) {
			transitive = true;
			stmts = stmtCollector.getStatements(resource, RDF.Res.TYPE, OWL.Res.TRANSITIVEPROPERTY);
			graphs = stmtCollector.getGraphsFor(VocabUtilities.nodeFactory.createStatement(resource, RDF.Res.TYPE, OWL.Res.TRANSITIVEPROPERTY));
			transitiveExplicit = graphs.contains(NodeFilters.MAINGRAPH);
			// Mark statements as processed
			stmtCollector.markAllStatementsAsProcessed(stmts);
		}

		Set<ARTStatement> inverseOfStmts = stmtCollector.getStatements(resource, OWL.Res.INVERSEOF,
				NodeFilters.ANY);

		List<STRDFNode> inverseOf = new ArrayList<STRDFNode>();

		for (ARTStatement stmt : inverseOfStmts) {
			graphs = stmtCollector.getGraphsFor(stmt);
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

		// Mark inverse of statements as processed
		stmtCollector.markAllStatementsAsProcessed(inverseOfStmts);

		return result;
	}

}
