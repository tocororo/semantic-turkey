package it.uniroma2.art.semanticturkey.services.core.metadata;

/**
 * Information about a dataset in the dataset metadata registry.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class DatasetInfo {
	private final String baseURI;
	private final String title;

	public DatasetInfo(String baseURI, String title) {
		this.baseURI = baseURI;
		this.title = title;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public String getTitle() {
		return title;
	}

}
