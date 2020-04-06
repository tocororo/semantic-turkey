package it.uniroma2.art.semanticturkey.mdr.core;

public class MetadataRegistryWritingException extends MetadataRegistryException {

	private static final long serialVersionUID = 1065067048834964363L;

	public MetadataRegistryWritingException() {
		super();
	}

	public MetadataRegistryWritingException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MetadataRegistryWritingException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataRegistryWritingException(String message) {
		super(message);
	}

	public MetadataRegistryWritingException(Throwable cause) {
		super(cause);
	}

}
