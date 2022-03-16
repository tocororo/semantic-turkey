package it.uniroma2.art.semanticturkey.customviews;

import org.eclipse.rdf4j.model.Value;

public class VectorData {

    private String headerName;
    private Value value; //cell content
    private SingleValueUpdate updateInfo; //provides info on how to update the value

    public VectorData() {}

    public VectorData(String headerName, Value value) {
        this.headerName = headerName;
        this.value = value;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public SingleValueUpdate getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(SingleValueUpdate updateInfo) {
        this.updateInfo = updateInfo;
    }

}
