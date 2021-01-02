package it.uniroma2.art.semanticturkey.rbac;

public class InvalidRoleFileException extends RBACException {

	private static final long serialVersionUID = -4652612123740922841L;

	public InvalidRoleFileException(Throwable cause) {
		super(InvalidRoleFileException.class.getName() + ".message", null, cause);
	}
}
