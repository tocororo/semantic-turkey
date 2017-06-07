package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.ontmanager.OntologyMirrorEntryInfo;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

/**
 * This class provides services for manipulating the ontology mirror.
 * 
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class OntManager extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(OntManager.class);

	public static enum UpdateType {
		webUpdate, localUpdate
	};

	/**
	 * Returns the mirrored ontologies.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<OntologyMirrorEntryInfo> getOntologyMirror() {
		return OntologiesMirror.getFullMirror().entrySet().stream()
				.map(entry -> new OntologyMirrorEntryInfo((String) entry.getKey(), (String) entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Deletes an entry (and its associated physical file) from the Ontology Mirror
	 * 
	 * @param baseURI
	 * @param cacheFileName
	 */
	@STServiceOperation
	public void deleteOntologyMirrorEntry(String baseURI, String cacheFileName) {
		OntologiesMirror.removeCachedOntologyEntry(baseURI);
		File cacheFile = new File(Resources.getOntologiesMirrorDir(), cacheFileName);
		cacheFile.delete();
	}

	/**
	 * Updates an entry (and its associated physical file) from the Ontology Mirror
	 * 
	 * @param baseURI
	 * @param cacheFileName
	 * @throws IOException 
	 */
	@STServiceOperation
	public void updateOntologyMirrorEntry(UpdateType updateType, String baseURI, String mirrorFileName,
			String location) throws IOException {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(mirrorFileName);

		if (updateType == UpdateType.webUpdate) { // use first a temporary file, just in case the download brokes in
			// the middle, then copies the temporary to the destination in the
			// mirror
			File tempFile = Files.createTempFile("updateOntMirrorEntry", "temp").toFile();
			try {
				Utilities.downloadRDF(new URL(location), tempFile.getAbsolutePath());
				Utilities.copy(tempFile.getAbsolutePath(), mirFile.getAbsolutePath());
			} finally {
				tempFile.delete();
			}
		} else if (updateType == UpdateType.localUpdate) {
			Utilities.copy(location, mirFile.getAbsolutePath());
		}
	}
}