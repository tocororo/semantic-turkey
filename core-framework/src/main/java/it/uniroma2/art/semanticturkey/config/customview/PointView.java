package it.uniroma2.art.semanticturkey.config.customview;

import com.google.common.collect.ImmutableSet;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

import java.util.Set;


public class PointView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.point;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.location, WidgetDataBindings.latitude, WidgetDataBindings.longitude);
//    }

    @Override
    public Set<CustomViewDataBindings> getUpdateMandatoryBindings() {
        return ImmutableSet.of(CustomViewDataBindings.location, CustomViewDataBindings.latitude, CustomViewDataBindings.longitude);
    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.location;
    }

}
