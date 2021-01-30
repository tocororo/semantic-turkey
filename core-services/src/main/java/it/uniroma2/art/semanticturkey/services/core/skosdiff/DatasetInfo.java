package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"username", "password"})
public class DatasetInfo {
    private String sparqlEndpoint;
    private String projectName;
    private String versionRepoId;
    private String lexicalizationIRI;
    private String username;
    private String password;


    @JsonCreator
    public DatasetInfo(@JsonProperty("projectName")String projectName, @JsonProperty("versionRepoId")String versionRepoId,
            @JsonProperty("sparqlEndpoint")String sparqlEndpoint, @JsonProperty("lexicalizationIR")String lexicalizationIRI,
            @JsonProperty("username")String username, @JsonProperty("password")String password) {
        this.projectName = projectName;
        this.versionRepoId = versionRepoId;
        this.sparqlEndpoint = sparqlEndpoint;
        this.lexicalizationIRI = lexicalizationIRI;
        this.username = username;
        this.password = password;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
