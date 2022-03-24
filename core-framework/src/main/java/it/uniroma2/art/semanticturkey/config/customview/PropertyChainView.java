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

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class PropertyChainView extends CustomView {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.PropertyChainView";
        public static final String shortName = keyBase + ".shortName";
        public static final String properties$description = keyBase + ".properties.description";
        public static final String properties$displayName = keyBase + ".properties.displayName";
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.property_chain;
    }

    @STProperty(description = "{" + MessageKeys.properties$description + "}", displayName = "{" + MessageKeys.properties$displayName + "}")
    @Required
    public List<IRI> properties;

    @Override
    public CustomViewData getData(RepositoryConnection connection, Resource resource, IRI property, IRI workingGraph) {
        CustomViewData cvData = new CustomViewData();
        cvData.setModel(getModelType());
        cvData.setDefaultView(suggestedView);

        String showChain = properties.stream().map(RenderUtils::toSPARQL).collect(joining("/"));

        String query = "SELECT ?obj ?value WHERE { \n" +
                "   $resource $trigprop ?obj . \n" +
                "   ?obj " + showChain + " ?value . \n" +
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


        List<CustomViewObjectDescription> valueDescriptions = new ArrayList<>();
        while (results.hasNext()) {
            BindingSet bs = results.next();
            Resource object = (Resource) bs.getValue("obj");
            Value value = bs.getValue("value");
            CustomViewRenderedValue renderedValue = new CustomViewRenderedValue("value", value);
            renderedValue.setUpdateInfo(new UpdateInfo(UpdateMode.widget));
            CustomViewObjectDescription cvObjectDescr = new CustomViewObjectDescription();
            cvObjectDescr.setResource(object);
            cvObjectDescr.setDescription(renderedValue);
            valueDescriptions.add(cvObjectDescr);
        }
        cvData.setData(valueDescriptions);

        return cvData;
    }

    public void updateData(RepositoryConnection connection, Resource resource, IRI property, Value oldValue, Value newValue, IRI workingGraph) {
        String updateQuery;
        if (properties.size() == 1) {
            updateQuery = "DELETE {  ?pivot " + RenderUtils.toSPARQL(properties.get(0)) + " ?oldValue . } \n" +
                    "INSERT {  ?pivot " + RenderUtils.toSPARQL(properties.get(0)) + " ?value . } \n" +
                    "WHERE { \n" +
                    "   $resource $trigprop ?pivot . \n" +
                    "   ?pivot " + RenderUtils.toSPARQL(properties.get(0)) + " ?oldValue . \n" +
                    "}";
        } else {
            IRI lastProp = properties.get(properties.size()-1);
            List<IRI> leadingProps = properties.subList(0, properties.size()-1);
            String chain = leadingProps.stream().map(RenderUtils::toSPARQL).collect(joining("/"));
            updateQuery = "DELETE {  ?pivot " + RenderUtils.toSPARQL(lastProp) + " ?oldValue . } \n" +
                    "INSERT {  ?pivot " + RenderUtils.toSPARQL(lastProp) + " ?value . } \n" +
                    "WHERE { \n" +
                    "   $resource $trigprop ?o . \n" +
                    "   ?o " + chain + " ?pivot . \n" +
                    "   ?pivot " + RenderUtils.toSPARQL(lastProp) + " ?oldValue . \n" +
                    "}";
        }

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
