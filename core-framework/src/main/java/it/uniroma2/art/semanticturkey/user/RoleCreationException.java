package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class RoleCreationException extends InternationalizedException {
	
	private static final long serialVersionUID = -1815175075795467796L;

	public RoleCreationException(Throwable cause) {
		super(cause);
	}

	public RoleCreationException(String key, Object[] args) {
		super(key, args);
	}

	public static RoleCreationException noRole(String role) {
		return new RoleCreationException(RoleCreationException.class.getName() + ".messages.no_role", new Object[] { role });
	}
	
	public static RoleCreationException roleAlreadyExists(String role) {
		return new RoleCreationException(RoleCreationException.class.getName() + ".messages.role_already_exists", new Object[] { role });
	}

	public static RoleCreationException roleAlreadyExistsInProject(String role, String project) {
		return new RoleCreationException(RoleCreationException.class.getName() + ".messages.role_already_exists_in_project", new Object[] { role, project });
	}


}
