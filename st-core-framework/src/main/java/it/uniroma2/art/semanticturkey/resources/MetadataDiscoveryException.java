package it.uniroma2.art.semanticturkey.resources;

public class MetadataDiscoveryException extends MetadataRegistryException {

	private static final long serialVersionUID = 275812922919804831L;

	public MetadataDiscoveryException() {
		super();
	}

	public MetadataDiscoveryException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MetadataDiscoveryException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataDiscoveryException(String message) {
		super(message);
	}

	public MetadataDiscoveryException(Throwable cause) {
		super(cause);
	}

}
