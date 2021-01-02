package it.uniroma2.art.semanticturkey.user;

public class EmailAlreadyUsedException extends UserException {

	private static final long serialVersionUID = -4351026433257973006L;

	public EmailAlreadyUsedException(String email) {
		super(EmailAlreadyUsedException.class.getName() + ".message", new Object[] {email});
	}
}
