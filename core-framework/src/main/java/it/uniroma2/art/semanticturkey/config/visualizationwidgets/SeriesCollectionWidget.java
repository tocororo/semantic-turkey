package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.widgets.WidgetDataBindings;

import java.util.Set;

public class SeriesCollectionWidget extends Widget {

    @Override
    public DataType getDataType() {
        return DataType.series_collection;
    }

    @Override
    public Set<WidgetDataBindings> getBindingSet() {
        return ImmutableSet.of(WidgetDataBindings.series_collection_id, WidgetDataBindings.series_label, WidgetDataBindings.value_label, WidgetDataBindings.series_name, WidgetDataBindings.name, WidgetDataBindings.value);
    }

    @Override
    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
        return ImmutableSet.of(WidgetDataBindings.series_collection_id, WidgetDataBindings.name, WidgetDataBindings.value);
    }

    @Override
    public WidgetDataBindings getIdBinding() {
        return WidgetDataBindings.series_collection_id;
    }

}
