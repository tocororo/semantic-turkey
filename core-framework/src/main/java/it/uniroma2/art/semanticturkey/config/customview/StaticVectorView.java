package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewValueDescription;
import it.uniroma2.art.semanticturkey.customviews.SingleValueUpdate;
import it.uniroma2.art.semanticturkey.customviews.VectorData;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
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

        String objectVar = "?obj";

        Map<IRI, String> propValueMap = new HashMap<>();
        List<String> valuesVariables = new ArrayList<>();
        List<String> whereBlocks = new ArrayList<>();

        for (int i = 0; i < properties.size(); i++) {
            IRI prop = properties.get(i);
            String valueVarName = "value" + i;
            propValueMap.put(prop, valueVarName);
            valuesVariables.add("?" + valueVarName);
            whereBlocks.add("OPTIONAL { " + objectVar + " " + RenderUtils.toSPARQL(prop) + " ?value" + i + " . }");
        }

        String query = "SELECT " + objectVar + " " + String.join(" ", valuesVariables) + " WHERE { \n" +
                "   $resource $trigprop " + objectVar + " . \n" +
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

        List<CustomViewValueDescription> valueDescriptions = new ArrayList<>();
        while (results.hasNext()) {
            BindingSet bs = results.next();
            Resource object = (Resource) bs.getValue("obj");

            List<VectorData> vectorDataList = new ArrayList<>();

            for (Map.Entry<IRI, String> propValueEntry : propValueMap.entrySet()) {
                IRI prop = propValueEntry.getKey();
                String var = propValueEntry.getValue();
                Value value = bs.getValue(var);
                VectorData vectorData = new VectorData(NTriplesUtil.toNTriplesString(prop), value);
                vectorDataList.add(vectorData);
            }

            CustomViewValueDescription vd = new CustomViewValueDescription();
            vd.setValue(object);
            vd.setDescription(vectorDataList);
            vd.setUpdateInfo(new SingleValueUpdate(SingleValueUpdate.UpdateMode.widget));
            valueDescriptions.add(vd);
        }
        cvData.setData(valueDescriptions);

        return cvData;
    }

}
