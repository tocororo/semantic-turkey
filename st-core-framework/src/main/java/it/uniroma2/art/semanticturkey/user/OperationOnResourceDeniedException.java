package it.uniroma2.art.semanticturkey.user;

public class OperationOnResourceDeniedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2146605441785396178L;
	
	public OperationOnResourceDeniedException(Throwable e) {
		super(e);
	}
	
	public OperationOnResourceDeniedException(String msg) {
		super(msg);
	}

}
