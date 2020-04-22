package it.uniroma2.art.semanticturkey.customservice;

public class DuplicateIdException extends CustomServiceException {

	private static final long serialVersionUID = 1L;

	public DuplicateIdException(String msg) {
		super(msg);
	}

}
