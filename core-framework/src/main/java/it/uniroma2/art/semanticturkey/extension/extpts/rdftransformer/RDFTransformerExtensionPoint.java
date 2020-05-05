package it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

public class RDFTransformerExtensionPoint implements ExtensionPoint, PUScopedConfigurableComponent<RDFTransformerChainConfiguration> {

	@Override
	public Class<?> getInterface() {
		return RDFTransformer.class;
	}


}
