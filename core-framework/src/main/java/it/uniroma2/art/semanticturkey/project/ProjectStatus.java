package it.uniroma2.art.semanticturkey.project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectStatus {
	
	public enum Status {
		ok, corrupted, error
	}
	
	private Status status;
	private String message;
	
	public ProjectStatus(Status status) {
		this(status, null);
	}
	
	@JsonCreator
	public ProjectStatus(@JsonProperty("status") Status status, @JsonProperty("message") String message) {
		this.status = status;
		this.message = message;
	}
	
	public Status getStatus() {
		return this.status;
	}
	
	public String getMessage() {
		return this.message;
	}
	
}
