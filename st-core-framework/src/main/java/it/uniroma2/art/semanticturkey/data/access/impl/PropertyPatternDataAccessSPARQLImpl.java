package it.uniroma2.art.semanticturkey.data.access.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.PropertyPatternDataAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PropertyPatternDataAccessSPARQLImpl implements PropertyPatternDataAccess {

	private CloseableTripleQueryModel queryModel;

	public PropertyPatternDataAccessSPARQLImpl(CloseableTripleQueryModel queryModel) {
		this.queryModel = queryModel;
	}

	@Override
	public Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> retrieveInformationAbout(Collection<ARTURIResource> uriResources,
			Multimap<ARTURIResource, ARTNode> propertyPattern) throws DataAccessException {

		List<ARTURIResource> relevantPredicates = new ArrayList<ARTURIResource>(propertyPattern.keySet());

		StringBuilder sb = new StringBuilder();
		sb.append("construct where {\n");

		for (int i = 0; i < relevantPredicates.size(); i++) {
			ARTURIResource pred = relevantPredicates.get(i);

			for (ARTNode obj : propertyPattern.get(pred)) {
				if (obj.equals(NodeFilters.ANY)) {
					sb.append("  ?resource ").append(RDFNodeSerializer.toNT(pred)).append(" ").append("?var")
							.append(i).append(" .");
				} else {
					sb.append("  ?resource ").append(RDFNodeSerializer.toNT(pred)).append(" ")
							.append(RDFNodeSerializer.toNT(obj)).append(" .");
				}
			}
		}

		sb.append("values(?resource){\n");

		// TODO: better quota management
		int remainingResouces = MAXIMUM_RESOURCE_COUNT_FOR_BATCH;

		for (ARTURIResource uriRes : uriResources) {
			if (remainingResouces == 0) {
				break;
			} else {
				remainingResouces--;
			}

			sb.append("(" + RDFNodeSerializer.toNT(uriRes) + ")\n");
		}
		
		sb.append("}\n");

		sb.append("}");

		try {
			GraphQuery query = queryModel.createGraphQuery(QueryLanguage.SPARQL, sb.toString(), null);
			Collection<ARTStatement> statements = RDFIterators.getCollectionFromIterator(query.evaluate(true));
			
			Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> result = new HashMap<ARTURIResource, Multimap<ARTURIResource,ARTNode>>();
			
			for (ARTStatement stmt : statements) {
				ARTURIResource subj = stmt.getSubject().asURIResource();
				ARTURIResource pred = stmt.getPredicate();
				ARTNode obj = stmt.getObject();

				Multimap<ARTURIResource, ARTNode> subjDescr = result.get(subj);
				
				if (subjDescr == null) {
					subjDescr = HashMultimap.<ARTURIResource, ARTNode>create();
					result.put(subj, subjDescr);
				}
				
				subjDescr.put(pred, obj);
			}

			return result;
	
		} catch (QueryEvaluationException | ModelAccessException | UnsupportedQueryLanguageException | MalformedQueryException e) {
			throw new DataAccessException(e);
		}
	}

}
