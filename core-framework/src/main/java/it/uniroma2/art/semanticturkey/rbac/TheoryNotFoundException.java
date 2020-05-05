package it.uniroma2.art.semanticturkey.rbac;

public class TheoryNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4025443930777802514L;

	
    public TheoryNotFoundException(Throwable cause) {
        super("the theory has not been loaded", cause);
    }
	
    
    public TheoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
