package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.alignment.AlignmentModel;
import it.uniroma2.art.owlart.alignment.AlignmentModelFactory;
import it.uniroma2.art.owlart.alignment.Cell;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
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
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.http.STServiceHTTPContext;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@GenerateSTServiceController
@Validated
@Component
@Controller //needed for saveAlignment method
public class Alignment extends STServiceAdapter {
	
	@Autowired
	private STServiceHTTPContext stServiceContext;
	
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
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response loadAlignment(MultipartFile inputFile) throws IOException, ModelAccessException,
			ModelUpdateException, UnsupportedRDFFormatException, UnavailableResourceException,
			ProjectInconsistentException, ParserConfigurationException, SAXException {
		
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
		
		//check that one of the two aligned ontologies matches the current project ontology
		if (!getOWLModel().getBaseURI().equals(alignModel.getOnto1())){
			if (getOWLModel().getBaseURI().equals(alignModel.getOnto2())){
				alignModel.reverse();
			} else {
				return createReplyFAIL("Failed to open and validate the given alignment file. "
						+ "None of the two aligned ontologies matches the current project ontology");
			}
		}
		
		Collection<Cell> cellList = alignModel.listCells();
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element alignmentElem = XMLHelp.newElement(dataElem, "Alignment");
		
		Element onto1Elem = XMLHelp.newElement(alignmentElem, "onto1");
		Element onto2Elem = XMLHelp.newElement(alignmentElem, "onto2");
		XMLHelp.newElement(onto1Elem, "Ontology", alignModel.getOnto1());
		XMLHelp.newElement(onto2Elem, "Ontology", alignModel.getOnto2());
		
		for (Cell c : cellList) {
			Element mapElem = XMLHelp.newElement(alignmentElem, "map");
			Element cellElem = XMLHelp.newElement(mapElem, "Cell");
			XMLHelp.newElement(cellElem, "entity1", c.getEntity1().getNominalValue());
			XMLHelp.newElement(cellElem, "entity2", c.getEntity2().getNominalValue());
			XMLHelp.newElement(cellElem, "measure", c.getMeasure()+"");
			XMLHelp.newElement(cellElem, "relation", c.getRelation());
		}
		
		return response;
	}
	
	@GenerateSTServiceController
	public Response validateAlignment(ARTURIResource entity1, ARTURIResource entity2, String relation) throws ModelUpdateException, ModelAccessException {
		OWLModel model = getOWLModel();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		ARTURIResource predicate = alignModel.convertRelation(entity1, relation, model);
		System.out.println("Add triple:\nS: " + entity1 + "\nP: " + predicate.getNominalValue() + "\nO: " + entity2);
//		model.addTriple(entity1, predicate, entity2, getWorkingGraph()); //TODO remove
		alignModel.deleteCell(entity1, entity2);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	@GenerateSTServiceController
	public Response validateAllAlignment() throws ModelAccessException, ModelUpdateException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element collElem = XMLHelp.newElement(dataElem, "collection");
		collElem.setAttribute("type", "validate");
		
		OWLModel model = getOWLModel();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		Collection<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			ARTURIResource entity1 = cell.getEntity1();
			ARTURIResource entity2 = cell.getEntity2();
			String relation = cell.getRelation();
			ARTURIResource predicate = alignModel.convertRelation(entity1, relation, model);
			System.out.println("Add triple:\nS: " + entity1 + "\nP: " + predicate.getNominalValue() + "\nO: " + entity2);
//			model.addTriple(entity1, predicate, entity2, getWorkingGraph()); //TODO remove
			alignModel.deleteCell(entity1, entity2);
			
			//report the validations in the response
			Element alignElem = XMLHelp.newElement(collElem, "alignment");
		    XMLHelp.newElement(alignElem, "entity1", entity1.getURI());
		    XMLHelp.newElement(alignElem, "relation", predicate.getURI());
		    XMLHelp.newElement(alignElem, "entity2", entity2.getURI());
		}
		return response;
	}
	
	@GenerateSTServiceController
	public Response validateAllAbove(float treshold) throws ModelAccessException, ModelUpdateException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element collElem = XMLHelp.newElement(dataElem, "collection");
		collElem.setAttribute("type", "validate");
		
		OWLModel model = getOWLModel();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		Collection<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure >= treshold) {
				ARTURIResource entity1 = cell.getEntity1();
				ARTURIResource entity2 = cell.getEntity2();
				String relation = cell.getRelation();
				ARTURIResource predicate = alignModel.convertRelation(entity1, relation, model);
				System.out.println("Add triple:\nS: " + entity1 + "\nP: " + predicate.getNominalValue() + "\nO: " + entity2);
//				model.addTriple(entity1, predicate, entity2, getWorkingGraph()); //TODO remove
				alignModel.deleteCell(entity1, entity2);
				
				//report the validations in the response
				Element alignElem = XMLHelp.newElement(collElem, "alignment");
			    XMLHelp.newElement(alignElem, "entity1", entity1.getURI());
			    XMLHelp.newElement(alignElem, "relation", predicate.getURI());
			    XMLHelp.newElement(alignElem, "entity2", entity2.getURI());
			}
		}
		return response;
	}
	
	@GenerateSTServiceController
	public Response rejectAlignment(ARTURIResource entity1, ARTURIResource entity2, String relation) throws ModelUpdateException, ModelAccessException {
		System.out.println("Rejecting triple:\nS: " + entity1 + "\nP: " + relation + "\nO: " + entity2);
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.deleteCell(entity1, entity2);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	@GenerateSTServiceController
	public Response rejectAllAlignment() throws ModelAccessException, ModelUpdateException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element collElem = XMLHelp.newElement(dataElem, "collection");
		collElem.setAttribute("type", "reject");
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		Collection<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			ARTURIResource entity1 = cell.getEntity1();
			ARTURIResource entity2 = cell.getEntity2();
			System.out.println("Rejecting alignment between " + entity1 + " and " + entity2);
			alignModel.deleteCell(entity1, entity2);
			
			//report the rejected triples in the response
			Element alignElem = XMLHelp.newElement(collElem, "alignment");
		    XMLHelp.newElement(alignElem, "entity1", entity1.getURI());
		    XMLHelp.newElement(alignElem, "relation", cell.getRelation());
		    XMLHelp.newElement(alignElem, "entity2", entity2.getURI());
		}
		return response;
	}
	
	@GenerateSTServiceController
	public Response rejectAllUnder(float treshold) throws ModelAccessException, ModelUpdateException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element collElem = XMLHelp.newElement(dataElem, "collection");
		collElem.setAttribute("type", "reject");
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		Collection<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure <= treshold) {
				ARTURIResource entity1 = cell.getEntity1();
				ARTURIResource entity2 = cell.getEntity2();
				System.out.println("Rejecting alignment between " + entity1 + " and " + entity2);
				alignModel.deleteCell(entity1, entity2);
				
				//report the rejected triples in the response
				Element alignElem = XMLHelp.newElement(collElem, "alignment");
			    XMLHelp.newElement(alignElem, "entity1", entity1.getURI());
			    XMLHelp.newElement(alignElem, "relation", cell.getRelation());
			    XMLHelp.newElement(alignElem, "entity2", entity2.getURI());
			}
		}
		return response;
	}
	
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Alignment/saveAlignment", 
			method = org.springframework.web.bind.annotation.RequestMethod.GET)
	public void saveAlignment(HttpServletResponse oRes) throws IOException, ModelAccessException, UnsupportedRDFFormatException, ModelUpdateException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		File tempServerFile = File.createTempFile("alignment", ".rdf");
		alignmentModel.serialize(tempServerFile);
		FileInputStream is = new FileInputStream(tempServerFile);
		IOUtils.copy(is, oRes.getOutputStream());
		oRes.setContentType(RDFFormat.RDFXML_ABBREV.getMIMEType());
		oRes.setContentLength((int) tempServerFile.length());
		oRes.setHeader("Content-Disposition", "attachment; filename=alignment.rdf");
		oRes.flushBuffer();
		is.close();
	}
	
	
	@GenerateSTServiceController
	public Response closeSession() throws ModelUpdateException {
		String token = stServiceContext.getSessionToken();
		modelsMap.get(token).close();
		modelsMap.remove(token);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		return response;
	}
}
