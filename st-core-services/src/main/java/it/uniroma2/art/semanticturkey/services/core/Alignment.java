package it.uniroma2.art.semanticturkey.services.core;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;

@GenerateSTServiceController
@Validated
@Component
public class Alignment extends STServiceAdapter {
	
	@GenerateSTServiceController
	public Response addAlignment(ARTResource sourceResource, ARTURIResource predicate, ARTURIResource targetResource)
			throws ModelAccessException, ModelUpdateException {
		OWLModel model = getOWLModel();
		
		if (model instanceof SKOSModel || model instanceof SKOSXLModel) {
			//check if predicate is valid for alignment
			ARTURIResourceIterator itAlignProps = model.listSubProperties(SKOS.Res.MAPPINGRELATION, true, NodeFilters.ANY);
			boolean validPred = false;
			while (itAlignProps.hasNext()){
				if (itAlignProps.next().equals(predicate)){
					validPred = true;
					break;
				}
			}
			if (validPred){
				model.addTriple(sourceResource, predicate, targetResource, getWorkingGraph());
			}
		} else if (model instanceof OWLModel) {
			//TODO what are the alignment properties in OWL? Individual: sameAs, differentFrom, allDifferent; Class: equivalentClass, disjointWith?
		}
		return createReplyResponse(RepliesStatus.ok);
	}

}
