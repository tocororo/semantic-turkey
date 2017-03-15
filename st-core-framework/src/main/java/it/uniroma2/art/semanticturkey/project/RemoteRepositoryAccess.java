package it.uniroma2.art.semanticturkey.project;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Abstract base class of concrete remote repository access options: {@link CreateRemote},
 * {@link AccessExistingRemote}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
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

	
	@Override
	public boolean isLocal() {
		return false;
	}
	
	@Override
	public boolean isRemote() {
		return true;
	}
}
