package it.uniroma2.art.semanticturkey.alignment;

public class ReversedAlignmentWithCustomRelationsException extends AlignmentInitializationException {

	private static final long serialVersionUID = -2549824460178043591L;

	public ReversedAlignmentWithCustomRelationsException() {
		super(ReversedAlignmentWithCustomRelationsException.class.getName() + ".message", null);
	}
}
