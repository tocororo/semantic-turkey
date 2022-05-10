package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.GraphElement;
import it.uniroma2.art.coda.pearl.model.GraphStruct;
import it.uniroma2.art.coda.pearl.model.OptionalGraphStruct;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemBNode;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemPlaceholder;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemUri;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemVar;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElement;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.customview.AbstractSparqlBasedCustomView;
import it.uniroma2.art.semanticturkey.config.customview.AdvSingleValueView;
import it.uniroma2.art.semanticturkey.config.customview.AreaView;
import it.uniroma2.art.semanticturkey.config.customview.CustomView;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewAssociation;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewDataBindings;
import it.uniroma2.art.semanticturkey.config.customview.DynamicVectorView;
import it.uniroma2.art.semanticturkey.config.customview.PointView;
import it.uniroma2.art.semanticturkey.config.customview.PropertyChainView;
import it.uniroma2.art.semanticturkey.config.customview.RouteView;
import it.uniroma2.art.semanticturkey.config.customview.SeriesCollectionView;
import it.uniroma2.art.semanticturkey.config.customview.SeriesView;
import it.uniroma2.art.semanticturkey.config.customview.StaticVectorView;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@STService
public class CustomViews extends STServiceAdapter {

    //TODO authorizations for all the services

    @Autowired
    protected ExtensionPointManager exptManager;

    @Autowired
    ProjectCustomViewsManager projCvMgr;

    @STServiceOperation
    public Collection<String> getViewsIdentifiers() {
        return projCvMgr.getCustomViewManager(getProject()).getCustomViewsIdentifiers();
    }

    /* ==== VIEWS ==== */

    @STServiceOperation(method = RequestMethod.POST)
    public void createCustomView(String reference, @JsonSerialized ObjectNode definition, CustomViewModelEnum model)
            throws IOException, WrongPropertiesException, STPropertyUpdateException {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        if (cvMgr.customViewExists(reference)) {
            throw new IllegalArgumentException("A CustomView with the same ID already exists");
        }
        this.storeCustomView(reference, definition, model);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void updateCustomView(String reference, @JsonSerialized ObjectNode definition, CustomViewModelEnum model)
            throws IOException, WrongPropertiesException, STPropertyUpdateException {
        this.storeCustomView(reference, definition, model);
    }

    private void storeCustomView(String reference, @JsonSerialized ObjectNode definition, CustomViewModelEnum model)
            throws IOException, WrongPropertiesException, STPropertyUpdateException {
        ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
        CustomView cv;
        if (model.equals(CustomViewModelEnum.area)) {
            cv = mapper.treeToValue(definition, AreaView.class);
        } else if (model.equals(CustomViewModelEnum.point)) {
            cv = mapper.treeToValue(definition, PointView.class);
        } else if (model.equals(CustomViewModelEnum.route)) {
            cv = mapper.treeToValue(definition, RouteView.class);
        } else if (model.equals(CustomViewModelEnum.series)) {
            cv = mapper.treeToValue(definition, SeriesView.class);
        } else if (model.equals(CustomViewModelEnum.series_collection)) {
            cv = mapper.treeToValue(definition, SeriesCollectionView.class);
        } else if (model.equals(CustomViewModelEnum.property_chain)) {
            cv = mapper.treeToValue(definition, PropertyChainView.class);
        } else if (model.equals(CustomViewModelEnum.adv_single_value)) {
            cv = mapper.treeToValue(definition, AdvSingleValueView.class);
        } else if (model.equals(CustomViewModelEnum.static_vector)) {
            cv = mapper.treeToValue(definition, StaticVectorView.class);
        } else if (model.equals(CustomViewModelEnum.dynamic_vector)) {
            cv = mapper.treeToValue(definition, DynamicVectorView.class);
        } else {
            throw new IllegalArgumentException("Invalid model: " + model);
        }
        Reference ref = parseReference(reference);
        projCvMgr.getCustomViewManager(getProject()).storeCustomView(ref, cv);
    }



    @STServiceOperation(method = RequestMethod.POST)
    public void deleteCustomView(String reference) throws ConfigurationNotFoundException {
        projCvMgr.getCustomViewManager(getProject()).deleteCustomView(parseReference(reference));
    }

    @STServiceOperation
    public CustomView getCustomView(String reference) {
        return projCvMgr.getCustomViewManager(getProject()).getCustomView(reference);
    }

    /**
     * Suggests a sparql select for a dynamic-vector CV starting from the graph section of the provided CF
     * @param cfId ID of a CustomForm with type graph
     * @return
     * @throws PRParserException
     */
    @STServiceOperation
    @Read
    public String suggestDynamicVectorCVFromCustomForm(String cfId) throws PRParserException {
        CustomForm cForm = cfManager.getCustomForm(getProject(), cfId);
        if (cForm != null && cForm.isTypeGraph()) {
            CustomFormGraph cFormGraph = (CustomFormGraph) cForm;

            RepositoryConnection repoConn = getManagedConnection();
            CODACore codaCore = getInitializedCodaCore(repoConn);

            Collection<GraphElement> pearlGraphSection = cFormGraph.getGraphSection(codaCore);
            Set<String> objectPlaceholders = new LinkedHashSet<>();
            collectObjectPHsInGraphElements(pearlGraphSection, objectPlaceholders);

            Map<String, String> phReplacementMap = new HashMap<>();
            String entryPointPh = cFormGraph.getEntryPointPlaceholder(codaCore);
            phReplacementMap.put(entryPointPh, "?pivot");
            for (String ph : objectPlaceholders) {
                phReplacementMap.put(ph, ph.replace("$", "?") + "_value");
            }

            Map<String, String> prefixNsMapping = new HashMap<>();
            Iterations.stream(repoConn.getNamespaces()).forEach(ns -> prefixNsMapping.put(ns.getPrefix(), ns.getName()));

            Set<String> requiredPrefix = new HashSet<>(); //collects the prefixes used in the query in order to be declared later

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * WHERE { \n");
            sb.append("\t$resource $trigprop ?pivot . \n");
            computeAndAppendGraphSectionQuery(pearlGraphSection, sb, phReplacementMap, prefixNsMapping, requiredPrefix, 1);
            sb.append("}");

            //prepend the prefix declaration
            String query = "";
            for (String pref: requiredPrefix) {
                query += "PREFIX " + pref + ":\t<" + prefixNsMapping.get(pref) + ">\n";
            }
            if (!requiredPrefix.isEmpty()) {
                query += "\n";
            }
            query += sb;
            return query;
        } else {
            throw new IllegalArgumentException(
                    "CustomForm with id " + cfId + " not found or not a valid CustomForm of type graph");
        }
    }

    /**
     * Suggests a sparql select for an adv-single-value CV starting from the graph section of the provided CF
     * @param cfId ID of a CustomForm with type graph
     * @param chosenPh specifies the placeholder that represents the value to be shown in the view
     *                 (required when the CF has multiple placeholders as object in graph section. In such case,
     *                 if this parameter is not provided, an IllegalStateException is thrown)
     * @return
     * @throws PRParserException
     */
    @STServiceOperation
    @Read
    public String suggestAdvSingleValueCVFromCustomForm(String cfId, @Optional String chosenPh) throws PRParserException {
        CustomForm cForm = cfManager.getCustomForm(getProject(), cfId);
        if (cForm != null && cForm.isTypeGraph()) {
            CustomFormGraph cFormGraph = (CustomFormGraph) cForm;

            RepositoryConnection repoConn = getManagedConnection();
            CODACore codaCore = getInitializedCodaCore(repoConn);

            Collection<GraphElement> pearlGraphSection = cFormGraph.getGraphSection(codaCore);
            Set<String> objectPlaceholders = new LinkedHashSet<>();
            collectObjectPHsInGraphElements(pearlGraphSection, objectPlaceholders);

            String targetObj = null;

            if (objectPlaceholders.size() == 1) {
                targetObj = objectPlaceholders.iterator().next();
            } else if (objectPlaceholders.size() > 1) {
                if (chosenPh != null) {
                    if (objectPlaceholders.contains(chosenPh)) {
                        targetObj = chosenPh;
                    } else {
                        throw new IllegalArgumentException("The provided placeholder " + chosenPh + " never appears as object in the CustomForm pearl.");
                    }
                } else {
                    throw new IllegalStateException("Multiple placeholders as object in graph section of CustomForm " + cfId + ". Expected parameter chosenPh not provided.");
                }
            }

            Map<String, String> phReplacementMap = new HashMap<>();
            String entryPointPh = cFormGraph.getEntryPointPlaceholder(codaCore);
            phReplacementMap.put(entryPointPh, "?pivot");
            phReplacementMap.put(targetObj, "$value");

            Map<String, String> prefixNsMapping = new HashMap<>();
            Iterations.stream(repoConn.getNamespaces()).forEach(ns -> prefixNsMapping.put(ns.getPrefix(), ns.getName()));

            Set<String> requiredPrefix = new HashSet<>(); //collects the prefixes used in the query in order to be declared later

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * WHERE { \n");
            sb.append("\t$resource $trigprop ?pivot . \n");
            List<GraphElement> pathToTargetObj = new ArrayList<>();
            computePathToObject(pearlGraphSection, entryPointPh, targetObj, pathToTargetObj);
            computeAndAppendGraphSectionQuery(pathToTargetObj, sb, phReplacementMap, prefixNsMapping, requiredPrefix, 1);
            sb.append("}");

            //prepend the prefix declaration
            String query = "";
            for (String pref: requiredPrefix) {
                query += "PREFIX " + pref + ":\t<" + prefixNsMapping.get(pref) + ">\n";
            }
            if (!requiredPrefix.isEmpty()) {
                query += "\n";
            }
            query += sb;
            return query;
        } else {
            throw new IllegalArgumentException(
                    "CustomForm with id " + cfId + " not found or not a valid CustomForm of type graph");
        }
    }

    /**
     * Returns the list of placeholders that appear as objects of triples in the graph section of the provided CF.
     * This API can be invoked before {@link #suggestAdvSingleValueCVFromCustomForm(String, String)} in order
     * to check if the optional param <code>chosenPh</code> needs to be provided
     * @param cfId
     * @return
     * @throws PRParserException
     */
    @STServiceOperation
    @Read
    public Set<String> getValueCandidates(String cfId) throws PRParserException {
        CustomForm cForm = cfManager.getCustomForm(getProject(), cfId);
        if (cForm != null && cForm.isTypeGraph()) {
            CustomFormGraph cFormGraph = (CustomFormGraph) cForm;
            CODACore codaCore = getInitializedCodaCore(getManagedConnection());
            Set<String> objectPlaceholders = new LinkedHashSet<>();
            collectObjectPHsInGraphElements(cFormGraph.getGraphSection(codaCore), objectPlaceholders);
            return objectPlaceholders;
        } else {
            throw new IllegalArgumentException(
                    "CustomForm with id " + cfId + " not found or not a valid CustomForm of type graph");
        }
    }

    /**
     * Returns all the placeholders that appears as object in the graph pearl section
     * @param graphElements
     * @param objectPlaceholders
     */
    private void collectObjectPHsInGraphElements(Collection<GraphElement> graphElements, Set<String> objectPlaceholders) {
        for (GraphElement graphElem : graphElements) {
            if (!graphElem.isOptionalGraphStruct()) {
                GraphStruct gs = graphElem.asGraphStruct();
                GraphSingleElement gsObj = gs.getObject();
                if (gsObj instanceof GraphSingleElemPlaceholder || gsObj instanceof GraphSingleElemVar) {
                    objectPlaceholders.add(gsObj.getValueAsString());
                }
            } else { // Optional
                OptionalGraphStruct optGS = graphElem.asOptionalGraphStruct();
                collectObjectPHsInGraphElements(optGS.getOptionalTriples(), objectPlaceholders);
            }
        }
    }

    /**
     * Given a graph section (list of graph elements), converts it to a SPARQL query.
     * The conversion replaces PEARL placeholder/variables with SPARQL variables according the provided replacement map.
     * @param graphElements
     * @param sb
     * @param phReplacementMap
     * @param prefixMapping
     * @param requiredPrefix set of prefixes used in the sparql query
     * @param indent
     */
    private void computeAndAppendGraphSectionQuery(Collection<GraphElement> graphElements, StringBuilder sb,
            Map<String, String> phReplacementMap, Map<String, String> prefixMapping, Set<String> requiredPrefix, int indent) {
        for (GraphElement graphElem : graphElements) {
            if (!graphElem.isOptionalGraphStruct()) {
                GraphStruct gs = graphElem.asGraphStruct();
                String subj = getSingleValueAsString(gs.getSubject(), prefixMapping, requiredPrefix);
                String pred = getSingleValueAsString(gs.getPredicate(), prefixMapping, requiredPrefix);
                String obj = getSingleValueAsString(gs.getObject(), prefixMapping, requiredPrefix);

                for (Map.Entry<String, String> phReplacement : phReplacementMap.entrySet()) {
                    subj = subj.replace(phReplacement.getKey(), phReplacement.getValue());
                    pred = pred.replace(phReplacement.getKey(), phReplacement.getValue());
                    obj = obj.replace(phReplacement.getKey(), phReplacement.getValue());
                }
                //rewrite the placeholders ($foo) as variable (?foo) (skip those PHs written as $foo on purpose in the replacement map, like $value)
                if (subj.startsWith("$") && !phReplacementMap.containsValue(subj)) {
                    subj = subj.replace("$", "?");
                }
                if (pred.startsWith("$") && !phReplacementMap.containsValue(pred)) {
                    pred = pred.replace("$", "?");
                }
                if (obj.startsWith("$") && !phReplacementMap.containsValue(obj)) {
                    obj = obj.replace("$", "?");
                }

                appendIndentation(sb, indent);
                sb.append(subj + " " + pred + " " + obj + " .\n");
            } else { // Optional
                OptionalGraphStruct optGS = graphElem.asOptionalGraphStruct();

                appendIndentation(sb, indent);
                sb.append("OPTIONAL {\n");
                indent += 1;
                computeAndAppendGraphSectionQuery(optGS.getOptionalTriples(), sb, phReplacementMap, prefixMapping, requiredPrefix, indent);
                indent -= 1;
                appendIndentation(sb, indent);
                sb.append("}\n");
            }
        }
    }

    /**
     * Compute a path of triples (graph elements) that goes from the provided entry point to the target object
     * @param graphElements
     * @param entryPoint
     * @param targetObject
     * @param path the path that is populated
     */
    private void computePathToObject(Collection<GraphElement> graphElements, String entryPoint, String targetObject, List<GraphElement> path) {
        for (GraphElement graphElem : graphElements) {
            if (!graphElem.isOptionalGraphStruct()) {
                GraphStruct gs = graphElem.asGraphStruct();
                //no need to check the class of object, if equals returns true, it means that the object was a var (?foo) or ph ($bar)
                if (gs.getObject().getValueAsString().equals(targetObject)) {
                    //target object found
                    path.add(0, graphElem);
                    if (!gs.getSubject().getValueAsString().equals(entryPoint)) {
                        computePathToObject(graphElements, entryPoint, gs.getSubject().getValueAsString(), path);
                    }
                }
            } else { // Optional
                OptionalGraphStruct optGS = graphElem.asOptionalGraphStruct();
                computePathToObject(optGS.getOptionalTriples(), entryPoint, targetObject, path);
            }
        }
    }

    /**
     * Returns a string representation of a GraphSingleElement. In case the element is an IRI, tries to collapse it as qname.
     * @param elem
     * @param prefixMapping
     * @param requiredPrefix
     * @return
     */
    private String getSingleValueAsString(GraphSingleElement elem, Map<String, String> prefixMapping, Set<String> requiredPrefix) {
        if (elem instanceof GraphSingleElemBNode) {
            return "?bnodeVar_" + ((GraphSingleElemBNode) elem).getBnodeIdentifier();
        } else if (elem instanceof GraphSingleElemUri) {
            GraphSingleElemUri elemUri = (GraphSingleElemUri)elem;
            //check if URI can be "qNamed"
            for (Map.Entry<String, String> prefNs : prefixMapping.entrySet()) {
                if (elemUri.getURI().startsWith(prefNs.getValue())) {
                    requiredPrefix.add(prefNs.getKey()); //add the prefix to the required list, so that it will be declared in query
                    return elemUri.getURI().replace(prefNs.getValue(), prefNs.getKey() + ":");
                }
            }
            return elem.getValueAsString();
        } else {
            return elem.getValueAsString();
        }
    }

    private void appendIndentation(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("\t");
        }
    }


    /* ==== ASSOCIATIONS ==== */

    @STServiceOperation()
    public JsonNode listAssociations() throws ConfigurationNotFoundException {
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ArrayNode associationJsonArray = jsonFactory.arrayNode();

        CustomViewsManager wm = projCvMgr.getCustomViewManager(getProject());
        Map<String, CustomViewAssociation> associationsMap = wm.listRefAssociationsMap();
        for (String ref : associationsMap.keySet()) {
            CustomViewAssociation association = associationsMap.get(ref);
            String widgetRef = association.customViewRef;
            if (wm.customViewExists(widgetRef)) {
                ObjectNode associationNode = jsonFactory.objectNode();
                associationNode.put("ref", ref);
                associationNode.put("property", association.property.stringValue());
                associationNode.put("customViewRef", association.customViewRef);
                associationJsonArray.add(associationNode);
            } else { //widget doesn't exist (in might have been deleted manually) => delete the association
                wm.deleteAssociation(parseReference(ref));
            }
        }
        return associationJsonArray;
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void addAssociation(IRI property, String customViewRef, ViewsEnum defaultView) throws STPropertyUpdateException,
            WrongPropertiesException, IOException, ConfigurationNotFoundException {
        projCvMgr.getCustomViewManager(getProject()).storeAssociation(property, customViewRef, defaultView);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void deleteAssociation(String reference) throws ConfigurationNotFoundException {
        projCvMgr.getCustomViewManager(getProject()).deleteAssociation(parseReference(reference));
    }

    /* ==== DATA ==== */

    @Read
    @STServiceOperation
    public CustomViewData getViewData(Resource resource, IRI property) {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        CustomView customView = cvMgr.getCustomViewForProperty(property);
        //no need to check if customView is not null since this API should be invoked from the client only in such case
        return customView.getData(getManagedConnection(), resource, property, (IRI) getWorkingGraph());
    }

    @Write
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', values)', 'U')")
    public void updateSparqlBasedData(Resource resource, IRI property, Map<CustomViewDataBindings, Value> bindings) {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());

        //by construction, this service should be invoked only if the property has a sparql-based custom view associated
        //(e.g. maps/charts views), so I avoid checks for null CV and cast to AbstractSparqlBasedCustomView
        AbstractSparqlBasedCustomView customView = (AbstractSparqlBasedCustomView) cvMgr.getCustomViewForProperty(property);
        //check if values for the required bindings are provided
        for (CustomViewDataBindings b : customView.getUpdateMandatoryBindings()) {
            if (!bindings.containsKey(b)) {
                throw new IllegalArgumentException("Missing value for required binding " + b.toString());
            }
        }

        String updateQuery = customView.update;
        Update update = getManagedConnection().prepareUpdate(updateQuery);

        //bind placeholders and provided variables
        update.setBinding("resource", resource);
        update.setBinding("trigprop", property);
        for (Map.Entry<CustomViewDataBindings, Value> binding : bindings.entrySet()) {
            update.setBinding(binding.getKey().toString(), binding.getValue());
        }

        SimpleDataset dataset = new SimpleDataset();
        dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
        dataset.addDefaultGraph((IRI) getWorkingGraph());
        dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
        update.setDataset(dataset);

        update.execute();
    }

    @Write
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', values)', '{lang: [''' +@auth.langof(#oldValue)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
    public void updateSingleValueData(Resource resource, IRI property, Value oldValue, Value newValue, @Optional Map<String, Value> pivots) {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        CustomView cv = cvMgr.getCustomViewForProperty(property);

        if (cv instanceof AdvSingleValueView) {
            ((AdvSingleValueView) cv).updateData(getManagedConnection(), resource, property, oldValue, newValue, pivots, (IRI) getWorkingGraph());
        } else if (cv instanceof PropertyChainView) {
            ((PropertyChainView) cv).updateData(getManagedConnection(), resource, property, oldValue, newValue, (IRI) getWorkingGraph());
        }
    }

    @Write
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', values)', '{lang: [''' +@auth.langof(#oldValue)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
    public void updateStaticVectorData(Resource resource, IRI property, IRI fieldProperty, Value oldValue, Value newValue) {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        StaticVectorView cv = (StaticVectorView) cvMgr.getCustomViewForProperty(property);

        cv.updateData(getManagedConnection(), resource, property, fieldProperty, oldValue, newValue, (IRI) getWorkingGraph());
    }

    @Write
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', values)', '{lang: [''' +@auth.langof(#oldValue)+ ''', ''' +@auth.langof(#newValue)+ ''']}', 'U')")
    public void updateDynamicVectorData(Resource resource, IRI property, String fieldName, Value oldValue, Value newValue, Map<String, Value> pivots) {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        DynamicVectorView cv = (DynamicVectorView) cvMgr.getCustomViewForProperty(property);

        cv.updateData(getManagedConnection(), resource, property, fieldName, oldValue, newValue, pivots, (IRI) getWorkingGraph());
    }



    @STServiceOperation
//    @PreAuthorize("@auth.isAuthorized('cform(form)', 'R')")
    public void exportCustomView(HttpServletResponse oRes, String reference)
            throws IOException {
        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        CustomView cv = cvMgr.getCustomView(reference);
        if (cv == null) {
            throw new IllegalArgumentException(
                    "Impossible to export '" + reference + "'. A CustomView for this reference doesn't exists");
        }

        Reference ref = parseReference(reference);
        File cvFile = cvMgr.getCustomViewFile(ref.getIdentifier());

        oRes.setHeader("Content-Disposition", "attachment; filename=" + ref.getIdentifier() + ".cfg");
        oRes.setContentType("text/plain");
        oRes.setContentLength((int) cvFile.length());
        try (InputStream is = new FileInputStream(cvFile)) {
            IOUtils.copy(is, oRes.getOutputStream());
        }
        oRes.flushBuffer();
    }

    @STServiceOperation(method = RequestMethod.POST)
//    @PreAuthorize("@auth.isAuthorized('cform(form)', 'C')")
    public void importCustomView(MultipartFile inputFile, String reference) throws IOException, STPropertyAccessException {

        CustomViewsManager cvMgr = projCvMgr.getCustomViewManager(getProject());
        if (cvMgr.customViewExists(reference)) {
            throw new IllegalArgumentException("A CustomView with the same ID already exists");
        }

        // create a temp file (in karaf data/temp folder) to copy the received file
        File tempServerFile = File.createTempFile("cvImport", inputFile.getOriginalFilename());
        try {
            Reference ref = parseReference(reference);
            inputFile.transferTo(tempServerFile);
            cvMgr.storeCustomViewFromFile(ref, tempServerFile);
        } finally {
            tempServerFile.delete();
        }
    }


}
