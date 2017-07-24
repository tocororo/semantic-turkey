package it.uniroma2.art.semanticturkey.services.support;

import org.eclipse.rdf4j.repository.Repository;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.VersionInfo;
import it.uniroma2.art.semanticturkey.services.InvalidContextException;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

/**
 * Utility class for decoding an {@link STServiceContext}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class STServiceContextUtils {
	/**
	 * Returns the repository that should be used for servicing a data operation. This method takes into
	 * consideration the possibility to use a version dump.
	 * 
	 * @param stServiceContext
	 * @return
	 */
	public static Repository getRepostory(STServiceContext stServiceContext) {
		Project project = stServiceContext.getProject();
		return project.getRepositoryManager().getRepository(getRepostoryId(stServiceContext));
	}

	/**
	 * Returns the id of the repository that should be used for servicing a data operation. This method takes
	 * into consideration the possibility to use a version dump.
	 * 
	 * @param stServiceContext
	 * @return
	 */
	public static String getRepostoryId(STServiceContext stServiceContext) {
		Project project = stServiceContext.getProject();
		String versionId = stServiceContext.getVersion();

		if (versionId != null) {
			String repoId = project.getVersionManager().getVersion(versionId)
					.map(VersionInfo::getRepositoryId)
					.orElseThrow(() -> new InvalidContextException("Not a dumped version: " + versionId));
			if (!project.getRepositoryManager().hasRepositoryConfig(repoId)) {
				throw new InvalidContextException("No repository for version: " + versionId);
			}
			return repoId;
		} else {
			return "core";
		}
	}

}
