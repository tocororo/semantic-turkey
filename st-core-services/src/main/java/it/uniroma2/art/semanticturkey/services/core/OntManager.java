package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
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
		updateFromBaseURI, updateFromAlternativeURL, updateFromFile
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
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(ontologyMirror)', 'D')")
	public void deleteOntologyMirrorEntry(String baseURI, String cacheFileName) {
		OntologiesMirror.removeCachedOntologyEntry(baseURI);
		File cacheFile = new File(Resources.getOntologiesMirrorDir(), cacheFileName);
		cacheFile.delete();
	}

	/**
	 * Updates an entry (and its associated physical file) from the Ontology Mirror. The entry can be updated
	 * in three different ways (determined by the parameter {@code updateType}, differentiating in the source
	 * of the updated ontology:
	 * <ul>
	 * <li>{@code updateFromBaseURI}: the source is retrieved from the supplied {@code baseURI}</li>
	 * <li>{@code updateFromAlternativeURL}: the source is retrieved from the address hold by the parameter
	 * {@code alternativeURL}</li>
	 * <li>{@code updateFromFile}: the source has been supplied in the request body (and mapped to the
	 * parameter {@code inputFile})</li>
	 * </ul>
	 * 
	 * @param baseURI
	 * @param cacheFileName
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(ontologyMirror)', 'CU')")
	public void updateOntologyMirrorEntry(UpdateType updateType, String baseURI, String mirrorFileName,
			@Optional String alternativeURL, @Optional MultipartFile inputFile) throws IOException {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(mirrorFileName);

		if (updateType == UpdateType.updateFromBaseURI || updateType == UpdateType.updateFromAlternativeURL) {
			String location;

			if (updateType == UpdateType.updateFromAlternativeURL) {
				location = alternativeURL;
			} else {
				location = baseURI;
			}

			File tempFile = Files.createTempFile("updateOntMirrorEntry", "temp").toFile();
			try {
				Utilities.downloadRDF(new URL(location), tempFile.getAbsolutePath());
				Utilities.copy(tempFile.getAbsolutePath(), mirFile.getAbsolutePath());
			} finally {
				tempFile.delete();
			}
		} else if (updateType == UpdateType.updateFromFile) {
			Objects.requireNonNull(inputFile, "Missing required input file");
			FileUtils.copyInputStreamToFile(inputFile.getInputStream(), mirFile.getFile());
		}
	}
}