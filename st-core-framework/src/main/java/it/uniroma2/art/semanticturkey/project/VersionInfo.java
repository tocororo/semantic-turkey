package it.uniroma2.art.semanticturkey.project;

import java.time.Instant;

/**
 * Information about a version dump.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersionInfo {
	private final String versionId;
	private final String repositoryId;
	private final Instant instant;

	public VersionInfo(String versionId, Instant instant, String repositoryId) {
		this.versionId = versionId;
		this.instant = instant;
		this.repositoryId = repositoryId;
	}

	public String getVersionId() {
		return this.versionId;
	}
	
	public Instant getInstant() {
		return instant;
	}

	public String getRepositoryId() {
		return this.repositoryId;
	}
}
