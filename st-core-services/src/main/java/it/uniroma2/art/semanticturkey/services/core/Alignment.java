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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.alignment.AlignmentInitializationException;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel.Status;
import it.uniroma2.art.semanticturkey.alignment.Cell;
import it.uniroma2.art.semanticturkey.alignment.InvalidAlignmentRelationException;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

@STService
public class Alignment extends STServiceAdapter {
	
	@Autowired
	private STServiceContext stServiceContext;
	
	private static List<IRI> skosMappingRelations;
	static {
		skosMappingRelations = new ArrayList<>();
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.MAPPING_RELATION);
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.EXACT_MATCH);
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.BROAD_MATCH);
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.NARROW_MATCH);
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.CLOSE_MATCH);
		skosMappingRelations.add(org.eclipse.rdf4j.model.vocabulary.SKOS.RELATED_MATCH);
	};
	
	private static List<IRI> owlMappingRelations;
	static {
		owlMappingRelations = new ArrayList<>();
		owlMappingRelations.add(OWL.SAMEAS);
		owlMappingRelations.add(OWL.DIFFERENTFROM);
		owlMappingRelations.add(OWL.ALLDIFFERENT);
		owlMappingRelations.add(OWL.EQUIVALENTCLASS);
		owlMappingRelations.add(OWL.DISJOINTWITH);
		owlMappingRelations.add(RDFS.SUBCLASSOF);
	};
	
	private static List<IRI> propertiesMappingRelations;
	static {
		propertiesMappingRelations = new ArrayList<>();
		propertiesMappingRelations.add(OWL.EQUIVALENTPROPERTY);
		propertiesMappingRelations.add(RDFS.SUBPROPERTYOF);
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
	public Collection<AnnotatedValue<IRI>> getMappingRelations(IRI resource, @Optional (defaultValue = "false") boolean allMappingProps) 
			throws ProjectInconsistentException {
		Collection<AnnotatedValue<IRI>> mappingProps = new ArrayList<>();
		if (allMappingProps) {
			for (IRI prop : propertiesMappingRelations) {
				AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
				annValue.setAttribute("show", getPropertyQName(prop));
				mappingProps.add(annValue);
			}
			for (IRI prop : skosMappingRelations) {
				AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
				annValue.setAttribute("show", getPropertyQName(prop));
				mappingProps.add(annValue);
			}
			for (IRI prop : owlMappingRelations) {
				AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
				annValue.setAttribute("show", getPropertyQName(prop));
				mappingProps.add(annValue);
			}
		} else {
			boolean isProperty = RDFResourceRolesEnum.isProperty(RoleRecognitionOrchestrator.computeRole(resource, getManagedConnection())); 
			if (isProperty) { //is Property?
				for (IRI prop : propertiesMappingRelations) {
					AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
					annValue.setAttribute("show", getPropertyQName(prop));
					mappingProps.add(annValue);
				}
			} else {
				if (getProject().getModelType().getName().contains("SKOS")) { //SKOS or SKOSXL
					for (IRI prop : skosMappingRelations) {
						AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
						annValue.setAttribute("show", getPropertyQName(prop));
						mappingProps.add(annValue);
					}
				} else { //OWL
					for (IRI prop : owlMappingRelations) {
						AnnotatedValue<IRI> annValue = new AnnotatedValue<IRI>(prop);
						annValue.setAttribute("show", getPropertyQName(prop));
						mappingProps.add(annValue);
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
//		AlignmentRepositoryConnectionWrapper alignModel = new AlignmentRepositoryConnectionWrapper(repository);
//		alignModel.add(inputServerFile, it.uniroma2.art.semanticturkey.vocabulary.Alignment.URI, RDFFormat.RDFXML);
		AlignmentModel alignModel = new AlignmentModel();
		alignModel.add(inputServerFile);
		
		String token = stServiceContext.getSessionToken();
		modelsMap.put(token, alignModel);
		
		//check that one of the two aligned ontologies matches the current project ontology
		String baseURI = getProject().getNewOntologyManager().getBaseURI();
		
		if (!baseURI.equals(alignModel.getOnto1())){
			if (baseURI.equals(alignModel.getOnto2())){
				alignModel.reverse();
			} else {
				throw new AlignmentInitializationException("Failed to open and validate the given alignment file. "
						+ "None of the two aligned ontologies matches the current project ontology");
			}
		}
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode alignmentNode = jsonFactory.objectNode();
		alignmentNode.set("onto1", jsonFactory.textNode(alignModel.getOnto1()));
		alignmentNode.set("onto2", jsonFactory.textNode(alignModel.getOnto2()));
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
	public JsonNode acceptAlignment(IRI entity1, IRI entity2, String relation) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAlignment(entity1, entity2, relation, getManagedConnection());
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
				alignModel.acceptAlignment(entity1, entity2, relation, getManagedConnection());
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
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		
		List<Cell> acceptedCells = alignModel.listCellsByStatus(Status.accepted);
		for (Cell cell : acceptedCells) {
			repoConn.add(cell.getEntity1(), cell.getMappingProperty(), cell.getEntity2(), getWorkingGraph());
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
					List<IRI> props = alignModel.suggestPropertiesForRelation(entity1, cell.getRelation(), repoConn);
					for (IRI p : props) {
						if (repoConn.hasStatement(entity1, p, entity2, false, getWorkingGraph())){
							repoConn.remove(entity1, p, entity2, getWorkingGraph());
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
		return reportArrayNode;
	}
	
	/**
	 * Save the alignment with the performed changes and export as rdf file
	 * @param oRes
	 * @throws IOException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public void exportAlignment(HttpServletResponse oRes) throws IOException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		File tempServerFile = File.createTempFile("alignment", ".rdf");
		alignmentModel.serialize(tempServerFile);
		FileInputStream is = new FileInputStream(tempServerFile);
		IOUtils.copy(is, oRes.getOutputStream());
		oRes.setHeader("Content-Disposition", "attachment; filename=alignment.rdf");
		oRes.setContentType(RDFFormat.RDFXML.getDefaultMIMEType());
		oRes.setContentLength((int) tempServerFile.length());
		oRes.flushBuffer();
		is.close();
	}
	
	/**
	 * Return a list of mapping properties suggested for the given entity and the alignment relation
	 * @param entity
	 * @param relation
	 * @return
	 * @throws InvalidAlignmentRelationException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<IRI>> listSuggestedProperties(IRI entity, String relation) throws InvalidAlignmentRelationException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		List<IRI> props = alignmentModel.suggestPropertiesForRelation(entity, relation, getManagedConnection());
		
		Collection<AnnotatedValue<IRI>> propColl = new ArrayList<>();
		for (IRI p : props) {
			AnnotatedValue<IRI> annValue = new AnnotatedValue<>(p);
			annValue.setAttribute("show", getPropertyQName(p));
			propColl.add(annValue);
		}
		return propColl;
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
		String token = stServiceContext.getSessionToken();
		AlignmentModel align = modelsMap.get(token);
		align.close();
		modelsMap.remove(token);
	}
}
