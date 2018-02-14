package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link XLabelDereificationRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XLabelDereificationRDFTransformerFactory implements
		ConfigurableExtensionFactory<XLabelDereificationRDFTransformer, XLabelDereificationRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<XLabelDereificationRDFTransformerConfiguration> {

	@Override
	public String getName() {
		return "XLabel Dereification RDF Transformer";
	}

	@Override
	public String getDescription() {
		return "An RDF transformer that performs XLabel dereification";

	}

	@Override
	public XLabelDereificationRDFTransformer createInstance(
			XLabelDereificationRDFTransformerConfiguration conf) {
		return new XLabelDereificationRDFTransformer(conf);
	}

	@Override
	public Collection<XLabelDereificationRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new XLabelDereificationRDFTransformerConfiguration());
	}

}
