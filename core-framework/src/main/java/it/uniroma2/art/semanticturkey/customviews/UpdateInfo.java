package it.uniroma2.art.semanticturkey.customviews;

import it.uniroma2.art.semanticturkey.vocabulary.RDFTypesEnum;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class UpdateInfo {

    private String field;
    private UpdateMode updateMode;
    private String updateQuery;
    private RDFTypesEnum valueType;
    private IRI datatype;
    private List<IRI> classes;

    public UpdateInfo() {
        this(UpdateMode.none);
    }

    public UpdateInfo(UpdateMode updateMode) {
        this(null, updateMode);
    }

    public UpdateInfo(String field, UpdateMode updateMode) {
        this.field = field;
        this.updateMode = updateMode;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public String getUpdateQuery() {
        return updateQuery;
    }

    public void setUpdateQuery(String updateQuery) {
        this.updateQuery = updateQuery;
    }

    public RDFTypesEnum getValueType() {
        return valueType;
    }

    public void setValueType(RDFTypesEnum valueType) {
        this.valueType = valueType;
    }

    public IRI getDatatype() {
        return datatype;
    }

    public void setDatatype(IRI datatype) {
        this.datatype = datatype;
    }

    public List<IRI> getClasses() {
        return classes;
    }

    public void setClasses(List<IRI> classes) {
        this.classes = classes;
    }

}
