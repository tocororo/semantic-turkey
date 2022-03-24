package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewObjectDescription;
import it.uniroma2.art.semanticturkey.customviews.CustomViewRenderedValue;
import it.uniroma2.art.semanticturkey.customviews.UpdateInfo;
import it.uniroma2.art.semanticturkey.customviews.UpdateMode;
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

public class DynamicVectorView extends CustomView {

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
    public List<UpdateInfo> update;

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.dynamic_vector;
    }

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

        List<CustomViewObjectDescription> objDescriptions = new ArrayList<>();
        String objectVar = CustomViewSparqlUtils.getRetrieveObjectVariable(retrieve);
        Set<String> pivotsVars = CustomViewSparqlUtils.getReturnedPivotVariables(retrieve);

        // each query result represent a description of an object of $resource $trigprop pair
        while (results.hasNext()) {
            BindingSet bs = results.next();

            Value object = bs.getValue(objectVar); //object described by the vector

            List<CustomViewRenderedValue> renderedValueList = new ArrayList<>();

            Map<String, String> variableFieldMap = CustomViewSparqlUtils.getValueVariables(retrieve); //map foo_value => foo; bar_value => bar
            //iterate over the fields foreseen in the retrieve query and create a CustomViewRenderedValue for each of them according the query results
            for (Map.Entry<String, String> entry : variableFieldMap.entrySet()) {
                String var = entry.getKey();
                String fieldName = entry.getValue();
                Value value = bs.getValue(var);
                CustomViewRenderedValue renderedValue = new CustomViewRenderedValue(fieldName, value);
                //get the update info for the current field
                UpdateInfo updateInfo = update.stream().filter(u -> u.getField().equals(fieldName)).findFirst().get();
                renderedValue.setUpdateInfo(updateInfo);

                Map<String, Value> pivotRefs = new HashMap<>();
                for (String pivotVar : pivotsVars) {
                    if (bs.hasBinding(pivotVar)) {
                        Value pivot = bs.getValue(pivotVar);
                        pivotRefs.put(pivotVar, pivot);
                    }
                }
                renderedValue.setPivots(pivotRefs);

                renderedValueList.add(renderedValue);
            }
            CustomViewObjectDescription cvObjectDescr = new CustomViewObjectDescription();
            cvObjectDescr.setResource(object);
            cvObjectDescr.setDescription(renderedValueList);
            objDescriptions.add(cvObjectDescr);
        }
        cvData.setData(objDescriptions);

        return cvData;
    }

    public void updateData(RepositoryConnection connection, Resource resource, IRI property, String fieldName, Value oldValue, Value newValue, Map<String, Value> pivots, IRI workingGraph) {
        UpdateInfo updateInfo = update.stream().filter(u -> u.getField().equals(fieldName)).findFirst().get();
        String updateQuery = updateInfo.getUpdateQuery();

        Update u = connection.prepareUpdate(updateQuery);

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
