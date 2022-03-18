package it.uniroma2.art.semanticturkey.customviews;

import org.eclipse.rdf4j.model.Value;

public class CustomViewRenderedValue {

    private String field;
    private Value resource;
    private UpdateInfo updateInfo;

    public CustomViewRenderedValue() {}

    public CustomViewRenderedValue(Value resource) {
        this(null, resource);
    }

    public CustomViewRenderedValue(String field, Value resource) {
        this.field = field;
        this.resource = resource;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Value getResource() {
        return resource;
    }

    public void setResource(Value resource) {
        this.resource = resource;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }
}
