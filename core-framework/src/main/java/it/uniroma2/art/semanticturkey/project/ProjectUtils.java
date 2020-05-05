package it.uniroma2.art.semanticturkey.project;

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
}
