package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link OntoPortalDeployer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntoPortalDeployerFactory implements ExtensionFactory<OntoPortalDeployer>,
		ConfigurableExtensionFactory<OntoPortalDeployer, OntoPortalDeployerConfiguration>,
		PUScopedConfigurableComponent<OntoPortalDeployerConfiguration> {

	@Override
	public String getName() {
		return "OntoPortal Deployer";
	}

	@Override
	public String getDescription() {
		return "A deployer that submits an ontology (or more precisely, an OWL ontology or a SKOS theaurus) to an OntoPortal repository (e.g. BioPortal, EcoPortal, AgroPortal, etc.)";
	}

	@Override
	public OntoPortalDeployer createInstance(OntoPortalDeployerConfiguration conf) {
		return new OntoPortalDeployer(conf);
	}

	@Override
	public Collection<OntoPortalDeployerConfiguration> getConfigurations() {
		return Arrays.asList(new OntoPortalDeployerConfiguration());
	}

}
