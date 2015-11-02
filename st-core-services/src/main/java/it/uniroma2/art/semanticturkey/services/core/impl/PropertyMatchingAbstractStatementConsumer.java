package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
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

public class PropertyMatchingAbstractStatementConsumer implements StatementConsumer {

	private String sectionName;
	private ARTURIResource property;
	
	public PropertyMatchingAbstractStatementConsumer(String sectionName, ARTURIResource property) {
		this.sectionName = sectionName;
		this.property = property;
	}
	
	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(
			Project<?> project, ARTResource resource,
			ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, ARTLiteral> xLabel2LiteralForm)
			throws ModelAccessException {
		Set<ARTStatement> relevantStmts = stmtCollector.getStatements(resource,
				property, NodeFilters.ANY);

		ArrayList<STRDFNode> objects = new ArrayList<STRDFNode>();

		for (ARTStatement stmt : relevantStmts) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
			STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt
					.getObject().asResource(), resource2Role.get(stmt
					.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
					resource2Rendering.get(stmt.getObject()));
			stRes.setInfo("graphs", Joiner.on(",").join(graphs));
			
			RDFResourceRolesEnum objRole = resource2Role.get(stmt.getObject());
			
			if (objRole == RDFResourceRolesEnum.xLabel) {
				ARTLiteral lit = xLabel2LiteralForm.get(stmt.getObject());
				
				if (lit != null) {
					stRes.setRendering(lit.getLabel());
					
					if (lit.getLanguage() != null) {
						stRes.setInfo("lang", lit.getLanguage());
					}
				}
			}
			
			objects.add(stRes);
		}

		filterObjectsInPlace(objects);

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put(sectionName, new NodeListSection(objects));

		
		
		// Mark the matched statements as processed
		stmtCollector.markAllStatementsAsProcessed(relevantStmts);

		return result;
	}

	protected void filterObjectsInPlace(ArrayList<STRDFNode> objects) {
	}
}
