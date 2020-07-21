package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatasetInfo {
    private String sparqlEndpoint;
    private String projectName;
    private String versionRepoId;
    private String lexicalizationIRI;


    @JsonCreator
    public DatasetInfo(@JsonProperty("projectName")String projectName, @JsonProperty("versionRepoId")String versionRepoId,
            @JsonProperty("sparqlEndpoint")String sparqlEndpoint, @JsonProperty("lexicalizationIR")String lexicalizationIRI) {
        this.projectName = projectName;
        this.versionRepoId = versionRepoId;
        this.sparqlEndpoint = sparqlEndpoint;
        this.lexicalizationIRI = lexicalizationIRI;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getVersionRepoId() {
        return versionRepoId;
    }

    public String getLexicalizationIRI() {
        return lexicalizationIRI;
    }
}
