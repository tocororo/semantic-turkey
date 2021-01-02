package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class ProjectBindingException extends InternationalizedException {

	
	public ProjectBindingException(Throwable cause) {
		super(cause);
	}

	public ProjectBindingException(String key, Object[] args) {
		super(key, args);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2818909745505194058L;

	public static ProjectBindingException noRole(String role) {
		return new ProjectBindingException(ProjectBindingException.class.getName() + ".messages.no_role",
				new Object[] { role });
	}

}
