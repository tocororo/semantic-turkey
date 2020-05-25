package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import java.util.Date;

import org.apache.felix.bundlerepository.impl.ReasonImpl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class TaskDTO {
	private String id;
	private DatasetInfo leftDataset;
	private DatasetInfo rightDataset;
	private String status;
	private ReasonInfo reason;
	private Integer progress;
	@JsonFormat(shape = Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "us")
	private Date startTime;
	@JsonFormat(shape = Shape.STRING, pattern = "EEE MMM dd HH:mm:ss Z yyyy", locale = "us")
	private Date endTime;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DatasetInfo getLeftDataset() {
		return leftDataset;
	}

	public void setLeftDataset(DatasetInfo leftDataset) {
		this.leftDataset = leftDataset;
	}

	public DatasetInfo getRightDataset() {
		return rightDataset;
	}

	public void setRightDataset(DatasetInfo rightDataset) {
		this.rightDataset = rightDataset;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public ReasonInfo getReason() {
		return reason;
	}
	
	public void setReason(ReasonInfo reason) {
		this.reason = reason;
	}

	public Integer getProgress() {
		return progress;
	}
	
	public void setProgress(Integer progress) {
		this.progress = progress;
	}
	
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

}
