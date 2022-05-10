package it.uniroma2.art.semanticturkey.mdr.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.DCAT3FRAGMENT;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;

/**
 * Describes the attachment of a concrete dataset to an abstract one. The relation shall be one among
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY#MASTER},
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY#LOD},
 * {@link DCAT3FRAGMENT#HAS_VERSION}
 */
public class AbstractDatasetAttachment {
    private IRI abstractDataset;
    private IRI relation;
    private String versionInfo;
    private Literal versionNotes;

    @JsonCreator
    public AbstractDatasetAttachment(
            @JsonProperty("abstractDataset") @JsonDeserialize(converter = String2IRIConverter.class) IRI abstractDataset,
            @JsonProperty("relation") @JsonDeserialize(converter = String2IRIConverter.class) IRI relation,
            @JsonProperty("versionInfo") String versionInfo,
            @JsonProperty("versionNotes") Literal versionNotes) {
        this.abstractDataset = abstractDataset;
        this.relation = relation;
        this.versionInfo = versionInfo;
        this.versionNotes = versionNotes;

        if ((versionInfo != null || versionNotes != null) && relation != DCAT3FRAGMENT.HAS_VERSION) {
            throw new IllegalArgumentException("Either version info or version notes is non null, but relation with the abstract dataset is not dcat:hasVersion");
        }
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getAbstractDataset() {
        return abstractDataset;
    }

    public void setAbstractDataset(IRI abstractDataset) {
        this.abstractDataset = abstractDataset;
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getRelation() {
        return relation;
    }

    public void setRelation(IRI relation) {
        this.relation = relation;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public Literal getVersionNotes() {
        return versionNotes;
    }

    public void setVersionNotes(Literal versionNotes) {
        this.versionNotes = versionNotes;
    }

    public void export(Model model, ValueFactory vf, IRI concreteDataset) {
        model.add(abstractDataset, relation, concreteDataset);
        if (versionInfo != null) {
            model.add(concreteDataset, OWL.VERSIONINFO, vf.createLiteral(versionInfo));
        }
        if (versionNotes != null) {
            model.add(concreteDataset, vf.createIRI("http://www.w3.org/ns/adms#versionNotes"), versionNotes);
        }
    }

}
