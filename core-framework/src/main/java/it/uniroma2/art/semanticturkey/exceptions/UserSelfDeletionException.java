package it.uniroma2.art.semanticturkey.exceptions;

public class UserSelfDeletionException extends DeniedOperationException {

	private static final long serialVersionUID = -857733059420244699L;

	public UserSelfDeletionException() {
		super(UserSelfDeletionException.class.getName() + ".message", null);
	}
}
