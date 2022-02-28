package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;

import java.util.Set;

public class PointWidget extends Widget {

    @Override
    public DataType getDataType() {
        return DataType.point;
    }

    @Override
    public Set<WidgetDataBindings> getBindingSet() {
        return ImmutableSet.of(WidgetDataBindings.location, WidgetDataBindings.latitude, WidgetDataBindings.longitude);
    }

    @Override
    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
        return getBindingSet();
    }

    @Override
    public WidgetDataBindings getIdBinding() {
        return WidgetDataBindings.location;
    }

}
