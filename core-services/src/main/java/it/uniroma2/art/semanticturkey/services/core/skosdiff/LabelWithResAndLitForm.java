package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

public class LabelWithResAndLitForm {
    private String resource;
    private String label;
    private String literalForm;

    @JsonCreator
    public LabelWithResAndLitForm(@JsonProperty("resource")String resource, @JsonProperty("label")String label,
            @JsonProperty("literalForm")String literalForm) {
        this.resource = resource;
        this.label = label;
        this.literalForm = literalForm;
    }

    public LabelWithResAndLitForm(IRI resource, IRI label, Literal literalForm) {
        this.resource = SkosDiffUtils.toNTriplesString(resource);
        this.label = SkosDiffUtils.toNTriplesString(label);
        this.literalForm = SkosDiffUtils.toNTriplesString(literalForm);
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
