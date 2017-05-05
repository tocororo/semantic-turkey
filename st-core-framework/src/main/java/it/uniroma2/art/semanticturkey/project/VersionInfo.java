package it.uniroma2.art.semanticturkey.project;

import java.util.Date;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about a version dump.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionInfo {
	public static enum RepositoryStatus {
		INITIALIZED, UNITIALIZED
	}

	private final String versionId;
	private final Date dateTime;
	private final String repositoryId;
	private final RepositoryStatus repositoryStatus;

	public VersionInfo(@JsonProperty("versionId") String versionId, @JsonProperty("dateTime") Date dateTime,
			@JsonProperty("repositoryId") String repositoryId) {
		this(versionId, dateTime, repositoryId, null);
	}

	public VersionInfo(String versionId, Date dateTime, String repositoryId,
			RepositoryStatus repositoryStatus) {
		this.versionId = versionId;
		this.dateTime = dateTime;
		this.repositoryId = repositoryId;
		this.repositoryStatus = repositoryStatus;
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
	
	public @Nullable RepositoryStatus getRepositoryStatus() {
		return repositoryStatus;
	}
	
	public VersionInfo newWithRepositoryStatus(RepositoryStatus repositoryStatus) {
		return new VersionInfo(this.versionId, this.dateTime, this.repositoryId, repositoryStatus);
	}
}
