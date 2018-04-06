package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import java.io.IOException;

/**
 * A {@link Deployer} which can deploy a {@link FormattedResourceSource}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface StreamSourcedDeployer extends Deployer {
	void deploy(FormattedResourceSource source) throws IOException;
}
