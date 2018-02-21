package it.uniroma2.art.semanticturkey.services.core.projects;

import javax.annotation.Nullable;

public class RepositorySummary {

	public static class RemoteRepositorySummary {
		private final String serverURL;
		private final String repositoryId;
		private final String username;
		private final String password;

		public RemoteRepositorySummary(String serverURL, String repositoryId, String username,
				String password) {
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

	public RepositorySummary(String id, String description,
			@Nullable RemoteRepositorySummary remoteRepoSummary) {
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
