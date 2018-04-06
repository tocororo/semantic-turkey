package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * The {@link StreamSourcedDeployer} extension point.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StreamSourcedDeployerExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return StreamSourcedDeployer.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
