package it.uniroma2.art.semanticturkey.resources;

public class DuplicateDatasetMetadataException extends MetadataRegistryException {

	private static final long serialVersionUID = 4073728812818804556L;

	public DuplicateDatasetMetadataException(String baseURI, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super("The repository already contains a dataset identified by the given base URI: '" + baseURI + "'", cause, enableSuppression, writableStackTrace);
	}

	public DuplicateDatasetMetadataException(String baseURI, Throwable cause) {
		super("The repository already contains a dataset identified by the given base URI: '" + baseURI + "'", cause);
	}

	public DuplicateDatasetMetadataException(String baseURI) {
		super("The repository already contains a dataset identified by the given base URI: '" + baseURI + "'");
	}	
	
}
