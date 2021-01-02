package it.uniroma2.art.semanticturkey.exceptions;

public class NonWorkingGraphUpdateException extends DeniedOperationException {

	private static final long serialVersionUID = -3800971029340690193L;

	public NonWorkingGraphUpdateException() {
		super(NonWorkingGraphUpdateException.class.getName() + ".message", null);
	}
}
