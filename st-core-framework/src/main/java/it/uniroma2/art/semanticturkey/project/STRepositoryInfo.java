package it.uniroma2.art.semanticturkey.project;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class STRepositoryInfo {
	private String backendType;
	private String username;
	private String password;

	@JsonCreator
	public STRepositoryInfo(@JsonProperty("backendType") String backendType,
			@JsonProperty("username") String username, @JsonProperty("password") String password) {
		this.backendType = backendType;
		this.username = username;
		this.password = password;
	}

	public @Nullable String getBackendType() {
		return backendType;
	}

	public void setBackendType(String backendType) {
		this.backendType = backendType;
	}

	public @Nullable String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}