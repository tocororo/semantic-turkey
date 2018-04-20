package it.uniroma2.art.semanticturkey.alignment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

public class AlignmentModel {
	
	protected static Logger logger = LoggerFactory.getLogger(AlignmentModel.class);
	
	public enum Status {
		accepted, rejected, error;
	}
	
	private static List<String> knownRelations = Arrays.asList("=", ">", "<", "%", "HasInstance", "InstanceOf");
	
	private static Map<String, IRI> relationPropertyMap = new HashMap<>();
	
	private RepositoryConnection repoConnection;
	
	public AlignmentModel() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.initialize();
		repoConnection = repository.getConnection();
	}
	
	public void add(File alignmentFile) throws AlignmentInitializationException {
		try {
			repoConnection.add(alignmentFile, Alignment.URI, RDFFormat.RDFXML);
		} catch (RDFParseException | RepositoryException | IOException e) {
			throw new AlignmentInitializationException(e);
		}
	}
	
	/**
	 * Processes all the cells and stores the relations in a map that will stores the default mapping
	 * properties for them
	 */
	public void preProcess() {
		String query = "SELECT DISTINCT ?relation WHERE { \n"
				+ " ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . \n"
				+ " ?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation . \n"
				+ "}";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			while (result.hasNext()) {
				String relation = result.next().getValue("relation").stringValue();
				relationPropertyMap.put(relation, null);
			}
		}
	}
	
	/**
	 * Returns a list of unknown relations (classnames or not known symbols) used in the alignment 
	 * @return
	 */
	public List<String> getUnknownRelations() {
		List<String> unknownRel = new ArrayList<>();
		Set<String> relations = relationPropertyMap.keySet();
		for (String rel : relations) {
			if (!knownRelations.contains(rel)) {
				unknownRel.add(rel);
			}
		}
		return unknownRel;
	}
	
	/**
	 * Gets the level of the alignment.
	 * Values: "0", "1", "2EDOAL"
	 * @return
	 */
	public String getLevel() {
		String query = "SELECT ?level WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " ?level . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("level").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the level of the alignment.
	 * Values: "0", "1", "2EDOAL"
	 * @param level
	 */
	public void setLevel(String level) {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " ?level } "
				+ "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " \"" + level + "\" } "
				+ "WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " ?level } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Gets the xml compatibility
	 * @return
	 */
	public boolean getXml() {
		String query = "SELECT ?xml WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " ?xml . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("xml").stringValue().equals("yes");
			}
		}
		return false;
	}

	/**
	 * Sets the xml compatibility
	 * @param xml
	 */
	public void setXml(boolean xml) {
		String xmlValue = xml ? "yes" : "no";
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " ?xml } "
				+ "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " \"" + xmlValue + "\" } "
				+ "WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " ?xml } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Gets the type (or arity) of the alignment.
	 * Values: "11", "1?", "1+", "1*", "?1", "??", "?+", "?*", "+1", "+?", 
	 * "++", "+*", "*1", "*?", "?+", "**"
	 * @return
	 */
	public String getType() {
		String query = "SELECT ?type WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE) + " ?type . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("type").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the type (or arity) of the alignment.
	 * Values: "11", "1?", "1+", "1*", "?1", "??", "?+", "?*", "+1", "+?", 
	 * "++", "+*", "*1", "*?", "?+", "**"
	 * @param type
	 */
	public void setType(String type) {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE) + " ?type } "
				+ "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE) + " \"" + type + "\" } "
				+ "WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE) + " ?type } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Gets the baseURI of the first aligned ontology
	 * @return
	 */
	public String getOnto1() {
		String query = "SELECT ?onto1 WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("onto1").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the baseURI of the first aligned ontology
	 * @param ontologyBaseURI
	 */
	public void setOnto1(String ontologyBaseURI) {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 . "
				+ "?onto1 ?p ?o . } "
				+ "INSERT {	?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " <" + ontologyBaseURI + "> . "
				+ "<" + ontologyBaseURI + "> a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " } "
				+ "WHERE {	?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .	"
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 } "
				+ "OPTIONAL { ?onto1 a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " "
				+ "OPTIONAL { ?onto1 ?p ?o } } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Gets the baseURI of the second aligned ontology
	 * @return
	 */
	public String getOnto2() {
		String query = "SELECT ?onto2 WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("onto2").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the baseURI of the second aligned ontology
	 * @param ontologyBaseURI
	 */
	public void setOnto2(String ontologyBaseURI) {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 . "
				+ "?onto2 ?p ?o . } "
				+ "INSERT {	?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " <" + ontologyBaseURI + "> . "
				+ "<" + ontologyBaseURI + "> a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " } "
				+ "WHERE {	?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .	"
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 } "
				+ "OPTIONAL { ?onto2 a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " "
				+ "OPTIONAL { ?onto2 ?p ?o } } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Returns if exists the aligned Cell with the given entities, null otherwise
	 * @param entity1
	 * @param entity2
	 * @return
	 */
	public Cell getCell(IRI entity1, IRI entity2) {
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE { "
				+ "BIND(URI('" + entity1.stringValue() + "') AS ?entity1)"
				+ "BIND(URI('" + entity2.stringValue() + "') AS ?entity2)"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . } } "
				+ "ORDERBY ?entity1 ?entity2";
		logger.debug(query);
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			List<Cell> cells = getCellFromTupleResult(result);
			if (!cells.isEmpty()) {
				return cells.get(0);
			}
		}
		return null;
	}
	
	/**
	 * Lists all <code>Cell</code>s declared in the alignment
	 * @return
	 */
	public List<Cell> listCells() {
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE { "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . } } "
				+ "ORDERBY ?entity1 ?entity2";
		logger.debug(query);
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			return getCellFromTupleResult(result);
		}
	}
	
	/**
	 * Lists all <code>Cell</code>s with the given status
	 * @return
	 */
	public List<Cell> listCellsByStatus(Status status) {
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE { "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status . "
				+ "FILTER (STR(?status) = '" + status + "')"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . } } "
				+ "ORDERBY ?entity1 ?entity2";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			return getCellFromTupleResult(result);
		}
	}
	
	private List<Cell> getCellFromTupleResult(TupleQueryResult result) {
		List<Cell> listCell = new ArrayList<>();
		while (result.hasNext()) {
			BindingSet bs = result.next();
			IRI entity1 = (IRI) bs.getValue("entity1");
			IRI entity2 = (IRI) bs.getValue("entity2");
			float measure = Float.parseFloat(bs.getValue("measure").stringValue());
			String relation = bs.getValue("relation").stringValue();
			Cell cell = new Cell(entity1, entity2, measure, relation);
			if (bs.hasBinding("prop")) {
				cell.setMappingProperty((IRI) bs.getValue("prop"));
			}
			if (bs.hasBinding("status")) {
				cell.setStatus(Status.valueOf(bs.getValue("status").stringValue()));
			}
			if (bs.hasBinding("comment")) {
				cell.setComment(bs.getValue("comment").stringValue());
			}
			listCell.add(cell);
		}
		return listCell;
	}

	/**
	 * Adds a <code>Cell</code> to the alignment
	 * @param cell
	 */
	public void addCell(Cell cell) {
		String query = "INSERT { "
				+ "_:cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " _:cell . "
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " . "
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(cell.getEntity2()) + " . "
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + cell.getMeasure() + "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " . "
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + cell.getRelation() + "' . ";
		if (cell.getMappingProperty() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " '" + cell.getMappingProperty() + "' . ";
		}
		if (cell.getStatus() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + cell.getStatus() + "' . ";
		}
		if (cell.getComment() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + cell.getComment() + "' . ";
		}
		query += "} WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Adds a collection of <code>Cell</code>s to the alignment
	 * @param cells
	 */
	public void addCells(Collection<Cell> cells) {
		for (Cell cell : cells) {
			String query = "INSERT { "
					+ "_:cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
					+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " _:cell . "
					+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " . "
					+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(cell.getEntity2()) + " . "
					+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + cell.getMeasure() + "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " . "
					+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + cell.getRelation() + "' . ";
			if (cell.getMappingProperty() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " '" + cell.getMappingProperty() + "' . ";
			}
			if (cell.getStatus() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + cell.getStatus() + "' . ";
			}
			if (cell.getComment() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + cell.getComment() + "' . ";
			}
			query += "} WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . }";
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Deletes the given <code>Cell</code> from the alignment
	 * @param cell
	 */
	public void deleteCell(Cell cell) {
		this.deleteCell(cell.getEntity1(), cell.getEntity2());
	}
	
	/**
	 * Deletes a <code>Cell</code> that contains the given entities from the alignment. The order of
	 * the entities is not relevant
	 * @param entity1
	 * @param entity2
	 */
	public void deleteCell(IRI entity1, IRI entity2) {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell . "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
				+ "WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell . "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Deletes all the <code>Cell</code>s from the alignment
	 * @param cell
	 */
	public void deleteAllCells() {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell . "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
				+ "WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell . "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Accepts an alignment and update the alignment model with the outcome of the validation
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @param projRepoConn repository connection of the ontology, used to check if relation is valid respect entity1
	 * @return
	 * @throws InvalidAlignmentRelationException 
	 */
	public void acceptAlignment(IRI entity1, IRI entity2, String relation, IRI forcedProperty, boolean setAsDefault, 
			RepositoryConnection projRepoConn) {
		String query;
		IRI prop;
		
		try {
			if (forcedProperty != null) { //if property is provided, accept the alignment with this one
				prop = forcedProperty;
				if (setAsDefault) {
					relationPropertyMap.put(relation, forcedProperty);
				}
			} else { //otherwise infer the mapping proprety from relation
				List<IRI> suggProps = suggestPropertiesForRelation(entity1, relation, true, projRepoConn);
				prop = suggProps.get(0);
			}

			query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
					+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " " + NTriplesUtil.toNTriplesString(prop) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.accepted + "' . }"
					+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		} catch (InvalidAlignmentRelationException e) {
			//in case of exception add the error status and the error as comment to the alignment cell
			query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
					+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.error + "' . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + e.getMessage() + "' . } "
					+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		}
		
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Accepts all the alignments and update the alignment model with the outcome of the validations
	 * 
	 * @param projRepoConn repository connection of the ontology, used to check if relation is valid respect entity1
	 * @return
	 */
	public void acceptAllAlignment(RepositoryConnection projRepoConn) {
		List<Cell> cells = listCells();
		for (Cell c : cells){
			String query;
			try {
				List<IRI> suggProps = suggestPropertiesForRelation(c.getEntity1(), c.getRelation(), true, projRepoConn);
				//in case of no exception add the accepted status and the suggested property to the alignment cell
				IRI sProp = suggProps.get(0);
				query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
						+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " " + NTriplesUtil.toNTriplesString(sProp) + " . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.accepted + "' . }"
						+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(c.getEntity1()) + " . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(c.getEntity2()) + " . "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
			} catch (InvalidAlignmentRelationException e) {
				//in case of exception add the error status and the error as comment to the alignment cell
				query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
						+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.error + "' . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + e.getMessage() + "' . } "
						+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(c.getEntity1()) + " . "
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(c.getEntity2()) + " . "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
			}
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}

	/**
	 * Rejects an alignment and update the model with the outcome of the validation
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	public void rejectAlignment(IRI entity1, IRI entity2) {
		String query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
				+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.rejected + "' . }"
				+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Rejects all the alignments and update the model with the outcome of the validations
	 * @return
	 */
	public void rejectAllAlignment() {
		List<Cell> cells = listCells();
		for (Cell c : cells){
			String query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } "
					+ "INSERT { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.rejected + "' . }"
					+ "WHERE { ?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(c.getEntity1()) + " . "
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(c.getEntity2()) + " . "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Sets (forcing) the relation between two entity.
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @param measure
	 */
	public void setRelation(IRI entity1, IRI entity2, String relation, float measure) {
		String query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . } "
				+ "INSERT {	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + relation + "' . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + String.format("%s", measure) + "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " . } "
				+ "WHERE { "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }	"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . } "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Sets (forcing) the mapping property of an alignment
	 * @param entity1
	 * @param entity2
	 * @param mappingProperty
	 */
	public void changeMappingProperty(IRI entity1, IRI entity2, IRI mappingProperty) {
		String query = "DELETE { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . } "
				+ "INSERT {	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " " + NTriplesUtil.toNTriplesString(mappingProperty) + " . } "
				+ "WHERE { "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " . "
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }	}";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Converts the given relation to a property. The conversion depends on:
	 * <ul>
	 * 	<li>Relation: =, >, <, %, InstanceOf, HasInstance</li>
	 * 	<li>Type of model: ontology or thesaurus</li>
	 * 	<li>Type of entity: property, class, instance, concept</li>
	 * </ul>
	 * 
	 * @param entity 
	 * @param relation
	 * @param withDefault if cannot infer a property, determine if consider an eventual
	 * 	default property assigned to the relation
	 * @param projRepoConn repository connectionof the ontology
	 * @return
	 * @throws InvalidAlignmentRelationException 
	 */
	public List<IRI> suggestPropertiesForRelation(IRI entity, String relation, boolean withDefault, RepositoryConnection projRepoConn) 
			throws InvalidAlignmentRelationException {
		List<IRI> suggested = new ArrayList<>();
		RDFResourceRole roleEnum = RoleRecognitionOrchestrator.computeRole(entity, projRepoConn);
		
		if (RDFResourceRole.isProperty(roleEnum)) {
			if (relation.equals("=")) {
				suggested.add(OWL.EQUIVALENTPROPERTY);
				suggested.add(OWL.SAMEAS);
			} else if (relation.equals(">")) {
				throw new InvalidAlignmentRelationException("a rdfs:subProperty alignment would "
						+ "require to assert a triple with the target resource as the subject, "
						+ "which is advisable not to do");
			} else if (relation.equals("<")) {
				suggested.add(RDFS.SUBPROPERTYOF);
			} else if (relation.equals("%")) {
				suggested.add(OWL2Fragment.PROPERTY_DISJOINT_WITH);
			} else if (relation.equals("InstanceOf")) {
				suggested.add(RDF.TYPE);
			} else if (relation.equals("HasInstance")) {
				throw new InvalidAlignmentRelationException("a rdf:type alignment would require to "
						+ "assert a triple with the target resource as the subject, "
						+ "which is advisable not to do");
			}
		} else if (roleEnum.equals(RDFResourceRole.concept)){
			if (relation.equals("=")) {
				suggested.add(SKOS.EXACT_MATCH);
				suggested.add(SKOS.CLOSE_MATCH);
			} else if (relation.equals(">")) {
				suggested.add(SKOS.NARROW_MATCH);
			} else if (relation.equals("<")) {
				suggested.add(SKOS.BROAD_MATCH);
			} else if (relation.equals("%")) {
				//TODO
			} else if (relation.equals("InstanceOf")) {
				suggested.add(SKOS.BROAD_MATCH);
				suggested.add(RDF.TYPE);
			} else if (relation.equals("HasInstance")) {
				suggested.add(SKOS.NARROW_MATCH);
			}
		} else if (roleEnum.equals(RDFResourceRole.cls)) {
			if (relation.equals("=")) {
				suggested.add(OWL.EQUIVALENTCLASS);
				suggested.add(OWL.SAMEAS);
			} else if (relation.equals(">")) {
				throw new InvalidAlignmentRelationException("a rdfs:subClass alignment would "
						+ "require to assert a triple with the target resource as the subject, "
						+ "which is advisable not to do");
			} else if (relation.equals("<")) {
				suggested.add(RDFS.SUBCLASSOF);
			} else if (relation.equals("%")) {
				suggested.add(OWL.DISJOINTWITH);
			} else if (relation.equals("InstanceOf")) {
				suggested.add(RDF.TYPE);
			} else if (relation.equals("HasInstance")) {
				throw new InvalidAlignmentRelationException("a rdf:type alignment would require "
						+ "to assert a triple with the target resource as the subject, "
						+ "which is advisable not to do");
			}
		} else if (roleEnum.equals(RDFResourceRole.individual)) {
			if (relation.equals("=")) {
				suggested.add(OWL.SAMEAS);
			} else if (relation.equals(">") || relation.equals("<")) {
				throw new InvalidAlignmentRelationException(
						"not possible to state a class subsumption on a individual");
			} else if (relation.equals("%")) {
				suggested.add(OWL.DIFFERENTFROM);
			} else if (relation.equals("InstanceOf")) {
				suggested.add(RDF.TYPE);
			} else if (relation.equals("HasInstance")) {
				throw new InvalidAlignmentRelationException("not possible to state a "
						+ "class denotation on an individual");
			}
		}
		if (suggested.isEmpty()) {
			if (withDefault) {
				IRI defaultProp = relationPropertyMap.get(relation);
				if (defaultProp != null) {
					suggested.add(defaultProp);
				}
			}
			if (suggested.isEmpty()) {
				throw new InvalidAlignmentRelationException("Not possible to convert relation "
						+ relation + " for entity " + entity.stringValue() + ". Possible reasons: "
								+ "unknown relation or unknown type of entity.");
			}
		}
		return suggested;
	}
	
	/**
	 * Returns true if the alignment contains a custom relation (e.g. a relation not in knownRelations)
	 * @return
	 */
	public boolean hasCustomRelation() {
		String query = "SELECT DISTINCT ?relation WHERE {"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + ".\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation .\n"
				+ "}";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		TupleQueryResult results = tq.evaluate();
		while (results.hasNext()) {
			String relation = results.next().getValue("relation").stringValue();
			if (!knownRelations.contains(relation)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Reverses the two ontologies aligned the alignment and the entities in the cells
	 */
	public void reverse() {
		String query = "DELETE { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 .	"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . }	"
				+ "INSERT { "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto2 .	"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto1 .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?rr . } "
				+ "WHERE { "
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 . "
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 . "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .	"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 . "
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r . "
				+ "BIND( IF(?r = '<', '>', IF (?r = '>', '<', "
				+ "IF (?r = 'HasInstance', 'InstanceOf', "
				+ "IF (?r = 'InstanceOf', 'HasInstance', ?r) ) ) ) as ?rr ) }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Serializes the Alignment content in the given file according to AlignAPI format
	 * @param outputFile
	 * @throws IOException 
	 * @throws  
	 */
	public void serialize(File outputFile) throws IOException {
		//In the AlignmentModel of the Owlart-API I did this, I don't remember why
		//Retrieve Alignment base node
		RepositoryResult<Statement> alignNodes = repoConnection.getStatements(null, RDF.TYPE, Alignment.ALIGNMENT);
		if (alignNodes.hasNext()) {
			Resource alignNode = alignNodes.next().getSubject();
			repoConnection.remove(alignNode, Alignment.XML, null);
			repoConnection.add(alignNode, Alignment.XML, repoConnection.getValueFactory().createLiteral("no"));
		}
		RepositoryResult<Statement> stmts = repoConnection.getStatements(null, null, null, false);
		Model model = Iterations.addAll(stmts, new LinkedHashModel());
		model.setNamespace("", Alignment.NAMESPACE);
		try (FileOutputStream out = new FileOutputStream(outputFile)) {
			Rio.write(model, out, RDFFormat.RDFXML);
		}
	}
	
	public void close() {
		repoConnection.close();
	}
	
	
}
