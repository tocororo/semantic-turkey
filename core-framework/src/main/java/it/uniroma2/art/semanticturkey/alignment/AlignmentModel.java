package it.uniroma2.art.semanticturkey.alignment;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class AlignmentModel {

	protected static Logger logger = LoggerFactory.getLogger(AlignmentModel.class);

	public enum Status {
		accepted, rejected, error
	}

	private static final List<String> knownRelations = Arrays.asList("=", ">", "<", "%", "HasInstance",
			"InstanceOf");

	private static Map<String, IRI> relationPropertyMap = new HashMap<>();

	private RepositoryConnection repoConnection;

	public AlignmentModel() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.init();
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
		String query = "SELECT DISTINCT ?relation WHERE { \n" + " ?cell a "
				+ NTriplesUtil.toNTriplesString(Alignment.CELL) + " . \n" + " ?cell "
				+ NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation . \n" + "}";
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
	 * 
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
	 * Gets the level of the alignment. Values: "0", "1", "2EDOAL"
	 * 
	 * @return
	 */
	public String getLevel() {
		String query = "SELECT ?level WHERE { " + "?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " ?level . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("level").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the level of the alignment. Values: "0", "1", "2EDOAL"
	 * 
	 * @param level
	 */
	public void setLevel(String level) {
		String query = "DELETE { " + "?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL)
				+ " ?level } " + "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.LEVEL)
				+ " \"" + level + "\" } " + "WHERE { ?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "OPTIONAL { ?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.LEVEL) + " ?level } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Gets the xml compatibility
	 * 
	 * @return
	 */
	public boolean getXml() {
		String query = "SELECT ?xml WHERE { " + "?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.XML) + " ?xml . }";
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
	 * 
	 * @param xml
	 */
	public void setXml(boolean xml) {
		String xmlValue = xml ? "yes" : "no";
		String query = "DELETE { " + "?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " ?xml } "
				+ "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML) + " \"" + xmlValue
				+ "\" } " + "WHERE { ?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT)
				+ " . " + "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.XML)
				+ " ?xml } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Gets the type (or arity) of the alignment. Values: "11", "1?", "1+", "1*", "?1", "??", "?+", "?*",
	 * "+1", "+?", "++", "+*", "*1", "*?", "?+", "**"
	 * 
	 * @return
	 */
	public String getType() {
		String query = "SELECT ?type WHERE { " + "?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.TYPE) + " ?type . }";
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("type").stringValue();
			}
		}
		return null;
	}

	/**
	 * Sets the type (or arity) of the alignment. Values: "11", "1?", "1+", "1*", "?1", "??", "?+", "?*",
	 * "+1", "+?", "++", "+*", "*1", "*?", "?+", "**"
	 * 
	 * @param type
	 */
	public void setType(String type) {
		String query = "DELETE { " + "?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE)
				+ " ?type } " + "INSERT { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.TYPE) + " \""
				+ type + "\" } " + "WHERE { ?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "OPTIONAL { ?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.TYPE) + " ?type } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Gets the baseURI of the first aligned ontology
	 * 
	 * @return
	 */
	public String getOnto1() {
		String query = "SELECT ?onto1 WHERE { " + "?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 . }";
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
	 * 
	 * @param ontologyBaseURI
	 */
	public void setOnto1(String ontologyBaseURI) {
		String query = "DELETE { " + "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1)
				+ " ?onto1 . " + "?onto1 ?p ?o . } " + "INSERT {	?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " <" + ontologyBaseURI + "> . " + "<"
				+ ontologyBaseURI + "> a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " } "
				+ "WHERE {	?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .	"
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 } "
				+ "OPTIONAL { ?onto1 a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " "
				+ "OPTIONAL { ?onto1 ?p ?o } } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Gets the baseURI of the second aligned ontology
	 * 
	 * @return
	 */
	public String getOnto2() {
		String query = "SELECT ?onto2 WHERE { " + "?alignment a "
				+ NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " . " + "?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 . }";
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
	 * 
	 * @param ontologyBaseURI
	 */
	public void setOnto2(String ontologyBaseURI) {
		String query = "DELETE { " + "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2)
				+ " ?onto2 . " + "?onto2 ?p ?o . } " + "INSERT {	?alignment "
				+ NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " <" + ontologyBaseURI + "> . " + "<"
				+ ontologyBaseURI + "> a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " } "
				+ "WHERE {	?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .	"
				+ "OPTIONAL { ?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 } "
				+ "OPTIONAL { ?onto2 a " + NTriplesUtil.toNTriplesString(Alignment.ONTOLOGY) + " "
				+ "OPTIONAL { ?onto2 ?p ?o } } }";
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Returns if exists the aligned Cell with the given entities, null otherwise
	 * 
	 * @param entity1
	 * @param entity2
	 * @return
	 */
	public Cell getCell(IRI entity1, IRI entity2, String relation) {
		//@formatter:off
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE {\n"
				+ "BIND(URI('" + entity1.stringValue() + "') AS ?entity1)\n"
				+ "BIND(URI('" + entity2.stringValue() + "') AS ?entity2)\n"
				+ "BIND('" + relation + "' AS ?relation)\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status .\n}"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . }\n"
				+ "}\n ORDERBY ?entity1 ?entity2";
		//@formatter:on
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
	 * 
	 * @return
	 */
	public List<Cell> listCells() {
		//@formatter:off
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE {\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . }\n"
				+"}\n ORDERBY ?entity1 ?entity2";
		//@formatter:on
		logger.debug(query);
		TupleQuery tq = repoConnection.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			return getCellFromTupleResult(result);
		}
	}

	/**
	 * Lists all <code>Cell</code>s with the given status
	 * 
	 * @return
	 */
	public List<Cell> listCellsByStatus(Status status) {
		//@formatter:off
		String query = "SELECT ?entity1 ?entity2 ?relation ?measure ?prop ?status ?comment WHERE {\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?entity1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?entity2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?measure .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?status .\n"
				+ "FILTER (STR(?status) = '" + status + "')\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?prop . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?comment . }\n"
				+ "}\n ORDERBY ?entity1 ?entity2";
		//@formatter:on
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
	 * 
	 * @param cell
	 */
	public void addCell(Cell cell) {
		//@formatter:off
		String query = "INSERT {\n"
				+ "_:cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " _:cell .\n"
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " .\n"
				+ "_:cell "	+ NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(cell.getEntity2()) + " .\n"
				+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + cell.getMeasure() + "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " .\n"
				+ "_:cell "	+ NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + cell.getRelation() + "' .\n";
		if (cell.getMappingProperty() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " '" + cell.getMappingProperty() + "' .\n";
		}
		if (cell.getStatus() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + cell.getStatus() + "' .\n";
		}
		if (cell.getComment() != null) {
			query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + cell.getComment() + "' .\n";
		}
		query += "} WHERE {\n"
				+"?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .\n"
				+ "}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Adds a collection of <code>Cell</code>s to the alignment
	 * 
	 * @param cells
	 */
	public void addCells(Collection<Cell> cells) {
		for (Cell cell : cells) {
			//@formatter:off
			String query = "INSERT {\n"
					+ "_:cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
					+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " _:cell .\n"
					+ "_:cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " .\n"
					+ "_:cell "	+ NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(cell.getEntity2()) + " .\n"
					+ "_:cell "	+ NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + cell.getMeasure() + "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " .\n"
					+ "_:cell "	+ NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + cell.getRelation() + "' .\n";
			if (cell.getMappingProperty() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " '" + cell.getMappingProperty() + "' .\n";
			}
			if (cell.getStatus() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + cell.getStatus() + "' .\n";
			}
			if (cell.getComment() != null) {
				query += "_:cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + cell.getComment() + "' .\n";
			}
			query += "} WHERE {\n"
					+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .\n"
					+ "}";
			//@formatter:on
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}

	/**
	 * Deletes the given <code>Cell</code> from the alignment
	 * 
	 * @param cell
	 */
	public void deleteCell(Cell cell) {
		this.deleteCell(cell.getEntity1(), cell.getEntity2());
	}

	/**
	 * Deletes a <code>Cell</code> that contains the given entities from the alignment. The order of the
	 * entities is not relevant
	 * 
	 * @param entity1
	 * @param entity2
	 */
	public void deleteCell(IRI entity1, IRI entity2) {
		//@formatter:off
		String query = "DELETE {\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell .\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " "	+ NTriplesUtil.toNTriplesString(entity1) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(entity2) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
				+ "} WHERE {\n"
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .\n"
				+ "?alignment "	+ NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell .\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " "	+ NTriplesUtil.toNTriplesString(entity1) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(entity2) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Deletes all the <code>Cell</code>s from the alignment
	 */
	public void deleteAllCells() {
		//@formatter:off
		String query = "DELETE {\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell .\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
				+ "} WHERE {\n"
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .\n"
				+ "?alignment "	+ NTriplesUtil.toNTriplesString(Alignment.MAP) + " ?cell .\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?sp . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Accepts an alignment and update the alignment model with the outcome of the validation
	 * 
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @param projRepoConn
	 *            repository connection of the ontology, used to check if relation is valid respect entity1
	 * @return
	 * @throws InvalidAlignmentRelationException
	 */
	public void acceptAlignment(IRI entity1, IRI entity2, String relation, IRI forcedProperty,
			boolean setAsDefault, RepositoryConnection projRepoConn) {
		String query;
		IRI prop;

		try {
			if (forcedProperty != null) { // if property is provided, accept the alignment with this one
				prop = forcedProperty;
				if (setAsDefault) {
					relationPropertyMap.put(relation, forcedProperty);
				}
			} else { // otherwise infer the mapping proprety from relation
				List<IRI> suggProps = suggestPropertiesForRelation(entity1, relation, true, projRepoConn);
				prop = suggProps.get(0);
			}
			//@formatter:off
			query = "DELETE {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
					+ "} INSERT {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " " + NTriplesUtil.toNTriplesString(prop) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.accepted + "' .\n"
					+ "} WHERE {\n"
					+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + relation + "' .\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
			//@formatter:on
		} catch (InvalidAlignmentRelationException e) {
			// in case of exception add the error status and the error as comment to the alignment cell
			//@formatter:off
			query = "DELETE {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
					+ "} INSERT {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.error + "' .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + e.getMessage() + "' .\n"
					+ "} WHERE {\n"
					+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(entity2) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + relation + "' .\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
			//@formatter:on
		}

		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Accepts all the alignments and update the alignment model with the outcome of the validations
	 * 
	 * @param projRepoConn
	 *            repository connection of the ontology, used to check if relation is valid respect entity1
	 * @return
	 */
	public void acceptAllAlignment(RepositoryConnection projRepoConn) {
		List<Cell> cells = listCells();
		for (Cell c : cells) {
			String query;
			try {
				List<IRI> suggProps = suggestPropertiesForRelation(c.getEntity1(), c.getRelation(), true,
						projRepoConn);
				// in case of no exception add the accepted status and the suggested property to the alignment
				// cell
				IRI sProp = suggProps.get(0);
				//@formatter:off
				query = "DELETE {\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
						+ "} INSERT {\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " "+  NTriplesUtil.toNTriplesString(sProp) + " .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.accepted + "' .\n"
						+ "} WHERE {\n"
						+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " "	+ NTriplesUtil.toNTriplesString(c.getEntity1()) + " .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(c.getEntity2()) + " .\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
				//@formatter:on
			} catch (InvalidAlignmentRelationException e) {
				// in case of exception add the error status and the error as comment to the alignment cell
				//@formatter:off
				query = "DELETE {\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
						+ "} INSERT {\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.error + "' .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " '" + e.getMessage() + "' .\n"
						+ "} WHERE {\n"
						+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(c.getEntity1()) + " .\n"
						+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(c.getEntity2()) + " .\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
						+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
				//@formatter:on
			}
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}

	/**
	 * Rejects an alignment and update the model with the outcome of the validation
	 * 
	 * @param entity1
	 * @param entity2
	 * @return
	 */
	public void rejectAlignment(IRI entity1, IRI entity2, String relation) {
		//@formatter:off
		String query = "DELETE {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
				+ "} INSERT {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '" + Status.rejected + "' .\n"
				+ "} WHERE {\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + relation + "' .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n" +
				"}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Rejects all the alignments and update the model with the outcome of the validations
	 * 
	 * @return
	 */
	public void rejectAllAlignment() {
		List<Cell> cells = listCells();
		for (Cell c : cells) {
			//@formatter:off
			String query = "DELETE {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
					+ "} INSERT {\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " '"	+ Status.rejected + "' .\n"
					+ "} WHERE {\n"
					+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " "	+ NTriplesUtil.toNTriplesString(c.getEntity1()) + " .\n"
					+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " "	+ NTriplesUtil.toNTriplesString(c.getEntity2()) + " .\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
					+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
			//@formatter:off
			Update update = repoConnection.prepareUpdate(query);
			update.execute();
		}
	}

	/**
	 * Sets (forcing) the relation between two entity.
	 * 
	 * @param entity1
	 * @param entity2
	 * @param oldRelation
	 * @param measure
	 */
	public void updateRelation(IRI entity1, IRI entity2, String oldRelation, String newRelation, float measure) {
		//@formatter:off
		String query = "DELETE {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?oldRelation .\n"
				+ "} INSERT {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + newRelation + "' .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " '" + String.format("%s", measure)	+ "'^^" + NTriplesUtil.toNTriplesString(XMLSchema.FLOAT) + " .\n"
				+ "} WHERE { "
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " "	+ NTriplesUtil.toNTriplesString(entity1) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MEASURE) + " ?m .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + oldRelation + "' .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.STATUS) + " ?s . }\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.COMMENT) + " ?c . }\n}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Sets (forcing) the mapping property of an alignment
	 * 
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @param mappingProperty
	 */
	public void changeMappingProperty(IRI entity1, IRI entity2, String relation, IRI mappingProperty) {
		//@formatter:off
		String query = "DELETE {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p .\n"
				+ "} INSERT {\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " " + NTriplesUtil.toNTriplesString(mappingProperty) + " .\n"
				+ "} WHERE {\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " " + NTriplesUtil.toNTriplesString(entity1) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " " + NTriplesUtil.toNTriplesString(entity2) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " '" + relation + "' .\n"
				+ "OPTIONAL { ?cell " + NTriplesUtil.toNTriplesString(Alignment.MAPPING_PROPERTY) + " ?p . }\n"
				+ "}";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Converts the given relation to a property. The conversion depends on:
	 * <ul>
	 * <li>Relation: =, >, <, %, InstanceOf, HasInstance</li>
	 * <li>Type of entity: property, class, instance, concept</li>
	 * </ul>
	 * 
	 * @param entity
	 * @param relation
	 * @param withDefault
	 *            if cannot infer a property, determine if consider an eventual default property assigned to
	 *            the relation
	 * @param projRepoConn
	 *            repository connectionof the ontology
	 * @return
	 * @throws InvalidAlignmentRelationException
	 */
	public List<IRI> suggestPropertiesForRelation(IRI entity, String relation, boolean withDefault,
			RepositoryConnection projRepoConn) throws InvalidAlignmentRelationException {
		RDFResourceRole roleEnum = RoleRecognitionOrchestrator.computeRole(entity, projRepoConn);
		return suggestPropertiesForRelation(roleEnum, relation, withDefault);
	}

	/**
	 * Converts the given relation to a property. The conversion depends on:
	 * <ul>
	 * <li>Relation: =, >, <, %, InstanceOf, HasInstance</li>
	 * <li>Type of entity: property, class, instance, concept</li>
	 * </ul>
	 *
	 * @param role
	 * @param relation
	 * @param withDefault
	 *            if cannot infer a property, determine if consider an eventual default property stored in the
	 *            alignment model as default for the relation
	 * @return
	 * @throws InvalidAlignmentRelationException
	 */
	public List<IRI> suggestPropertiesForRelation(RDFResourceRole role, String relation, boolean withDefault) throws InvalidAlignmentRelationException {
		List<IRI> suggested = AlignmentUtils.suggestPropertiesForRelation(role, relation);
		if (suggested.isEmpty() && withDefault) {
			IRI defaultProp = relationPropertyMap.get(relation);
			if (defaultProp != null) {
				suggested.add(defaultProp);
			}
			if (suggested.isEmpty()) {
				throw new InvalidAlignmentRelationException(
						"Not possible to convert relation " + relation + " for an entity of type " + role
								+ ". Possible reasons: " + "unknown relation or unknown type of entity.");
			}
		}
		return suggested;
	}

	/**
	 * Returns true if the alignment contains a custom relation (e.g. a relation not in knownRelations)
	 * 
	 * @return
	 */
	public boolean hasCustomRelation() {
		String query = "SELECT DISTINCT ?relation WHERE {" + "?cell a "
				+ NTriplesUtil.toNTriplesString(Alignment.CELL) + ".\n" + "?cell "
				+ NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?relation .\n" + "}";
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
		//@formatter:off
		String query = "DELETE {\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 .\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "} INSERT {\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto2 .\n"
				+ "?alignment " + NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?rr .\n"
				+ "} WHERE {\n"
				+ "?alignment a " + NTriplesUtil.toNTriplesString(Alignment.ALIGNMENT) + " .\n"
				+ "?alignment "	+ NTriplesUtil.toNTriplesString(Alignment.ONTO1) + " ?onto1 .\n"
				+ "?alignment "	+ NTriplesUtil.toNTriplesString(Alignment.ONTO2) + " ?onto2 .\n"
				+ "?cell a " + NTriplesUtil.toNTriplesString(Alignment.CELL) + " .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY1) + " ?e1 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.ENTITY2) + " ?e2 .\n"
				+ "?cell " + NTriplesUtil.toNTriplesString(Alignment.RELATION) + " ?r .\n"
				+ "BIND( IF(?r = '<', '>', IF (?r = '>', '<', " + "IF (?r = 'HasInstance', 'InstanceOf', "
				+ "IF (?r = 'InstanceOf', 'HasInstance', ?r) ) ) ) as ?rr ) }";
		//@formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.execute();
	}

	/**
	 * Serializes the Alignment content in the given file according to AlignAPI format @param
	 * outputFile @throws IOException @throws
	 */
	public void serialize(File outputFile) throws IOException {
		// In the AlignmentModel of the Owlart-API I did this, I don't remember why
		// Retrieve Alignment base node
		RepositoryResult<Statement> alignNodes = repoConnection.getStatements(null, RDF.TYPE,
				Alignment.ALIGNMENT);
		if (alignNodes.hasNext()) {
			Resource alignNode = alignNodes.next().getSubject();
			repoConnection.remove(alignNode, Alignment.XML, null);
			repoConnection.add(alignNode, Alignment.XML,
					repoConnection.getValueFactory().createLiteral("no"));
		}

		Model model;
		try (RepositoryResult<Statement> stmts = repoConnection.getStatements(null, null, null, false)) {
			model = Iterations.addAll(stmts, new TreeModel());
		}

		// Makes sure that the "alignment" is the first resource being described. This is important in order
		// to have the alignment as the root of the RDF/XML document
		Resource alignmentNode = Models.subject(model.filter(null, RDF.TYPE, Alignment.ALIGNMENT))
				.orElse(null);
		if (alignmentNode != null) {
			Model newModel = new LinkedHashModel(model.filter(alignmentNode, null, null));
			model.remove(alignmentNode, null, null);
			newModel.addAll(model);
			model = newModel;
		}

		// Uses the ArrangedWriter (from Turtle) to reorder the statements in a suitable order for bnode
		// inlining (i.e. the statements about a bnode object comes immediatly after) together with bnode
		// duplication
		LinkedHashModel arrangedModel = new LinkedHashModel();

		RDFWriter rdfWriter;
		try {
			Class<?> arrangedWriter = TurtleWriterFactory.class.getClassLoader()
					.loadClass("org.eclipse.rdf4j.rio.turtle.ArrangedWriter");
			Constructor<?> arrangedWriterConstructor = arrangedWriter.getConstructor(RDFWriter.class);
			arrangedWriterConstructor.setAccessible(true);
			rdfWriter = (RDFWriter) arrangedWriterConstructor.newInstance(new AbstractRDFWriter() {

				RDFHandler delegate = new StatementCollector(arrangedModel);

				{
					namespaceTable = new HashMap<>();
				}

				@Override
				public void startRDF() throws RDFHandlerException {
					delegate.startRDF();
				}

				@Override
				public void handleStatement(Statement st) throws RDFHandlerException {
					delegate.handleStatement(st);
				}

				@Override
				public void handleComment(String comment) throws RDFHandlerException {
					delegate.handleComment(comment);
				}

				@Override
				public void endRDF() throws RDFHandlerException {
					delegate.endRDF();
				}

				@Override
				public RDFFormat getRDFFormat() {
					return RDFFormat.NTRIPLES;
				}
			});
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
		rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
		rdfWriter.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		Rio.write(model, rdfWriter);

		// In the rearranged model most of the metadata about the alignment comes after the individual
		// mappings. Reorder the statements, and place the metadata at the beginning of the description

		if (alignmentNode != null) {
			List<Statement> otherStatements = arrangedModel.stream()
					.filter(st -> !(st.getSubject().equals(alignmentNode)
							&& !st.getPredicate().equals(Alignment.MAP)))
					.collect(Collectors.toList());
			arrangedModel.removeAll(otherStatements);
			arrangedModel.addAll(otherStatements);
		}

		arrangedModel.setNamespace("", Alignment.NAMESPACE);

		try (FileOutputStream out = new FileOutputStream(outputFile)) {
			RDFWriter rdfXmlWriter = new RDFXMLPrettyWriterFactory().getWriter(out);
			Rio.write(arrangedModel, rdfXmlWriter);
		}

		// Removes the rdf:bnodeID attributes and save the alignment to file
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(outputFile);
			XPath xPath = XPathFactory.newInstance().newXPath();
			SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
			nsContext.bindNamespaceUri(RDF.PREFIX, RDF.NAMESPACE);
			xPath.setNamespaceContext(nsContext);
			String expression = "//*/@rdf:nodeID";
			NodeList bnodeIDAttributes = (NodeList) xPath.compile(expression).evaluate(doc,
					XPathConstants.NODESET);
			for (int i = 0; i < bnodeIDAttributes.getLength(); i++) {
				Attr attributeNode = (Attr) bnodeIDAttributes.item(i);
				attributeNode.getOwnerElement().removeAttributeNode(attributeNode);
			}

			// Use a Transformer for output
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			try (OutputStream out = new FileOutputStream(outputFile)) {
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(out);
				transformer.transform(source, result);
			}
		} catch (XPathException | ParserConfigurationException | SAXException | TransformerException e) {
			throw new IOException(e);
		}
	}

	public void close() {
		repoConnection.close();
	}

}
