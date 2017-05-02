package it.uniroma2.art.semanticturkey.project;

import org.eclipse.rdf4j.repository.manager.RepositoryInfo;

/**
 * Utility class for {@link Project}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class ProjectUtils {
	/**
	 * Computes the name of the (local) repository holding a version dump.
	 * 
	 * @param versionId
	 * @return
	 */
	public static String computeVersionRepository(String versionId) {
		return "version-" + versionId;
	}

	/**
	 * Establishes whether a given reposutory is a version dump (base on its id).
	 * 
	 * @param repositoryInfo
	 * @return
	 */
	public static boolean isVersionRepostory(RepositoryInfo repositoryInfo) {
		return repositoryInfo.getId().startsWith("version-");
	}

	/**
	 * Extracts the version id from a mangled repository id.
	 * 
	 * @param repositoryId
	 * @return
	 */
	public static String extractVersionFromRepositoryId(String repositoryId) {
		return repositoryId.substring("version-".length());
	}

}
