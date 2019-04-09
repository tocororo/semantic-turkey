package it.uniroma2.art.semanticturkey.services.core;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DOMException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.interfaces.ParserPR;
import it.uniroma2.art.coda.interfaces.annotations.converters.RDFCapabilityType;
import it.uniroma2.art.coda.pearl.parser.PearlParserAntlr4;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
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
import it.uniroma2.art.semanticturkey.services.core.sheet2rdf.S2RDFContext;
import it.uniroma2.art.sheet2rdf.coda.CODAConverter;
import it.uniroma2.art.sheet2rdf.coda.Sheet2RDFCODA;
import it.uniroma2.art.sheet2rdf.core.MappingStruct;
import it.uniroma2.art.sheet2rdf.core.Sheet2RDFCore;
import it.uniroma2.art.sheet2rdf.header.NodeConversion;
import it.uniroma2.art.sheet2rdf.header.SimpleGraphApplication;
import it.uniroma2.art.sheet2rdf.header.SimpleHeader;
import it.uniroma2.art.sheet2rdf.header.SubjectHeader;
import it.uniroma2.art.sheet2rdf.sheet.SheetManager;
import it.uniroma2.art.sheet2rdf.sheet.SheetManagerFactory;
import it.uniroma2.art.sheet2rdf.utils.S2RDFUtils;

@STService
public class Sheet2RDF extends STServiceAdapter {

	//map that contain <id, context> pairs to handle multiple sessions
	private Map<String, S2RDFContext> contextMap = new HashMap<String, S2RDFContext>();
	
	@Autowired
	private STServiceContext stServiceContext;
	
	/**
	 * Uploads an excel file into a server directory
	 * @param name
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws ProjectInconsistentException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public void uploadSpreadsheet(MultipartFile file) throws IOException, ProjectInconsistentException {
		String fileName = file.getOriginalFilename();
        //create a temp file (in karaf data/temp folder) to copy the received file 
		File serverSpreadsheetFile = File.createTempFile("sheet", fileName.substring(fileName.lastIndexOf(".")));
		BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverSpreadsheetFile));
		byte[] bytes = file.getBytes();
        stream.write(bytes);
		stream.close();

		// initialize the S2RDFContext and register it
		RepositoryConnection connection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(connection);
		codaCore.initialize(connection);
		Sheet2RDFCore s2rdfCore = new Sheet2RDFCore(serverSpreadsheetFile, connection);
		S2RDFContext s2rdfCtx = new S2RDFContext(s2rdfCore, codaCore, serverSpreadsheetFile);
		String token = stServiceContext.getSessionToken();
		contextMap.put(token, s2rdfCtx);
	}

	/**
	 * Returns an array list containing the headers of the Excel file's data sheet
	 * @return
	 * @throws ModelAccessException 
	 * @throws DOMException 
	 */
	@STServiceOperation
	@Read
	public JsonNode getHeaders() throws DOMException {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		RepositoryConnection connection = getManagedConnection();
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode responseJson = jsonFactory.objectNode();
		
		//subject header
		ObjectNode subjHeaderJson = jsonFactory.objectNode();
		
		SubjectHeader subjectHeader = mappingStruct.getSubjectHeader();
		subjHeaderJson.set("id", jsonFactory.textNode(subjectHeader.getId()));
		subjHeaderJson.set("pearlFeature", jsonFactory.textNode(mappingStruct.getFeatureStructName(subjectHeader)));
		//TODO decide name of the two sections (nodes and graphs) of the header editor
		subjHeaderJson.set("node", getNodeConversionsAsJson(subjectHeader.getNodeConversion(), connection, ctx.getSheet2RDFCore()));
		subjHeaderJson.set("graph", getGraphApplicationAsJson(subjectHeader.getGraphApplication(), connection, ctx.getSheet2RDFCore()));
		responseJson.set("subject", subjHeaderJson);
		
		//simple headers: headers of the sheet columns
		ArrayNode headerJsonArray = jsonFactory.arrayNode();
		List<SimpleHeader> headers = mappingStruct.getHeaders();
		for (SimpleHeader h : headers){
			headerJsonArray.add(getSimpleHeaderAsJson(h, mappingStruct, connection, ctx.getSheet2RDFCore()));
		}
		responseJson.set("headers", headerJsonArray);
		
		return responseJson;
	}
	
	@STServiceOperation
	@Read
	public JsonNode getSubjectHeader() throws DOMException {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		RepositoryConnection connection = getManagedConnection();
		
		SubjectHeader subjectHeader = mappingStruct.getSubjectHeader();
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode headerJson = jsonFactory.objectNode();
		
		headerJson.set("id", jsonFactory.textNode(subjectHeader.getId()));
		headerJson.set("pearlFeature", jsonFactory.textNode(mappingStruct.getFeatureStructName(subjectHeader)));
		
		
		//TODO decide name of the two sections (nodes and graphs) of the header editor
		ArrayNode nodesArray = jsonFactory.arrayNode();
		headerJson.set("nodes", nodesArray);
		nodesArray.add(getNodeConversionsAsJson(subjectHeader.getNodeConversion(), connection, ctx.getSheet2RDFCore()));
		
		ArrayNode graphArray = jsonFactory.arrayNode();
		headerJson.set("graph", graphArray);
		graphArray.add(getGraphApplicationAsJson(subjectHeader.getGraphApplication(), connection, ctx.getSheet2RDFCore()));
		
		return headerJson;
	}
	
	/**
	 * Returns information about the header structure with the given id
	 * @param headerId
	 * @return
	 * @throws ModelAccessException 
	 * @throws DOMException 
	 */
	@STServiceOperation
	@Read
	public JsonNode getHeaderFromId(String headerId) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		return getSimpleHeaderAsJson(h, mappingStruct, getManagedConnection(), ctx.getSheet2RDFCore());
	}
	
	/**
	 * Creates and adds a new graph application to an header
	 * @param headerId id of the header
	 * @param property property of the graph application
	 * @param nodeId node id of the graph application
	 * @param type the optional rdf:type of the node
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void addGraphApplicationToHeader(String headerId, IRI property, String nodeId, @Optional IRI type) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		SimpleGraphApplication g = new SimpleGraphApplication();
		g.setProperty(property);
		g.setNodeId(nodeId);
		g.setType(type);
		h.addGraphApplication(g);
	}
	
	/**
	 * Updates an existing graph application of an header
	 * @param headerId id of the header
	 * @param graphId id of the existing graph application
	 * @param property property of the graph application
	 * @param nodeId node id of the graph application
	 * @param type the optional rdf:type of the node
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void updateGraphApplication(String headerId, String graphId, IRI property, String nodeId, @Optional IRI type) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		for (SimpleGraphApplication g: h.getGraphApplications()) {
			if (g.getId().equals(graphId)) {
				g.setProperty(property);
				g.setNodeId(nodeId);
				g.setType(type);
				break;
			}
		}
	}
	
	/**
	 * Removes a graph application from an header
	 * @param headerId id of the header
	 * @param graphId id of the existing graph application
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void removeGraphApplicationFromHeader(String headerId, String graphId) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		Iterator<SimpleGraphApplication> it = h.getGraphApplications().iterator();
		while (it.hasNext()) {
			if (it.next().getId().equals(graphId)) {
				it.remove();
			}
		}
	}
	
	/**
	 * Returns true if the given node id is already in use. Check useful when creating/updating a node
	 * @param nodeId
	 * @return
	 */
	@STServiceOperation
	@Read
	public Boolean isNodeIdAlreadyUsed(String nodeId) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		if (nodeId.equals(mappingStruct.getSubjectHeader().getNodeConversion().getProducedNodeId())) {
			return true;
		}
		for (SimpleHeader h: mappingStruct.getHeaders()) {
			for (NodeConversion n: h.getNodeConversions()) {
				if (n.getProducedNodeId().equals(nodeId)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Creates and adds a node to a header. 
	 * Note: This service does not perform any check on node ID collision. This check shoul be done separately
	 * via {@link #isNodeIdAlreadyUsed(String)}
	 * Note 2: The creation of a node doesn't imply that it is used in a graph application.
	 * This needs to be done separately (with {@link #addGraphApplicationToHeader(String, IRI, String, IRI)} 
	 * or {@link #updateGraphApplication(String, String, IRI, String, IRI)}  
	 * 
	 * @param headerId
	 * @param nodeId
	 * @param converterCapability
	 * @param converterContract
	 * @param converterDatatype
	 * @param converterLanguage
	 * @param converterParams
	 * @param converterXRole
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void addNodeToHeader(String headerId, String nodeId, RDFCapabilityType converterCapability, 
			String converterContract, @Optional IRI converterDatatype, @Optional String converterLanguage, 
			@Optional Map<String, String> converterParams, @Optional String converterXRole, 
			@Optional(defaultValue = "false") boolean memoize) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		//create node and add it to the header
		NodeConversion n = new NodeConversion();
		n.setProducedNodeId(nodeId);
		CODAConverter c = new CODAConverter(converterCapability, converterContract, converterXRole);
		c.setDatatype(converterDatatype);
		c.setLanguage(converterLanguage);
		if (converterParams != null) {
			c.setParams(converterParams);
		}
		n.setConverter(c);
		n.setMemoize(memoize);
		h.addNodeConversions(n);
	}
	
	/**
	 * Removes a node from an header. If the node is used/referenced by a graph application, updates
	 * the graph application as well.
	 * @param headerId id of the header
	 * @param nodeId id of the existing node
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void removeNodeFromHeader(String headerId, String nodeId) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
		//remove the node
		Iterator<NodeConversion> itNodes = h.getNodeConversions().iterator();
		while (itNodes.hasNext()) {
			if (itNodes.next().getProducedNodeId().equals(nodeId)) {
				itNodes.remove();
			}
		}
		//remove the node from the graphs that use it
		for (SimpleGraphApplication g: h.getGraphApplications()) {
			if (g.getNodeId().equals(nodeId)) {
				g.setNodeId(null);
			}
		}
	}
	
	/**
	 * Update the subject header
	 * @param headerId
	 * @param type
	 * @param converterContract
	 * @param converterParams
	 * @param converterXRole
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void updateSubjectHeader(String headerId, String converterContract, @Optional IRI type,
			@Optional Map<String, String> converterParams, @Optional String converterXRole, 
			@Optional(defaultValue = "false") boolean memoize) {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
		SubjectHeader subjHeader = mappingStruct.getSubjectHeader();
		subjHeader.setId(headerId);
		//update the converter in the node conversion
		NodeConversion n = subjHeader.getNodeConversion();
		CODAConverter c = new CODAConverter(RDFCapabilityType.uri, converterContract, converterXRole);
		if (converterParams != null) {
			c.setParams(converterParams);
		}
		n.setConverter(c);
		n.setMemoize(memoize);
		//update the type in the graph application
		subjHeader.getGraphApplication().setType(type);
	}
	
	
	/**
	 * Returns a preview of the sheet containing the first <code>maxRows</code> rows
	 * @param maxRows
	 * @return
	 */
	@STServiceOperation
	public JsonNode getTablePreview(int maxRows){
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		SheetManager sheetMgr = SheetManagerFactory.getSheetManager(ctx.getSpreadsheetFile());
		ArrayList<ArrayList<String>> table = sheetMgr.getDataTable();
		
		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respJson = jf.objectNode();
		
		int rowsToReturn = maxRows;
		if (table.size() < rowsToReturn) {
			rowsToReturn = table.size();
		}
		
		respJson.set("returned", jf.numberNode(rowsToReturn));
		respJson.set("total", jf.numberNode(table.size()));
		
		ArrayNode rowsJsonArray = jf.arrayNode();
		respJson.set("rows", rowsJsonArray);
		for (int r = 0; r < rowsToReturn; r++){
			ObjectNode rowJson = jf.objectNode();
			rowJson.set("idx", jf.numberNode(r+1));
			ArrayNode cellsJsonArray = jf.arrayNode();
			rowJson.set("cells", cellsJsonArray);
			for (int c = 0; c < table.get(0).size(); c++){
				String cellValue = table.get(r).get(c);
				ObjectNode cellJson = jf.objectNode();
				cellJson.set("idx", jf.numberNode(c+1));//column index in excel spreadsheet
				cellJson.set("value", jf.textNode(cellValue));
				cellsJsonArray.add(cellJson);
			}
			rowsJsonArray.add(rowJson);
		}
		return respJson;
	}

	/**
	 * Returns the PEARL code generated by Excel2RDF
	 * @return
	 * @throws IOException 
	 * @throws ModelAccessException 
	 */
	@STServiceOperation
	@Read
	public JsonNode getPearl(@Optional IRI skosSchema) throws IOException {
		File pearlFile = File.createTempFile("pearl", ".pr");
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		Sheet2RDFCore s2rdfCore = ctx.getSheet2RDFCore();
		s2rdfCore.generatePearlFile(pearlFile, getManagedConnection());
		ctx.setPearlFile(pearlFile);
		String pearl = S2RDFUtils.pearlFileToString(pearlFile);
		return JsonNodeFactory.instance.textNode(pearl);
	}
	
	/**
	 * Saves/updates the PEARL code eventually edited by user
	 * @return
	 * @throws FileNotFoundException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void savePearl(String pearlCode) throws FileNotFoundException{		
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		PrintWriter pw = new PrintWriter(ctx.getPearlFile());
		pw.print(pearlCode);
		pw.close();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public JsonNode validatePearl(String pearlCode){
		InputStream pearlStream = new ByteArrayInputStream(pearlCode.getBytes(StandardCharsets.UTF_8));
		//PearlParser pearlParser = new PearlParser("", "");
		ParserPR pearlParser = new PearlParserAntlr4("", "");
		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respNode = jf.objectNode();
		boolean pearlValid;
		String details = null;
		try {
			pearlParser.parsePearlDocument(pearlStream);
			pearlValid = true;
		} catch (PRParserException e) {
			pearlValid = false;
			details = e.getErrorAsString();
		}
		respNode.set("valid", jf.booleanNode(pearlValid));
		respNode.set("details", jf.textNode(details));
		return respNode;
	}
	
	/**
	 * Uploads a pearl file into a server directory and returns the code
	 * @param name
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public JsonNode uploadPearl(MultipartFile file) throws IOException {
		//upload the file on the server (in karaf data/temp folder)
        S2RDFContext s2rdfCtx = contextMap.get(stServiceContext.getSessionToken());
        File pearlServerFile = File.createTempFile("pearl", ".pr");
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(pearlServerFile));
        byte[] bytes = file.getBytes();
        stream.write(bytes);
        stream.close();
        //update the pearl in the context
        s2rdfCtx.setPearlFile(pearlServerFile);
            
        String pearlCode = S2RDFUtils.pearlFileToString(pearlServerFile);
    	return JsonNodeFactory.instance.textNode(pearlCode);
	}
	
	/**
	 * Returns triples generated by Sheet2RDF
	 * XML Response structure: data -> triple -> subject, predicate, object. This means that inside
	 * data element will be some triple element containing subject predicate and object element
	 * @param maxTableRows max number of table rows to consider in the triples preview (e.g. returns the triples
	 * generated considering the first 10 rows)
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws ConverterException 
	 * @throws ComponentProvisioningException 
	 * @throws PRParserException 
	 * @throws UIMAException 
	 * @throws DependencyException 
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws RDFModelNotSetException 
	 * @throws UnassignableFeaturePathException 
	 * @throws ProjectionRuleModelNotSet 
	 * @throws Exception 
	 */
	@STServiceOperation
	@Read
	public JsonNode getTriplesPreview(int maxTableRows) throws UIMAException, PRParserException,
			ComponentProvisioningException, ConverterException, DependencyException,
			ProjectInconsistentException, RDFModelNotSetException,
			ProjectionRuleModelNotSet, UnassignableFeaturePathException {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		JCas jcas = ctx.getSheet2RDFCore().executeAnnotator();
		Sheet2RDFCODA s2rdfCoda = new Sheet2RDFCODA(getManagedConnection(), ctx.getCodaCore());
		List<SuggOntologyCoda> listSuggOntCoda = new ArrayList<SuggOntologyCoda>();
		listSuggOntCoda = s2rdfCoda.suggestTriples(jcas, ctx.getPearlFile());
		ctx.setSuggestedTriples(listSuggOntCoda);

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode respJson = jsonFactory.objectNode();
		ArrayNode tripleJsonArray = jsonFactory.arrayNode();
		respJson.set("triples", tripleJsonArray);
		
		if (listSuggOntCoda.size() < maxTableRows) {
			maxTableRows = listSuggOntCoda.size();
		}
		
		int triplesPreviewCount = 0;
		int tripleTotCount = 0;

		//consider just the coda suggestions generated from the first 'maxTableRows' annotations 
		//(one annotation for each table rows)
		for (int i = 0; i < maxTableRows; i++) { 
			SuggOntologyCoda suggOntCoda = listSuggOntCoda.get(i);
			List<ARTTriple> tripleList = suggOntCoda.getAllInsertARTTriple();
			triplesPreviewCount = triplesPreviewCount + tripleList.size();
			tripleTotCount = tripleTotCount + tripleList.size();
			for (ARTTriple t : tripleList) {
				ObjectNode tripleJson = jsonFactory.objectNode();
				tripleJsonArray.add(tripleJson);
				tripleJson.set("row", jsonFactory.textNode(suggOntCoda.getAnnotation().getBegin() + ""));
				tripleJson.set("subject", jsonFactory.textNode(NTriplesUtil.toNTriplesString(t.getSubject())));
				tripleJson.set("predicate", jsonFactory.textNode(NTriplesUtil.toNTriplesString(t.getPredicate())));
				tripleJson.set("object", jsonFactory.textNode(NTriplesUtil.toNTriplesString(t.getObject())));
			}
		}
		for (int i = maxTableRows; i < listSuggOntCoda.size(); i++) {
			SuggOntologyCoda suggOntCoda = listSuggOntCoda.get(i);
			tripleTotCount = tripleTotCount + suggOntCoda.getAllInsertARTTriple().size();
		}
		
		respJson.set("total", jsonFactory.numberNode(tripleTotCount));
		respJson.set("returned", jsonFactory.numberNode(triplesPreviewCount));
		
		return respJson;
	}
	
	/**
	 * Simply adds the triples generated to the model
	 * @return
	 */
	@STServiceOperation
	@Write
	public void addTriples() {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		List<SuggOntologyCoda> suggTriples = ctx.getCachedSuggestedTriples();
		RepositoryConnection connection = getManagedConnection();
		for (SuggOntologyCoda sugg : suggTriples){
			List<ARTTriple> triples = sugg.getAllInsertARTTriple();
			for (ARTTriple t : triples){
				connection.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
			}
		}
	}
	
	@STServiceOperation
	public void exportTriples(HttpServletResponse oRes, RDFFormat outputFormat) 
			throws IOException {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		//Write suggested triples in a temporary model
		List<SuggOntologyCoda> suggTriples = ctx.getCachedSuggestedTriples();
		
		Repository rep = new SailRepository(new MemoryStore());
		rep.initialize();
		RepositoryConnection connection = rep.getConnection();

		for (SuggOntologyCoda sugg : suggTriples){
			List<ARTTriple> triples = sugg.getAllInsertARTTriple();
			for (ARTTriple t : triples) {
				connection.add(t.getSubject(), t.getPredicate(), t.getObject());
			}
		}
		//serialize the temporary model on a server side temporary file
		File tempServerFile = File.createTempFile("triples", "."+outputFormat.getDefaultFileExtension());

		try {
			try (OutputStream tempServerFileStream = new FileOutputStream(tempServerFile)) {
				connection.exportStatements(null, null, null, false, Rio.createWriter(outputFormat, tempServerFileStream));
			}
			oRes.setHeader("Content-Disposition", "attachment; filename=export." + outputFormat.getDefaultFileExtension());
			oRes.setContentType(outputFormat.getDefaultMIMEType());
			oRes.setContentLength((int) tempServerFile.length());

			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
			connection.close();
		}
	}
	
	@STServiceOperation
	public void closeSession() {
		if (stServiceContext.hasContextParameter("token")) {
			String token = stServiceContext.getSessionToken();
			contextMap.remove(token);
		}
	}

	
	private JsonNode getSimpleHeaderAsJson(SimpleHeader h, MappingStruct mappingStruct,
			RepositoryConnection connection, Sheet2RDFCore s2rdfCore) {
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode headerJson = jsonFactory.objectNode();
		
		headerJson.set("id", jsonFactory.textNode(h.getId()));
		headerJson.set("name", jsonFactory.textNode(h.getHeaderName()));
		headerJson.set("pearlFeature", jsonFactory.textNode(mappingStruct.getFeatureStructName(h)));
		headerJson.set("isMultiple", jsonFactory.booleanNode(mappingStruct.isHeaderMultiple(h.getHeaderName())));
		
		//TODO decide name of the two sections (nodes and graphs) of the header editor
		ArrayNode nodesArray = jsonFactory.arrayNode();
		headerJson.set("nodes", nodesArray);
		for (NodeConversion c: h.getNodeConversions()) {
			nodesArray.add(getNodeConversionsAsJson(c, connection, s2rdfCore));
			
		}
		
		ArrayNode graphArray = jsonFactory.arrayNode();
		headerJson.set("graph", graphArray);
		for (SimpleGraphApplication ga: h.getGraphApplications()) {
			graphArray.add(getGraphApplicationAsJson(ga, connection, s2rdfCore));
		}
		
		return headerJson;
	}
	
	private JsonNode getNodeConversionsAsJson(NodeConversion n, RepositoryConnection connection, Sheet2RDFCore s2rdfCore) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ObjectNode nodeJson = jsonFactory.objectNode();
		
		nodeJson.set("nodeId", jsonFactory.textNode(n.getProducedNodeId()));
		
		CODAConverter converter = n.getConverter();
		if (converter != null) {
			nodeJson.set("converter", converter.getAsJsonObject());
		}
		
		nodeJson.set("memoize", jsonFactory.booleanNode(n.isMemoize()));
		
		return nodeJson;
	}
	
	private JsonNode getGraphApplicationAsJson(SimpleGraphApplication ga, RepositoryConnection connection, Sheet2RDFCore s2rdfCore) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ObjectNode graphJson = jsonFactory.objectNode();
		
		graphJson.set("id", jsonFactory.textNode(ga.getId()));
		
		IRI prop = ga.getProperty();
		if (prop != null) {
			AnnotatedValue<IRI> annotatedRes = new AnnotatedValue<IRI>(prop);
			annotatedRes.setAttribute("role", RoleRecognitionOrchestrator.computeRole(prop, connection).name());
			annotatedRes.setAttribute("show", S2RDFUtils.asQName(prop, s2rdfCore.getMergedPrefixMapping()));
			graphJson.putPOJO("property", annotatedRes);
		} else {
			graphJson.putPOJO("property", null);
		}
		
		IRI type = ga.getType();
		if (type != null) {
			AnnotatedValue<IRI> annotatedRes = new AnnotatedValue<IRI>(type);
			annotatedRes.setAttribute("role", RDFResourceRole.cls.name());
			annotatedRes.setAttribute("show", S2RDFUtils.asQName(type, s2rdfCore.getMergedPrefixMapping()));
			graphJson.putPOJO("type", annotatedRes);
		} else {
			graphJson.putPOJO("type", null);
		}
		
		graphJson.set("nodeId", jsonFactory.textNode(ga.getNodeId()));
		
		return graphJson;
	}
}
