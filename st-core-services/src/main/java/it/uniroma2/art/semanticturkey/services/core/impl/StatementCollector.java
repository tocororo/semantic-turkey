package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenComponents_Predicate;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;

public class StatementCollector {
	
	public final static ARTResource INFERENCE_GRAPH = VocabUtilities.nodeFactory.createURIResource("http://semanticturkey/inference-graph");
	
	private HashMultimap<ARTStatement, ARTResource> data = HashMultimap.create();
	
	public void addStatement(ARTResource subj, ARTURIResource pred, ARTNode obj, ARTResource graph) {
		ARTStatement stmt = VocabUtilities.nodeFactory.createStatement(subj, pred, obj);
		
		Set<ARTResource> graphs = data.get(stmt);
		if (!graphs.contains(graph)) {
			graphs.add(graph);
		}
	}
	
	public void addInferredStatement(ARTResource subj, ARTURIResource pred, ARTNode obj) {
		ARTStatement stmt = VocabUtilities.nodeFactory.createStatement(subj, pred, obj);
		
		Set<ARTResource> graphs = data.get(stmt);
		
		if (graphs.isEmpty()) {
			graphs.add(INFERENCE_GRAPH);
		}
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Entry<ARTStatement, ARTResource> entry : data.entries()) {
			sb.append(entry.getKey()).append(" in ").append(entry.getValue()).append("\n");
		}
		
		return sb.toString();
	}

	public Set<ARTStatement> getStatements() {
		return data.keySet();
	}
	
	public Set<ARTStatement> getStatements(ARTResource subject, ARTURIResource predicate, ARTNode object) {
		return Sets.filter(getStatements(), StatementWithAnyOfGivenComponents_Predicate.getFilter(subject, predicate, object));
	}

	
	public Set<ARTResource> getGraphsFor(ARTStatement stmt) {
		return data.get(stmt);
	}

	public boolean hasStatement(ARTResource subj, ARTURIResource pred, ARTNode obj, ARTResource graph) {
		for (ARTStatement stmt : Collections2.filter(getStatements(), StatementWithAnyOfGivenComponents_Predicate.getFilter(subj, pred, obj))) {
			if (NodeFilters.ANY.equals(graph) || getGraphsFor(stmt).contains(graph)) {
				return true;
			}
		}
		
		return false;
	}

}
