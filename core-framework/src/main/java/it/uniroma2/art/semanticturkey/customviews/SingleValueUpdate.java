package it.uniroma2.art.semanticturkey.customviews;

import it.uniroma2.art.semanticturkey.vocabulary.RDFTypesEnum;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class SingleValueUpdate {

    public enum UpdateMode {
        none, //no update in widget
        inline, //inline NT editing
        picker, //value picker
        widget, //the widget is aware of the update mode, no chance to customize it
    }

    private String field;
    private UpdateMode updateMode;
    private String updateQuery;
    private RDFTypesEnum valueType;
    private IRI datatype;
    private List<IRI> classes;

    public SingleValueUpdate() {
        this(UpdateMode.none);
    }

    public SingleValueUpdate(UpdateMode updateMode) {
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
