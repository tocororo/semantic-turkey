package it.uniroma2.art.semanticturkey.services.core.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.project.Project;

public class MultiPropertyMatchingAbstractStatementConsumer implements StatementConsumer {

	private String sectionName;
	private ARTURIResource[] properties;

	public MultiPropertyMatchingAbstractStatementConsumer(String sectionName, ARTURIResource[] properties) {
		this.sectionName = sectionName;
		this.properties = properties;
	}

	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(Project<?> project,
			ARTResource resource, ResourcePosition resourcePosition, ARTResource workingGraph,
			RDFResourceRolesEnum resourceRole, StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role, Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, ARTLiteral> xLabel2LiteralForm) throws ModelAccessException {

		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition)resourcePosition).getProject().equals(project);
		}

		Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new LinkedHashMap<ARTURIResource, STRDFResource>();
		Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		for (ARTURIResource property : properties) {
			// Selects the relevant statements
			Set<ARTStatement> relevantStmts = stmtCollector.getStatements(resource, property,
					NodeFilters.ANY);

			// Skips empty sections
			if (relevantStmts.isEmpty())
				continue;

			// Creates the ST RDF URI for the property
			STRDFURI stPred = STRDFNodeFactory.createSTRDFURI(property, resource2Role.get(property), true,
					project.getOntModel().getQName(property.getURI()));
			art2STRDFPredicates.put(property, stPred);

			// Processes the statements
			for (ARTStatement stmt : relevantStmts) {
				ARTNode obj = stmt.getObject();
				Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
				STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(project.getOntModel(), obj, false,
						currentProject && graphs.contains(workingGraph), false);

				if (stNode.isResource()) {
					RDFResourceRolesEnum role = resource2Role.get(obj);

					STRDFResource stRes = (STRDFResource) stNode;

					stRes.setRendering(resource2Rendering.get(obj));

					if (RDFResourceRolesEnum.xLabel == role) {
						ARTLiteral lit = xLabel2LiteralForm.get(obj);

						if (lit != null) {
							stRes.setRendering(lit.getLabel());

							if (lit.getLanguage() != null) {
								stRes.setInfo("lang", lit.getLanguage());
							}
						}
					}

					stRes.setRole(role);
				}

				stNode.setInfo("graphs", Joiner.on(",").join(graphs));

				resultPredicateObjectValues.put(property, stNode);
			}

			// Mark all matched statements as processed
			stmtCollector.markAllStatementsAsProcessed(relevantStmts);
		}

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory
				.createPredicateObjectsList(art2STRDFPredicates, resultPredicateObjectValues);

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put(sectionName, new PredicateObjectsListSection(predicateObjectsList));

		return result;
	}

	protected void filterObjectsInPlace(Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues) {
	}
}
