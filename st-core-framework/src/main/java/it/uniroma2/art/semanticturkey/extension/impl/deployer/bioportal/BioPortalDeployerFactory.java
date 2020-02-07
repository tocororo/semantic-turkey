package it.uniroma2.art.semanticturkey.extension.impl.deployer.bioportal;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link BioPortalDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class BioPortalDeployerFactory implements ExtensionFactory<BioPortalDeployer>,
		ConfigurableExtensionFactory<BioPortalDeployer, BioPortalDeployerConfiguration>,
		PUScopedConfigurableComponent<BioPortalDeployerConfiguration> {

	@Override
	public String getName() {
		return "BioPortal Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that submits an ontology (or more precisely, an OWL ontology or a SKOS theaurus) to BioPortal";
	}

	@Override
	public BioPortalDeployer createInstance(BioPortalDeployerConfiguration conf) {
		return new BioPortalDeployer(conf);
	}

	@Override
	public Collection<BioPortalDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new BioPortalDeployerConfiguration());
	}

}
