package it.uniroma2.art.semanticturkey.plugin.impls;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.pearl.model.ConverterArgumentExpression;
import it.uniroma2.art.coda.pearl.model.ConverterMapLiteralArgument;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ConverterStringLiteralArgument;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.semanticturkey.customrange.CODACoreProvider;
import it.uniroma2.art.semanticturkey.data.id.ARTURIResAndRandomString;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.util.Arrays;
import java.util.Map;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Implementation of the {@link URIGenerator} extension point that delegates to a CODA converter. 
 * 
 */
public class DefaultCODABasedURIGenerator implements URIGenerator {

	public static class Configuration extends AbstractPluginConfiguration {

		@Override
		public String getShortName() {
			return "Default URI Gen Configuration";
		}

		@ConfigurationParameter(defaultValue = { "it.uniroma2.art.coda.converters.RandomTemplateBasedIdGenerator" })
		public String converterClassName;
	}

	private Configuration config;
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	public DefaultCODABasedURIGenerator(Configuration config,
			ObjectFactory<CODACoreProvider> codaCoreProviderFactory) {
		this.config = config;
		this.codaCoreProviderFactory = codaCoreProviderFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResAndRandomString generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, String> args) throws URIGenerationException {
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();

		try {
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(
					stServiceContext.getProject().getOntologyManagerImplID()).createModelFactory();
			codaCore.initialize(stServiceContext.getProject().getOntModel(), ontFact);
			ConverterMention converterMention = new ConverterMention(
					"http://art.uniroma2.it/coda/contracts/randIdGen",
					Arrays.<ConverterArgumentExpression> asList(new ConverterStringLiteralArgument(xRole),
							new ConverterMapLiteralArgument(args)));

			ARTURIResource uriRes = codaCore.executeURIConverter(converterMention);
			ARTURIResAndRandomString rv = new ARTURIResAndRandomString();
			rv.setRandomValue("");
			rv.setArtURIResource(uriRes);
			return rv;
		} catch (ComponentProvisioningException | ConverterException | UnavailableResourceException
				| ProjectInconsistentException e) {
			throw new URIGenerationException(e);
		} finally {
			// / codaCore.stopAndClose();
		}
	}

}
