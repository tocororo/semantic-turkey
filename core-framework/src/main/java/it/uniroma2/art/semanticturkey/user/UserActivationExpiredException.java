package it.uniroma2.art.semanticturkey.user;

public class UserActivationExpiredException extends UserException {

	private static final long serialVersionUID = 6394358465856442370L;

	public UserActivationExpiredException(String email) {
		super(UserActivationExpiredException.class.getName() + ".message", new Object[] {email});
	}
}
