package it.uniroma2.art.semanticturkey.mdr.core;

import org.eclipse.rdf4j.model.IRI;

public class NoSuchCatalogRecordException extends MetadataRegistryException {

	private static final long serialVersionUID = 5646006474896819426L;

	public NoSuchCatalogRecordException(IRI catalogRecord, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super("The repository does not contain the catalog record '" + catalogRecord + "'", cause,
				enableSuppression, writableStackTrace);
	}

	public NoSuchCatalogRecordException(IRI catalogRecord, Throwable cause) {
		super("The repository does not contain the catalog record '" + catalogRecord + "'", cause);
	}

	public NoSuchCatalogRecordException(IRI catalogRecord) {
		super("The repository does not contain the catalog record '" + catalogRecord + "'");
	}

}
