package it.uniroma2.art.semanticturkey.mdr.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;

/**
 * A distribution of a concrete dataset. The nature of a distribution shall be one among
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY#SPARQL_ENDPOINT},
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY#RDF4J_HTTP_REPOSITORY},
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY#GRAPHDB_REPOSITORY},
 * {@link it.uniroma2.art.semanticturkey.mdr.core.vocabulary.STMETADATAREGISTRY#PROJECT}.
 */
public class Distribution {
    private IRI identity;
    private IRI sparqlEndpoint;
    private IRI nature;
    private String projectName;

    @JsonCreator
    public Distribution(@JsonProperty("identity") @JsonDeserialize(converter = String2IRIConverter.class) IRI identity,
                        @JsonProperty("nature") @JsonDeserialize(converter = String2IRIConverter.class) IRI nature,
                        @JsonProperty("sparqlEndpoint") @JsonDeserialize(converter = String2IRIConverter.class) IRI sparqlEndpoint,
                        @JsonProperty("projectName") String projectName) {
        this.identity = identity;
        this.nature = nature;
        this.sparqlEndpoint = sparqlEndpoint;
        this.projectName = projectName;
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getIdentity() {
        return identity;
    }

    public void setIdentity(IRI identity) {
        this.identity = identity;
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public void setSparqlEndpoint(IRI sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getNature() {
        return nature;
    }

    public void setNature(IRI nature) {
        this.nature = nature;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void export(Model model, ValueFactory vf) {
        model.add(identity, RDF.TYPE, DCAT.DISTRIBUTION);
        model.add(identity, RDF.TYPE, nature);
        if (this.sparqlEndpoint != null) {
            model.add(identity, VOID.SPARQL_ENDPOINT, sparqlEndpoint);
        }
        if (projectName != null) {
            model.add(identity, FOAF.NAME, vf.createLiteral(projectName));
        }
    }
}
