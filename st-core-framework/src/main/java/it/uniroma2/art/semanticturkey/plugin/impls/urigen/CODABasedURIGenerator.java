package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

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
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODABasedAnyTemplatedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.CODABasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Implementation of the {@link URIGenerator} extension point that delegates to a CODA converter. 
 * 
 */
public class CODABasedURIGenerator implements URIGenerator {

	/**
	 * Contract URL for random ID generation.
	 */
	public static final String CODA_RANDOM_ID_GENERATOR_CONTRACT = "http://art.uniroma2.it/coda/contracts/randIdGen";

	private static final Logger logger = LoggerFactory.getLogger(CODABasedURIGenerator.class);
	
	private CODABasedURIGeneratorConfiguration config;
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	public CODABasedURIGenerator(CODABasedURIGeneratorConfiguration config,
			ObjectFactory<CODACoreProvider> codaCoreProviderFactory) {
		this.config = config;
		this.codaCoreProviderFactory = codaCoreProviderFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResource generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, String> args) throws URIGenerationException {
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		String converter = "http://art.uniroma2.it/coda/converters/templateBasedRandIdGen";
		
		if (config instanceof CODABasedAnyTemplatedURIGeneratorConfiguration) {
			 converter = CODABasedAnyTemplatedURIGeneratorConfiguration.class.cast(config).converter;
		}
		codaCore.setGlobalContractBinding(CODA_RANDOM_ID_GENERATOR_CONTRACT, converter);
		try {
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(
					stServiceContext.getProject().getOntologyManagerImplID()).createModelFactory();
			codaCore.initialize(stServiceContext.getProject().getOntModel(), ontFact);
			ConverterMention converterMention = new ConverterMention(CODA_RANDOM_ID_GENERATOR_CONTRACT,
					Arrays.<ConverterArgumentExpression> asList(new ConverterStringLiteralArgument(xRole),
							new ConverterMapLiteralArgument(args)));

			logger.debug("Going to execute a CODA converter");

			return codaCore.executeURIConverter(converterMention);
		} catch (ComponentProvisioningException | ConverterException | UnavailableResourceException
				| ProjectInconsistentException e) {
			logger.debug("An exceprtion occuring during the generation of a URI", e);
			throw new URIGenerationException(e);
		} finally {
			// / codaCore.stopAndClose();
		}
	}

}
