package it.uniroma2.art.semanticturkey.resources;

public class NoSuchDatasetMetadataException extends MetadataRegistryException {

	private static final long serialVersionUID = 4073728812818804556L;

	public NoSuchDatasetMetadataException(String baseURI, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super("The repository does not contain metadata about the dataset '" + baseURI + "'", cause, enableSuppression, writableStackTrace);
	}

	public NoSuchDatasetMetadataException(String baseURI, Throwable cause) {
		super("The repository does not contain metadata about the dataset '" + baseURI + "'", cause);
	}

	public NoSuchDatasetMetadataException(String baseURI) {
		super("The repository does not contain metadata about the dataset '" + baseURI + "'");
	}	
	
}
