package it.uniroma2.art.semanticturkey.user;

public class RoleCreationException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1815175075795467796L;

	public RoleCreationException(Throwable e) {
		super(e);
	}
	
	public RoleCreationException(String msg) {
		super(msg);
	}

}
