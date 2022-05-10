package it.uniroma2.art.semanticturkey.mdr.core;

import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;

public class DatasetSpecification {
    private IRI identity;
    private String uriSpace;
    private Literal title;
    private Literal description;
    private Boolean dereferenceable;
    private Distribution distribution;

    public DatasetSpecification(IRI identity, String uriSpace, Literal title, Literal description, Boolean dereferenceable, Distribution distribution) {
        this.identity = identity;
        this.uriSpace = uriSpace;
        this.title = title;
        this.description = description;
        this.dereferenceable = dereferenceable;
        this.distribution = distribution;
    }

    public void export(Model model, ValueFactory vf) {
        model.add(identity, RDF.TYPE, DCAT.DATASET);
        model.add(identity, VOID.URI_SPACE, vf.createLiteral(uriSpace));
        if (title != null) {
            model.add(identity, DCTERMS.TITLE, title);
        }
        if (description != null) {
            model.add(identity, DCTERMS.DESCRIPTION, description);
        }
        METADATAREGISTRY.getDereferenciationSystem(dereferenceable).ifPresent(ds ->
            model.add(identity, METADATAREGISTRY.DEREFERENCIATION_SYSTEM, ds)
        );
        if (distribution != null) {
            model.add(identity, DCAT.HAS_DISTRIBUTION, distribution.getIdentity());
            distribution.export(model, vf);
        }
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public IRI getIdentity() {
        return identity;
    }

    public void setIdentity(IRI identity) {
        this.identity = identity;
    }
}
