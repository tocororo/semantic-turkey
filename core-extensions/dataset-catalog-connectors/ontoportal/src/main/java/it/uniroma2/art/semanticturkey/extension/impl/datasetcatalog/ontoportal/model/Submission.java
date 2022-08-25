package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.time.ZonedDateTime;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Submission {
    public enum OntologyLanguage {

        OBO,
        OWL(Values.iri("http://www.w3.org/2002/07/owl")),
        SKOS(Values.iri("http://www.w3.org/2004/02/skos/core")),
        UMLS;

        OntologyLanguage() {
            this(null);
        }

        OntologyLanguage(IRI model) {
            this.model = Optional.ofNullable(model);
        };

        public Optional<IRI> getModel() {
            return model;
        }

        private Optional<IRI> model;
    }

    private long submissionId = Long.MIN_VALUE;
    private String id;
    private String description;
    private OntologyLanguage hasOntologyLanguage;
    private ZonedDateTime creationDate;

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OntologyLanguage getHasOntologyLanguage() {
        return hasOntologyLanguage;
    }

    public void setHasOntologyLanguage(OntologyLanguage hasOntologyLanguage) {
        this.hasOntologyLanguage = hasOntologyLanguage;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
