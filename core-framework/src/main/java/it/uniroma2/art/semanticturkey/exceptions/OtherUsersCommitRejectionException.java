package it.uniroma2.art.semanticturkey.exceptions;

public class OtherUsersCommitRejectionException extends DeniedOperationException {

	private static final long serialVersionUID = -8766979232111859833L;

	public OtherUsersCommitRejectionException() {
		super(OtherUsersCommitRejectionException.class.getName() + ".message", null);
	}
}
