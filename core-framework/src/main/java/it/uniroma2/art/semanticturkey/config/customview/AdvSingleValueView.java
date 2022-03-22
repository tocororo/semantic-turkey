package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewObjectDescription;
import it.uniroma2.art.semanticturkey.customviews.CustomViewRenderedValue;
import it.uniroma2.art.semanticturkey.customviews.UpdateInfo;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvSingleValueView extends CustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.adv_single_value;
    }

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.AbstractSparqlBasedCustomView";

        public static final String shortName = keyBase + ".shortName";
        public static final String retrieve$description = keyBase + ".retrieve.description";
        public static final String retrieve$displayName = keyBase + ".retrieve.displayName";
        public static final String update$description = keyBase + ".update.description";
        public static final String update$displayName = keyBase + ".update.displayName";
    }

    @Required
    @STProperty(description = "{" + MessageKeys.retrieve$description + "}", displayName = "{" + MessageKeys.retrieve$displayName + "}")
    public String retrieve;

    @Required
    @STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
    public UpdateInfo update;

    @Override
    public CustomViewData getData(RepositoryConnection connection, Resource resource, IRI property, IRI workingGraph) {

        CustomViewData cvData = new CustomViewData();
        cvData.setModel(getModelType());
        cvData.setDefaultView(suggestedView);

        TupleQuery tupleQuery = connection.prepareTupleQuery(retrieve);
        tupleQuery.setBinding("resource", resource);
        tupleQuery.setBinding("trigprop", property);

        SimpleDataset dataset = new SimpleDataset();
        dataset.setDefaultInsertGraph(workingGraph);
        dataset.addDefaultGraph(workingGraph);
        dataset.addDefaultRemoveGraph(workingGraph);
        tupleQuery.setDataset(dataset);

        TupleQueryResult results = tupleQuery.evaluate();

        List<CustomViewObjectDescription> valueDescriptions = new ArrayList<>();
        String objectVar = CustomViewSparqlUtils.getRetrieveObjectVariable(retrieve);
        Set<String> pivotsVars = CustomViewSparqlUtils.getReturnedPivotVariables(retrieve);

        while (results.hasNext()) {
            BindingSet bs = results.next();
            //if objectVar is value (e.g. resource $trigprop ?value), object and value will clash and the value description is the same value
            Value object = bs.getValue(objectVar);
            Value value = bs.getValue("value");

            CustomViewRenderedValue renderedValue = new CustomViewRenderedValue(value);
            renderedValue.setField(objectVar);
            renderedValue.setUpdateInfo(update);

            Map<String, Value> pivotRefs = new HashMap<>();
            for (String pivotVar : pivotsVars) {
                if (bs.hasBinding(pivotVar)) {
                    Value pivot = bs.getValue(pivotVar);
                    pivotRefs.put(pivotVar, pivot);
                }
            }
            renderedValue.setPivots(pivotRefs);

            CustomViewObjectDescription cvObjectDescr = new CustomViewObjectDescription();
            cvObjectDescr.setResource(object);
            cvObjectDescr.setDescription(renderedValue);
            valueDescriptions.add(cvObjectDescr);
        }
        cvData.setData(valueDescriptions);

        return cvData;
    }

    public void updateData(RepositoryConnection connection, Resource resource, IRI property, Value oldValue, Value newValue, Map<String, Value> pivots, IRI workingGraph) {

        Update u = connection.prepareUpdate(update.getUpdateQuery());

        //bind placeholders and provided variables
        u.setBinding("resource", resource);
        u.setBinding("trigprop", property);
        u.setBinding("value", newValue);
        u.setBinding("oldValue", oldValue);

        if (pivots != null) {
            for (Map.Entry<String, Value> pivot: pivots.entrySet()) {
                u.setBinding(pivot.getKey(), pivot.getValue());
            }
        }

        SimpleDataset dataset = new SimpleDataset();
        dataset.setDefaultInsertGraph(workingGraph);
        dataset.addDefaultGraph(workingGraph);
        dataset.addDefaultRemoveGraph(workingGraph);
        u.setDataset(dataset);

        u.execute();
    }


}
