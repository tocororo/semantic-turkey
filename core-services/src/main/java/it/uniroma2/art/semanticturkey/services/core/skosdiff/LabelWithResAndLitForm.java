package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

public class LabelWithResAndLitForm {
    private String resource;
    private String label;
    private String literalForm;

    public LabelWithResAndLitForm() {
    }

    public LabelWithResAndLitForm(IRI resource, IRI label, Literal literalForm) {
        this.resource = NTriplesUtil.toNTriplesString(resource);
        this.label = NTriplesUtil.toNTriplesString(label);
        this.literalForm = NTriplesUtil.toNTriplesString(literalForm);
    }

    public String getResource() {
        return resource;
    }

    public String getLabel() {
        return label;
    }

    public String getLiteralForm() {
        return literalForm;
    }
}
