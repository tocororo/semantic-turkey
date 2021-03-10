package it.uniroma2.art.semanticturkey.exceptions;

public class MissingDatasetMetadataException extends DatasetMetadataException {

	private static final long serialVersionUID = 4801132399639013355L;

	public MissingDatasetMetadataException(String position) {
		super(MissingDatasetMetadataException.class.getName() + ".message", new Object[] { position });
	}
}
