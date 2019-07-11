package it.uniroma2.art.semanticturkey.services.core;

/**
 * Indicates that a service was unable to determine the language to index (and thus sort) the correspondences
 * in an EDOAL project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class IndexingLanguageNotFound extends Exception {

	private static final long serialVersionUID = 1L;

	public IndexingLanguageNotFound(String message, Throwable cause) {
		super(message, cause);
	}

	public IndexingLanguageNotFound(String message) {
		super(message);
	}

}
