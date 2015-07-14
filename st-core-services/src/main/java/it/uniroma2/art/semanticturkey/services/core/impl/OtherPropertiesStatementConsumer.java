package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class OtherPropertiesStatementConsumer implements StatementConsumer {

	private CustomRangeProvider customRangeProvider;

	public OtherPropertiesStatementConsumer(CustomRangeProvider customRangeProvider) {
		this.customRangeProvider = customRangeProvider;
	}
	
	@Override
	public LinkedHashMap<String, ResourceViewSection> consumeStatements(
			Project<?> project, ARTResource resource, ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering,
			Map<ARTResource, ARTLiteral> xLabel2LiteralForm)
			throws ModelAccessException {

		RDFModel ontModel = project.getOntModel();
		
		Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new HashMap<ARTURIResource, STRDFResource>();
		Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory.createPredicateObjectsList(
				art2STRDFPredicates, resultPredicateObjectValues);

		for (ARTStatement stmt : stmtCollector.getStatements()) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

			if (!stmt.getSubject().equals(resource))
				continue;

			ARTURIResource pred = stmt.getPredicate();

			if (STVocabUtilities.isHiddenResource(pred, project.getOntologyManager())) {
				continue;
			}

			STRDFResource stPred = art2STRDFPredicates.get(pred);

			if (stPred == null) {
				stPred = STRDFNodeFactory.createSTRDFURI(pred,
						resource2Role.containsKey(pred) ? resource2Role.get(pred)
								: RDFResourceRolesEnum.property, true, ontModel.getQName(pred.getURI()));
				stPred.setInfo("hasCustomRange", Boolean.toString(customRangeProvider.existsCustomRangeEntryGraphForProperty(pred.getURI())));
				art2STRDFPredicates.put(pred, stPred);
			}

			ARTNode obj = stmt.getObject();

			STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(ontModel, obj, false,
					graphs.contains(NodeFilters.MAINGRAPH), false);

			if (stNode.isResource()) {
				((STRDFResource) stNode).setRendering(resource2Rendering.get(obj));					

				RDFResourceRolesEnum nodeRole = resource2Role.get(obj);
				((STRDFResource) stNode).setRole(nodeRole);

				if (nodeRole == RDFResourceRolesEnum.xLabel) {
					ARTLiteral lit = xLabel2LiteralForm.get(obj);

					if (lit != null) {
						((STRDFResource) stNode).setRendering(lit.getLabel());					

						if (lit.getLanguage() != null) {
							((STRDFResource) stNode).setInfo("lang", lit.getLanguage());
						}
					}
				}
			}

			stNode.setInfo("graphs", Joiner.on(",").join(graphs));

			resultPredicateObjectValues.put(pred, stNode);
		}

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put("properties", new PredicateObjectsListSection(
				predicateObjectsList));
		return result;
	}

}
