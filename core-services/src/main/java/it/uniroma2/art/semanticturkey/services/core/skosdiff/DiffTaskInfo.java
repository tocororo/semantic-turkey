package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DiffTaskInfo {
	private DatasetInfo leftDataset;
	private DatasetInfo rightDataset;
	private String taskId;
	private String executionTime;
	private String status;
	private List<String> langsShown;

	public DiffTaskInfo() {
	}

	@JsonCreator
	public DiffTaskInfo(@JsonProperty("leftDataset") DatasetInfo leftDataset,
						@JsonProperty("rightDataset") DatasetInfo rightDataset,
						@JsonProperty("taskId") String taskId, @JsonProperty("executionTime") String executionTime,
						@JsonProperty("status") String status, @JsonProperty("langsShown") List<String> langsShown) {
		this.leftDataset = leftDataset;
		this.rightDataset = rightDataset;
		this.taskId = taskId;
		this.executionTime = executionTime;
		this.status = status;
		this.langsShown = langsShown;
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

	public String getExecutionTime() {
		return executionTime;
	}

	public String getStatus() {
		return status;
	}

	public List<String> getLangsShown() {
		return langsShown;
	}

}