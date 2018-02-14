package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link XNoteDereificationRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationRDFTransformerFactory implements
		ConfigurableExtensionFactory<XNoteDereificationRDFTransformer, XNoteDereificationRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<XNoteDereificationRDFTransformerConfiguration> {

	@Override
	public String getName() {
		return "XNote Dereification RDF Transformer";
	}

	@Override
	public String getDescription() {
		return "An RDF transformer that performs XNote dereification";
	}

	@Override
	public XNoteDereificationRDFTransformer createInstance(
			XNoteDereificationRDFTransformerConfiguration conf) {
		return new XNoteDereificationRDFTransformer(conf);
	}

	@Override
	public Collection<XNoteDereificationRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new XNoteDereificationRDFTransformerConfiguration());
	}

}
