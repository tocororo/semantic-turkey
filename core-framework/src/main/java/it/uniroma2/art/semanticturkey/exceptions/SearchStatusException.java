package it.uniroma2.art.semanticturkey.exceptions;

public class SearchStatusException extends DeniedOperationException {

	private static final long serialVersionUID = 8715725626348721167L;

	public SearchStatusException(String key) {
		super(key, new Object[] {});
	}
}
