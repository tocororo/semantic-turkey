package it.uniroma2.art.semanticturkey.resources;

import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * Represents a data format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class DataFormat {
	private final String name;
	private final String mimeType;
	private final String fileExtension;

	public DataFormat(String name, String mimeType, String fileExtension) {
		this.name = name;
		this.mimeType = mimeType;
		this.fileExtension = fileExtension;

	}

	public String getName() {
		return name;
	}

	public String getDefaultMimeType() {
		return mimeType;
	}

	public String getDefaultFileExtension() {
		return fileExtension;
	}

	public static DataFormat valueOf(RDFFormat rdfFormat) {
		return new DataFormat(rdfFormat.getName(), rdfFormat.getDefaultMIMEType(),
				rdfFormat.getDefaultFileExtension());
	}
}
