package it.uniroma2.art.semanticturkey.pmki;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a contribution approved by the administrator (the projects has been created)
 * which the data has still not loaded by the contributor.
 * A PendingContribution is useful in order to check that the token and the project passed by the contributor
 * matches and so the contributor is authorized to load the data.
 * A PendingContribution is stored as Json serialized object in a system setting file.
 */
public class PendingContribution {
	private String projectName;
	private String contributorEmail;
	private String contributorName;
	private String contributorLastName;
	private long timestamp; //useful to check expired pending contributions

	@JsonCreator
	public PendingContribution(@JsonProperty("projectName") String projectName,
			@JsonProperty("contributorEmail") String contributorEmail,
			@JsonProperty("contributorName") String contributorName,
			@JsonProperty("contributorLastName") String contributorLastName,
			@JsonProperty("timestamp") long timestamp) {
		this.projectName = projectName;
		this.contributorEmail = contributorEmail;
		this.contributorName = contributorName;
		this.contributorLastName = contributorLastName;
		this.timestamp = timestamp;
	}

	public PendingContribution(String projectName, String contributorEmail, String contributorName, String contributorLastName) {
		this(projectName, contributorEmail, contributorName, contributorLastName, System.currentTimeMillis());
	}

	public String getProjectName() {
		return projectName;
	}

	public String getContributorEmail() {
		return contributorEmail;
	}

	public String getContributorName() {
		return contributorName;
	}

	public String getContributorLastName() {
		return contributorLastName;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
