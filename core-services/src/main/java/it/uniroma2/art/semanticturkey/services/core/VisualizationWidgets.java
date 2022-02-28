package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.AreaWidget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.PointWidget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.RouteWidget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.SeriesCollectionWidget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.SeriesWidget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.Widget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.WidgetAssociation;
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
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ProjectWidgetsManager;
import it.uniroma2.art.semanticturkey.widgets.WidgetData;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;
import it.uniroma2.art.semanticturkey.widgets.WidgetsManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@STService
public class VisualizationWidgets extends STServiceAdapter  {

    @Autowired
    protected ExtensionPointManager exptManager;

    @Autowired
    ProjectWidgetsManager projWidgetMgr;


    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'C')")
    public void createWidget(String reference, @JsonSerialized ObjectNode definition, Widget.DataType type)
            throws IOException, WrongPropertiesException, STPropertyUpdateException {
        ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
        Widget widget;
        if (type.equals(Widget.DataType.area)) {
            widget = mapper.treeToValue(definition, AreaWidget.class);
        } else if (type.equals(Widget.DataType.point)) {
            widget = mapper.treeToValue(definition, PointWidget.class);
        } else if (type.equals(Widget.DataType.route)) {
            widget = mapper.treeToValue(definition, RouteWidget.class);
        } else if (type.equals(Widget.DataType.series)) {
            widget = mapper.treeToValue(definition, SeriesWidget.class);
        } else { //if (type.equals(Widget.DataType.series_collection)) {
            widget = mapper.treeToValue(definition, SeriesCollectionWidget.class);
        }
        Reference ref = parseReference(reference);
        projWidgetMgr.getWidgetManager(getProject()).storeWidget(ref, widget);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'D')")
    public void deleteWidget(String reference) throws ConfigurationNotFoundException {
        projWidgetMgr.getWidgetManager(getProject()).deleteWidget(parseReference(reference));
    }


    @STServiceOperation
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'R')")
    public Collection<String> getWidgetIdentifiers() {
        return projWidgetMgr.getWidgetManager(getProject()).getWidgetIdentifiers();
    }

    @STServiceOperation
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'R')")
    public Widget getWidget(String reference) {
        return projWidgetMgr.getWidgetManager(getProject()).getWidget(reference);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'C')")
    public void addAssociation(IRI predicate, String widgetRef) throws STPropertyUpdateException,
            WrongPropertiesException, IOException, ConfigurationNotFoundException {
        WidgetsManager wm = projWidgetMgr.getWidgetManager(getProject());
        wm.storeAssociation(predicate, widgetRef);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'D')")
    public void deleteAssociation(String reference) throws ConfigurationNotFoundException {
        projWidgetMgr.getWidgetManager(getProject()).deleteAssociation(parseReference(reference));
    }

    @STServiceOperation()
    @PreAuthorize("@auth.isAuthorized('pm(widget)', 'R')")
    public JsonNode listAssociations() throws ConfigurationNotFoundException {
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ArrayNode associationJsonArray = jsonFactory.arrayNode();

        WidgetsManager wm = projWidgetMgr.getWidgetManager(getProject());
        Map<String, WidgetAssociation> associationsMap = wm.listRefAssociationsMap();
        for (String ref : associationsMap.keySet()) {
            WidgetAssociation association = associationsMap.get(ref);
            IRI trigger = association.trigger;
            String widgetRef = association.widgetReference;
            if (wm.widgetExists(widgetRef)) {
                ObjectNode associationNode = jsonFactory.objectNode();
                associationNode.put("ref", ref);
                associationNode.put("trigger", association.trigger.stringValue());
                associationNode.put("widgetRef", association.widgetReference);
                associationJsonArray.add(associationNode);
            } else { //widget doesn't exist (in might have been deleted manually) => delete the association
                wm.deleteAssociation(parseReference(ref));
            }
        }
        return associationJsonArray;
    }

//    @Read
//    @STServiceOperation()
//    @PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
//    public JsonNode getVisualizationData(Resource resource, IRI predicate) throws STPropertyAccessException, NoSuchConfigurationManager {
//        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
//
//        ArrayNode resp = jsonFactory.arrayNode();
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        WidgetsManager wm = new WidgetsManager(getProject(), exptManager);
//        Widget widget = wm.getWidgetForTrigger(predicate);
//        if (widget != null) { //widget should always exist since this API should be invoked from the client only in such case
//            String retrieveQuery = widget.retrieve;
//            retrieveQuery = retrieveQuery.replace("<trigger>", RenderUtils.toSeRQL(predicate));
//
//            TupleQuery tq = getManagedConnection().prepareTupleQuery(retrieveQuery);
//            tq.setBinding("resource", resource);
//
//            SimpleDataset dataset = new SimpleDataset();
//            dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
//            dataset.addDefaultGraph((IRI) getWorkingGraph());
//            dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
//            tq.setDataset(dataset);
//
//            TupleQueryResult results = tq.evaluate();
//            while (results.hasNext()) {
//                ObjectNode record = jsonFactory.objectNode();
//                BindingSet bs = results.next();
//                bs.forEach(b-> {
//                    Value value = b.getValue();
//                    AnnotatedValue<Value> av = new AnnotatedValue<>(value);
//                    record.set(b.getName(), objectMapper.valueToTree(av));
//                });
//                resp.add(record);
//            }
//        }
//        return resp;
//    }

    @Read
    @STServiceOperation()
    @PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
    public List<WidgetData> getWidgetData(Resource resource, IRI predicate) throws STPropertyAccessException, NoSuchConfigurationManager {
        ObjectMapper objectMapper = new ObjectMapper();

        List<WidgetData> wdList = new ArrayList<>();

        WidgetsManager wm = new WidgetsManager(getProject(), exptManager);
        Widget widget = wm.getWidgetForTrigger(predicate);
        if (widget != null) { //widget should always exist since this API should be invoked from the client only in such case

            String retrieveQuery = widget.retrieve;
            retrieveQuery = retrieveQuery.replace("<trigger>", RenderUtils.toSeRQL(predicate));

            TupleQuery tq = getManagedConnection().prepareTupleQuery(retrieveQuery);
            tq.setBinding("resource", resource);

            SimpleDataset dataset = new SimpleDataset();
            dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
            dataset.addDefaultGraph((IRI) getWorkingGraph());
            dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
            tq.setDataset(dataset);

            TupleQueryResult results = tq.evaluate();

            /**
             * according to the type, here I need to group the returned data into the response
             * - point: location, latitude, longitude; no need to group, each record is a single data set
             * - area/route: polyline_id, location, latitude, longitude; grouped by polyline_id
             * - series: series_id, series_label, value_label, name, value; grouped by series_id
             * - series_collection: series_collection_id, series_label, value_label, series_name, name, value; grouped by series_collection_id
             */
            //first cache the results into a list of mappings binding-value
            List<Map<String, AnnotatedValue<Value>>> records = new ArrayList<>();
            while (results.hasNext()) {
                Map<String, AnnotatedValue<Value>> record = new HashMap<>();
                BindingSet bs = results.next();
                bs.forEach(b-> {
                    record.put(b.getName(), new AnnotatedValue<>(b.getValue()));
                });
                records.add(record);
            }
            //iterate over the cache and add the records to the widgetData list grouped by the id value
            for (Map<String, AnnotatedValue<Value>> record : records) {
                //look for the widget data for the given id value (if already collected)
                WidgetData wd = wdList.stream().filter(data -> {
                    AnnotatedValue<Value> recordId = record.get(widget.getIdBinding().name());
                    AnnotatedValue<Value> wdId = data.getId(widget.getIdBinding().name());
                    return recordId.getValue().equals(wdId.getValue());
                }).findFirst().orElse(null);
                if (wd != null) {
                    //if the given ID value has been already collected/grouped, add a new record to it
                    wd.addBindings(record);
                } else { //otherwise create a new WidgetData
                    wd = new WidgetData(widget.getDataType());
                    wd.addBindings(record);
                    wdList.add(wd);
                }
            };
        }
        return wdList;
    }

    @Write
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('rdf(resource)', 'U')")
    public void updateWidgetData(Resource resource, IRI predicate, Map<WidgetDataBindings, Value> bindings) throws NoSuchConfigurationManager, STPropertyAccessException {
        WidgetsManager wm = new WidgetsManager(getProject(), exptManager);
        Widget widget = wm.getWidgetForTrigger(predicate);
        if (widget != null) { //widget should always exist since this API should be invoked from the client only in such case

            //check if values for the required bindings are provided
            for (WidgetDataBindings b: widget.getUpdateMandatoryBindings()) {
                if (!bindings.containsKey(b)) {
                    throw new IllegalArgumentException("Missing value for required binding " + b.toString());
                }
            }

            String updateQuery = widget.update;
            updateQuery = updateQuery.replace("<trigger>", RenderUtils.toSPARQL(predicate));
            Update update = getManagedConnection().prepareUpdate(updateQuery);

            for (Entry<WidgetDataBindings, Value> binding: bindings.entrySet()) {
                update.setBinding(binding.getKey().toString(), binding.getValue());
            }
            update.setBinding("resource", resource);

            SimpleDataset dataset = new SimpleDataset();
            dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
            dataset.addDefaultGraph((IRI) getWorkingGraph());
            dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
            update.setDataset(dataset);

            update.execute();
        }
    }


}
