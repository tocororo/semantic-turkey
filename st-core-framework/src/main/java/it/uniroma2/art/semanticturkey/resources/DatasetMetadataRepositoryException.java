package it.uniroma2.art.semanticturkey.resources;

public abstract class DatasetMetadataRepositoryException extends Exception {

	private static final long serialVersionUID = 275812922919804831L;

	public DatasetMetadataRepositoryException() {
		super();
	}

	public DatasetMetadataRepositoryException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DatasetMetadataRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatasetMetadataRepositoryException(String message) {
		super(message);
	}

	public DatasetMetadataRepositoryException(Throwable cause) {
		super(cause);
	}

}
