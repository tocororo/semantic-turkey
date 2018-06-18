package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.alignment.AlignmentInitializationException;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel.Status;
import it.uniroma2.art.semanticturkey.alignment.Cell;
import it.uniroma2.art.semanticturkey.alignment.InvalidAlignmentRelationException;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.resources.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.search.AdvancedSearch;
import it.uniroma2.art.semanticturkey.search.AdvancedSearch.InWhatToSearch;
import it.uniroma2.art.semanticturkey.search.AdvancedSearch.WhatToShow;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.SearchScope;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

@STService
public class Alignment extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(Alignment.class);
	
	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;
	
	@Autowired
	private MediationFramework mediationFramework;
	
	@Autowired
	private STServiceContext stServiceContext;
	
	private static List<IRI> skosMappingRelations = Arrays.asList(
			SKOS.MAPPING_RELATION, SKOS.EXACT_MATCH, SKOS.BROAD_MATCH,
			SKOS.NARROW_MATCH, SKOS.CLOSE_MATCH, SKOS.RELATED_MATCH
	);
	
	private static List<IRI> owlMappingRelations = Arrays.asList(
		OWL.SAMEAS, OWL.DIFFERENTFROM, OWL.EQUIVALENTCLASS, OWL.DISJOINTWITH, RDFS.SUBCLASSOF
	);
	
	private static List<IRI> propertiesMappingRelations = Arrays.asList(
		OWL.EQUIVALENTPROPERTY, OWL2Fragment.PROPERTY_DISJOINT_WITH, RDFS.SUBPROPERTYOF
	);
	
	//map that contain <id, context> pairs to handle multiple sessions
	private Map<String, AlignmentModel> modelsMap = new HashMap<>();
	
	
	//@formatter:off
	//SERVICES FOR ALIGMENT FOR THE SEARCH
	/**
	 * Perform a search (a SPARQL query) on another resoure identifies by an IRI 
	 * @param searchTerm
	 * @param datasetIRI
	 * @param rolesArray
	 * @param searchModeList
	 * @param searchScope
	 * @param langs
	 * @param useIndexes
	 * @return
	 * @throws MetadataRegistryStateException 
	 * @throws NoSuchDatasetMetadataException 
	 */
	/*@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResources(String searchTerm, IRI datasetIRI, 
			String[] rolesArray, List<SearchMode> searchModeList, SearchScope searchScope, List<String> langs) {
		
		//get the datasetMetadata associated to the desired dataset
		DatasetMetadata datasetMetadata = metadataRegistryBackend.getDatasetMetadata(datasetIRI);
		if(datasetMetadata==null) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no datasetMetadata "
					+ "associated");
		}
		
		//consult metadataRegistryBackend to get the LexicalModel to see in what to search (to set inWhatToSearch)
		AdvancedSearch advancedSearch = new AdvancedSearch();
		InWhatToSearch inWhatToSearch = advancedSearch.new InWhatToSearch();
		//TODO
		
		//get the connection from metadataRegistryBackend
		java.util.Optional<IRI> sparqlEndPoint = datasetMetadata.getSparqlEndpoint();
		if(!sparqlEndPoint.isPresent()) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no SPARQL endpoint "
					+ "associated");
		}
		
		SPARQLRepository sparqlRepository = new SPARQLRepository(sparqlEndPoint.get().stringValue());
		sparqlRepository.initialize();
		
		RepositoryConnection conn = sparqlRepository.getConnection();
		
		//what to show is dependend on the Lexicalization Model
		WhatToShow whatToShow = advancedSearch.new WhatToShow();
		//TODO
		
		
		return advancedSearch.searchResources(searchTerm, rolesArray, searchModeList, searchScope, langs, conn, 
				inWhatToSearch, whatToShow);
	}
	*/
	//@formatter:on
	
	
	//new service 
	
	// SERVICES FOR ALIGNMENT IN RESOURCE VIEW
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResources(IRI intputRes, IRI datasetIRI, 
			String[] rolesArray, @Optional List<String> langs, 
			@Optional List<SearchMode> searchModeList ) throws NoSuchDatasetMetadataException, MetadataRegistryStateException {
		//get the datasetMetadata associated to the desired dataset
		DatasetMetadata datasetMetadata = metadataRegistryBackend.getDatasetMetadata(datasetIRI);
		if(datasetMetadata==null) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no datasetMetadata "
					+ "associated");
		}
		
		//consult metadataRegistryBackend to get the LexicalModel to see in what to search (to set inWhatToSearch)
		AdvancedSearch advancedSearch = new AdvancedSearch();
		
		//if no languages are passed by the client, then ask MAPLE which languages should be used
		if(langs == null || langs.size()==0) {
			//TODO
			//this service does not exists at the moment, so, once it does, call it in the right way
			//langs = mediationFramework.getLanguages();
		}
		
		//if not searchModeList is passed, then assume they are contains and fuzzy (since those two contains 
		// all the others)
		if(searchModeList == null || searchModeList.size()==0) {
			searchModeList = new ArrayList<>();
			searchModeList.add(SearchMode.contains);
			searchModeList.add(SearchMode.fuzzy);
		}
		
		//now get all the lexicalization in the desired languages, using the current Lexical Model
		IRI currLexModel =  getProject().getLexicalizationModel();
		List<Literal> labelsList = advancedSearch.getLabelsFromLangs(stServiceContext, intputRes, 
				currLexModel, langs, getManagedConnection());
		
		//get the SPARQL endpoint from metadataRegistryBackend
		java.util.Optional<IRI> sparqlEndPoint = datasetMetadata.getSparqlEndpoint();
		if(!sparqlEndPoint.isPresent()) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no SPARQL endpoint "
					+ "associated");
		}
		
		
		//get the lexical model for the target datasetIRI
		//TODO
		//IRI targetLexMod = mediationFramework();
		IRI targetLexMod = Project.SKOSXL_LEXICALIZATION_MODEL;
		
		InWhatToSearch inWhatToSearch = advancedSearch.new InWhatToSearch();
		WhatToShow whatToShow = advancedSearch.new WhatToShow();
		//according to the Lexicalization model of the target dataset, set the corresponding values in 
		// inWhatToSearch and whatToShow
		if(targetLexMod.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
			inWhatToSearch.setSearchInRDFLabel(true);
			whatToShow.setShowRDFLabel(true);
		} else if(targetLexMod.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			inWhatToSearch.setSearchInSkosLabel(true);
			whatToShow.setShowSKOSLabel(true);
		} else if(targetLexMod.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			inWhatToSearch.setSearchInSkosxlLabel(true);
			whatToShow.setShowSKOSXLLabel(true);
		} else if(targetLexMod.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			inWhatToSearch.setSearchInDCTitle(true);
			inWhatToSearch.setSearchInWrittenRep(true);
			whatToShow.setShowDCTitle(true);
			whatToShow.setShowWrittenRep(true);
		}
		
		logger.debug("SPARQL endpoint = "+sparqlEndPoint.get().stringValue());
		SPARQLRepository sparqlRepository = new SPARQLRepository(sparqlEndPoint.get().stringValue());
		sparqlRepository.initialize();
		
		RepositoryConnection conn = sparqlRepository.getConnection();
		
		//for every label from the current project, get all matching resources from the target dataset
		int maxCount = 0;
		Map<String, Integer> resToCountMap = new HashMap<>();
		Map<String, AnnotatedValue<Resource>> resToAnnValueMap = new HashMap<>();
		
 		for(Literal label : labelsList) {
			List<AnnotatedValue<Resource>> currentAnnValuList = advancedSearch.searchResources(label, 
					rolesArray, searchModeList, conn, targetLexMod, inWhatToSearch, whatToShow);
			//analyze the return list and find a way to rank the results
			// maybe order them according to how may times a resource is returned
			for(AnnotatedValue<Resource> annValue : currentAnnValuList) {
				String stringValue = annValue.getStringValue();
				if(!resToCountMap.containsKey(stringValue)) {
					resToCountMap.put(stringValue, 1);
					//since it is the first time this AnnotatedValue is found, add it to 
					resToAnnValueMap.put(stringValue, annValue);
					if(maxCount==0) {
						maxCount = 1;
					}
				} else {
					resToCountMap.put(stringValue, resToCountMap.get(stringValue)+1);
					if(maxCount<resToCountMap.get(stringValue)) {
						maxCount = resToCountMap.get(stringValue);
					}
				}
			}
		}
 		
 		//all labels have been analyze, so not order the Annotated Value according to how manytimes they were
 		// returned, so start by picing all Annotated Value returnd maxValue and go down to 1
 		List<AnnotatedValue<Resource>>annValueList = new ArrayList<>();
 		for(int i=maxCount; i>0; --i) {
 			for(String stringValue : resToCountMap.keySet()) {
 				if(resToCountMap.get(stringValue) == i) {
 					annValueList.add(resToAnnValueMap.get(stringValue));
 				}
 			}
 		}
 		
 		return annValueList;
 		
		
	}
	
	
	
	/**
	 * Adds the given alignment triple only if predicate is a valid alignment property
	 * @param sourceResource
	 * @param predicate
	 * @param targetResource
	 * @return
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#sourceResource)+ ', alignment)', 'C')")
	public void addAlignment(Resource sourceResource, IRI predicate, IRI targetResource) {
		RepositoryConnection repoConn = getManagedConnection();
		repoConn.add(sourceResource, predicate, targetResource, getWorkingGraph());
	}
	
	/**
	 * Returns the available alignment properties depending on the type resource to align (property,
	 * or concept, or class,...).
	 * 
	 * @param resource resource to align
	 * @param allMappingProps if false returns just the mapping properties available for the current
	 * model type; if true returns all the mapping properties independently from the model type
	 * @return
	 * @throws ProjectInconsistentException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<IRI>> getMappingProperties(IRI resource, @Optional (defaultValue = "false") boolean allMappingProps) 
			throws ProjectInconsistentException {
		List<IRI> props = getAvailableMappingProperties(resource, allMappingProps, getManagedConnection());
		Collection<AnnotatedValue<IRI>> propColl = new ArrayList<>();
		for (IRI p : props) {
			AnnotatedValue<IRI> annValue = new AnnotatedValue<>(p);
			annValue.setAttribute("show", getPropertyQName(p));
			propColl.add(annValue);
		}
		return propColl;
	}
	
	// SERVICES FOR ALIGNMENT VALIDATION
	
	/**
	 * Loads an alignment file (that is compliant with AlignmentAPI format) and if one of the 
	 * two aligned ontologies has the same baseURI of the current model, then return a response
	 * with its content.
	 * @param inputFile
	 * @return
	 * @throws AlignmentInitializationException 
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public JsonNode loadAlignment(MultipartFile inputFile) throws AlignmentInitializationException, IOException {
		
		//create a temp file (in karaf data/temp folder) to copy the received file 
		File inputServerFile = File.createTempFile("alignment", inputFile.getOriginalFilename());
		inputFile.transferTo(inputServerFile);
		
		//creating model for loading alignment
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.initialize();
		AlignmentModel alignModel = new AlignmentModel();
		alignModel.add(inputServerFile);
		
		String token = stServiceContext.getSessionToken();
		modelsMap.put(token, alignModel);
		
		//check that one of the two aligned ontologies matches the current project ontology
		String projectBaseURI = getProject().getNewOntologyManager().getBaseURI();
		
		String onto1BaseURI = alignModel.getOnto1();
		String onto2BaseURI = alignModel.getOnto2();
		
		//removes final / or # from baseURIs in order to avoid failing of the following check
		//(one of the aligned ontologies must refers to the current open project)
		if (projectBaseURI.endsWith("/") || projectBaseURI.endsWith("#")) {
			projectBaseURI = projectBaseURI.substring(0, projectBaseURI.length()-1);
		}
		if (onto1BaseURI.endsWith("/") || onto1BaseURI.endsWith("#")) {
			onto1BaseURI = onto1BaseURI.substring(0, onto1BaseURI.length()-1);
		}
		if (onto2BaseURI.endsWith("/") || onto2BaseURI.endsWith("#")) {
			onto2BaseURI = onto2BaseURI.substring(0, onto2BaseURI.length()-1);
		}
		
		if (!projectBaseURI.equals(onto1BaseURI)){
			if (projectBaseURI.equals(onto2BaseURI)){
				if (alignModel.hasCustomRelation()) {
					throw new AlignmentInitializationException("The alignment file is reversed "
							+ "(the source ontology in the alignment file is your target ontology in your project) "
							+ "and it contains custom relations. It is possible to work with inverted alignment files "
							+ "only when they do not contain custom relations. Please invert the order of ontologies "
							+ "in the alignment file and adjust the custom relations by replacing them with their "
							+ "inverse in the custom alignment model that is being adopted.");
				}
				alignModel.reverse();
			} else {
				throw new AlignmentInitializationException("Failed to open and validate the given alignment file. "
						+ "None of the two aligned ontologies matches the current project ontology");
			}
		}
		
		alignModel.preProcess();
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode alignmentNode = jsonFactory.objectNode();
		alignmentNode.set("onto1", jsonFactory.textNode(alignModel.getOnto1()));
		alignmentNode.set("onto2", jsonFactory.textNode(alignModel.getOnto2()));
		
		ArrayNode unknownRelationsArrayNode = jsonFactory.arrayNode();
		List<String> unknownRel = alignModel.getUnknownRelations();
		for (String rel: unknownRel) {
			unknownRelationsArrayNode.add(jsonFactory.textNode(rel));
		}
		alignmentNode.set("unknownRelations", unknownRelationsArrayNode);
		return alignmentNode;
	}
	
	/**
	 * Returns the cells of the alignment file. Handles the scalability returning a portion of cells
	 * if <code>pageIdx</code> and <code>range</code> are provided as parameters 
	 * @param pageIdx index of the page in case 
	 * @param range alignment per page to show. If 0, returns all the alignments.
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public JsonNode listCells(@Optional (defaultValue = "0") int pageIdx, @Optional (defaultValue = "0") int range) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode mapNode = jsonFactory.objectNode();
		ArrayNode cellArrayNode = jsonFactory.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cells = alignModel.listCells();
		//if range = 0 => return all cells
		if (range == 0) {
			mapNode.set("page", jsonFactory.numberNode(1));
			mapNode.set("totPage", jsonFactory.numberNode(1));
			for (Cell c : cells) {
				cellArrayNode.add(createCellJsonNode(c));
			}
		} else {
			int size = cells.size();
			int begin = pageIdx * range;
			int end = begin + range;
			
			//if index of first cell > size of cell list (index out of bound) => return empty list of cells
			if (begin > size) {
				mapNode.set("page", jsonFactory.numberNode(1));
				mapNode.set("totPage", jsonFactory.numberNode(1));
			} else {
				int maxPage = size / range;
				if (size % range > 0) {
					maxPage++;
				}
				mapNode.set("page", jsonFactory.numberNode(pageIdx));
				mapNode.set("totPage", jsonFactory.numberNode(maxPage));
				if (end > size) {
					end = size;
				}
				for (int i=begin; i<end; i++){
					cellArrayNode.add(createCellJsonNode(cells.get(i)));
				}
			}
		}
		mapNode.set("cells", cellArrayNode);
		return mapNode;
	}
	
	/**
	 * Accepts the alignment updating the alignment model
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#entity1)+ ', alignment)', 'C')")
	public JsonNode acceptAlignment(IRI entity1, IRI entity2, String relation, @Optional IRI forcedProperty,
			@Optional(defaultValue = "false") boolean setAsDefault) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAlignment(entity1, entity2, relation, forcedProperty, setAsDefault, getManagedConnection());
		Cell c = alignModel.getCell(entity1, entity2);
		return createCellJsonNode(c);
	}
	
	/**
	 * Accepts all the alignment updating the alignment model
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'C')")
	public JsonNode acceptAllAlignment() {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAllAlignment(getManagedConnection());
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cellsArrayNode = jsonFactory.arrayNode();
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			cellsArrayNode.add(createCellJsonNode(c));
		}
		return cellsArrayNode;
	}
	
	/**
	 * Accepts all the alignment with measure above the given threshold updating the alignment model.
	 * The response contains the description of all the cells affected by the accept
	 * 
	 * @param threshold
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'C')")
	public JsonNode acceptAllAbove(float threshold) {
		ArrayNode cellsArrayNode = JsonNodeFactory.instance.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure >= threshold) {
				IRI entity1 = cell.getEntity1();
				IRI entity2 = cell.getEntity2();
				String relation = cell.getRelation();
				alignModel.acceptAlignment(entity1, entity2, relation, null, false, getManagedConnection());
				Cell updatedCell = alignModel.getCell(entity1, entity2);
				cellsArrayNode.add(createCellJsonNode(updatedCell));
			}
		}
		return cellsArrayNode;
	}
	
	/**
	 * Rejects the alignment
	 *  
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAlignment(IRI entity1, IRI entity2, String relation) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAlignment(entity1, entity2);
		Cell c = alignModel.getCell(entity1, entity2);
		return createCellJsonNode(c);
	}
	
	/**
	 * Rejects all the alignments
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAllAlignment() {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAllAlignment();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cellsArrayNode = jsonFactory.arrayNode();
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			cellsArrayNode.add(createCellJsonNode(c));
		}
		return cellsArrayNode;
	}
	
	/**
	 * Rejects all the alignments under the given threshold
	 * 
	 * @param threshold
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAllUnder(float threshold) {
		ArrayNode cellsArrayNode = JsonNodeFactory.instance.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure < threshold) {
				IRI entity1 = cell.getEntity1();
				IRI entity2 = cell.getEntity2();
				alignModel.rejectAlignment(entity1, entity2);
				Cell updatedCell = alignModel.getCell(entity1, entity2);
				cellsArrayNode.add(createCellJsonNode(updatedCell));
			}
		}
		return cellsArrayNode;
	}
	
	/**
	 * Change the relation of an alignment
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	@STServiceOperation
	@Read
	public JsonNode changeRelation(IRI entity1, IRI entity2, String relation) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.setRelation(entity1, entity2, relation, 1.0f);
		Cell updatedCell = alignModel.getCell(entity1, entity2);
		return createCellJsonNode(updatedCell);
	}
	
	/**
	 * Change the mapping property of an alignment
	 * @param entity1
	 * @param entity2
	 * @param mappingProperty
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'U')")
	public JsonNode changeMappingProperty(IRI entity1, IRI entity2, IRI mappingProperty) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.changeMappingProperty(entity1, entity2, mappingProperty);
		Cell updatedCell = alignModel.getCell(entity1, entity2);
		return createCellJsonNode(updatedCell);
	}
	
	/**
	 * Adds the accepted alignment cell to the ontology model and delete the rejected ones (if 
	 * previously added to the ontology)
	 * @param deleteRejected tells if remove the triples related to rejected alignments
	 * @return
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'CUD')")
	public JsonNode applyValidation(@Optional (defaultValue = "false") boolean deleteRejected) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode reportArrayNode = jsonFactory.arrayNode();
		
		RepositoryConnection repoConn = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		
		List<Cell> acceptedCells = alignModel.listCellsByStatus(Status.accepted);
		for (Cell cell : acceptedCells) {
			modelAdditions.add(cell.getEntity1(), cell.getMappingProperty(), cell.getEntity2());
			ObjectNode cellNode = jsonFactory.objectNode();
			cellNode.set("entity1", jsonFactory.textNode(cell.getEntity1().stringValue()));
			cellNode.set("entity2", jsonFactory.textNode(cell.getEntity2().stringValue()));
			cellNode.set("property", jsonFactory.textNode(cell.getMappingProperty().stringValue()));
			cellNode.set("action", jsonFactory.textNode("Added"));
			reportArrayNode.add(cellNode);
		}
		
		if (deleteRejected) {
			List<Cell> rejectedCells = alignModel.listCellsByStatus(Status.rejected);
			for (Cell cell : rejectedCells) {
				try {
					IRI entity1 = cell.getEntity1();
					IRI entity2 = cell.getEntity2();
					List<IRI> props = alignModel.suggestPropertiesForRelation(entity1, cell.getRelation(), false, repoConn);
					for (IRI p : props) {
						if (repoConn.hasStatement(entity1, p, entity2, true, getWorkingGraph())){
							modelRemovals.add(entity1, p, entity2);
							ObjectNode cellNode = jsonFactory.objectNode();
							cellNode.set("entity1", jsonFactory.textNode(cell.getEntity1().stringValue()));
							cellNode.set("entity2", jsonFactory.textNode(cell.getEntity2().stringValue()));
							cellNode.set("property", jsonFactory.textNode(p.stringValue()));
							cellNode.set("action", jsonFactory.textNode("Deleted"));
							reportArrayNode.add(cellNode);
						}
					}
				} catch (InvalidAlignmentRelationException e) {} //in case of invalid relation, simply do nothing
			}
		}
		
		repoConn.add(modelAdditions, getWorkingGraph());
		repoConn.remove(modelRemovals, getWorkingGraph());
		
		return reportArrayNode;
	}
	
	/**
	 * Save the alignment with the performed changes and export as rdf file
	 * @param oRes
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public void exportAlignment(HttpServletResponse oRes) throws IOException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		File tempServerFile = File.createTempFile("alignment", ".rdf");
		try {
			alignmentModel.serialize(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=alignment.rdf");
			oRes.setContentType(RDFFormat.RDFXML.getDefaultMIMEType());
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Return a list of mapping properties suggested for the given entity and the alignment relation
	 * @param entity
	 * @param relation
	 * @return
	 * @throws InvalidAlignmentRelationException
	 * @throws ProjectInconsistentException 
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<IRI>> getSuggestedProperties(IRI entity, String relation)
			throws InvalidAlignmentRelationException, ProjectInconsistentException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		List<IRI> props;
		try {
			props = alignmentModel.suggestPropertiesForRelation(entity, relation, false, getManagedConnection());
		} catch (InvalidAlignmentRelationException e) { 
			/* If it is not possible to find properties to suggest, (probably because the relation is not known)
			 * returns all the mapping properties. */
			props = getAvailableMappingProperties(entity, false, getManagedConnection());
		}
			
		Collection<AnnotatedValue<IRI>> propColl = new ArrayList<>();
		for (IRI p : props) {
			AnnotatedValue<IRI> annValue = new AnnotatedValue<>(p);
			annValue.setAttribute("show", getPropertyQName(p));
			propColl.add(annValue);
		}
		return propColl;
	}
	
	/**
	 * Returns a list of mapping properties
	 * @param resource 
	 * @param allMappingProps if true, returns all the known mapping properties, if false filters out the properties
	 * 	not compatible with the resource and the project type
	 * @param repoConn
	 * @return
	 * @throws ProjectInconsistentException
	 */
	private List<IRI> getAvailableMappingProperties(IRI resource, boolean allMappingProps, RepositoryConnection repoConn)
			throws ProjectInconsistentException {
		List<IRI> mappingProps = new ArrayList<>();
		if (allMappingProps) {
			for (IRI prop : propertiesMappingRelations) {
				mappingProps.add(prop);
			}
			for (IRI prop : skosMappingRelations) {
				mappingProps.add(prop);
			}
			for (IRI prop : owlMappingRelations) {
				mappingProps.add(prop);
			}
		} else {
			boolean isProperty = RDFResourceRole.isProperty(RoleRecognitionOrchestrator.computeRole(resource, repoConn)); 
			if (isProperty) { //is Property?
				for (IRI prop : propertiesMappingRelations) {
					mappingProps.add(prop);
				}
			} else {
				if (getProject().getModel().equals(Project.SKOS_MODEL)) { //SKOS or SKOSXL
					for (IRI prop : skosMappingRelations) {
						mappingProps.add(prop);
					}
				} else { //OWL
					for (IRI prop : owlMappingRelations) {
						mappingProps.add(prop);
					}
				}
			}
		}
		return mappingProps;
	}
	
	/**
	 * Returns the qname of a property if a known namespace is found in its URI, the URI of the same property otherwise.
	 * @param property
	 * @return
	 */
	private String getPropertyQName(IRI property) {
		RepositoryResult<Namespace> namespaces = getManagedConnection().getNamespaces();
		while (namespaces.hasNext()) {
			Namespace ns = namespaces.next();
			if (ns.getName().equals(property.getNamespace())) {
				return ns.getPrefix() + ":" + property.getLocalName();
			}
		}
		return property.stringValue();
	}
	
	private JsonNode createCellJsonNode(Cell c) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode cellNode = jsonFactory.objectNode();
		cellNode.set("entity1", jsonFactory.textNode(c.getEntity1().stringValue()));
		cellNode.set("entity2", jsonFactory.textNode(c.getEntity2().stringValue()));
		cellNode.set("measure", jsonFactory.numberNode(c.getMeasure()));
		cellNode.set("relation", jsonFactory.textNode(c.getRelation()));
		if (c.getMappingProperty() != null) {
			ObjectNode mapPropNode = jsonFactory.objectNode();
			mapPropNode.set("@id", jsonFactory.textNode(c.getMappingProperty().stringValue()));
			mapPropNode.set("show", jsonFactory.textNode(getPropertyQName(c.getMappingProperty())));
			cellNode.set("mappingProperty", mapPropNode);
		}
		if (c.getStatus() != null) {
			cellNode.set("status", jsonFactory.textNode(c.getStatus().name()));
		}
		if (c.getComment() != null) {
			cellNode.set("comment", jsonFactory.textNode(c.getComment()));
		}
		return cellNode;
	}
	
	/**
	 * Remove the saved alignment from the session
	 * @return
	 */
	@STServiceOperation
	@Read
	public void closeSession() {
		if (stServiceContext.hasContextParameter("token")) {
			String token = stServiceContext.getSessionToken();
			AlignmentModel align = modelsMap.get(token);
			align.close();
			modelsMap.remove(token);
		}
	}
}
