package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

//@GenerateSTServiceController
@Validated
@Component
public class Resource extends STServiceAdapter {

	@GenerateSTServiceController
	@AutoRendering
	public Collection<STRDFNode> getPropertyValues(@Existing ARTResource subject, ARTURIResource predicate) throws ModelAccessException {
		OWLModel model = getOWLModel();
		ARTResource[] graphs;
		graphs = getUserNamedGraphs();
		ARTNodeIterator it = model.listValuesOfSubjPredPair(subject, predicate, true, graphs);

		Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(model
				.listValuesOfSubjPredPair(subject, predicate, false, getWorkingGraph()));

		Collection<STRDFNode> values = STRDFNodeFactory.createEmptyNodeCollection();
		while (it.streamOpen()) {
			ARTNode next = it.getNext();
			boolean explicit;
			if (explicitValues.contains(next))
				explicit = true;
			else
				explicit = false;
			values.add(STRDFNodeFactory.createSTRDFNode(model, next, true, explicit, false)); // disables rendering
		}
		it.close();
		
		return values;
	}
	
	@Override
	public OWLModel getOWLModel() {
		return ProjectManager.getCurrentProject().getOWLModel();
	}
	
	public ARTResource[] getUserNamedGraphs() {
		ARTResource[] graphs = {NodeFilters.ANY};
		return graphs;
	}
	
	public ARTResource getWorkingGraph() {
		return NodeFilters.MAINGRAPH;
	}
	
}
