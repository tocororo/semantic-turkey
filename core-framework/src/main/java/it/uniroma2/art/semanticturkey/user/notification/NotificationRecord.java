package it.uniroma2.art.semanticturkey.user.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

public class NotificationRecord {
	private String proj;
	private String resource;
	private RDFResourceRole role;
	private NotificationPreferencesAPI.Action action;
	private String timestamp;

	public NotificationRecord(String proj, String resource, RDFResourceRole role, NotificationPreferencesAPI.Action action, String timestamp) {
		this.proj = proj;
		this.resource = resource;
		this.role = role;
		this.action = action;
		this.timestamp = timestamp;
	}

	public String getProj() {
		return proj;
	}

	public String getResource() {
		return resource;
	}

	public RDFResourceRole getRole() {
		return role;
	}

	public NotificationPreferencesAPI.Action getAction() {
		return action;
	}

	public String getTimestamp() {
		return timestamp;
	}
}
