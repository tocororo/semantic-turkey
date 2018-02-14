package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link UpdatePropertyValueRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueRDFTranformerFactory implements
		ConfigurableExtensionFactory<UpdatePropertyValueRDFTransformer, UpdatePropertyValueRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<UpdatePropertyValueRDFTransformerConfiguration> {

	@Override
	public String getName() {
		return "Update Property Value RDF Transformer";

	}

	@Override
	public String getDescription() {
		return "An RDF transformer that can be configured to update a property value";
	}

	@Override
	public UpdatePropertyValueRDFTransformer createInstance(
			UpdatePropertyValueRDFTransformerConfiguration conf) {
		return new UpdatePropertyValueRDFTransformer(conf);
	}

	@Override
	public Collection<UpdatePropertyValueRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new UpdatePropertyValueRDFTransformerConfiguration());
	}

}
