package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.alignment.AlignmentModel;
import it.uniroma2.art.owlart.alignment.AlignmentModelFactory;
import it.uniroma2.art.owlart.alignment.Cell;
import it.uniroma2.art.owlart.alignment.AlignmentModel.Status;
import it.uniroma2.art.owlart.exceptions.InvalidAlignmentRelationException;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLArtModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.models.impl.RDFModelImpl;
import it.uniroma2.art.owlart.navigation.ARTURIResourceIterator;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.generation.annotation.RequestMethod;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
@Controller //needed for saveAlignment method
public class Alignment extends STServiceAdapter {
	
	@Autowired
	private STServiceContext stServiceContext;
	
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
		owlMappingRelations.add(RDFS.Res.SUBCLASSOF);
		
	};
	
	private static List<ARTURIResource> propertiesMappingRelations;
	static {
		propertiesMappingRelations = new ArrayList<>();
		propertiesMappingRelations.add(OWL.Res.EQUIVALENTPROPERTY);
		propertiesMappingRelations.add(RDFS.Res.SUBPROPERTYOF);
	};
	
	//map that contain <id, context> pairs to handle multiple sessions
	private Map<String, AlignmentModel> modelsMap = new HashMap<>();
	
	// SERVICES FOR ALIGNMENT IN RESOURCE VIEW
	
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
	 * Returns the available alignment properties depending on the type resource to align (property,
	 * or concept, or class,...).
	 * 
	 * @param resource resource to align
	 * @param allMappingProps if false returns just the mapping properties available for the current
	 * model type; if true returns all the mapping properties independently from the model type
	 * @return
	 * @throws ModelAccessException
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws UnsupportedRDFFormatException 
	 * @throws ModelUpdateException 
	 * @throws IOException 
	 */
	@GenerateSTServiceController
	public Response getMappingRelations(ARTURIResource resource, @Optional (defaultValue = "false") boolean allMappingProps)
			throws ModelAccessException, UnavailableResourceException, ProjectInconsistentException, IOException, ModelUpdateException, UnsupportedRDFFormatException {

		Collection<STRDFURI> result = STRDFNodeFactory.createEmptyURICollection();
		
		OWLModel model = getOWLModel();
		
		if (model.isProperty(resource, NodeFilters.ANY)){
			for (ARTURIResource prop : propertiesMappingRelations){
				result.add(STRDFNodeFactory.createSTRDFURI(prop, 
						ModelUtilities.getPropertyRole(prop, model), true,
						model.getQName(prop.getURI())));
			}
		} else {
			if (model instanceof SKOSModel || model instanceof SKOSXLModel) {
				for (ARTURIResource prop : skosMappingRelations){
					result.add(STRDFNodeFactory.createSTRDFURI(prop, 
							ModelUtilities.getPropertyRole(prop, model), true,
							model.getQName(prop.getURI())));
				}
				if (allMappingProps) {
					for (ARTURIResource prop : owlMappingRelations){
						result.add(STRDFNodeFactory.createSTRDFURI(prop, 
								ModelUtilities.getPropertyRole(prop, model), true,
								model.getQName(prop.getURI())));
					}
				}
			} else if (model instanceof OWLModel) {
				for (ARTURIResource prop : owlMappingRelations){
					result.add(STRDFNodeFactory.createSTRDFURI(prop, 
							ModelUtilities.getPropertyRole(prop, model), true,
							model.getQName(prop.getURI())));
				}
				if (allMappingProps){
					RDFModel tempModel = getTempModelForVocabularies();
					for (ARTURIResource prop : skosMappingRelations){
						result.add(STRDFNodeFactory.createSTRDFURI(prop, 
								ModelUtilities.getPropertyRole(prop, tempModel), true,
								tempModel.getQName(prop.getURI())));
					}
				}
			}
		}
		
		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(resp, result);
		return resp;
	}

	/* 
	 * Set up a temporary model to load all vocabularies
	 */
	private RDFModel getTempModelForVocabularies() throws UnavailableResourceException,
			ProjectInconsistentException, ModelAccessException, IOException, ModelUpdateException,
			UnsupportedRDFFormatException {
		OWLArtModelFactory<?> mf = OWLArtModelFactory.createModelFactory(PluginManager.getOntManagerImpl(
				getProject().getOntologyManagerImplID()).createModelFactory());
		mf.setPopulatingW3CVocabularies(true);
		RDFModel tempModel = new RDFModelImpl(mf.createLightweightRDFModel());
		ArrayList<String> vocabs = new ArrayList<String>();
		vocabs.add(RDFS.NAMESPACE);
		vocabs.add(OWL.NAMESPACE);
		vocabs.add(SKOS.NAMESPACE);
		mf.checkVocabularyData(tempModel, vocabs);
		return tempModel;
	}
	
	// SERVICES FOR ALIGNMENT VALIDATION
	
	/**
	 * Loads an alignment file (that is compliant with AlignmentAPI format) and if one of the 
	 * two aligned ontologies has the same baseURI of the current model, then return a response
	 * with its content.
	 * @param inputFile
	 * @return
	 * @throws IOException
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 * @throws UnsupportedRDFFormatException
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response loadAlignment(MultipartFile inputFile) 
			throws IOException, ModelAccessException, ModelUpdateException, UnsupportedRDFFormatException, 
			UnavailableResourceException, ProjectInconsistentException, UnsupportedQueryLanguageException, 
			MalformedQueryException, QueryEvaluationException {
		
		//create a temp file (in karaf data/temp folder) to copy the received file 
		File inputServerFile = File.createTempFile("alignment", inputFile.getOriginalFilename());
		inputFile.transferTo(inputServerFile);
		
		//creating temporary model for loading alignment
		OWLArtModelFactory<?> mf = OWLArtModelFactory.createModelFactory(PluginManager.getOntManagerImpl(
				getProject().getOntologyManagerImplID()).createModelFactory());
		AlignmentModel alignModel = AlignmentModelFactory.createAlignmentModel(mf.createLightweightRDFModel());		
		alignModel.addRDF(inputServerFile, null, RDFFormat.RDFXML_ABBREV, NodeFilters.MAINGRAPH);
		
		String token = stServiceContext.getSessionToken();
		modelsMap.put(token, alignModel);
		
		OWLModel model = getOWLModel();
		//check that one of the two aligned ontologies matches the current project ontology
		if (!model.getBaseURI().equals(alignModel.getOnto1())){
			if (model.getBaseURI().equals(alignModel.getOnto2())){
				alignModel.reverse();
			} else {
				return createReplyFAIL("Failed to open and validate the given alignment file. "
						+ "None of the two aligned ontologies matches the current project ontology");
			}
		}

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element alignmentElem = XMLHelp.newElement(dataElem, "Alignment");
		Element onto1Elem = XMLHelp.newElement(alignmentElem, "onto1");
		Element onto2Elem = XMLHelp.newElement(alignmentElem, "onto2");
		XMLHelp.newElement(onto1Elem, "Ontology", alignModel.getOnto1());
		XMLHelp.newElement(onto2Elem, "Ontology", alignModel.getOnto2());

		return response;
	}
	
	/**
	 * Returns the cells of the alignment file. Handles the scalability returning a portion of cells
	 * if <code>pageIdx</code> and <code>range</code> are provided as parameters 
	 * @param pageIdx index of the page in case 
	 * @param range alignment per page to show. If 0, returns all the alignments.
	 * @return
	 * @throws ModelAccessException
	 * @throws UnsupportedQueryLanguageException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	public Response listCells(@Optional (defaultValue = "0") int pageIdx, @Optional (defaultValue = "0") int range) 
			throws ModelAccessException, UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element mapElem = XMLHelp.newElement(dataElem, "map");
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cells = alignModel.listCells();
		//if range = 0 => return all cells
		if (range == 0) {
			mapElem.setAttribute("page", "1");
			mapElem.setAttribute("totPage", "1");
			for (Cell c : cells) {
				fillCellXMLResponse(c, mapElem);
			}
		} else {
			int size = cells.size();
			int begin = pageIdx * range;
			int end = begin + range;
			
			//if index of first cell > size of cell list (index out of bound) => return empty list of cells
			if (begin > size) {
				mapElem.setAttribute("page", "1");
				mapElem.setAttribute("totPage", "1");
			} else {
				int maxPage = size / range;
				if (size % range > 0) {
					maxPage++;
				}
				mapElem.setAttribute("page", pageIdx+1+"");
				mapElem.setAttribute("totPage", maxPage+"");
				if (end > size) {
					end = size;
				}
				for (int i=begin; i<end; i++){
					fillCellXMLResponse(cells.get(i), mapElem);
				}
			}
		}
		return response;
	}
	
	/**
	 * Accepts the alignment updating the alignment model
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 * @throws ModelUpdateException
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws AlignmentException 
	 */
	@GenerateSTServiceController
	public Response acceptAlignment(ARTURIResource entity1, ARTURIResource entity2, String relation) 
			throws ModelAccessException, UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAlignment(entity1, entity2, relation, getOWLModel());
		Cell c = alignModel.getCell(entity1, entity2);
		Element dataElem = response.getDataElement();
		fillCellXMLResponse(c, dataElem);
		return response;
	}
	
	/**
	 * Accepts all the alignment updating the alignment model
	 * 
	 * @return
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response acceptAllAlignment() throws ModelAccessException, UnsupportedQueryLanguageException, 
			MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAllAlignment(getOWLModel());
		
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			fillCellXMLResponse(c, dataElem);
		}
		return response;
	}
	
	/**
	 * Accepts all the alignment with measure above the given threshold updating the alignment model.
	 * The response contains the description of all the cells affected by the accept
	 * 
	 * @param threshold
	 * @return
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	public Response acceptAllAbove(float threshold) throws ModelAccessException, 
			UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure >= threshold) {
				ARTURIResource entity1 = cell.getEntity1();
				ARTURIResource entity2 = cell.getEntity2();
				String relation = cell.getRelation();
				alignModel.acceptAlignment(entity1, entity2, relation, getOWLModel());
				Cell updatedCell = alignModel.getCell(entity1, entity2);
				fillCellXMLResponse(updatedCell, dataElem);
			}
		}
		return response;
	}
	
	/**
	 * Rejects the alignment
	 *  
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 * @throws ModelUpdateException
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 */
	@GenerateSTServiceController
	public Response rejectAlignment(ARTURIResource entity1, ARTURIResource entity2, String relation) 
			throws ModelAccessException, UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAlignment(entity1, entity2);
		Cell c = alignModel.getCell(entity1, entity2);
		Element dataElem = response.getDataElement();
		fillCellXMLResponse(c, dataElem);
		return response;
	}
	
	/**
	 * Rejects all the alignments
	 * 
	 * @return
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response rejectAllAlignment() throws ModelAccessException, UnsupportedQueryLanguageException, 
			MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAllAlignment();
		
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			fillCellXMLResponse(c, dataElem);
		}
		return response;
	}
	
	/**
	 * Rejects all the alignments under the given threshold
	 * 
	 * @param threshold
	 * @return
	 * @throws ModelAccessException
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response rejectAllUnder(float threshold) throws ModelAccessException, 
			UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure < threshold) {
				ARTURIResource entity1 = cell.getEntity1();
				ARTURIResource entity2 = cell.getEntity2();
				alignModel.rejectAlignment(entity1, entity2);
				Cell updatedCell = alignModel.getCell(entity1, entity2);
				fillCellXMLResponse(updatedCell, dataElem);
			}
		}
		return response;
	}
	
	/**
	 * Change the relation of an alignment
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	public Response changeRelation(ARTURIResource entity1, ARTURIResource entity2, String relation)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.setRelation(entity1, entity2, relation, 1.0f);
		Cell updatedCell = alignModel.getCell(entity1, entity2);
		fillCellXMLResponse(updatedCell, dataElem);
		return response;
	}
	
	/**
	 * Change the mapping property of an alignment
	 * @param entity1
	 * @param entity2
	 * @param mappingProperty
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	@GenerateSTServiceController
	public Response changeMappingProperty(ARTURIResource entity1, ARTURIResource entity2, ARTURIResource mappingProperty)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException, QueryEvaluationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.changeMappingProperty(entity1, entity2, mappingProperty);
		Cell updatedCell = alignModel.getCell(entity1, entity2);
		fillCellXMLResponse(updatedCell, dataElem);
		return response;
	}
	
	/**
	 * Adds the accepted alignment cell to the ontology model and delete the rejected ones (if 
	 * previously added to the ontology)
	 * @param deleteRejected tells if remove the triples related to rejected alignments
	 * @return
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response applyValidation(@Optional (defaultValue = "false") boolean deleteRejected) throws UnsupportedQueryLanguageException, ModelAccessException, 
			MalformedQueryException, QueryEvaluationException, ModelUpdateException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element collElem = XMLHelp.newElement(dataElem, "collection");
		
		OWLModel model = getOWLModel();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		
		List<Cell> acceptedCells = alignModel.listCellsByStatus(Status.accepted);
		for (Cell cell : acceptedCells) {
			model.addTriple(cell.getEntity1(), cell.getMappingProperty(), cell.getEntity2(), getWorkingGraph());
			Element cellElem = XMLHelp.newElement(collElem, "cell");
			cellElem.setAttribute("entity1", cell.getEntity1().getURI());
			cellElem.setAttribute("entity2", cell.getEntity2().getURI());
			cellElem.setAttribute("property", cell.getMappingProperty().getURI());
			cellElem.setAttribute("action", "Added");
		}
		
		if (deleteRejected) {
			List<Cell> rejectedCells = alignModel.listCellsByStatus(Status.rejected);
			for (Cell cell : rejectedCells) {
				try {
					ARTURIResource entity1 = cell.getEntity1();
					ARTURIResource entity2 = cell.getEntity2();
					List<ARTURIResource> props = alignModel.suggestPropertiesForRelation(entity1, cell.getRelation(), model);
					for (ARTURIResource p : props) {
						if (model.hasTriple(entity1, p, entity2, false, getWorkingGraph())){
							model.deleteTriple(entity1, p, entity2, getWorkingGraph());
							Element cellElem = XMLHelp.newElement(collElem, "cell");
							cellElem.setAttribute("entity1", cell.getEntity1().getURI());
							cellElem.setAttribute("entity2", cell.getEntity2().getURI());
							cellElem.setAttribute("property", p.getURI());
							cellElem.setAttribute("action", "Deleted");
						}
					}
				} catch (InvalidAlignmentRelationException e) {} //in case of invalid relation, simply do nothing
			}
		}
		return response;
	}
	
	/**
	 * Save the alignment with the performed changes and export as rdf file
	 * @param oRes
	 * @throws IOException
	 * @throws ModelAccessException
	 * @throws UnsupportedRDFFormatException
	 * @throws ModelUpdateException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Alignment/exportAlignment", 
			method = org.springframework.web.bind.annotation.RequestMethod.GET)
	public void exportAlignment(HttpServletResponse oRes) throws IOException, ModelAccessException, UnsupportedRDFFormatException, ModelUpdateException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		File tempServerFile = File.createTempFile("alignment", ".rdf");
		alignmentModel.serialize(tempServerFile);
		FileInputStream is = new FileInputStream(tempServerFile);
		IOUtils.copy(is, oRes.getOutputStream());
		oRes.setHeader("Access-Control-Allow-Origin", "*");
		oRes.setHeader("Content-Disposition", "attachment; filename=alignment.rdf");
		oRes.setContentType(RDFFormat.RDFXML_ABBREV.getMIMEType());
		oRes.setContentLength((int) tempServerFile.length());
		oRes.flushBuffer();
		is.close();
	}
	
	/**
	 * Return a list of mapping properties suggested for the given entity and the alignment relation
	 * @param entity
	 * @param relation
	 * @return
	 * @throws ModelAccessException
	 * @throws InvalidAlignmentRelationException
	 */
	@GenerateSTServiceController
	public Response listSuggestedProperties(ARTURIResource entity, String relation) 
			throws ModelAccessException, InvalidAlignmentRelationException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		OWLModel ontoModel = getOWLModel();
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		List<ARTURIResource> props = alignmentModel.suggestPropertiesForRelation(entity, relation, ontoModel);
		
		Collection<STRDFURI> propColl = STRDFNodeFactory.createEmptyURICollection();
		for (ARTURIResource p : props) {
			propColl.add(STRDFNodeFactory.createSTRDFURI(p,	ModelUtilities.getPropertyRole(p, ontoModel), true,
					ontoModel.getQName(p.getURI())));
		}
		RDFXMLHelp.addRDFNodes(response, propColl);
		
		return response;
	}
	
	private void fillCellXMLResponse(Cell c, Element parentElement) throws ModelAccessException {
		Element cellElem = XMLHelp.newElement(parentElement, "cell");
		XMLHelp.newElement(cellElem, "entity1", c.getEntity1().getNominalValue());
		XMLHelp.newElement(cellElem, "entity2", c.getEntity2().getNominalValue());
		XMLHelp.newElement(cellElem, "measure", c.getMeasure()+"");
		XMLHelp.newElement(cellElem, "relation", c.getRelation());
		if (c.getMappingProperty() != null) {
			Element mpElem = XMLHelp.newElement(cellElem, "mappingProperty");
			mpElem.setTextContent(c.getMappingProperty().getURI());
			mpElem.setAttribute("show", getOWLModel().getQName(c.getMappingProperty().getURI()));
		}
		if (c.getStatus() != null) {
			XMLHelp.newElement(cellElem, "status", c.getStatus().name());
		}
		if (c.getComment() != null) {
			XMLHelp.newElement(cellElem, "comment", c.getComment());
		}
	}
	
	/**
	 * Remove the saved alignment from the session
	 * @return
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response closeSession() throws ModelUpdateException {
		String token = stServiceContext.getSessionToken();
		AlignmentModel align = modelsMap.get(token);
		align.close();
		modelsMap.remove(token);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		return response;
	}
}
