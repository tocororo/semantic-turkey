package it.uniroma2.art.semanticturkey.services.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RDFReporter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.export.ExportPreconditionViolationException;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.sparql.Graph2TupleQueryResultAdapter;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * This class provides services for SPARQL queries/updates.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class SPARQL extends STServiceAdapter {

	@Autowired
	private ExtensionPointManager exptManager;

	private static final Logger logger = LoggerFactory.getLogger(SPARQL.class);

	/**
	 * Evaluates a query. The parameters controlling the dataset (i.e. {@code defaultGraphs} and
	 * {@code namedGraphs}) are not orthogonal: see {@link Dataset} for an explanation of their meaning.
	 * 
	 * @param query
	 *            the query
	 * @param ql
	 *            the query language (see {@link QueryLanguage}. Default value is {@code SPARQL}
	 * @param includeInferred
	 *            indicates whether inferred statements should be included in the evaluation of the query.
	 *            Default value is {@code true}
	 * @param bindings
	 *            variable to value bindings
	 * @param maxExecTime
	 *            maximum execution time measured in seconds (a zero or negative value indicates an unlimited
	 *            execution time). Default value is {@code 0}
	 * @param defaultGraphs
	 *            the graphs that constitute the default graph. The default value is the empty set.
	 * @param namedGraphs
	 *            the graphs that constitute the set of named graphs.
	 * 
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(sparql)', 'R')")
	public JsonNode evaluateQuery(String query, @Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs) throws JsonProcessingException, IOException {
		RepositoryConnection conn = getManagedConnection();

		Query preparedQuery = conn.prepareQuery(ql, query);

		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs, null, null,
				preparedQuery);

		if (preparedQuery instanceof BooleanQuery) {
			boolean result = ((BooleanQuery) preparedQuery).evaluate();
			ObjectNode sparqlObj = JsonNodeFactory.instance.objectNode();
			sparqlObj.set("head", JsonNodeFactory.instance.objectNode());
			sparqlObj.set("boolean", JsonNodeFactory.instance.booleanNode(result));

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("boolean"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		} else if (preparedQuery instanceof TupleQuery) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			((TupleQuery) preparedQuery).evaluate(new SPARQLResultsJSONWriter(baos));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode sparqlObj = mapper.readTree(baos.toString(StandardCharsets.UTF_8.name()));

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("tuple"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		} else { // must be (preparedQuery instanceof GraphQuery)
			JsonNode sparqlObj;
			try (TupleQueryResult queryResult = new IteratingTupleQueryResult(
					Arrays.asList("subj", "pred", "obj"),
					new Graph2TupleQueryResultAdapter(((GraphQuery) preparedQuery).evaluate()))) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				QueryResults.report(queryResult, new SPARQLResultsJSONWriter(baos));
				ObjectMapper mapper = new ObjectMapper();
				sparqlObj = mapper.readTree(baos.toString(StandardCharsets.UTF_8.name()));
			}

			ObjectNode wrapObj = JsonNodeFactory.instance.objectNode();
			wrapObj.set("resultType", JsonNodeFactory.instance.textNode("graph"));
			wrapObj.set("sparql", sparqlObj);

			return wrapObj;
		}
	}

	/**
	 * Executes an update. The parameters controlling the dataset (i.e. {@code defaultGraphs},
	 * {@code namedGraphs}, {@code defaultInsertGraph} and {@code defaultRemoveGraphs} are not orthogonal):
	 * see {@link Dataset} for an explanation of their meaning.
	 * 
	 * @param query
	 *            the query
	 * @param ql
	 *            the query language (see {@link QueryLanguage}. Default value is {@code SPARQL}
	 * @param includeInferred
	 *            indicates whether inferred statements should be included in the evaluation of the query.
	 *            Default value is {@code true}
	 * @param bindings
	 *            variable to value bindings
	 * @param maxExecTime
	 *            maximum execution time measured in seconds (a zero or negative value indicates an unlimited
	 *            execution time). Default value is {@code 0}
	 * @param defaultGraphs
	 *            the graphs that constitute the default graph. The default value is the empty set.
	 * @param namedGraphs
	 *            the graphs that constitute the set of named graphs.
	 * @param defaultInsertGraph
	 *            the default insert graph to be used. The default value is {@code null}.
	 * @param defaultRemoveGraphs
	 *            the default remove graphs.
	 * 
	 * 
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(sparql)', 'U')")
	public void executeUpdate(String query, @Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs, @Optional IRI defaultInsertGraph,
			@Optional(defaultValue = "") IRI[] defaultRemoveGraphs) {

		RepositoryConnection conn = getManagedConnection();

		Update preparedUpdate = conn.prepareUpdate(ql, query);

		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs,
				defaultInsertGraph, defaultRemoveGraphs, preparedUpdate);

		preparedUpdate.execute();
	}

	/**
	 * Exports the query result as spreadsheet
	 * 
	 * @param oRes
	 * @param format
	 *            xlsx for Microsoft Excel spreadsheet or ods for LibreOffice spreadsheet
	 * @param query
	 * @param ql
	 * @param includeInferred
	 * @param bindings
	 * @param maxExecTime
	 * @param defaultGraphs
	 * @param namedGraphs
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(sparql)', 'R')")
	public void exportQueryResultAsSpreadsheet(HttpServletResponse oRes, String format, String query,
			@Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs) throws JsonProcessingException, IOException {

		if (!format.equals("xlsx") && !format.equals("ods")) {
			throw new IllegalArgumentException(
					"Invalid spreadsheet format " + format + ". Available formats are: 'xlsx' and 'ods'");
		}

		RepositoryConnection conn = getManagedConnection();
		Query preparedQuery = conn.prepareQuery(ql, query);
		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs, null, null,
				preparedQuery);

		List<List<String>> table = new ArrayList<>();

		if (preparedQuery instanceof TupleQuery) {
			TupleQueryResult result = ((TupleQuery) preparedQuery).evaluate();
			List<String> bindingNames = result.getBindingNames();
			table.add(bindingNames);
			while (result.hasNext()) {
				List<String> row = new ArrayList<>();
				BindingSet tuple = result.next();
				for (String b : bindingNames) {
					Value value = tuple.getValue(b);
					if (value != null) {
						//row.add(NTriplesUtil.toNTriplesString(value));
						row.add(toNTriplesString(value));
					} else {
						row.add(null);
					}
				}
				table.add(row);
			}
		} else { // must be (preparedQuery instanceof GraphQuery)
			GraphQueryResult result = ((GraphQuery) preparedQuery).evaluate();
			table.add(Arrays.asList("subj", "pred", "obj"));
			while (result.hasNext()) {
				List<String> row = new ArrayList<>();
				Statement stmt = result.next();
				//row.add(NTriplesUtil.toNTriplesString(stmt.getSubject()));
				row.add(toNTriplesString(stmt.getSubject()));
				//row.add(NTriplesUtil.toNTriplesString(stmt.getPredicate()));
				row.add(toNTriplesString(stmt.getPredicate()));
				//row.add(NTriplesUtil.toNTriplesString(stmt.getObject()));
				row.add(toNTriplesString(stmt.getObject()));
				table.add(row);
			}
		}
		dumpSpreadsheet(oRes, table, format);
	}
	
	private String toNTriplesString(Value value) {
		String valueString = "";
		if(value instanceof IRI) {
			//"<" + NTriplesUtil.escapeString(value.toString()) + ">";
			valueString = NTriplesUtil.toNTriplesString(value);
		} else if(value instanceof BNode) {
			valueString = NTriplesUtil.toNTriplesString(value);
		} 
		else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			//valueString = "\""+ NTriplesUtil.escapeString(literal.getLabel()) + "\"";
			valueString = "\""+ literal.getLabel() + "\"";
			if(Literals.isLanguageLiteral(literal)) {
				valueString += "@"+literal.getLanguage().get();
			} else {
				valueString += "^^"+toNTriplesString(literal.getDatatype());
			}
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
		return valueString;
	}

	private void dumpSpreadsheet(HttpServletResponse oRes, List<List<String>> table, String format)
			throws IOException {
		File tempServerFile = File.createTempFile("sparqlExport", "." + format);

		if (format.equals("xlsx")) {
			Workbook wb = new XSSFWorkbook();
			Sheet sheet = wb.createSheet();
			for (int rowIdx = 0; rowIdx < table.size(); rowIdx++) {
				List<String> tableRow = table.get(rowIdx);
				Row row = sheet.createRow(rowIdx);
				for (int colIdx = 0; colIdx < tableRow.size(); colIdx++) {
					row.createCell(colIdx).setCellValue(table.get(rowIdx).get(colIdx));
				}
			}
			try (OutputStream tempServerFileStream = new FileOutputStream(tempServerFile)) {
				wb.write(tempServerFileStream);
			}
		} else { // format .ods (do not perform any check since it is already done in the calling method
			SpreadSheet spreadSheet = SpreadSheet.create(1, table.get(0).size(), table.size());
			org.jopendocument.dom.spreadsheet.Sheet sheet = spreadSheet.getSheet(0);
			for (int rowIdx = 0; rowIdx < table.size(); rowIdx++) {
				List<String> tableRow = table.get(rowIdx);
				for (int colIdx = 0; colIdx < tableRow.size(); colIdx++) {
					sheet.setValueAt(tableRow.get(colIdx), colIdx, rowIdx);
				}
			}
			spreadSheet.saveAs(tempServerFile);
		}

		try {
			oRes.setHeader("Content-Disposition", "attachment; filename=export." + format);
			if (format.equals("xlsx")) {
				oRes.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			} else {
				oRes.setContentType("application/vnd.oasis.opendocument.spreadsheet");
			}
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
	 * Evaluates a graph query and dumps its result (a graph) to an RDF file, using our filtered export
	 * mechanism (see {@link Export}).
	 * 
	 * See {@link #evaluateQuery(String, QueryLanguage, boolean, Map, int, IRI[], IRI[])} and
	 * {@link Export#export(HttpServletResponse, IRI[], TransformationPipeline, boolean, RDFFormat, boolean)}
	 * for the meaning of the parameters.
	 * 
	 * The results of the query are stored in a randomly named graph, which would appear in the output if the
	 * output format supports named graphs.
	 * 
	 * @param oRes
	 * @param query
	 * @param ql
	 * @param includeInferred
	 * @param bindings
	 * @param maxExecTime
	 * @param defaultGraphs
	 * @param namedGraphs
	 * @param filteringPipeline
	 * @param outputFormat
	 * @param deployerSpec
	 * @param reformattingExporterSpec
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws UnloadablePluginConfigurationException
	 * @throws UnsupportedPluginConfigurationException
	 * @throws ClassNotFoundException
	 * @throws ExportPreconditionViolationException
	 * @throws STPropertyAccessException
	 * @throws IllegalArgumentException
	 * @throws InvalidConfigurationException
	 * @throws ReformattingException
	 * @throws NoSuchExtensionException
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(sparql)', 'R')")
	public void exportGraphQueryResultAsRdf(HttpServletResponse oRes, String query,
			@Optional(defaultValue = "SPARQL") QueryLanguage ql,
			@Optional(defaultValue = "true") boolean includeInferred,
			@Optional(defaultValue = "{}") Map<String, Value> bindings,
			@Optional(defaultValue = "0") int maxExecTime, @Optional(defaultValue = "") IRI[] defaultGraphs,
			@Optional(defaultValue = "") IRI[] namedGraphs,
			@Optional(defaultValue = "[]") TransformationPipeline filteringPipeline,
			@Optional String outputFormat, @Optional PluginSpecification deployerSpec,
			@Optional PluginSpecification reformattingExporterSpec)
			throws IOException, ClassNotFoundException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException,
			ExportPreconditionViolationException, IllegalArgumentException, STPropertyAccessException,
			InvalidConfigurationException, NoSuchExtensionException, ReformattingException {

		RepositoryConnection conn = getManagedConnection();

		Query preparedQuery = conn.prepareQuery(ql, query);

		if (!(preparedQuery instanceof GraphQuery)) {
			throw new IllegalArgumentException("Not a graph query");
		}
		configureOperation(includeInferred, bindings, maxExecTime, defaultGraphs, namedGraphs, null, null,
				preparedQuery);

		try (GraphQueryResult result = ((GraphQuery) preparedQuery).evaluate()) {
			if (filteringPipeline.isEmpty() && reformattingExporterSpec == null && deployerSpec == null) {
				Objects.requireNonNull(outputFormat, "Output format must be specified");
				Export.write2requestResponse(oRes, RDFReporter.fromGraphQueryResult(result),
						RDF4JUtilities.getRDFFormat(outputFormat));
			} else {
				Repository tempSourceRepository = new SailRepository(new MemoryStore());
				tempSourceRepository.init();
				try {
					try (RepositoryConnection sourceConnection = tempSourceRepository.getConnection()) {
						RDFInserter rdfInserter = new RDFInserter(sourceConnection);
						rdfInserter.enforceContext(
								SimpleValueFactory.getInstance().createIRI("urn:uuid:" + UUID.randomUUID()));
						QueryResults.report(result, rdfInserter);

						Export.exportHelper(exptManager, stServiceContext, oRes, sourceConnection, new IRI[0],
								filteringPipeline, includeInferred, outputFormat, false, deployerSpec,
								reformattingExporterSpec);
					}
				} finally {
					tempSourceRepository.shutDown();
				}
			}
		}
	}

	protected void configureOperation(boolean includeInferred, Map<String, Value> bindings, int maxExecTime,
			IRI[] defaultGraphs, IRI[] namedGraphs, @Nullable IRI defaultInsertGraph,
			@Nullable IRI[] defaultRemoveGraphs, Operation preparedUpdate) {
		preparedUpdate.setIncludeInferred(includeInferred);

		bindings.forEach((varName, varValue) -> preparedUpdate.setBinding(varName, varValue));

		SimpleDataset dataset = new SimpleDataset();
		Arrays.stream(defaultGraphs).forEach(dataset::addDefaultGraph);
		Arrays.stream(namedGraphs).forEach(dataset::addNamedGraph);

		if (defaultRemoveGraphs != null) {
			Arrays.stream(defaultRemoveGraphs).forEach(dataset::addDefaultRemoveGraph);
		}
		if (defaultInsertGraph != null) {
			dataset.setDefaultInsertGraph(defaultInsertGraph);
		}

		preparedUpdate.setDataset(dataset);

		preparedUpdate.setMaxExecutionTime(maxExecTime);
	}

}