package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;

import java.util.Set;

public class SeriesWidget extends Widget {

    @Override
    public DataType getDataType() {
        return DataType.series;
    }

    @Override
    public Set<WidgetDataBindings> getBindingSet() {
        return ImmutableSet.of(WidgetDataBindings.series_id, WidgetDataBindings.y_axis_label, WidgetDataBindings.x_axis_label, WidgetDataBindings.name, WidgetDataBindings.value);
    }

    @Override
    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
        return ImmutableSet.of(WidgetDataBindings.series_id, WidgetDataBindings.name, WidgetDataBindings.value);
    }

    @Override
    public WidgetDataBindings getIdBinding() {
        return WidgetDataBindings.series_id;
    }

}