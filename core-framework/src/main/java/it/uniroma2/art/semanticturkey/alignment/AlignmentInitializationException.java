package it.uniroma2.art.semanticturkey.alignment;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class AlignmentInitializationException extends InternationalizedException {

	private static final long serialVersionUID = 4459950090079076588L;

	public AlignmentInitializationException(String key, Object[] args) {
		super(key, args);
	}
	
	public AlignmentInitializationException(Throwable cause) {
		super(cause);
	}

}
