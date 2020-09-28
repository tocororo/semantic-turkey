package it.uniroma2.art.semanticturkey.resources;

import java.util.List;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.google.common.collect.ImmutableList;

/**
 * Represents a data format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class DataFormat {
	private final String name;
	private final String defaultMimeType;
	private final List<String> mimeTypes;
	private final String defaultFileExtension;
	private final List<String> fileExtensions;

	public DataFormat(String name, String defaultMimeType, String defaultFileExtension) {
		this(name, defaultMimeType, defaultFileExtension, ImmutableList.of(defaultMimeType),
				ImmutableList.of(defaultFileExtension));
	}

	public DataFormat(String name, String defaultMimeType, String defaultFileExtension,
			List<String> mimeTypes, List<String> fileExtensions) {
		this.name = name;
		this.defaultMimeType = defaultMimeType;
		this.defaultFileExtension = defaultFileExtension;
		this.mimeTypes = ImmutableList.copyOf(mimeTypes);
		this.fileExtensions = ImmutableList.copyOf(fileExtensions);
	}

	public String getName() {
		return name;
	}

	public String getDefaultMimeType() {
		return defaultMimeType;
	}

	public List<String> getMimeTypes() {
		return mimeTypes;
	}

	public String getDefaultFileExtension() {
		return defaultFileExtension;
	}

	public List<String> getFileExtensions() {
		return fileExtensions;
	}

	public static DataFormat valueOf(RDFFormat rdfFormat) {
		return new DataFormat(rdfFormat.getName(), rdfFormat.getDefaultMIMEType(),
				rdfFormat.getDefaultFileExtension(), rdfFormat.getMIMETypes(), rdfFormat.getFileExtensions());
	}
}
