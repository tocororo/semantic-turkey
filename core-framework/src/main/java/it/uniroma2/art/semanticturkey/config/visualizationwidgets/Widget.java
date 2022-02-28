package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;

import java.util.Set;

public abstract class Widget implements Configuration {

    public enum DataType {
        area, point, route, series, series_collection
    }

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.visualizationwidgets.Widget";

        public static final String shortName = keyBase + ".shortName";
        public static final String datatype$description = keyBase + ".datatype.description";
        public static final String datatype$displayName = keyBase + ".datatype.displayName";
        public static final String retrieve$description = keyBase + ".retrieve.description";
        public static final String retrieve$displayName = keyBase + ".retrieve.displayName";
        public static final String update$description = keyBase + ".update.description";
        public static final String update$displayName = keyBase + ".update.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + Widget.MessageKeys.shortName + "}";
    }

    @Required
    @STProperty(description = "{" + Widget.MessageKeys.retrieve$description + "}", displayName = "{" + Widget.MessageKeys.retrieve$displayName + "}")
    public String retrieve;

    @STProperty(description = "{" + Widget.MessageKeys.update$description + "}", displayName = "{" + Widget.MessageKeys.update$displayName + "}")
    public String update;


    public abstract DataType getDataType();

    /**
     * Returns the list of bindings that the retrieve query must return
     * @return
     */
    public abstract Set<WidgetDataBindings> getBindingSet();

    /**
     * Returns the list of bindings that are mandatory in the update
     * @return
     */
    public abstract Set<WidgetDataBindings> getUpdateMandatoryBindings();

    /**
     * Returns the binding used as ID of the resource (e.g. location, trace_id, series_id, series_collection_id)
     * @return
     */
    public abstract WidgetDataBindings getIdBinding();

}
