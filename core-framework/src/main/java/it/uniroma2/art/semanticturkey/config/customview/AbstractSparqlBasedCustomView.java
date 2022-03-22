package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewObjectDescription;
import it.uniroma2.art.semanticturkey.customviews.SparqlBasedViewDTO;
import it.uniroma2.art.semanticturkey.customviews.UpdateMode;
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
import java.util.Set;

public abstract class AbstractSparqlBasedCustomView extends CustomView {

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

    @STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
    public String update;

    /**
     * Returns the binding used as ID of the view (e.g. location, trace_id, series_id, series_collection_id)
     * @return
     */
    public abstract CustomViewDataBindings getIdBinding();

    public abstract Set<CustomViewDataBindings> getUpdateMandatoryBindings();

    public CustomViewData getData(RepositoryConnection connection, Resource resource, IRI property, IRI workingGraph) {
        CustomViewData cvData = new CustomViewData(this.getModelType());
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

        /**
         * according to the type, here I need to group the returned data into the response
         * - point: location, latitude, longitude; no need to group, each record is a single data set
         * - area/route: route_id, location, latitude, longitude; grouped by polyline_id
         * - series: series_id, series_label, value_label, name, value; grouped by series_id
         * - series_collection: series_collection_id, series_label, value_label, series_name, name, value; grouped by series_collection_id
         */
        //first cache the results into a list of mappings binding-value
        List<Map<String, Value>> records = new ArrayList<>();
        while (results.hasNext()) {
            Map<String, Value> record = new HashMap<>();
            BindingSet bs = results.next();
            bs.forEach(b -> {
                record.put(b.getName(), b.getValue());
            });
            records.add(record);
        }
        List<CustomViewObjectDescription> objDescriptions = new ArrayList<>();
        //iterate over the cache and add the records to the widgetData list grouped by the id value
        for (Map<String, Value> record : records) {
            Resource recordId = (Resource) record.get(getIdBinding().name()); //identifier resource of the current grouped bindings
            //look for the widget data for the given id value (if already collected)
            CustomViewObjectDescription cvObjectDescr = objDescriptions.stream()
                    .filter(descr -> recordId.equals(descr.getResource())) //true if they are the same
                    .findFirst().orElse(null);
            if (cvObjectDescr != null) {
                //if the given ID value has been already collected/grouped, add a new record to it
                SparqlBasedViewDTO dto = (SparqlBasedViewDTO) cvObjectDescr.getDescription();
                dto.addBindings(record);
            } else { //otherwise create a new WidgetData
                SparqlBasedViewDTO dto = new SparqlBasedViewDTO();
                dto.addBindings(record);
                if (this.update == null || this.update.isEmpty()) { //if no update is provided, disable update
                    dto.setUpdateMode(UpdateMode.none);
                }
                cvObjectDescr = new CustomViewObjectDescription(recordId);
                cvObjectDescr.setDescription(dto);
                objDescriptions.add(cvObjectDescr);
            }
        }
        cvData.setData(objDescriptions);

        return cvData;
    }

}
