package it.uniroma2.art.semanticturkey.user;

public class EmailVerificationExpiredException extends UserException {

	private static final long serialVersionUID = 6394358465856442370L;

	public EmailVerificationExpiredException(String email) {
		super(EmailVerificationExpiredException.class.getName() + ".message", new Object[] {email});
	}
}
