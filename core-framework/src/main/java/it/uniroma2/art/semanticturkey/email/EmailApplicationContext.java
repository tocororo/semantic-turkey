package it.uniroma2.art.semanticturkey.email;

/**
 * This enum is useful to distinguish the application that is requiring the usage of an email-service in order
 * to send and email notification (e.g. if it is required the password reset, the system needs to know if it
 * has been required from the VB or ShowVoc client application)
 */
public enum EmailApplicationContext {
	SHOWVOC, VB
}
