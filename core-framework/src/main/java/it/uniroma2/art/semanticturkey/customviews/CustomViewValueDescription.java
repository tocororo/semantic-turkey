package it.uniroma2.art.semanticturkey.customviews;

import org.eclipse.rdf4j.model.Value;

public class CustomViewValueDescription {

    private Value value;
    private Object description;
    private SingleValueUpdate updateInfo; //provides info on how to update the value

    /*
    TODO decide/restrict the type of the description
    Currently the description provides info for the client to inform how the value needs to be rendered
    - In sparql based views (e.g. maps and charts) it is a list of bindings
    - In single-value views (prop-chain or advanced) it is just the value
    - In vector views (static or dynamic) ...?
     */

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

    public SingleValueUpdate getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(SingleValueUpdate updateInfo) {
        this.updateInfo = updateInfo;
    }
}
