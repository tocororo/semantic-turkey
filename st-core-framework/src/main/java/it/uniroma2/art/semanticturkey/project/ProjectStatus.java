package it.uniroma2.art.semanticturkey.project;

public class ProjectStatus {
	
	public enum Status {
		ok, corrupted, error
	}
	
	private Status status;
	private String message;
	
	public ProjectStatus(Status status) {
		this(status, null);
	}
	
	public ProjectStatus(Status status, String message) {
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
