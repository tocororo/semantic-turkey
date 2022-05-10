package it.uniroma2.art.semanticturkey.mdr.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

public class ConcreteDatasetSpecification extends DatasetSpecification {
    public ConcreteDatasetSpecification(IRI identity, String uriSpace, Literal title, Literal description, Boolean dereferenceable, Distribution distribution) {
        super(identity, uriSpace, title, description, dereferenceable, distribution);
    }
}
