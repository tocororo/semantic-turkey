package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.NodeNotDefinedException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.exception.parserexception.PrefixNotDefinedException;
import it.uniroma2.art.coda.interfaces.ParserPR;
import it.uniroma2.art.coda.interfaces.annotations.converters.RDFCapabilityType;
import it.uniroma2.art.coda.pearl.model.GraphElement;
import it.uniroma2.art.coda.pearl.model.GraphStruct;
import it.uniroma2.art.coda.pearl.model.OptionalGraphStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemPlaceholder;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElement;
import it.uniroma2.art.coda.pearl.parser.PearlParserAntlr4;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.provisioning.ConverterContractDescription;
import it.uniroma2.art.coda.structures.CODATriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.sheet2rdf.StoredAdvancedGraphApplicationConfiguration;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.Metadata.PrefixMapping;
import it.uniroma2.art.semanticturkey.services.core.sheet2rdf.S2RDFContext;
import it.uniroma2.art.sheet2rdf.cfg.GraphApplicationConfigurationLoader;
import it.uniroma2.art.sheet2rdf.coda.CODAConverter;
import it.uniroma2.art.sheet2rdf.coda.Sheet2RDFCODA;
import it.uniroma2.art.sheet2rdf.core.MappingStruct;
import it.uniroma2.art.sheet2rdf.core.Sheet2RDFCore;
import it.uniroma2.art.sheet2rdf.exception.InvalidWizardStatusException;
import it.uniroma2.art.sheet2rdf.header.AdvancedGraphApplication;
import it.uniroma2.art.sheet2rdf.header.GraphApplication;
import it.uniroma2.art.sheet2rdf.header.NodeConversion;
import it.uniroma2.art.sheet2rdf.header.SimpleGraphApplication;
import it.uniroma2.art.sheet2rdf.header.SimpleHeader;
import it.uniroma2.art.sheet2rdf.header.SubjectHeader;
import it.uniroma2.art.sheet2rdf.sheet.SheetManager;
import it.uniroma2.art.sheet2rdf.sheet.SheetManagerFactory;
import it.uniroma2.art.sheet2rdf.utils.FsNamingStrategy;
import it.uniroma2.art.sheet2rdf.utils.S2RDFUtils;
import it.uniroma2.art.sheet2rdf.utils.StatusHandler;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DOMException;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@STService
public class Sheet2RDF extends STServiceAdapter {

    //map that contain <id, context> pairs to handle multiple sessions
    private Map<String, S2RDFContext> contextMap = new HashMap<>();

    /**
     * Uploads an excel file into a server directory
     *
     * @param file
     * @param fsNamingStrategy
     * @return
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @Read
    public void uploadSpreadsheet(MultipartFile file, @Optional(defaultValue = "columnNumericIndex") FsNamingStrategy fsNamingStrategy)
            throws IOException {
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
        Sheet2RDFCore s2rdfCore = new Sheet2RDFCore(serverSpreadsheetFile, connection, fsNamingStrategy);
        S2RDFContext s2rdfCtx = new S2RDFContext(s2rdfCore, codaCore, serverSpreadsheetFile);
        String token = stServiceContext.getSessionToken();
        contextMap.put(token, s2rdfCtx);
    }

    /**
     * Returns an array list containing the headers of the Excel file's data sheet
     *
     * @return
     * @throws DOMException
     */
    @STServiceOperation
    @Read
    public JsonNode getHeaders() throws DOMException {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode responseJson = jsonFactory.objectNode();

        //subject header
        JsonNode subjHeaderJson = mappingStruct.getSubjectHeaderAsJson();
        responseJson.set("subject", subjHeaderJson);

        //simple headers: headers of the sheet columns
        ArrayNode headerJsonArray = jsonFactory.arrayNode();
        List<SimpleHeader> headers = mappingStruct.getHeaders();
        for (SimpleHeader h : headers) {
            headerJsonArray.add(mappingStruct.getSimpleHeaderAsJson(h));
        }
        responseJson.set("headers", headerJsonArray);

        return responseJson;
    }

    /**
     * Returns information about the header structure with the given id
     *
     * @param headerId
     * @return
     * @throws DOMException
     */
    @STServiceOperation
    @Read
    public JsonNode getHeaderFromId(String headerId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        return mappingStruct.getSimpleHeaderAsJson(h);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void ignoreHeader(String headerId, boolean ignore) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        h.setIgnore(ignore);
    }

    /**
     * Creates and adds a new graph application to an header
     *
     * @param headerId id of the header
     * @param property property of the graph application
     * @param nodeId   node id of the graph application
     * @param type     the optional rdf:type of the node
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void addSimpleGraphApplicationToHeader(String headerId, IRI property, String nodeId, @Optional Resource type) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        SimpleGraphApplication g = new SimpleGraphApplication();
        g.setProperty(property);
        g.setNodeId(nodeId);
        g.setValue(type);
        h.addGraphApplication(g);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void addAdvancedGraphApplicationToHeader(String headerId, String graphPattern, List<String> nodeIds,
                                                    Map<String, String> prefixMapping, @Optional IRI defaultPredicate) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        AdvancedGraphApplication g = new AdvancedGraphApplication();
        g.setPattern(graphPattern);
        g.setNodeIds(nodeIds);
        g.setPrefixMapping(prefixMapping);
        g.setDefaultPredicate(defaultPredicate);
        h.addGraphApplication(g);
    }

    /**
     * Updates an existing graph application of an header
     *
     * @param headerId id of the header
     * @param graphId  id of the existing graph application
     * @param property property of the graph application
     * @param nodeId   node id of the graph application
     * @param type     the optional rdf:type of the node
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void updateSimpleGraphApplication(String headerId, String graphId, IRI property, String nodeId, @Optional Resource type) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        for (GraphApplication g : h.getGraphApplications()) {
            if (g.getId().equals(graphId) && g instanceof SimpleGraphApplication) {
                SimpleGraphApplication sga = (SimpleGraphApplication) g;
                sga.setProperty(property);
                sga.setNodeId(nodeId);
                sga.setValue(type);
                break;
            }
        }
    }

    /**
     * Updates an existing advanced graph application of an header
     *
     * @param headerId     id of the header
     * @param graphId      id of the existing graph application
     * @param graphPattern updated graph pattern
     * @param nodeIds      updated list of referenced node ids
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void updateAdvancedGraphApplication(String headerId, String graphId, String graphPattern, List<String> nodeIds,
                                               Map<String, String> prefixMapping, @Optional IRI defaultPredicate) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        for (GraphApplication g : h.getGraphApplications()) {
            if (g.getId().equals(graphId) && g instanceof AdvancedGraphApplication) {
                AdvancedGraphApplication aga = (AdvancedGraphApplication) g;
                aga.setNodeIds(nodeIds);
                aga.setPattern(graphPattern);
                aga.setPrefixMapping(prefixMapping);
                aga.setDefaultPredicate(defaultPredicate);
                break;
            }
        }
    }

    /**
     * Removes a graph application from an header
     *
     * @param headerId id of the header
     * @param graphId  id of the existing graph application
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void removeGraphApplicationFromHeader(String headerId, String graphId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        h.getGraphApplications().removeIf(graphApplication -> graphApplication.getId().equals(graphId));
    }

    /**
     * Returns true if the given node id is already in use. Check useful when creating/updating a node
     *
     * @param nodeId
     * @return
     */
    @STServiceOperation
    @Read
    public Boolean isNodeIdAlreadyUsed(String nodeId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        if (nodeId.equals(mappingStruct.getSubjectHeader().getNodeConversion().getNodeId())) {
            return true;
        }
        for (SimpleHeader h : mappingStruct.getHeaders()) {
            for (NodeConversion n : h.getNodeConversions()) {
                if (n.getNodeId().equals(nodeId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates and adds a node to a header.
     * Note: This service does not perform any check on node ID collision. This check should be done separately
     * via {@link #isNodeIdAlreadyUsed(String)}
     * Note 2: The creation of a node doesn't imply that it is used in a graph application.
     * This needs to be done separately (with add/update Simple/Advanced GraphApplicationToHeader}
     *
     * @param headerId
     * @param nodeId
     * @param converterCapability
     * @param converterContract
     * @param converterDatatypeUri
     * @param converterLanguage
     * @param converterParams
     * @param memoize
     */
    @Read
    @STServiceOperation(method = RequestMethod.POST)
    public void addNodeToHeader(String headerId, String nodeId, RDFCapabilityType converterCapability,
                                String converterContract, @Optional String converterDatatypeUri, @Optional String converterLanguage,
                                @Optional Map<String, String> converterParams, @Optional(defaultValue = "false") boolean memoize) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        //create node and add it to the header
        NodeConversion n = new NodeConversion();
        n.setNodeId(nodeId);
        CODAConverter c = resolveCodaConverter(converterContract, converterCapability);
        c.setDatatypeUri(converterDatatypeUri);
        c.setLanguage(converterLanguage);
        if (converterParams != null) {
            Map<String, Object> resolvedConvParams = resolveConverterParamsMap(converterParams);
            c.setParams(resolvedConvParams);
        }
        n.setConverter(c);
        n.setMemoize(memoize);
        h.addNodeConversions(n);
    }

    @Read
    @STServiceOperation(method = RequestMethod.POST)
    public void updateNodeInHeader(String headerId, String nodeId, RDFCapabilityType converterCapability,
                                   String converterContract, @Optional String converterDatatypeUri, @Optional String converterLanguage,
                                   @Optional Map<String, String> converterParams, @Optional(defaultValue = "false") boolean memoize) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);

        List<NodeConversion> nodes = h.getNodeConversions();
        for (NodeConversion node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                //update the node
                CODAConverter c = resolveCodaConverter(converterContract, converterCapability);
                c.setDatatypeUri(converterDatatypeUri);
                c.setLanguage(converterLanguage);
                if (converterParams != null) {
                    Map<String, Object> resolvedConvParams = resolveConverterParamsMap(converterParams);
                    c.setParams(resolvedConvParams);
                }
                node.setConverter(c);
                node.setMemoize(memoize);
                break;
            }
        }
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void renameNodeId(String headerId, String nodeId, String newNodeId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();

        //check if newNodeId is already used in some header
        for (SimpleHeader h: mappingStruct.getHeaders()) {
            if (h.getNodeConversion(newNodeId) != null) {
                throw new IllegalArgumentException("Cannot rename node '" + nodeId + "' in '" + newNodeId +
                        "'. A node with ID '" + newNodeId + "' already exists");
            }
        }

        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        //rename the node ID
        NodeConversion node = h.getNodeConversion(nodeId);
        node.setNodeId(newNodeId);
        //rename occurrences (if any) of nodeID in the graph applications
        for (GraphApplication ga: h.getGraphApplications()) {
            if (ga instanceof SimpleGraphApplication) {
                SimpleGraphApplication sga = (SimpleGraphApplication) ga;
                if (sga.getNodeId().equals(nodeId)) {
                    sga.setNodeId(newNodeId);
                }
            } else { //instance of AdvancedGraphApplication
                AdvancedGraphApplication aga = (AdvancedGraphApplication) ga;
                //replace referenced node ID in node list
                List<String> nodeIDs = aga.getNodeIds();
                if (nodeIDs.contains(nodeId)) {
                    nodeIDs.set(nodeIDs.indexOf(nodeId), newNodeId);
                }
                //replace referenced node ID in graph pattern
                String pattern = aga.getPattern();
                pattern = pattern.replaceAll("\\b"+nodeId+"\\b", newNodeId);
                aga.setPattern(pattern);
            }
        }
    }

    /**
     * Removes a node from an header. If the node is used/referenced by a graph application, updates
     * the graph application as well.
     *
     * @param headerId id of the header
     * @param nodeId   id of the existing node
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void removeNodeFromHeader(String headerId, String nodeId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader h = mappingStruct.getHeaderFromId(headerId);
        //remove the node
        h.getNodeConversions().removeIf(nodeConversion -> nodeConversion.getNodeId().equals(nodeId));
        //Remove the node from the graphApplications that use it.
        for (GraphApplication g : h.getGraphApplications()) {
            g.removeNode(nodeId);
        }
    }

    /**
     * Given the header with the headerId, replicate its configuration for all the other headers with the same name
     *
     * @param headerId
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void replicateMultipleHeader(String headerId) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SimpleHeader sourceHeader = mappingStruct.getHeaderFromId(headerId);
        mappingStruct.replicateMultipleHeader(sourceHeader);
    }

    /**
     * Update the subject header
     *
     * @param headerId
     * @param type
     * @param converterContract
     * @param converterParams
     * @param memoize
     */
    @Read
    @STServiceOperation(method = RequestMethod.POST)
    public void updateSubjectHeader(String headerId, String converterContract, @Optional Map<String, String> converterParams,
            @Optional IRI type, @Optional(defaultValue = "[]") @JsonSerialized List<Pair<IRI, Value>> additionalPredObjs,
            @Optional(defaultValue = "false") boolean memoize) {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct mappingStruct = ctx.getSheet2RDFCore().getMappingStruct();
        SubjectHeader subjHeader = mappingStruct.getSubjectHeader();
        subjHeader.setId(headerId);
        //update the converter in the node conversion
        NodeConversion n = subjHeader.getNodeConversion();
        CODAConverter c = resolveCodaConverter(converterContract, RDFCapabilityType.uri);
        if (converterParams != null) {
            Map<String, Object> resolvedConvParams = resolveConverterParamsMap(converterParams);
            c.setParams(resolvedConvParams);
        }
        n.setConverter(c);
        n.setMemoize(memoize);
        //update the type in the graph application
        subjHeader.getGraphApplication().setType(type);
        //update the additional graph applicati
        List<SimpleGraphApplication> additionalGraphApplications = new ArrayList<>();
        for (Pair<IRI, Value> additionalPO: additionalPredObjs) {
            SimpleGraphApplication sga = new SimpleGraphApplication();
            sga.setNodeId(subjHeader.getNodeConversion().getNodeId());
            sga.setProperty(additionalPO.getFirst());
            sga.setValue(additionalPO.getSecond());
            additionalGraphApplications.add(sga);
        }
        subjHeader.setAdditionalGraphApplications(additionalGraphApplications);
    }

    /**
     * Returns a preview of the sheet containing the first <code>maxRows</code> rows
     *
     * @param maxRows
     * @return
     */
    @STServiceOperation
    public JsonNode getTablePreview(int maxRows) {
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
        for (int r = 0; r < rowsToReturn; r++) {
            ObjectNode rowJson = jf.objectNode();
            rowJson.set("idx", jf.numberNode(r + 1));
            ArrayNode cellsJsonArray = jf.arrayNode();
            rowJson.set("cells", cellsJsonArray);
            for (int c = 0; c < table.get(0).size(); c++) {
                String cellValue = table.get(r).get(c);
                ObjectNode cellJson = jf.objectNode();
                cellJson.set("idx", jf.numberNode(c + 1));//column index in excel spreadsheet
                cellJson.set("value", jf.textNode(cellValue));
                cellsJsonArray.add(cellJson);
            }
            rowsJsonArray.add(rowJson);
        }
        return respJson;
    }

    /**
     * Returns the PEARL code generated by Excel2RDF
     *
     * @return
     * @throws IOException
     */
    @STServiceOperation
    public JsonNode getPearl() throws IOException {
        File pearlFile = File.createTempFile("pearl", ".pr");
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        Sheet2RDFCore s2rdfCore = ctx.getSheet2RDFCore();
        s2rdfCore.generatePearlFile(pearlFile);
        ctx.setPearlFile(pearlFile);
        String pearl = S2RDFUtils.pearlFileToString(pearlFile);
        return JsonNodeFactory.instance.textNode(pearl);
    }

    /**
     * Saves/updates the PEARL code eventually edited by user
     *
     * @return
     * @throws FileNotFoundException
     */
    @STServiceOperation(method = RequestMethod.POST)
    public void savePearl(String pearlCode) throws IOException {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        OutputStream os = new FileOutputStream(ctx.getPearlFile());
        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        bf.write(pearlCode);
        bf.flush();
        bf.close();
    }

    /**
     * It validated the PEARL rule and returns the used prefixes in the graph section
     */
    @STServiceOperation(method = RequestMethod.POST)
    public JsonNode validateGraphPattern(String pearlCode, @Optional(defaultValue = "true") boolean rulesShouldExists) {
        InputStream pearlStream = new ByteArrayInputStream(pearlCode.getBytes(StandardCharsets.UTF_8));
        //PearlParser pearlParser = new PearlParser("", "");
        ParserPR pearlParser = new PearlParserAntlr4("", "");
        JsonNodeFactory jf = JsonNodeFactory.instance;
        ProjectionRulesModel prModel = null;
        ObjectNode respNode = jf.objectNode();
        boolean pearlValid;
        String details = null;
        try {
            prModel = pearlParser.parsePearlDocument(pearlStream, rulesShouldExists);
            pearlValid = true;
        } catch (PrefixNotDefinedException e) {
            pearlValid = false;
            details = "Prefix " + e.getPrefixName() + " is used but not defined";
        } catch (NodeNotDefinedException e) {
            pearlValid = false;
            details = "Node " + e.getPlcName() + " is used but not defined";
        } catch (PRParserException e) {
            pearlValid = false;
            details = "Syntactic error in the graph pattern";
        }
        respNode.set("valid", jf.booleanNode(pearlValid));
        respNode.set("details", jf.textNode(details));
        //get the list of used prefix in the graph section and returned it as an array
        ArrayNode usedPrefixJsonArray = jf.arrayNode();
        respNode.set("usedPrefixes", usedPrefixJsonArray);
        if (prModel != null) {
            List<String> usedPrefixList = prModel.getUsedPrefixedList();
            for (String usedPrefix : usedPrefixList) {
                usedPrefixJsonArray.add(usedPrefix);
            }
        }
        //get all placeholder from all the ProjectionRule and add their name in the json array of the response
        Set<String> nodeNameSet = new HashSet<>();
        ArrayNode usedNodesJsonArray = jf.arrayNode();
        respNode.set("usedNodes", usedNodesJsonArray);
        if (prModel != null) {
            for (String prId : prModel.getProjRule().keySet()) {
                ProjectionRule pr = prModel.getProjRuleFromId(prId);
                for (GraphElement graphElement : pr.getInsertGraphList()) {
                    getPlchNameFromGraphElement(graphElement, nodeNameSet);
                }
                for (GraphElement graphElement : pr.getDeleteGraphList()) {
                    getPlchNameFromGraphElement(graphElement, nodeNameSet);
                }
            }
        }
        for (String plchName : nodeNameSet) {
            usedNodesJsonArray.add(plchName);
        }
        return respNode;
    }

    private void getPlchNameFromGraphElement(GraphElement graphElement, Set<String> nodeNameSet) {
        if (graphElement.isGraphStruct()) {
            getUsedNodesFromGraphStruct((GraphStruct) graphElement, nodeNameSet);
        } else {
            //it is an optional struct
            getUsedNodesFromOptional((OptionalGraphStruct) graphElement, nodeNameSet);
        }
    }

    private void getUsedNodesFromGraphStruct(GraphStruct graphElement, Set<String> nodeNameSet) {
        getPlchName(graphElement.getSubject(), nodeNameSet);
        getPlchName(graphElement.getPredicate(), nodeNameSet);
        getPlchName(graphElement.getObject(), nodeNameSet);
    }

    private void getPlchName(GraphSingleElement gse, Set<String> nodeNameSet) {
        if (gse instanceof GraphSingleElemPlaceholder) {
            String plchName = ((GraphSingleElemPlaceholder) gse).getName();
            nodeNameSet.add(plchName);
        }
    }

    private void getUsedNodesFromOptional(OptionalGraphStruct optionalGraphElement, Set<String> nodeNameSet) {
        for (GraphElement graphElement : optionalGraphElement.getOptionalTriples()) {
            getPlchNameFromGraphElement(graphElement, nodeNameSet);
        }
    }

    /**
     * Uploads a pearl file into a server directory and returns the code
     *
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
     *
     * @param maxTableRows max number of table rows to consider in the triples preview (e.g. returns the triples
     *                     generated considering the first 10 rows)
     * @return
     * @throws QueryEvaluationException
     * @throws MalformedQueryException
     * @throws UnsupportedQueryLanguageException
     * @throws ConverterException
     * @throws ComponentProvisioningException
     * @throws PRParserException
     * @throws UIMAException
     * @throws DependencyException
     * @throws RDFModelNotSetException
     * @throws UnassignableFeaturePathException
     * @throws ProjectionRuleModelNotSet
     * @throws Exception
     */
    @STServiceOperation
    @Read
    public JsonNode getTriplesPreview(int maxTableRows) throws UIMAException, PRParserException,
            ComponentProvisioningException, ConverterException, DependencyException, RDFModelNotSetException,
            ProjectionRuleModelNotSet, UnassignableFeaturePathException {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        JCas jcas = ctx.getSheet2RDFCore().executeAnnotator();
        Sheet2RDFCODA s2rdfCoda = new Sheet2RDFCODA(getManagedConnection(), ctx.getCodaCore());
        List<SuggOntologyCoda> listSuggOntCoda = s2rdfCoda.suggestTriples(jcas, ctx.getPearlFile());
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
            List<CODATriple> tripleList = suggOntCoda.getAllInsertARTTriple();
            triplesPreviewCount = triplesPreviewCount + tripleList.size();
            tripleTotCount = tripleTotCount + tripleList.size();
            for (CODATriple t : tripleList) {
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
     *
     * @return
     */
    @STServiceOperation
    @Write
    public void addTriples() {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        List<SuggOntologyCoda> suggTriples = ctx.getCachedSuggestedTriples();
        RepositoryConnection connection = getManagedConnection();
        for (SuggOntologyCoda sugg : suggTriples) {
            List<CODATriple> triples = sugg.getAllInsertARTTriple();
            for (CODATriple t : triples) {
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
        rep.init();
        RepositoryConnection connection = rep.getConnection();

        for (SuggOntologyCoda sugg : suggTriples) {
            List<CODATriple> triples = sugg.getAllInsertARTTriple();
            for (CODATriple t : triples) {
                connection.add(t.getSubject(), t.getPredicate(), t.getObject());
            }
        }
        //serialize the temporary model on a server side temporary file
        File tempServerFile = File.createTempFile("triples", "." + outputFormat.getDefaultFileExtension());

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
    public Collection<PrefixMapping> getPrefixMappings() {
        List<PrefixMapping> prefMappings = new ArrayList<>();
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        Map<String, String> s2rdfPrefixMappings = ctx.getSheet2RDFCore().getMergedPrefixMapping();
        for (Entry<String, String> entry : s2rdfPrefixMappings.entrySet()) {
            prefMappings.add(new PrefixMapping(entry.getKey(), entry.getValue(), false));
        }
        return prefMappings;
    }

    /**
     * Serializes a JSON file that represents the status of the conversion.
     *
     * @param oRes
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    @STServiceOperation
    public void exportStatus(HttpServletResponse oRes) throws JsonGenerationException, JsonMappingException, IOException {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct ms = ctx.getSheet2RDFCore().getMappingStruct();
        File tempServerFile = File.createTempFile("sheet2rdf_status", ".json");
        try {
            StatusHandler statusHandler = new StatusHandler(ms);
            statusHandler.toJson(tempServerFile);
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
     * @param statusFile
     * @throws InvalidWizardStatusException Load a JSON file that represents the status of the conversion and restore the mapping struct
     * @throws IOException
     * @throws
     */
    @Read
    @STServiceOperation(method = RequestMethod.POST)
    public void importStatus(MultipartFile statusFile) throws IOException, InvalidWizardStatusException {
        S2RDFContext ctx = contextMap.get(stServiceContext.getSessionToken());
        MappingStruct ms = ctx.getSheet2RDFCore().getMappingStruct();
        File inputServerFile = File.createTempFile("sheet2rdf_status", statusFile.getOriginalFilename());
        statusFile.transferTo(inputServerFile);
        StatusHandler statusHandler = new StatusHandler(ms);
        statusHandler.fromJson(inputServerFile, getManagedConnection());
    }


    /**
     * Returns the default configurations references stored directly in the Sheet2RDF classpath
     *
     * @return
     */
    @STServiceOperation
    public Collection<Reference> getDefaultAdvancedGraphApplicationConfigurations() {
        ArrayList<Reference> references = new ArrayList<>();
        List<String> confList = GraphApplicationConfigurationLoader.getAvailableConfigurations();
        for (String c : confList) {
            references.add(new Reference(null, null, c));
        }
        return references;
    }

    /**
     * Returns the serialization of the configuration identified through the given identifier.
     *
     * @param identifier
     * @return
     * @throws STPropertyAccessException
     * @throws IOException
     */
    @STServiceOperation
    public Configuration getConfiguration(String identifier) throws STPropertyAccessException, IOException {
        String fileName = identifier + ".cfg";
        File configFile = GraphApplicationConfigurationLoader.getConfigurationFile(fileName);
        if (configFile == null) {
            throw new FileNotFoundException(fileName + " configuration file does not exist");
        }
        return STPropertiesManager.loadSTPropertiesFromYAMLFiles(StoredAdvancedGraphApplicationConfiguration.class, true, configFile);
    }


    @STServiceOperation
    public void closeSession() {
        if (stServiceContext.hasContextParameter("token")) {
            String token = stServiceContext.getSessionToken();
            contextMap.remove(token);
        }
    }

    /**
     * The converter parameters object should be a map key-value where the key are string and the values could be:
     * - String
     * - List<Object>: (e.g args of TurtleCollection converter)
     * actually it should be List<Value> but value can be represented by a rdf4j Value or by a nodeId (string).
     * - Map<String, Object> (e.g. args parameter of random converter)
     * actually it should be Map<String, Value> but value can be represented by a rdf4j Value or by a nodeId (string).
     * <p>
     * Unfortunately the Map<String, Object>, where Object could be a Map or a List, cannot be parsed, by a spring converter.
     * So as a workaround I get a Map<String, String> and here I manually parse the values.
     */
    private Map<String, Object> resolveConverterParamsMap(Map<String, String> convParamsMap) {
        Map<String, Object> resolvedConvParams = new LinkedHashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Iterator<Entry<String, String>> itEntries = convParamsMap.entrySet().iterator();
        while (itEntries.hasNext()) {
            Entry<String, String> entry = itEntries.next();
            String value = entry.getValue();
            if (value.startsWith("{")) { //value is a map => convert it
                try {
                    Map<String, String> valueAsMap = mapper.readValue(value, new TypeReference<Map<String, String>>() {
                    });
                    //resolve in turn the map value
                    Map<String, Object> nestedResolvedMap = resolveConverterParamsMap(valueAsMap);
                    resolvedConvParams.put(entry.getKey(), nestedResolvedMap);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (value.startsWith("[")) { //value is a list
                try {
                    List<String> valueAsList = mapper.readValue(value, new TypeReference<List<String>>() {
                    });
                    //resolve in turn the list value
                    List<Object> nestedResolvedList = new ArrayList<>();
                    for (String v : valueAsList) {
                        nestedResolvedList.add(parseStringOrValue(v));
                    }
                    resolvedConvParams.put(entry.getKey(), nestedResolvedList);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            } else { //simple string or rdf4j Ntriples serialization?
                Object resolvedValue = parseStringOrValue(value);
                resolvedConvParams.put(entry.getKey(), resolvedValue);
            }
        }
        return resolvedConvParams;
    }

    /**
     * Initializes an instance of {@link CODAConverter} for the given contract and with the specified capability.
     * (Note: the capability is necessary for distinguish the usage of those converter that can produces node, namely
     * both uri and literal nodes). The instantiated converter is initialized also with the datatpe capability that is
     * useful in Sheet2RDF client side.
     *
     * @param converterContract
     * @param converterCapability
     * @return
     */
    private CODAConverter resolveCodaConverter(String converterContract, RDFCapabilityType converterCapability) {
        CODAConverter converter = null;
        CODACore codaCore = getInitializedCodaCore(getManagedConnection());
        for (ConverterContractDescription aDescr : codaCore.listConverterContracts()) {
            if (converterContract.equals(aDescr.getContractURI())) {
                converter = new CODAConverter(converterCapability, converterContract);
                Set<IRI> datatypes = aDescr.getDatatypes();
                if (datatypes.size() > 0) { //even if getDatatypes returns a list, I can assume a converter has (at max) only one declared datatype
                    converter.setDatatypeCapability(datatypes.iterator().next().stringValue());
                }
            }
        }
        shutDownCodaCore(codaCore);
        return converter;
    }

    /**
     * Try to parse a string as a rdf4j Value. If the parsing fails, returns it as a plain String.
     *
     * @param s
     * @return
     */
    private Object parseStringOrValue(String s) {
        try {
            return NTriplesUtil.parseValue(s, SimpleValueFactory.getInstance());
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

}
