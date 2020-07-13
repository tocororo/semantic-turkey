package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import java.util.List;

public class DiffResultStructure {
    private String sparqlEndpoint1;
    private String sparqlEndpoint2;
    private String taskId;
    private String  lexicalization1;
    private String lexicalization2;
    private List<String> langList;

    private List<ResourceWithLexicalization> removedResources;
    private List<ResourceWithLexicalization> addedResources;

    private List<ChangedResource> changedResources;

    private List<LabelWithResAndLitForm> removeLabels;
    private List<LabelWithResAndLitForm> addedLabels;

    private List<ChangedLabel> changedLabels;


    public DiffResultStructure() {
    }

    public DiffResultStructure(String sparqlEndpoint1, String sparqlEndpoint2, String taskId, String lexicalization1, String lexicalization2, List<String> langList,
            List<ResourceWithLexicalization> removedResources, List<ResourceWithLexicalization> addedResources, List<ChangedResource> changedResources,
            List<LabelWithResAndLitForm> removeLabels, List<LabelWithResAndLitForm> addedLabels, List<ChangedLabel> changedLabels) {
        this.sparqlEndpoint1 = sparqlEndpoint1;
        this.sparqlEndpoint2 = sparqlEndpoint2;
        this.taskId = taskId;
        this.lexicalization1 = lexicalization1;
        this.lexicalization2 = lexicalization2;
        this.langList = langList;
        this.removedResources = removedResources;
        this.addedResources = addedResources;
        this.changedResources = changedResources;
        this.removeLabels = removeLabels;
        this.addedLabels = addedLabels;
        this.changedLabels = changedLabels;
    }

    public String getSparqlEndpoint1() {
        return sparqlEndpoint1;
    }

    public String getSparqlEndpoint2() {
        return sparqlEndpoint2;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getLexicalization1() {
        return lexicalization1;
    }

    public String getLexicalization2() {
        return lexicalization2;
    }

    public List<String> getLangList() {
        return langList;
    }

    public List<ResourceWithLexicalization> getRemovedResources() {
        return removedResources;
    }

    public List<ResourceWithLexicalization> getAddedResources() {
        return addedResources;
    }

    public List<ChangedResource> getChangedResources() {
        return changedResources;
    }

    public List<LabelWithResAndLitForm> getRemoveLabels() {
        return removeLabels;
    }

    public List<LabelWithResAndLitForm> getAddedLabels() {
        return addedLabels;
    }

    public List<ChangedLabel> getChangedLabels() {
        return changedLabels;
    }
}


