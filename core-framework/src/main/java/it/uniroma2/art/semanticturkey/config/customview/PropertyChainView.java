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
            renderedValue.setUpdateInfo(new UpdateInfo(UpdateInfo.UpdateMode.widget));
            CustomViewObjectDescription cvObjectDescr = new CustomViewObjectDescription();
            cvObjectDescr.setResource(object);
            cvObjectDescr.setDescription(renderedValue);
            valueDescriptions.add(cvObjectDescr);
        }
        cvData.setData(valueDescriptions);

        return cvData;
    }

}
