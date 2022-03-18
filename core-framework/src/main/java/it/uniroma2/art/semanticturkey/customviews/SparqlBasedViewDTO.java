package it.uniroma2.art.semanticturkey.customviews;

import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SparqlBasedViewDTO {

    private List<Map<String, Value>> bindingsList; //list of binding mappings: binding name -> value
    private UpdateMode updateMode;

    public SparqlBasedViewDTO() {
        bindingsList = new ArrayList<>();
        this.updateMode = UpdateMode.widget;
    }

    public List<Map<String, Value>> getBindingsList() {
        return bindingsList;
    }

    public void setBindingsList(List<Map<String, Value>> bindingsList) {
        this.bindingsList = bindingsList;
    }

    public void addBindings(Map<String, Value> bindings) {
        this.bindingsList.add(bindings);
    }

    /**
     * Returns the value for the given binding
     * @param bindingName
     * @return
     */
    public Value getBindingValue(String bindingName) {
        if (!bindingsList.isEmpty()) {
            return bindingsList.get(0).get(bindingName);
        }
        return null;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

}
