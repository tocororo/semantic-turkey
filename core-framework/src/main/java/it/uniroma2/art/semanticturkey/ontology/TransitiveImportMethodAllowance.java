package it.uniroma2.art.semanticturkey.ontology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This enumeration defines the sources from which the system is authorized to resolve transitive imports.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public enum TransitiveImportMethodAllowance {
	/**
	 * Transitive imports are disallowed
	 */
	nowhere(Collections.emptyList()),
	/**
	 * Only the download from the web is allowed
	 */
	web(Arrays.asList(ImportMethod.fromWeb)),
	/**
	 * Download form the web is preferred, but in case of failure it is possible to use the ontology mirror
	 */
	webFallbackToMirror(Arrays.asList(ImportMethod.fromWeb, ImportMethod.fromOntologyMirror)),
	/**
	 * Use of the ontology mirror is preferred, but in case of failure it is possible to download from the web
	 */
	mirrorFallbackToWeb(Arrays.asList(ImportMethod.fromOntologyMirror, ImportMethod.fromWeb)),
	/**
	 * Only use of the ontology manager is allowed
	 */
	mirror((Arrays.asList(ImportMethod.fromOntologyMirror)));

	private final List<ImportMethod> allowedMethods;

	TransitiveImportMethodAllowance(List<ImportMethod> allowedMethods) {
		this.allowedMethods = Collections.unmodifiableList(new ArrayList<>(allowedMethods));
	}

	/**
	 * Returns an unmodifiable list of allowed import methods
	 * 
	 * @return
	 */
	public List<ImportMethod> getAllowedMethods() {
		return allowedMethods;
	}
}
