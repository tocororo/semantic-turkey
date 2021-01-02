package it.uniroma2.art.semanticturkey.alignment;

public class AlignmentNotMatchingCurrentProject extends AlignmentInitializationException {

	private static final long serialVersionUID = -2549824460178043591L;

	public AlignmentNotMatchingCurrentProject() {
		super(AlignmentNotMatchingCurrentProject.class.getName() + ".message", null);
	}
}
