package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLArtModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.models.impl.SKOSModelImpl;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

@GenerateSTServiceController
@Validated
@Component
public class Alignment extends STServiceAdapter {
	
	private static List<ARTURIResource> skosMappingRelations;
	static {
		skosMappingRelations = new ArrayList<>();
		skosMappingRelations.add(SKOS.Res.MAPPINGRELATION);
		skosMappingRelations.add(SKOS.Res.EXACTMATCH);
		skosMappingRelations.add(SKOS.Res.BROADMATCH);
		skosMappingRelations.add(SKOS.Res.NARROWMATCH);
		skosMappingRelations.add(SKOS.Res.CLOSEMATCH);
		skosMappingRelations.add(SKOS.Res.RELATEDMATCH);
	};
	
	private static List<ARTURIResource> owlMappingRelations;
	static {
		owlMappingRelations = new ArrayList<>();
		owlMappingRelations.add(OWL.Res.SAMEAS);
		owlMappingRelations.add(OWL.Res.DIFFERENTFROM);
		owlMappingRelations.add(OWL.Res.ALLDIFFERENT);
		owlMappingRelations.add(OWL.Res.EQUIVALENTCLASS);
		owlMappingRelations.add(OWL.Res.DISJOINTWITH);
	};
	
	
	/**
	 * Adds the given alignment triple only if predicate is a valid alignment property
	 * @param sourceResource
	 * @param predicate
	 * @param targetResource
	 * @return
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 */
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
			} else {
				return createReplyFAIL(predicate.getNominalValue() + " is not a valid alignment SKOS property");
			}
		} else if (model instanceof OWLModel) {
			if (predicate.equals(OWL.Res.SAMEAS) || predicate.equals(OWL.Res.DIFFERENTFROM) || 
					predicate.equals(OWL.Res.ALLDIFFERENT) || predicate.equals(OWL.Res.EQUIVALENTCLASS) || 
					predicate.equals(OWL.Res.DISJOINTWITH)){
				model.addTriple(sourceResource, predicate, targetResource, getWorkingGraph());
			} else {
				return createReplyFAIL(predicate.getNominalValue() + " is not a valid alignment OWL property");
			}
		}
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Based on the model type, returns the available alignment properties
	 * @return
	 * @throws ModelAccessException
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws UnsupportedRDFFormatException 
	 * @throws ModelUpdateException 
	 * @throws IOException 
	 */
	@GenerateSTServiceController
	public Response getMappingRelations(@Optional (defaultValue = "false") boolean includeSKOSProps)
			throws ModelAccessException, UnavailableResourceException, ProjectInconsistentException, IOException, ModelUpdateException, UnsupportedRDFFormatException{

		Collection<STRDFURI> result = STRDFNodeFactory.createEmptyURICollection();
		
		OWLModel model = getOWLModel();
		if (model instanceof SKOSModel || model instanceof SKOSXLModel) {
			for (ARTURIResource prop : skosMappingRelations){
				result.add(STRDFNodeFactory.createSTRDFURI(prop, 
						ModelUtilities.getPropertyRole(prop, model), true,
						model.getQName(prop.getURI())));
			}
		} else if (model instanceof OWLModel) {
			for (ARTURIResource prop : owlMappingRelations){
				result.add(STRDFNodeFactory.createSTRDFURI(prop, 
						ModelUtilities.getPropertyRole(prop, model), true,
						model.getQName(prop.getURI())));
			}
			if (includeSKOSProps){
				OWLArtModelFactory<?> mf = OWLArtModelFactory.createModelFactory(PluginManager.getOntManagerImpl(
						getProject().getOntologyManagerImplID()).createModelFactory());
				mf.setPopulatingW3CVocabularies(true);
				SKOSModel tempSkosModel = new SKOSModelImpl(mf.createLightweightRDFModel());
				ArrayList<String> vocabs = new ArrayList<String>();
				vocabs.add(RDFS.NAMESPACE);
				vocabs.add(OWL.NAMESPACE);
				vocabs.add(SKOS.NAMESPACE);
				vocabs.add(SKOSXL.NAMESPACE);
				mf.checkVocabularyData(tempSkosModel, vocabs);
				
				for (ARTURIResource prop : skosMappingRelations){
					result.add(STRDFNodeFactory.createSTRDFURI(prop, 
							ModelUtilities.getPropertyRole(prop, tempSkosModel), true,
							tempSkosModel.getQName(prop.getURI())));
				}
			}
		}
		
		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(resp, result);
		return resp;
	}

}
