package it.uniroma2.art.semanticturkey.extension.impl.urigen.coda;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * Factory for the instantiation of {@link CODAURIGenerator}.
 */
public class CODAURIGeneratorFactory implements NonConfigurableExtensionFactory<CODAURIGenerator>,
		ConfigurableExtensionFactory<CODAURIGenerator, CODAURIGeneratorConfiguration> {

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	@Override
	public String getName() {
		return "CODA URI Generator";
	}

	@Override
	public String getDescription() {
		return "A URI Generator based on a CODA converter";
	}

	@Override
	public Collection<CODAURIGeneratorConfiguration> getConfigurations() {
		return Arrays.<CODAURIGeneratorConfiguration> asList(new CODATemplateBasedURIGeneratorConfiguration(),
				new CODAAnyURIGeneratorConfiguration());
	}

	@Override
	public CODAURIGenerator createInstance(CODAURIGeneratorConfiguration config) {
		return new CODAURIGenerator((CODAURIGeneratorConfiguration) config, codaCoreProviderFactory);
	}

	@Override
	public CODAURIGenerator createInstance() {
		return new CODAURIGenerator(new CODATemplateBasedURIGeneratorConfiguration(),
				codaCoreProviderFactory);
	}

}
