package it.uniroma2.art.semanticturkey.rbac;

public class DuplicatedCapabilityException extends RBACException {

	private static final long serialVersionUID = -4652612123740922841L;

	public DuplicatedCapabilityException(String capability, String role) {
		super(DuplicatedCapabilityException.class.getName() + ".message", new Object[] {capability, role});
	}
}
