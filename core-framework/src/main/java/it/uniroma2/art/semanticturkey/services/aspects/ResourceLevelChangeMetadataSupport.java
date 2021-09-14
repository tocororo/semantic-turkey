package it.uniroma2.art.semanticturkey.services.aspects;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A convenience class providing per-thread storage of the resource-level change metadata about the currently
 * executing operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ResourceLevelChangeMetadataSupport {
	private static ThreadLocal<ResourceLevelChangeMetadata> versioningMetadataHolder = ThreadLocal
			.withInitial(ResourceLevelChangeMetadata::new);

	public static ResourceLevelChangeMetadata currentVersioningMetadata() {
		return versioningMetadataHolder.get();
	}

	public static void removeVersioningMetadata() {
		versioningMetadataHolder.remove();
	}

	public static Pair<Resource, RDFResourceRole> enhanceResourceChangeInfo(RepositoryConnection repConn,
																			Pair<Resource, RDFResourceRole> pair) {
		Resource resource = pair.getLeft();
		RDFResourceRole resourceRole = pair.getRight();

		Pair<Resource, RDFResourceRole> enhancedPair;
		if (resourceRole == RDFResourceRole.undetermined) {
			RDFResourceRole computedResourceRole = RoleRecognitionOrchestrator.computeRole(resource, repConn);
			enhancedPair = ImmutablePair.of(resource, computedResourceRole);
		} else {
			enhancedPair = pair;
		}
		return enhancedPair;
	}
}