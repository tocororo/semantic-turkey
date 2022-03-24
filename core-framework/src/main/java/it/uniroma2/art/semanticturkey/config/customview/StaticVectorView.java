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
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticVectorView extends CustomView {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.StaticVectorView";
        public static final String shortName = keyBase + ".shortName";
        public static final String properties$description = keyBase + ".properties.description";
        public static final String properties$displayName = keyBase + ".properties.displayName";
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.static_vector;
    }

    /**
     * List of properties used as table headers
     */
    @STProperty(description = "{" + MessageKeys.properties$description + "}", displayName = "{" + MessageKeys.properties$displayName + "}")
    @Required
    public List<IRI> properties;

    @Override
    public CustomViewData getData(RepositoryConnection connection, Resource resource, IRI property, IRI workingGraph) {
        CustomViewData cvData = new CustomViewData();
        cvData.setModel(getModelType());
        cvData.setDefaultView(suggestedView);

        Map<IRI, String> propValueMap = new HashMap<>();
        List<String> valuesVariables = new ArrayList<>();
        List<String> whereBlocks = new ArrayList<>();

        for (int i = 0; i < properties.size(); i++) {
            IRI prop = properties.get(i);
            String valueVarName = "value" + i;
            propValueMap.put(prop, valueVarName);
            valuesVariables.add("?" + valueVarName);
            whereBlocks.add("OPTIONAL { ?obj " + RenderUtils.toSPARQL(prop) + " ?value" + i + " . }");
        }

        String query = "SELECT ?obj " + String.join(" ", valuesVariables) + " WHERE { \n" +
                "   $resource $trigprop ?obj . \n" +
                String.join("\n", whereBlocks) + " \n" +
                "}";

        TupleQuery tupleQuery = connection.prepareTupleQuery(query);
        tupleQuery.setBinding("resource", resource);
        tupleQuery.setBinding("trigprop", property);

        SimpleDataset dataset = new SimpleDataset();
        dataset.setDefaultInsertGraph(workingGraph);
        dataset.addDefaultGraph(workingGraph);
        dataset.addDefaultRemoveGraph(workingGraph);
        tupleQuery.setDataset(dataset);

        TupleQueryResult results = tupleQuery.evaluate();

        List<CustomViewObjectDescription> objDescriptions = new ArrayList<>();
        while (results.hasNext()) {
            BindingSet bs = results.next();

            Value object = bs.getValue("obj");

            if (object.isResource()) { //if not a resource, namely a literal, the object cannot be represented as table
                List<CustomViewRenderedValue> renderedValueList = new ArrayList<>();

                for (Map.Entry<IRI, String> propValueEntry : propValueMap.entrySet()) {
                    IRI prop = propValueEntry.getKey();
                    String var = propValueEntry.getValue();
                    Value value = bs.getValue(var);
                    CustomViewRenderedValue renderedValue = new CustomViewRenderedValue(NTriplesUtil.toNTriplesString(prop), value);
                    renderedValue.setUpdateInfo(new UpdateInfo(UpdateMode.widget));
                    renderedValueList.add(renderedValue);
                }

                CustomViewObjectDescription cvObjectDescr = new CustomViewObjectDescription();
                cvObjectDescr.setResource(object);
                cvObjectDescr.setDescription(renderedValueList);
                objDescriptions.add(cvObjectDescr);
            }
        }
        cvData.setData(objDescriptions);

        return cvData;
    }

    public void updateData(RepositoryConnection connection, Resource resource, IRI property, IRI fieldProp, Value oldValue, Value newValue, IRI workingGraph) {
        String updateQuery = "DELETE {  ?pivot " + RenderUtils.toSPARQL(fieldProp) + " ?oldValue . } \n" +
                    "INSERT {  ?pivot " + RenderUtils.toSPARQL(fieldProp) + " ?value . } \n" +
                    "WHERE { \n" +
                    "   $resource $trigprop ?pivot . \n" +
                    "   ?pivot " + RenderUtils.toSPARQL(fieldProp) + " ?oldValue . \n" +
                    "}";

        Update u = connection.prepareUpdate(updateQuery);

        //bind placeholders and provided variables
        u.setBinding("resource", resource);
        u.setBinding("trigprop", property);
        u.setBinding("value", newValue);
        u.setBinding("oldValue", oldValue);

        SimpleDataset dataset = new SimpleDataset();
        dataset.setDefaultInsertGraph(workingGraph);
        dataset.addDefaultGraph(workingGraph);
        dataset.addDefaultRemoveGraph(workingGraph);
        u.setDataset(dataset);

        u.execute();
    }

}
