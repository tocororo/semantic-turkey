package it.uniroma2.art.semanticturkey.config.customview;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

import java.util.Set;

public class SeriesCollectionView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.series_collection;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.series_collection_id, WidgetDataBindings.series_label, WidgetDataBindings.value_label, WidgetDataBindings.series_name, WidgetDataBindings.name, WidgetDataBindings.value);
//    }

    @Override
    public Set<CustomViewDataBindings> getUpdateMandatoryBindings() {
        return ImmutableSet.of(CustomViewDataBindings.series_collection_id, CustomViewDataBindings.name, CustomViewDataBindings.value);
    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.series_collection_id;
    }

}
