package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

public class OntologyImportsStatementConsumer implements StatementConsumer {

	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(
			Project<?> project, ARTResource resource, ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, String> xLabel2LiteralForm)
			throws ModelAccessException {
		Set<ARTStatement> importStmts = stmtCollector.getStatements(resource, OWL.Res.IMPORTS,
				NodeFilters.ANY);

		List<STRDFNode> imports = new ArrayList<STRDFNode>();

		for (ARTStatement stmt : importStmts) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

			ARTNode obj = stmt.getObject();

			if (obj.isURIResource()) {
				STRDFURI stUri = STRDFNodeFactory.createSTRDFURI(obj.asURIResource(),
						resource2Role.get(stmt.getObject()),
						graphs.contains(NodeFilters.MAINGRAPH),
						resource2Rendering.get(stmt.getObject()));
				stUri.setInfo("graphs", Joiner.on(",").join(graphs));
				imports.add(stUri);
			}
		}

		importStmts.clear();

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result .put("imports", new NodeListSection(imports));
		return result;
	}

}
