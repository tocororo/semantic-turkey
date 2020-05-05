package it.uniroma2.art.semanticturkey.mdr.core;

public class MetadataRegistryStateException extends MetadataRegistryException {

	private static final long serialVersionUID = -6991720615239985258L;

	public MetadataRegistryStateException() {
		super();
	}

	public MetadataRegistryStateException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MetadataRegistryStateException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataRegistryStateException(String message) {
		super(message);
	}

	public MetadataRegistryStateException(Throwable cause) {
		super(cause);
	}

}
