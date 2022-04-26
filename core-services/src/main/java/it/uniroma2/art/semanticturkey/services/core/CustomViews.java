package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.Collection;
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
    @STServiceOperation
    public CustomViewData getViewData(Resource resource, IRI property) {
        ObjectMapper objectMapper = new ObjectMapper();

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
        for (CustomViewDataBindings b: customView.getUpdateMandatoryBindings()) {
            if (!bindings.containsKey(b)) {
                throw new IllegalArgumentException("Missing value for required binding " + b.toString());
            }
        }

        String updateQuery = customView.update;
        Update update = getManagedConnection().prepareUpdate(updateQuery);

        //bind placeholders and provided variables
        update.setBinding("resource", resource);
        update.setBinding("trigprop", property);
        for (Map.Entry<CustomViewDataBindings, Value> binding: bindings.entrySet()) {
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
            ((AdvSingleValueView)cv).updateData(getManagedConnection(), resource, property, oldValue, newValue, pivots, (IRI) getWorkingGraph());
        } else if (cv instanceof PropertyChainView) {
            ((PropertyChainView)cv).updateData(getManagedConnection(), resource, property, oldValue, newValue, (IRI) getWorkingGraph());
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


}
