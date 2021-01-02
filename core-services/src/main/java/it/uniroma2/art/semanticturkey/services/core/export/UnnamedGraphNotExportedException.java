package it.uniroma2.art.semanticturkey.services.core.export;

public class UnnamedGraphNotExportedException extends ExportPreconditionViolationException {

	private static final long serialVersionUID = 8685618562060325463L;

	public UnnamedGraphNotExportedException() {
		super(UnnamedGraphNotExportedException.class.getName() + ".message", null);
	}
}
