package it.uniroma2.art.semanticturkey.extension.impl.deployer.sftp;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link SFTPDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPDeployerFactory implements ExtensionFactory<SFTPDeployer>,
		ConfigurableExtensionFactory<SFTPDeployer, SFTPDeployerConfiguration>,
		PUScopedConfigurableComponent<SFTPDeployerConfiguration> {

	@Override
	public String getName() {
		return "SFTP Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that uses the SFTP Protocol";
	}

	@Override
	public SFTPDeployer createInstance(SFTPDeployerConfiguration conf) {
		return new SFTPDeployer(conf);
	}

	@Override
	public Collection<SFTPDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new SFTPDeployerConfiguration());
	}

}
