package it.uniroma2.art.semanticturkey.project;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

/**
 * Information about a version dump.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersionInfo {
	private final String versionId;
	private final String repositoryId;
	private final Date dateTime;

	public VersionInfo(@JsonProperty("versionId") String versionId,
			@JsonProperty("dateTime") Date dateTime,
			@JsonProperty("repositoryId") String repositoryId) {
		this.versionId = versionId;
		this.dateTime = dateTime;
		this.repositoryId = repositoryId;
	}

	public String getVersionId() {
		return this.versionId;
	}

	@JsonFormat(shape = Shape.STRING)
	public Date getDateTime() {
		return dateTime;
	}

	public String getRepositoryId() {
		return this.repositoryId;
	}
}
