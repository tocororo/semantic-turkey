package it.uniroma2.art.semanticturkey.customviews;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomViewData {

    private CustomViewModelEnum model;
    //list of bindings map (binding-value) for the same id of the represented data (e.g. trace_id for area/route, series_id for series, ...)
    private List<Map<String, AnnotatedValue<Value>>> bindingsList;

    public CustomViewData(CustomViewModelEnum model) {
        this.model = model;
        bindingsList = new ArrayList<>();
    }

    public CustomViewModelEnum getModel() {
        return model;
    }

    public void setModel(CustomViewModelEnum model) {
        this.model = model;
    }

    public List<Map<String, AnnotatedValue<Value>>> getBindingsList() {
        return bindingsList;
    }

    public void setBindingsList(List<Map<String, AnnotatedValue<Value>>> bindingsList) {
        this.bindingsList = bindingsList;
    }

    public void addBindings(Map<String, AnnotatedValue<Value>> bindings) {
        this.bindingsList.add(bindings);
    }

    /**
     * Returns the value for the given binding
     * @param bindingName
     * @return
     */
    public AnnotatedValue<Value> getBindingValue(String bindingName) {
        if (!bindingsList.isEmpty()) {
            return bindingsList.get(0).get(bindingName);
        }
        return null;
    }

}
