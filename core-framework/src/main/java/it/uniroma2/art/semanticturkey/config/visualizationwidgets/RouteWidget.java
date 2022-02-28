package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;

import java.util.Set;

public class RouteWidget extends Widget {

    @Override
    public DataType getDataType() {
        return DataType.route;
    }

    @Override
    public Set<WidgetDataBindings> getBindingSet() {
        return ImmutableSet.of(WidgetDataBindings.route_id, WidgetDataBindings.location, WidgetDataBindings.latitude, WidgetDataBindings.longitude);
    }

    @Override
    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
        return getBindingSet();
    }

    @Override
    public WidgetDataBindings getIdBinding() {
        return WidgetDataBindings.route_id;
    }

}