package it.uniroma2.art.semanticturkey.rbac;

public class RoleNotExistingException extends RBACException {

	private static final long serialVersionUID = 3728225532383630262L;

	public RoleNotExistingException(String role) {
		super(RoleNotExistingException.class.getName() + ".message", new Object[] {role});
	}
}
