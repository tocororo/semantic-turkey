package it.uniroma2.art.semanticturkey.rbac;

public class RBACException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -619227838879011063L;
	
	public RBACException(String message) {
        super(message);
    }
	
	public RBACException(Exception e) {
		super(e);
	}
	
	public RBACException(String message, Exception e) {
        super(message, e);
    }

}
