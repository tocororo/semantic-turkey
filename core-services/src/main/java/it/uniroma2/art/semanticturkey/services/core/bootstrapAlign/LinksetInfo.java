package it.uniroma2.art.semanticturkey.services.core.bootstrapAlign;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"username", "password"})
public class LinksetInfo {
    private String sparqlEndpoint;
    private String namespaceSubj;
    private String namespaceObj;
    private String username;
    private String password;
    private boolean isReverse;


    @JsonCreator
    public LinksetInfo(@JsonProperty("namespaceSubj")String namespaceSubj, @JsonProperty("namespaceObj")String namespaceObj,
                       @JsonProperty("sparqlEndpoint")String sparqlEndpoint,
                       @JsonProperty("username")String username, @JsonProperty("password") String password,
                       @JsonProperty("reverse")boolean isReverse) {
        this.namespaceSubj = namespaceSubj;
        this.namespaceObj = namespaceObj;
        this.sparqlEndpoint = sparqlEndpoint;
        this.username = username;
        this.password = password;
        this.isReverse = isReverse;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public String getNamespaceSubj() {
        return namespaceSubj;
    }

    public String getNamespaceObj() {
        return namespaceObj;
    }

    public boolean isReverse() {
        return isReverse;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
