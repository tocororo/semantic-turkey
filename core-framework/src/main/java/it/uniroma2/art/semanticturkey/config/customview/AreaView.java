package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

public class AreaView extends AbstractSparqlBasedCustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.area;
    }

//    @Override
//    public Set<WidgetDataBindings> getBindingSet() {
//        return ImmutableSet.of(WidgetDataBindings.route_id, WidgetDataBindings.location, WidgetDataBindings.latitude, WidgetDataBindings.longitude);
//    }
//
//    @Override
//    public Set<WidgetDataBindings> getUpdateMandatoryBindings() {
//        return getBindingSet();
//    }

    @Override
    public CustomViewDataBindings getIdBinding() {
        return CustomViewDataBindings.route_id;
    }


}