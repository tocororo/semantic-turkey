package it.uniroma2.art.semanticturkey.mdr.core;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import java.util.Collection;

public class DatasetMetadata2 {
    private final IRI identity;
    private final DatasetNature nature;
    private final String uriSpace;
    private final Collection<String> otherURISpaces;
    private final Collection<Literal> titles;
    private final Collection<Literal> descriptions;
    private final DatasetRole role;
    private final Literal versionInfo;
    private final Literal versionNotes;

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
                            Collection<Literal> titles,
                            Collection<Literal> descriptions,
                            DatasetRole role,
                            Literal versionInfo,
                            Literal versionNotes) {

        this.identity = identity;
        this.nature = nature;
        this.uriSpace = uriSpace;
        this.otherURISpaces = otherURISpaces;
        this.titles = titles;
        this.descriptions = descriptions;
        this.role = role;
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
                .append("versionInfo", versionInfo)
                .append("versionNotes", versionNotes)
                .toString();
    }
}
