package it.uniroma2.art.semanticturkey.mdr.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

public class AbstractDatasetSpecification extends DatasetSpecification {
    public AbstractDatasetSpecification(IRI identity, String uriSpace, Literal title, Literal description, Boolean dereferenceable) {
        super(identity, uriSpace, title, description, dereferenceable, null);
    }
}
