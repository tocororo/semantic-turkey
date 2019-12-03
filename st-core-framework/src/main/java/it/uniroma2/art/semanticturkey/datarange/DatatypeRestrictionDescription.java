package it.uniroma2.art.semanticturkey.datarange;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DatatypeRestrictionDescription {

    private Map<IRI, Value> facets;
    private Collection<AnnotatedValue<Literal>> enumerations;

    public DatatypeRestrictionDescription() {
        this.facets = new HashMap<>();
        this.enumerations = new ArrayList<>();
    }

    public Map<IRI, Value> getFacets() {
        return facets;
    }

    public void setFacets(Map<IRI, Value> facets) {
        this.facets = facets;
    }

    public Collection<AnnotatedValue<Literal>> getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(Collection<AnnotatedValue<Literal>> enumerations) {
        this.enumerations = enumerations;
    }
}
