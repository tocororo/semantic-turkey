package it.uniroma2.art.semanticturkey.rbac;

public class HaltedEngineException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4025443930777802514L;

	
    public HaltedEngineException() {
        super("the RBAC engine has been halted for some reason");
    }
	
    
    public HaltedEngineException(String message) {
        super(message);
    }
    
}
