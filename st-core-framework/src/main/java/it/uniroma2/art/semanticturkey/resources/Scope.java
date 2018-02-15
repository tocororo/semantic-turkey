package it.uniroma2.art.semanticturkey.resources;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;

public enum Scope {
	PROJECT("proj"), SYSTEM("sys"), USER("usr"), PROJECT_USER("pu");

	private String serializationCode;

	private Scope(String serializationCode) {
		this.serializationCode = serializationCode;
	}

	public String getSerializationCode() {
		return serializationCode;
	}

	public static Scope deserializeScope(String serializationCode) {
		switch (serializationCode) {
		case "sys":
			return SYSTEM;
		case "proj":
			return PROJECT;
		case "usr":
			return USER;
		case "pu":
			return PROJECT_USER;
		default:
			throw new IllegalArgumentException("Invalid serialization code: " + serializationCode);
		}
	}

	public static Scope computeScope(@Nullable Project project, @Nullable STUser user) {
		if (project == null) {
			if (user == null) {
				return Scope.SYSTEM;
			} else {
				return Scope.USER;
			}
		} else {
			if (user == null) {
				return Scope.PROJECT;
			} else {
				return PROJECT_USER;
			}
		}
	}

}
