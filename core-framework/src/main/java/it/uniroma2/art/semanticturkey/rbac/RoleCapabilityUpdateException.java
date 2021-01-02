package it.uniroma2.art.semanticturkey.rbac;

public class RoleCapabilityUpdateException extends RBACException {

	private static final long serialVersionUID = -505937836072612762L;

	public RoleCapabilityUpdateException(String role, Throwable cause) {
		super(RoleCapabilityUpdateException.class.getName() + ".message", new Object[] {role});
	}
	
	public RoleCapabilityUpdateException(String role) {
		this(role, null);
	}

}
