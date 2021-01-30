package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DiffResultStructure {

    private DatasetInfo leftDataset;
    private DatasetInfo rightDataset;

    private String taskId;
    private List<String> langList;

    private List<ResourceWithLexicalization> removedResources;
    private List<ResourceWithLexicalization> addedResources;

    private List<ChangedResource> changedResources;

    private List<LabelWithResAndLitForm> removeLabels;
    private List<LabelWithResAndLitForm> addedLabels;

    private List<ChangedLabel> changedLabels;


    @JsonCreator
    public DiffResultStructure(@JsonProperty("projectName1")String projectName1, @JsonProperty("versionRepoId1")String versionRepoId1,
            @JsonProperty("sparqlEndpoint1")String sparqlEndpoint1, @JsonProperty("lexicalization1")String lexicalization1,
            @JsonProperty("username1")String username1, @JsonProperty("password1")String password1,
            @JsonProperty("projectName2")String projectName2, @JsonProperty("versionRepoId2")String versionRepoId2,
            @JsonProperty("sparqlEndpoint2")String sparqlEndpoint2, @JsonProperty("lexicalization2")String lexicalization2,
            @JsonProperty("username2")String username2, @JsonProperty("password2")String password2,
            @JsonProperty("taskId")String taskId,  @JsonProperty("langList")List<String> langList,
            @JsonProperty("removedResources")List<ResourceWithLexicalization> removedResources,
            @JsonProperty("addedResources")List<ResourceWithLexicalization> addedResources,
            @JsonProperty("changedResources")List<ChangedResource> changedResources,
            @JsonProperty("removeLabels")List<LabelWithResAndLitForm> removeLabels,
            @JsonProperty("addedLabels")List<LabelWithResAndLitForm> addedLabels,
            @JsonProperty("changedLabels")List<ChangedLabel> changedLabels) {
        this.leftDataset = new DatasetInfo(projectName1, versionRepoId1, sparqlEndpoint1, lexicalization1, username1, password1);
        this.rightDataset = new DatasetInfo(projectName2, versionRepoId2, sparqlEndpoint2, lexicalization2, username2, password2);
        this.taskId = taskId;
        this.langList = langList;
        this.removedResources = removedResources;
        this.addedResources = addedResources;
        this.changedResources = changedResources;
        this.removeLabels = removeLabels;
        this.addedLabels = addedLabels;
        this.changedLabels = changedLabels;
    }

    public DatasetInfo getLeftDataset() {
        return leftDataset;
    }

    public DatasetInfo getRightDataset() {
        return rightDataset;
    }

    public String getTaskId() {
        return taskId;
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


