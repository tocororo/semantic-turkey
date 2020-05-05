package it.uniroma2.art.semanticturkey.rbac;

public class HarmingGoalException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4025443930777802514L;

	
    public HarmingGoalException() {
        super("the RBAC engine has been halted for some reason");
    }
	
    
    public HarmingGoalException(String message) {
        super(message);
    }
    
}
