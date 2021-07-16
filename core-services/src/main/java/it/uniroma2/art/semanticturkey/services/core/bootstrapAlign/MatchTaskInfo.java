package it.uniroma2.art.semanticturkey.services.core.bootstrapAlign;


import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchTaskInfo {
    private String sourceProject;
    private LinksetInfo linkset1;
    private LinksetInfo linkset2;
    private String taskId;
    private String executionTime;
    private String status;

    public MatchTaskInfo() {
    }

    public MatchTaskInfo(@JsonProperty("sourceProject")String sourceProject,
                         @JsonProperty("linkset1")LinksetInfo linkset1, @JsonProperty("linkset2")LinksetInfo linkset2,
                         @JsonProperty("taskId")String taskId,
                         @JsonProperty("executionTime")String executionTime, @JsonProperty("status")String status) {
        this.sourceProject = sourceProject;
        this.linkset1 = linkset1;
        this.linkset2 = linkset2;
        this.taskId = taskId;
        this.executionTime = executionTime;
        this.status = status;
    }

    public String getSourceProject() {
        return sourceProject;
    }

    public LinksetInfo getLinkset1() {
        return linkset1;
    }

    public LinksetInfo getLinkset2() {
        return linkset2;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public String getStatus() {
        return status;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
