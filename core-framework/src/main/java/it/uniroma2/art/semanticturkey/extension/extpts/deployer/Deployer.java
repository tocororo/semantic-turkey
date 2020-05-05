package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import java.io.IOException;

import it.uniroma2.art.semanticturkey.extension.Extension;

/**
 * Extension point for deployers. They are placed at the end of an export process to deploy the exported data
 * somewhere, e.g. a server conforming to the Graph Store API, an FTP server etc...
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface Deployer extends Extension {
	/**
	 * Deploys the provided resource. The default implementation of this operation delegates the deployment of
	 * specific resource types to known subclasses.
	 * 
	 * @param source
	 * @throws IOException
	 */
	default void deploy(Source source) throws IOException {
		boolean isStreamSource = (source instanceof FormattedResourceSource);
		boolean isRepositorySource = (source instanceof RepositorySource);

		if (isStreamSource && isRepositorySource) {
			throw new IllegalArgumentException("Ambiguous source");
		}

		if (isStreamSource) {
			if (this instanceof StreamSourcedDeployer) {
				((StreamSourcedDeployer) this).deploy((FormattedResourceSource) source);
			} else {
				throw new IllegalArgumentException("Unable to handle " + FormattedResourceSource.class.getSimpleName());
			}
		} else if (isRepositorySource) {
			if (this instanceof RepositorySourcedDeployer) {
				((RepositorySourcedDeployer) this).deploy((RepositorySource) source);
			} else {
				throw new IllegalArgumentException(
						"Unable to handle " + RepositorySource.class.getSimpleName());
			}

		} else {
			throw new IllegalArgumentException("Unknown source type: " + source.getClass().getName());
		}
	}
}
