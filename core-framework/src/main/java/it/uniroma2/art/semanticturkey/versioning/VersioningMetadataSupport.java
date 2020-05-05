package it.uniroma2.art.semanticturkey.versioning;

/**
 * A convenience class providing per-thread storage of the versioning-related metadata about the currently
 * executing operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersioningMetadataSupport {
	private static ThreadLocal<VersioningMetadata> versioningMetadataHolder = ThreadLocal
			.withInitial(VersioningMetadata::new);

	public static VersioningMetadata currentVersioningMetadata() {
		return versioningMetadataHolder.get();
	}

	public static void removeVersioningMetadata() {
		versioningMetadataHolder.remove();
	}

}