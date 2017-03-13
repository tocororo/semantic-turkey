package it.uniroma2.art.semanticturkey.services.core.projects;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRemote extends RemoteRepositoryAccess {

	@JsonCreator
	public CreateRemote(@JsonProperty("serverURL") URL serverURL, @JsonProperty("username") String username,
			@JsonProperty("password") String password) {
		super(serverURL, username, password);
	}

}
