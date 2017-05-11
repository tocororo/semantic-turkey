package it.uniroma2.art.semanticturkey.plugin.extpts;

/**
 * Exception occurred during the generation of dataset metadata (see {@link DatasetMetadataExporter})
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DatasetMetadataExporterException extends Exception {

	private static final long serialVersionUID = 1L;

	public DatasetMetadataExporterException() {
		super();
	}

	public DatasetMetadataExporterException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DatasetMetadataExporterException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatasetMetadataExporterException(String message) {
		super(message);
	}

	public DatasetMetadataExporterException(Throwable cause) {
		super(cause);
	}

}
