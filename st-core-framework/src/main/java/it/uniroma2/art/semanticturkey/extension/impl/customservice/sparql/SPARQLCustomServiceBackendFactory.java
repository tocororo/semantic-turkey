package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * The {@link ExtensionFactory} for the the {@link SPARQLCustomServiceBackend}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLCustomServiceBackendFactory implements ExtensionFactory<SPARQLCustomServiceBackend>,
		ConfigurableExtensionFactory<SPARQLCustomServiceBackend, SPARQLOperation> {

	@Override
	public String getName() {
		return "SPARQL Custom Service Backend";
	}

	@Override
	public String getDescription() {
		return "A backend for implementing custom services using SPARQL";
	}

	@Override
	public SPARQLCustomServiceBackend createInstance(SPARQLOperation conf) {
		return new SPARQLCustomServiceBackend(conf);
	}

	@Override
	public Collection<SPARQLOperation> getConfigurations() {
		return Arrays.asList(new SPARQLOperation());
	}

}
