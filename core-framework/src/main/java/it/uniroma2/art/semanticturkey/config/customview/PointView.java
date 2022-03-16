package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;


public class PointView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.point;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.location, WidgetDataBindings.latitude, WidgetDataBindings.longitude);
//    }
//
//    @Override
//    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
//        return getBindingSet();
//    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.location;
    }

}
