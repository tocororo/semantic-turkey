package it.uniroma2.art.semanticturkey.services.core.ontmanager;

/**
 * Information about an entry of the ontology mirror.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntologyMirrorEntryInfo {

	private final String baseURI;
	private final String file;

	public OntologyMirrorEntryInfo(String baseURI, String file) {
		this.baseURI = baseURI;
		this.file = file;
	}
	
	public String getBaseURI() {
		return baseURI;
	}

	public String getFile() {
		return file;
	}
}
