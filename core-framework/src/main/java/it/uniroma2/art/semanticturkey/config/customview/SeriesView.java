package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

public class SeriesView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.series;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.series_id, WidgetDataBindings.series_label, WidgetDataBindings.value_label, WidgetDataBindings.name, WidgetDataBindings.value);
//    }
//
//    @Override
//    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
//        return ImmutableSet.of(WidgetDataBindings.series_id, WidgetDataBindings.name, WidgetDataBindings.value);
//    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.series_id;
    }

}