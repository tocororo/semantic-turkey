package it.uniroma2.art.semanticturkey.customviews;

import java.util.List;

public class CustomViewData {

    private CustomViewModelEnum model;
    private List<CustomViewValueDescription> data;
    private ViewsEnum defaultView;
//    private SingleValueUpdate valueUpdate;

    public CustomViewModelEnum getModel() {
        return model;
    }

    public void setModel(CustomViewModelEnum model) {
        this.model = model;
    }

    public List<CustomViewValueDescription> getData() {
        return data;
    }

    public void setData(List<CustomViewValueDescription> data) {
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
