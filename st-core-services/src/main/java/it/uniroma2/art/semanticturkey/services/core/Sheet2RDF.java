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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
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
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.parser.PearlParserAntlr4;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
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
import it.uniroma2.art.sheet2rdf.core.Sheet2RDFCore;
import it.uniroma2.art.sheet2rdf.header.Header;
import it.uniroma2.art.sheet2rdf.header.HeadersStruct;
import it.uniroma2.art.sheet2rdf.resolver.ConverterResolver;
import it.uniroma2.art.sheet2rdf.sheet.SheetManager;
import it.uniroma2.art.sheet2rdf.sheet.SheetManagerFactory;
import it.uniroma2.art.sheet2rdf.utils.S2RDFUtils;
import it.uniroma2.art.sheet2rdf.vocabulary.RDFTypesEnum;

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
		Sheet2RDFCore s2rdfCore = new Sheet2RDFCore(serverSpreadsheetFile, connection, getProject().getModel());
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
		ArrayNode headerJsonArray = JsonNodeFactory.instance.arrayNode();
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		HeadersStruct headersStruct = ctx.getSheet2RDFCore().getHeadersStruct();
		List<Header> headers = headersStruct.getHeaders();
		for (Header h : headers){
			headerJsonArray.add(getHeaderAsJson(h, headersStruct, getManagedConnection(), 
					ctx.getSheet2RDFCore(), ctx.getCodaCore()));
		}
		return headerJsonArray;
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
		HeadersStruct headersStruct = ctx.getSheet2RDFCore().getHeadersStruct();
		Header h = headersStruct.getHeaderFromId(headerId);
		return getHeaderAsJson(h, headersStruct, getManagedConnection(), ctx.getSheet2RDFCore(), ctx.getCodaCore());
	}
	
	private JsonNode getHeaderAsJson(Header h, HeadersStruct headersStruct,
			RepositoryConnection connection, Sheet2RDFCore s2rdfCore, CODACore codaCore) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode headerJson = jsonFactory.objectNode();
		headerJson.set("id", jsonFactory.textNode(h.getId()));
		headerJson.set("name", jsonFactory.textNode(h.getHeaderName()));
		headerJson.set("isMultiple", jsonFactory.booleanNode(headersStruct.isHeaderMultiple(h.getHeaderName())));
		
		//check if h.getHeaderResource is a valid property or a class
		IRI headerRes = h.getHeaderResource();
		if (headerRes != null) {
			AnnotatedValue<IRI> annotatedRes = new AnnotatedValue<IRI>(headerRes);
			if (h.isClass()) {
				annotatedRes.setAttribute("role", RDFResourceRole.cls.name());
			} else {
				annotatedRes.setAttribute("role", RDFResourceRole.property.name());
			}
			annotatedRes.setAttribute("show", S2RDFUtils.asQName(headerRes.stringValue(), s2rdfCore.getMergedPrefixMapping()));
			headerJson.putPOJO("resource", annotatedRes);
		} else {
			headerJson.set("resource", null);
		}
		
		headerJson.set("lang", jsonFactory.textNode(h.getRangeLanguage()));
		
		ObjectNode rangeJson = jsonFactory.objectNode();
		headerJson.set("range", rangeJson);
		rangeJson.set("type", jsonFactory.textNode(h.getRangeType().name()));
		if (h.getRangeType().equals(RDFTypesEnum.resource) && h.getRangeClass() != null) {
			AnnotatedValue<Resource> annotatedRes= new AnnotatedValue<Resource>(h.getRangeClass());
			//compute role, qname, ecc?
			rangeJson.putPOJO("cls", annotatedRes);
		} else if (h.getRangeType().equals(RDFTypesEnum.typedLiteral) && h.getRangeDatatype() != null) {
			AnnotatedValue<Resource> annotatedRes= new AnnotatedValue<Resource>(h.getRangeDatatype());
			//compute role, qname, ecc?
			rangeJson.putPOJO("resource", annotatedRes);
		}
		
		ObjectNode converterJson = jsonFactory.objectNode();
		headerJson.set("converter", converterJson);
		CODAConverter converter = h.getConverter();
		if (converter != null) {
			converterJson.set("uri", jsonFactory.textNode(converter.getContractUri()));
			converterJson.set("type", jsonFactory.textNode(converter.getType().name()));
			converterJson.set("xRole", jsonFactory.textNode(converter.getxRole()));
			converterJson.set("memoize", jsonFactory.booleanNode(h.isMemoize()));
		} else {
			converterJson.set("uri", null);
			converterJson.set("type", null);
			converterJson.set("xRole", null);
			converterJson.set("memoize", jsonFactory.booleanNode(h.isMemoize()));
		}
		
		return headerJson;
	}
	
	/**
	 * Updates information about the header with the given id. In particulars, update the associated
	 * property, eventually the rangeType and the language (if available for the property).
	 * If there are multiple headers with the same name, is possible to apply the changes to all of
	 * them. 
	 * @param headerId
	 * @param property
	 * @param rangeType
	 * @param lang
	 * @param applyToAll Tells if the changes must be applied to all the headers with the same name
	 * @return
	 * @throws PRParserException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public void updateHeader(String headerId, IRI headerResource, 
			@Optional RDFTypesEnum rangeType,
			@Optional IRI rangeClass,
			@Optional RDFCapabilityType converterType,
			@Optional String converterMention,
			@Optional String converterXRole,
			@Optional (defaultValue = "false") boolean memoize,
//			@Optional String lang,
//			@Optional IRI rangeDatatype, 
			@Optional (defaultValue = "false") boolean applyToAll) throws PRParserException {
		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
		HeadersStruct headersStruct = ctx.getSheet2RDFCore().getHeadersStruct();
		Header header = headersStruct.getHeaderFromId(headerId);
		List<Header> headers = new ArrayList<>(); //headers to update
		if (applyToAll){ //apply changes to all the headers with same name
			headers = headersStruct.getHeadersFromName(header.getHeaderName());
		} else {
			headers.add(header);
		}
		RepositoryConnection connection = getManagedConnection();
		ConverterResolver converterResolver = new ConverterResolver(connection);
		/*
		 * apply the change to the collected headers (In most of the case there is just one header,
		 * there are multiple headers only if there are more with the same name and applyToAll was true)
		 */
		for (Header h : headers){ 
			h.setHeaderResource(headerResource);
			
			//update isClass
			h.setIsClass(S2RDFUtils.isClass(headerResource, connection));
			
			//update converter
			CODAConverter converter;
			if (converterMention != null && converterType != null) { //converter is provided by the client
				//set converter contract description retrieving the ConverterContractDescription object from its uri
				ConverterMention mention = ctx.getCodaCore().parseConverterMention(converterMention, ctx.getSheet2RDFCore().getMergedPrefixMapping());
				String convUri = mention.getURI();
				converter = new CODAConverter(converterType, convUri);
				converter.setxRole(converterXRole);
			} else { 
				//resolve the converter with the default choice for the resource assigned to the header
				converter = converterResolver.getConverter(headerResource);
			}
			h.setConverter(converter);
			
			h.setMemoize(memoize);
			
			if (rangeType != null) {
				h.setRangeType(rangeType);
			}
			
//			//Set the optional fields, so that if not passed they will be set to null
//			h.setRangeLanguage(lang);
			h.setRangeClass(rangeClass);
//			h.setRangeDatatype(rangeDatatype);
		}
	}
	
//	//TODO: soon it will be useful to pass a parameter to indicate whether is required the list 
//	//of CODA converter for uri or literal type
//	/**
//	 * Returns the list of available CODA converters
//	 * rangeType values: resource, plainLiteral, typedLiteral
//	 * @return
//	 */
//	@STServiceOperation
//	public JsonNode listConverters(@Optional String rangeType, @Optional IRI datatype) {
//		
//		JsonNodeFactory jf = JsonNodeFactory.instance;
//		ArrayNode converterJsonArray = jf.arrayNode();
//		
//		//align range type with RDFCapability name
//		if (rangeType != null && rangeType.equals("resource")) {
//			rangeType = "uri";
//		}
//		S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
//		Collection<ConverterContractDescription> codaConvList = ctx.getCodaCore().listConverterContracts();
//		for (ConverterContractDescription convDescr : codaConvList) {
//			RDFCapabilityType rdfCapability = convDescr.getRDFCapability();
//			Set<IRI> datatypes = convDescr.getDatatypes();
//			if (rangeType != null && datatype != null) {
//				/* if datatype is provided, return converters only if the required range is typedLiteral
//				 * since "reource" and "plainLiteral" range cannot have a datatype */
//				if (rangeType.equals("typedLiteral") && (rdfCapability.equals(RDFCapabilityType.node) || 
//						rdfCapability.equals(RDFCapabilityType.literal) ||
//						(rdfCapability.equals(RDFCapabilityType.typedLiteral) && datatypes.contains(datatype)))) {
//					converterJsonArray.add(getConverterAsJson(convDescr));
//				}
//			} else if (rangeType != null && datatype == null) {
//				//return converter with the required range or which the RDFCapabillity includes the required one
//				//(rdfCapability is node or is literal && required range is typed/plainLiteral)
//				if (rdfCapability.name().equals(rangeType) || rdfCapability.equals(RDFCapabilityType.node) ||
//						(rdfCapability.equals(RDFCapabilityType.literal) && (rangeType.contains("Literal")))) {
//					converterJsonArray.add(getConverterAsJson(convDescr));
//				}
//			} else if (rangeType == null && datatype != null) {
//				//return converters with RDFCapabililty node || literal || typedLiteral with the given datatype
//				if (rdfCapability.equals(RDFCapabilityType.literal) || rdfCapability.equals(RDFCapabilityType.node)) {
//					converterJsonArray.add(getConverterAsJson(convDescr));
//				} else if (rdfCapability.equals(RDFCapabilityType.typedLiteral) && datatypes.contains(datatype)) {
//					converterJsonArray.add(getConverterAsJson(convDescr));
//				}
//			} else if (rangeType == null && datatype == null) {//return all converters
//				converterJsonArray.add(getConverterAsJson(convDescr));
//			}
//		}
//		return converterJsonArray;
//	}
//
//	private JsonNode getConverterAsJson(ConverterContractDescription convDescr) {
//		JsonNodeFactory jf = JsonNodeFactory.instance;
//		ObjectNode converterJson = jf.objectNode();
//		converterJson.set("uri", jf.textNode(convDescr.getContractURI()));
//		converterJson.set("name", jf.textNode(convDescr.getContractName()));
//		converterJson.set("description", jf.textNode(convDescr.getContractDescription()));
//		return converterJson;
//	}
	
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
		if (skosSchema != null) {
			s2rdfCore.generatePearlFile(pearlFile, getManagedConnection(), skosSchema);
		} else {
			s2rdfCore.generatePearlFile(pearlFile, getManagedConnection());
		}
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
		for (SuggOntologyCoda sugg : suggTriples){
			List<ARTTriple> triples = sugg.getAllInsertARTTriple();
			for (ARTTriple t : triples){
				getManagedConnection().add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
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

}
