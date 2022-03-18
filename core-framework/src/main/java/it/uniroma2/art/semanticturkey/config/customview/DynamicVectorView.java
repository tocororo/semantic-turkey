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
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicVectorView extends CustomView {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.AbstractSparqlBasedCustomView";

        public static final String shortName = keyBase + ".shortName";
        public static final String retrieve$description = keyBase + ".retrieve.description";
        public static final String retrieve$displayName = keyBase + ".retrieve.displayName";
        public static final String update$description = keyBase + ".update.description";
        public static final String update$displayName = keyBase + ".update.displayName";
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.dynamic_vector;
    }

    @Required
    @STProperty(description = "{" + MessageKeys.retrieve$description + "}", displayName = "{" + MessageKeys.retrieve$displayName + "}")
    public String retrieve;

    @Required
    @STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
    public List<UpdateInfo> update;

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
        while (results.hasNext()) {
            BindingSet bs = results.next();

            String objectVar = getRetrieveObjectVariable();
            Value object = bs.getValue(objectVar);

            List<CustomViewRenderedValue> renderedValueList = new ArrayList<>();

            Map<String, String> variableFieldMap = getValueVariablesFields();
            for (Map.Entry<String, String> entry : variableFieldMap.entrySet()) {
                String var = entry.getKey();
                String field = entry.getValue();
                Value value = bs.getValue(var);
                CustomViewRenderedValue renderedValue = new CustomViewRenderedValue(field, value);
                UpdateInfo updateInfo = update.stream().filter(u -> u.getField().equals(field)).findFirst().get();
                renderedValue.setUpdateInfo(updateInfo);
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

    /**
     * Returns a mapping (for each variable like ?foo_value ?bar_value in the select)
     * ?foo_value => foo
     * ?bar_value => bar
     * @return
     */
    private Map<String, String> getValueVariablesFields() {
        String selectWhere = retrieve.substring(retrieve.toLowerCase().indexOf("select"), retrieve.toLowerCase().indexOf("where"));
        String VALUE_PATTERN = "[$|?](([a-zA-Z0-9_]+)_value)";
        Pattern pattern = Pattern.compile(VALUE_PATTERN);
        Matcher matcher = pattern.matcher(selectWhere);

        Map<String, String> map = new HashMap<>();
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2)); //group 1 "?foo_value", group 2 "foo"
        }
        return map;
    }

    private String getValueVariableField(String variable) {
        String VALUE_PATTERN = "[$|?]([a-zA-Z0-9_]+_value)";
        Pattern pattern = Pattern.compile(VALUE_PATTERN);
        Matcher matcher = pattern.matcher(variable);
        if (matcher.find()) {
            return matcher.group(1); //group 0 is the whole captured pattern; 1 is the one in VAR_PATTERN
        }
        return null;
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
