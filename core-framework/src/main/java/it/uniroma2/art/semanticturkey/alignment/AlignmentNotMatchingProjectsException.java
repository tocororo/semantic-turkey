package it.uniroma2.art.semanticturkey.alignment;

public class AlignmentNotMatchingProjectsException extends AlignmentInitializationException {

	private static final long serialVersionUID = -2549824460178043591L;

	public AlignmentNotMatchingProjectsException() {
		super(AlignmentNotMatchingProjectsException.class.getName() + ".message", null);
	}
}
