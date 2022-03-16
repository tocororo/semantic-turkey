package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.CustomViewValueDescription;
import it.uniroma2.art.semanticturkey.customviews.SingleValueUpdate;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public SingleValueUpdate update;

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

        List<CustomViewValueDescription> valueDescriptions = new ArrayList<>();
        while (results.hasNext()) {
            BindingSet bs = results.next();
            String objectVar = getRetrieveObjectVariable();
            //if objectVar is value (e.g. resource $trigprop ?value), object and value will clash and the value description is the same value
            Value object = bs.getValue(objectVar);
            Value value = bs.getValue("value");
            CustomViewValueDescription vd = new CustomViewValueDescription();
            vd.setValue(object);
            vd.setDescription(value);
            vd.setUpdateInfo(this.update);
            valueDescriptions.add(vd);
        }
        cvData.setData(valueDescriptions);

        return cvData;
    }

    /**
     * Returns the variable name of the trigprop object in the retrieve query
     * e.g. $resource $trigprop ?myObject => myObject
     * @return
     */
    private String getRetrieveObjectVariable() {
        String VAR_PATTERN = "[$|?]([a-zA-Z0-9_]+)"; //group capture only the var name (without ? or $)
        String OBJ_PATTERN = "\\$resource\\s*\\$trigprop\\s*" + VAR_PATTERN + "\\s*\\.";
        Pattern pattern = Pattern.compile(OBJ_PATTERN);
        Matcher matcher = pattern.matcher(retrieve);
        if (matcher.find()) {
            return matcher.group(1); //group 0 is the whole captured pattern; 1 is the one in VAR_PATTERN
        } else {
            return null; //should never happen: retrieve query should be checked when submitted
        }
    }

}
