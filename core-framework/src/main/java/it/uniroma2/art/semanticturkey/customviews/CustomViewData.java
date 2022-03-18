package it.uniroma2.art.semanticturkey.customviews;

import java.util.List;

public class CustomViewData {

    private CustomViewModelEnum model;
    private List<CustomViewObjectDescription> data;
    private ViewsEnum defaultView;

    public CustomViewData() {}

    public CustomViewData(CustomViewModelEnum model) {
        this.model = model;
    }

    public CustomViewModelEnum getModel() {
        return model;
    }

    public void setModel(CustomViewModelEnum model) {
        this.model = model;
    }

    public List<CustomViewObjectDescription> getData() {
        return data;
    }

    public void setData(List<CustomViewObjectDescription> data) {
        this.data = data;
    }

    public ViewsEnum getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(ViewsEnum defaultView) {
        this.defaultView = defaultView;
    }

//    public SingleValueUpdate getValueUpdate() {
//        return valueUpdate;
//    }
//
//    public void setValueUpdate(SingleValueUpdate valueUpdate) {
//        this.valueUpdate = valueUpdate;
//    }
}
