package it.uniroma2.art.semanticturkey.customform;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFormValueTablePreview {

    private Map<Resource, List<Pair<AnnotatedValue<IRI>, AnnotatedValue<Value>>>> table = new HashMap<>();

    @JsonAnySetter
    public void addRow(Resource reifiedRes, List<Pair<AnnotatedValue<IRI>, AnnotatedValue<Value>>> predObjPairs) {
        this.table.put(reifiedRes, predObjPairs);
    }

    @JsonAnyGetter
    public Map<Resource, List<Pair<AnnotatedValue<IRI>, AnnotatedValue<Value>>>> getTable() {
        return table;
    }
}
