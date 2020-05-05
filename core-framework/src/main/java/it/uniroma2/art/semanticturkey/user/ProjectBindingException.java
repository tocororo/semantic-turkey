package it.uniroma2.art.semanticturkey.user;

public class ProjectBindingException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2818909745505194058L;

	public ProjectBindingException(Throwable e) {
		super(e);
	}
	
	public ProjectBindingException(String msg) {
		super(msg);
	}

}
