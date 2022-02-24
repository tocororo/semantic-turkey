package it.uniroma2.art.semanticturkey.widgets;

import it.uniroma2.art.semanticturkey.config.visualizationwidgets.Widget;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WidgetData {

    private Widget.DataType widgetDataType;
    //list of bindings map (binding-value) for the same id of the represented data (e.g. trace_id for area/route, series_id for series, ...)
    private List<Map<String, AnnotatedValue<Value>>> bindingsList;

    public WidgetData(Widget.DataType widgetDataType) {
        this.widgetDataType = widgetDataType;
        bindingsList = new ArrayList<>();
    }

    public Widget.DataType getWidgetDataType() {
        return widgetDataType;
    }

    public void setWidgetDataType(Widget.DataType widgetDataType) {
        this.widgetDataType = widgetDataType;
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
     * Returns the value of the data identifier (e.g. for a route, should return the value of trace_id)
     * @param idBinding
     * @return
     */
    public AnnotatedValue<Value> getId(String idBinding) {
        if (!bindingsList.isEmpty()) {
            return bindingsList.get(0).get(idBinding);
        }
        return null;
    }

}
