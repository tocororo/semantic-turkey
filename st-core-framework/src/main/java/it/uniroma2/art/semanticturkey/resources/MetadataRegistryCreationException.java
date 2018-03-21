package it.uniroma2.art.semanticturkey.resources;

public class MetadataRegistryCreationException extends MetadataRegistryException {

	private static final long serialVersionUID = 550859343249855283L;

	public MetadataRegistryCreationException() {
		super();
	}

	public MetadataRegistryCreationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MetadataRegistryCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataRegistryCreationException(String message) {
		super(message);
	}

	public MetadataRegistryCreationException(Throwable cause) {
		super(cause);
	}

}
