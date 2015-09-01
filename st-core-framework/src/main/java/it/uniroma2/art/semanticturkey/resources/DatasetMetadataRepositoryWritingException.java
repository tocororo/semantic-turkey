package it.uniroma2.art.semanticturkey.resources;

public class DatasetMetadataRepositoryWritingException extends DatasetMetadataRepositoryException {

	private static final long serialVersionUID = 1065067048834964363L;

	public DatasetMetadataRepositoryWritingException() {
		super();
	}

	public DatasetMetadataRepositoryWritingException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DatasetMetadataRepositoryWritingException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatasetMetadataRepositoryWritingException(String message) {
		super(message);
	}

	public DatasetMetadataRepositoryWritingException(Throwable cause) {
		super(cause);
	}

}
