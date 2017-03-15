package it.uniroma2.art.semanticturkey.project;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes the access to a remote server for accessing existing (remote) repositories.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class AccessExistingRemote extends RemoteRepositoryAccess {

	@JsonCreator
	public AccessExistingRemote(@JsonProperty("serverURL") URL serverURL, @JsonProperty("username") String username,
			@JsonProperty("password") String password) {
		super(serverURL, username, password);
	}

}
