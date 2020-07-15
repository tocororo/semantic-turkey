package it.uniroma2.art.semanticturkey.services.core.skosdiff;

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


    public DiffResultStructure() {
    }

    public DiffResultStructure(String sparqlEndpoint1, String lexicalization1, String projectName1, String versionRepoId1,
            String sparqlEndpoint2, String lexicalization2, String projectName2, String versionRepoId2,
            String taskId,  List<String> langList,
            List<ResourceWithLexicalization> removedResources, List<ResourceWithLexicalization> addedResources, List<ChangedResource> changedResources,
            List<LabelWithResAndLitForm> removeLabels, List<LabelWithResAndLitForm> addedLabels, List<ChangedLabel> changedLabels) {
        this.leftDataset = new DatasetInfo(projectName1, versionRepoId1, sparqlEndpoint1, lexicalization1);
        this.rightDataset = new DatasetInfo(projectName2, versionRepoId2, sparqlEndpoint2, lexicalization2);
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


