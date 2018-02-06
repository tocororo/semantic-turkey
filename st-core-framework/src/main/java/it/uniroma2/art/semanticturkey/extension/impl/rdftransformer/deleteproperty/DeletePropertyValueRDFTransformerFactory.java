package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.deleteproperty;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link DeletePropertyValueRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueRDFTransformerFactory implements
		ConfigurableExtensionFactory<DeletePropertyValueRDFTransformer, DeletePropertyValueRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<DeletePropertyValueRDFTransformerConfiguration> {

	@Override
	public String getName() {
		return "Delete Property Value RDF Transformer";

	}

	@Override
	public String getDescription() {
		return "An RDF transformer that can be configured to delete a property value";
	}

	@Override
	public DeletePropertyValueRDFTransformer createInstance(
			DeletePropertyValueRDFTransformerConfiguration conf) {
		return new DeletePropertyValueRDFTransformer(conf);
	}

	@Override
	public Collection<DeletePropertyValueRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new DeletePropertyValueRDFTransformerConfiguration());
	}

}
