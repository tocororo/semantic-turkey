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
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.UnknownResourcePosition;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.core.ResourceView;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LexicalizationsStatementConsumer implements StatementConsumer {

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
		Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new LinkedHashMap<ARTURIResource, STRDFResource>();
		Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap
				.create();

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory
				.createPredicateObjectsList(art2STRDFPredicates,
						resultPredicateObjectValues);

		for (ARTURIResource pred : Arrays.asList(RDFS.Res.LABEL,
				SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL, SKOS.Res.HIDDENLABEL,
				SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL,
				SKOSXL.Res.HIDDENLABEL)) {
			STRDFURI stPred = STRDFNodeFactory
					.createSTRDFURI(
							pred,
							resource2Role.containsKey(pred) ? resource2Role
									.get(pred)
									: pred.getNamespace().equals(
											SKOSXL.NAMESPACE) ? RDFResourceRolesEnum.objectProperty
											: RDFResourceRolesEnum.annotationProperty,
							true,
							ontModel.getQName(pred.getURI()));
			art2STRDFPredicates.put(pred, stPred);
		}

		Iterator<ARTStatement> stmtIt = stmtCollector.getStatements()
				.iterator();

		List<ARTURIResource> relevantLexPreds = ResourceView.getLexicalizationPropertiesHelper(resource, resourcePosition);

		while (stmtIt.hasNext()) {
			ARTStatement stmt = stmtIt.next();

			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

			if (!stmt.getSubject().equals(resource))
				continue;

			ARTURIResource pred = stmt.getPredicate();

			STRDFResource stPred = art2STRDFPredicates.get(pred);

			if (stPred == null)
				continue;

			ARTNode obj = stmt.getObject();

			STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(ontModel, obj, false, graphs
					.contains(NodeFilters.MAINGRAPH), false);

			if (!(stNode.isExplicit() || relevantLexPreds.contains(pred))) continue;
			
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

			resultPredicateObjectValues.put(pred, stNode);
			stmtIt.remove();
		}
		
		Iterator<ARTURIResource> keyIt = art2STRDFPredicates.keySet().iterator();
		
		// Removes predicates with no associated lexicalization
		while (keyIt.hasNext()) {
			ARTURIResource key = keyIt.next();
						
			if (!resultPredicateObjectValues.containsKey(key)){
				keyIt.remove();
			}
		}
		
		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		result.put("lexicalizations", new PredicateObjectsListSection(
				predicateObjectsList));
		return result;
	}

}
