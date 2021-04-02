package it.uniroma2.art.semanticturkey.user;

public enum UserStatus {
	UNVERIFIED, //just registered, email not yet verified
	NEW, //verified, not yet enabled
	ACTIVE,
	INACTIVE
}
