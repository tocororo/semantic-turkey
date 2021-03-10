package it.uniroma2.art.semanticturkey.exceptions;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class DatasetMetadataException extends InternationalizedException {

	private static final long serialVersionUID = -78312546224860171L;

	public DatasetMetadataException(String key, Object[] args, Throwable cause) {
		super(key, args, cause);
		// TODO Auto-generated constructor stub
	}

	public DatasetMetadataException(String key, Object[] args) {
		super(key, args);
		// TODO Auto-generated constructor stub
	}

	public DatasetMetadataException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	
}
