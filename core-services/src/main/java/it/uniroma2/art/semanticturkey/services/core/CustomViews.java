package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.customview.AdvSingleValueView;
import it.uniroma2.art.semanticturkey.config.customview.AreaView;
import it.uniroma2.art.semanticturkey.config.customview.CustomView;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewAssociation;
import it.uniroma2.art.semanticturkey.config.customview.DynamicVectorView;
import it.uniroma2.art.semanticturkey.config.customview.PointView;
import it.uniroma2.art.semanticturkey.config.customview.PropertyChainView;
import it.uniroma2.art.semanticturkey.config.customview.RouteView;
import it.uniroma2.art.semanticturkey.config.customview.SeriesCollectionView;
import it.uniroma2.art.semanticturkey.config.customview.SeriesView;
import it.uniroma2.art.semanticturkey.config.customview.StaticVectorView;
import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@STService
public class CustomViews extends STServiceAdapter  {

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

    /* ==== ASSOCIATIONS ==== */

    @STServiceOperation()
    public JsonNode listAssociations() throws ConfigurationNotFoundException {
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ArrayNode associationJsonArray = jsonFactory.arrayNode();

        CustomViewsManager wm = projCvMgr.getCustomViewManager(getProject());
        Map<String, CustomViewAssociation> associationsMap = wm.listRefAssociationsMap();
        for (String ref : associationsMap.keySet()) {
            CustomViewAssociation association = associationsMap.get(ref);
            IRI property = association.property;
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
    @STServiceOperation()
    public List<CustomViewData> getViewData(Resource resource, IRI property) throws STPropertyAccessException, NoSuchConfigurationManager {
        ObjectMapper objectMapper = new ObjectMapper();

        List<CustomViewData> dataList = new ArrayList<>();

        CustomViewsManager cvMgr = new CustomViewsManager(getProject(), exptManager);
        CustomView customView = cvMgr.getCustomViewsForProperty(property);
//        if (customView != null) { //cv should always exist since this API should be invoked from the client only in such case
//
//            String retrieveQuery = customView.retrieve;
//
//            TupleQuery tq = getManagedConnection().prepareTupleQuery(retrieveQuery);
//            tq.setBinding("resource", resource);
//            tq.setBinding("trigprop", property);
//
//            SimpleDataset dataset = new SimpleDataset();
//            dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
//            dataset.addDefaultGraph((IRI) getWorkingGraph());
//            dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
//            tq.setDataset(dataset);
//
//            TupleQueryResult results = tq.evaluate();
//
//            /**
//             * according to the type, here I need to group the returned data into the response
//             * - point: location, latitude, longitude; no need to group, each record is a single data set
//             * - area/route: route_id, location, latitude, longitude; grouped by polyline_id
//             * - series: series_id, series_label, value_label, name, value; grouped by series_id
//             * - series_collection: series_collection_id, series_label, value_label, series_name, name, value; grouped by series_collection_id
//             */
//            //first cache the results into a list of mappings binding-value
//            List<Map<String, AnnotatedValue<Value>>> records = new ArrayList<>();
//            while (results.hasNext()) {
//                Map<String, AnnotatedValue<Value>> record = new HashMap<>();
//                BindingSet bs = results.next();
//                bs.forEach(b-> {
//                    record.put(b.getName(), new AnnotatedValue<>(b.getValue()));
//                });
//                records.add(record);
//            }
//            //iterate over the cache and add the records to the widgetData list grouped by the id value
//            for (Map<String, AnnotatedValue<Value>> record : records) {
//                //look for the widget data for the given id value (if already collected)
//                CustomViewData vd = dataList.stream().filter(data -> {
//                    AnnotatedValue<Value> recordId = record.get(customView.getIdBinding().name());
//                    AnnotatedValue<Value> wdId = data.getBindingValue(customView.getIdBinding().name());
//                    return recordId.getValue().equals(wdId.getValue());
//                }).findFirst().orElse(null);
//                if (vd != null) {
//                    //if the given ID value has been already collected/grouped, add a new record to it
//                    vd.addBindings(record);
//                } else { //otherwise create a new WidgetData
//                    vd = new CustomViewData(customView.getModelType());
//                    vd.addBindings(record);
//                    dataList.add(vd);
//                }
//            }
//        }
        return dataList;
    }
//
//    @Write
//    @STServiceOperation(method = RequestMethod.POST)
//    @PreAuthorize("@auth.isAuthorized('rdf(resource)', 'U')")
//    public void updateWidgetData(Resource resource, IRI predicate, Map<WidgetDataBindings, Value> bindings) throws NoSuchConfigurationManager, STPropertyAccessException {
//        WidgetsManager wm = new WidgetsManager(getProject(), exptManager);
//        Widget widget = wm.getWidgetForTrigger(predicate);
//        if (widget != null) { //widget should always exist since this API should be invoked from the client only in such case
//
//            //check if values for the required bindings are provided
//            for (WidgetDataBindings b: widget.getUpdateMandatoryBindings()) {
//                if (!bindings.containsKey(b)) {
//                    throw new IllegalArgumentException("Missing value for required binding " + b.toString());
//                }
//            }
//
//            String updateQuery = widget.update;
//            updateQuery = updateQuery.replace("<trigger>", RenderUtils.toSPARQL(predicate));
//            Update update = getManagedConnection().prepareUpdate(updateQuery);
//
//            for (Entry<WidgetDataBindings, Value> binding: bindings.entrySet()) {
//                update.setBinding(binding.getKey().toString(), binding.getValue());
//            }
//            update.setBinding("resource", resource);
//
//            SimpleDataset dataset = new SimpleDataset();
//            dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
//            dataset.addDefaultGraph((IRI) getWorkingGraph());
//            dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
//            update.setDataset(dataset);
//
//            update.execute();
//        }
//    }


}
