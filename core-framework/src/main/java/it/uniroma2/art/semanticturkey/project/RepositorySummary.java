package it.uniroma2.art.semanticturkey.project;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RepositorySummary {

	public static class RemoteRepositorySummary {
		private final String serverURL;
		private final String repositoryId;
		private final String username;
		private final String password;

		@JsonCreator
		public RemoteRepositorySummary(@JsonProperty("serverURL") String serverURL,
				@JsonProperty("repositoryId") String repositoryId, @JsonProperty("username") String username,
				@JsonProperty("password") String password) {
			this.serverURL = serverURL;
			this.repositoryId = repositoryId;
			this.username = username;
			this.password = password;
		}

		public String getServerURL() {
			return serverURL;
		}

		public String getRepositoryId() {
			return repositoryId;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
	}

	private final String id;
	private final String description;
	private RemoteRepositorySummary remoteRepoSummary;

	@JsonCreator
	public RepositorySummary(@JsonProperty("id") String id, @JsonProperty("description") String description,
			@JsonProperty("remoteRepoSummary") @Nullable RemoteRepositorySummary remoteRepoSummary) {
		this.id = id;
		this.description = description;
		this.remoteRepoSummary = remoteRepoSummary;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	@Nullable
	public RemoteRepositorySummary getRemoteRepoSummary() {
		return remoteRepoSummary;
	}
}
