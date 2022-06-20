package it.uniroma2.art.semanticturkey.mdr.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.Optional2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class DatasetMetadata2 {
    public static class SPARQLEndpointMedatadata {
        private final IRI endpoint;
        private final Set<IRI> limitations;

        @JsonCreator
        public SPARQLEndpointMedatadata(@JsonProperty("@id") @JsonDeserialize(converter = String2IRIConverter.class) IRI endpoint,
                                        @JsonProperty("limitations") @JsonDeserialize(contentConverter = String2IRIConverter.class) Set<IRI> limitations) {
            this.endpoint = endpoint;
            this.limitations = limitations;
        }

        @JsonProperty("@id")
        @JsonSerialize(converter = IRI2StringConverter.class)
        public IRI getEndpoint() {
            return endpoint;
        }

        @JsonSerialize(contentConverter = IRI2StringConverter.class)
        public Set<IRI> getLimitations() {
            return limitations;
        }
    }

    private final IRI identity;
    private final DatasetNature nature;
    private final String uriSpace;
    private final Collection<String> otherURISpaces;
    private final Collection<Literal> titles;
    private final Collection<Literal> descriptions;
    private final DatasetRole role;
    private final String projectName;
    private final Literal versionInfo;
    private final Literal versionNotes;
    private final Optional<IRI> dereferenciationSystem;
    private final Optional<SPARQLEndpointMedatadata> endpointMedatadata;


    public enum DatasetRole {
        ROOT, VERSION, MASTER, LOD
    }

    public enum DatasetNature {
        ABSTRACT, PROJECT, SPARQL_ENDPOINT, RDF4J_REPOSITORY, GRAPHDB_REPOSITORY, MIX
    }

    public DatasetMetadata2(IRI identity,
                            DatasetNature nature,
                            String uriSpace,
                            Collection<String> otherURISpaces,
                            IRI dereferenciationSystem,
                            SPARQLEndpointMedatadata endpointMetadata,
                            Collection<Literal> titles,
                            Collection<Literal> descriptions,
                            DatasetRole role,
                            Literal versionInfo,
                            Literal versionNotes, String projectName) {

        this.identity = identity;
        this.nature = nature;
        this.uriSpace = uriSpace;
        this.otherURISpaces = otherURISpaces;
        this.dereferenciationSystem = Optional.ofNullable(dereferenciationSystem);
        this.endpointMedatadata = Optional.ofNullable(endpointMetadata);
        this.titles = titles;
        this.descriptions = descriptions;
        this.role = role;
        this.projectName = projectName;
        this.versionInfo = versionInfo;
        this.versionNotes = versionNotes;
    }

    @JsonSerialize(converter = IRI2StringConverter.class)
    public IRI getIdentity() {
        return identity;
    }

    public DatasetNature getNature() {
        return nature;
    }

    public String getUriSpace() {
        return uriSpace;
    }

    @JsonSerialize(converter = Optional2StringConverter.class)
    public Optional<IRI> getDereferenciationSystem() {
        return dereferenciationSystem;
    }

    public Optional<SPARQLEndpointMedatadata> getSparqlEndpoint() {
        return endpointMedatadata;
    }

    public Collection<String> getOtherURISpaces() {
        return otherURISpaces;
    }

    public Collection<Literal> getTitles() {
        return titles;
    }

    public Collection<Literal> getDescriptions() {
        return descriptions;
    }

    public DatasetRole getRole() {
        return role;
    }

    public String getProjectName() {
        return projectName;
    }

    public Literal getVersionInfo() {
        return versionInfo;
    }

    public Literal getVersionNotes() {
        return versionNotes;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("identity", identity)
                .append("nature", nature)
                .append("uriSpace", uriSpace)
                .append("otherURISpaces", otherURISpaces)
                .append("titles", titles)
                .append("descriptions", descriptions)
                .append("role", role)
                .append("projectName", projectName)
                .append("versionInfo", versionInfo)
                .append("versionNotes", versionNotes)
                .toString();
    }
}
