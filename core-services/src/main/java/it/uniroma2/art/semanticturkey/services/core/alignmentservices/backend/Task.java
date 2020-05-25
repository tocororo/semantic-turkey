package it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend;

import java.time.OffsetDateTime;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;

public class Task {
	private String id;
	@JsonSerialize(converter = IRI2StringConverter.class)
	@JsonDeserialize(converter = String2IRIConverter.class)
	private IRI leftDataset;
	@JsonSerialize(converter = IRI2StringConverter.class)
	@JsonDeserialize(converter = String2IRIConverter.class)
	private IRI rightDataset;
	private String status;
	private OffsetDateTime startTime;
	private OffsetDateTime submissionTime;
	private OffsetDateTime endTime;
	private int progress;
	private ReasonInfo reason;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public IRI getLeftDataset() {
		return leftDataset;
	}

	public void setLeftDataset(IRI leftDataset) {
		this.leftDataset = leftDataset;
	}

	public IRI getRightDataset() {
		return rightDataset;
	}

	public void setRightDataset(IRI rightDataset) {
		this.rightDataset = rightDataset;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public OffsetDateTime getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(OffsetDateTime submissionTime) {
		this.submissionTime = submissionTime;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public OffsetDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(OffsetDateTime endTime) {
		this.endTime = endTime;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public ReasonInfo getReason() {
		return reason;
	}

	public void setReason(ReasonInfo reason) {
		this.reason = reason;
	}

}
