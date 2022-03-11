package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

public class SeriesCollectionView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.series_collection;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.series_collection_id, WidgetDataBindings.series_label, WidgetDataBindings.value_label, WidgetDataBindings.series_name, WidgetDataBindings.name, WidgetDataBindings.value);
//    }
//
//    @Override
//    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
//        return ImmutableSet.of(WidgetDataBindings.series_collection_id, WidgetDataBindings.name, WidgetDataBindings.value);
//    }
//
    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.series_collection_id;
    }

}
