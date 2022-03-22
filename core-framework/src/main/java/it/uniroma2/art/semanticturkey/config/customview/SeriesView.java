package it.uniroma2.art.semanticturkey.config.customview;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

import java.util.Set;

public class SeriesView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.series;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.series_id, WidgetDataBindings.series_label, WidgetDataBindings.value_label, WidgetDataBindings.name, WidgetDataBindings.value);
//    }

    @Override
    public Set<CustomViewDataBindings> getUpdateMandatoryBindings() {
        return ImmutableSet.of(CustomViewDataBindings.series_id, CustomViewDataBindings.name, CustomViewDataBindings.value);
    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.series_id;
    }


}