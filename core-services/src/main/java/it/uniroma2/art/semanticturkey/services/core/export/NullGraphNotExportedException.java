package it.uniroma2.art.semanticturkey.services.core.export;

public class NullGraphNotExportedException extends ExportPreconditionViolationException {

	private static final long serialVersionUID = 8685618562060325463L;

	public NullGraphNotExportedException() {
		super(NullGraphNotExportedException.class.getName() + ".message", null);
	}
}
