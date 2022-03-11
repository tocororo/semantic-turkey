package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;

public class DynamicVectorView extends CustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.dynamic_vector;
    }

}
