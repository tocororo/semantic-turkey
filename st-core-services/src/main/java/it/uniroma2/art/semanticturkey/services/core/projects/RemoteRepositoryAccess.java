package it.uniroma2.art.semanticturkey.services.core.projects;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class RemoteRepositoryAccess extends RepositoryAccess {

	private URL serverURL;
	private String username;
	private String password;

	public RemoteRepositoryAccess(@JsonProperty URL serverURL, @JsonProperty String username,
			@JsonProperty String password) {
		this.serverURL = serverURL;
		this.username = username;
		this.password = password;
	}

	public URL getServerURL() {
		return serverURL;
	}

	public void setServerURL(URL serverURL) {
		this.serverURL = serverURL;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
