package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A {@link Deployer} which can deploy a {@link RepositoryConnection}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface RepositorySourcedDeployer extends Deployer {
	void deploy(RepositorySource source) throws IOException;
}
