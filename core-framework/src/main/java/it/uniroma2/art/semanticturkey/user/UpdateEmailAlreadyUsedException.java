package it.uniroma2.art.semanticturkey.user;

public class UpdateEmailAlreadyUsedException extends UserException {

	private static final long serialVersionUID = -7780197387823051464L;

	public UpdateEmailAlreadyUsedException(String email, String newEmail) {
		super(UpdateEmailAlreadyUsedException.class.getName() + ".message", new Object[] {email, newEmail});
	}
	
}
