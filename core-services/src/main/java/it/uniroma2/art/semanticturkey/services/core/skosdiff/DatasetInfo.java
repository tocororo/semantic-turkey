package it.uniroma2.art.semanticturkey.services.core.skosdiff;

public class DatasetInfo {
    private String sparqlEndpoint;
    private String projectName;
    private String versionRepoId;
    private String lexicalizationIRI;

    public DatasetInfo() {
    }

    public DatasetInfo(String projectName, String versionRepoId, String sparqlEndpoint, String lexicalizationIR) {
        this.projectName = projectName;
        this.versionRepoId = versionRepoId;
        this.sparqlEndpoint = sparqlEndpoint;
        this.lexicalizationIRI = lexicalizationIR;
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
