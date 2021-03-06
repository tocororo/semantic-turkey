package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import java.io.IOException;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Extension point for loaders. They are placed at the start of an import chain to fetch data from some
 * external source.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface Loader extends Extension {
	/**
	 * Loads data into the provided target. The default implementation of this operation delegates the
	 * operation to concrete subclasses matching the given target.
	 * 
	 * @param source
	 * @param acceptedFormat
	 * @throws IOException
	 */
	default void load(Target target, @Nullable DataFormat acceptedFormat) throws IOException {
		boolean isStreamSource = (target instanceof FormattedResourceTarget);
		boolean isRepositorySource = (target instanceof RepositoryTarget);

		if (isStreamSource && isRepositorySource) {
			throw new IllegalArgumentException("Ambiguous target");
		}

		if (isStreamSource) {
			if (this instanceof StreamTargetingLoader) {
				((StreamTargetingLoader) this).load((FormattedResourceTarget) target, acceptedFormat);
			} else {
				throw new IllegalArgumentException(
						"Unable to handle " + FormattedResourceTarget.class.getSimpleName());
			}
		} else if (isRepositorySource) {
			if (this instanceof RepositoryTargetingLoader) {
				if (acceptedFormat != null) {
					throw new IllegalArgumentException("A " + RepositoryTargetingLoader.class.getSimpleName() + " should not receive a non-null data format");
				}
				((RepositoryTargetingLoader) this).load((RepositoryTarget) target, acceptedFormat);
			} else {
				throw new IllegalArgumentException(
						"Unable to handle " + RepositoryTarget.class.getSimpleName());
			}

		} else {
			throw new IllegalArgumentException("Unknown target type: " + target.getClass().getName());
		}
	}

}
