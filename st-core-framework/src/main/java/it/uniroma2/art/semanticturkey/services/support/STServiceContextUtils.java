package it.uniroma2.art.semanticturkey.services.support;

import org.eclipse.rdf4j.repository.Repository;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectUtils;
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
		Project<?> project = stServiceContext.getProject();
		String versionId = stServiceContext.getVersion();

		if (versionId != null) {
			Repository repo = project.getRepositoryManager()
					.getRepository(ProjectUtils.computeVersionRepository(versionId));
			if (repo == null) {
				throw new InvalidContextException("No repository for version: " + versionId);
			}
			return repo;
		} else {
			return project.getRepository();
		}
	}
}
