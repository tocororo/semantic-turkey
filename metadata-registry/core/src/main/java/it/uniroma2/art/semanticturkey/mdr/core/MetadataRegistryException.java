package it.uniroma2.art.semanticturkey.mdr.core;

public abstract class MetadataRegistryException extends Exception {

	private static final long serialVersionUID = 275812922919804831L;

	public MetadataRegistryException() {
		super();
	}

	public MetadataRegistryException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MetadataRegistryException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataRegistryException(String message) {
		super(message);
	}

	public MetadataRegistryException(Throwable cause) {
		super(cause);
	}

}
