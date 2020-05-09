package it.uniroma2.art.semanticturkey.services.aspects;

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

}