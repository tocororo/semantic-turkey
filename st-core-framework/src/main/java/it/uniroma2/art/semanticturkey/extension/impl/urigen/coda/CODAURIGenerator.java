package it.uniroma2.art.semanticturkey.extension.impl.urigen.coda;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.pearl.model.ConverterArgumentExpression;
import it.uniroma2.art.coda.pearl.model.ConverterMapArgument;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ConverterRDFLiteralArgument;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Implementation of the {@link URIGenerator} extension point that delegates to a CODA converter.
 * 
 */
public class CODAURIGenerator implements URIGenerator {

	/**
	 * Contract URL for random ID generation.
	 */
	public static final String CODA_RANDOM_ID_GENERATOR_CONTRACT = "http://art.uniroma2.it/coda/contracts/randIdGen";

	private static final Logger logger = LoggerFactory.getLogger(CODAURIGenerator.class);

	private CODAURIGeneratorConfiguration config;
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	public CODAURIGenerator(CODAURIGeneratorConfiguration config2,
			ObjectFactory<CODACoreProvider> codaCoreProviderFactory) {
		this.config = config2;
		this.codaCoreProviderFactory = codaCoreProviderFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.
	 * services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public IRI generateIRI(STServiceContext stServiceContext, String xRole, Map<String, Value> args)
			throws URIGenerationException {
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		String converter = "http://art.uniroma2.it/coda/converters/templateBasedRandIdGen";

		if (config instanceof CODAAnyURIGeneratorConfiguration) {
			converter = CODAAnyURIGeneratorConfiguration.class.cast(config).converter;
		}
		codaCore.setGlobalContractBinding(CODA_RANDOM_ID_GENERATOR_CONTRACT, converter);
		try {
			Properties converterProperties = new Properties();
			for (String par : config.getProperties()) {
				if (config.getPropertyValue(par) != null) {
					converterProperties.setProperty(par, config.getPropertyValue(par).toString());
				}
			}

			codaCore.setConverterProperties(converter, converterProperties);

			Repository repo = stServiceContext.getProject().getRepository();
			RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo);
			try {
				codaCore.initialize(conn);
				ConverterMention converterMention = new ConverterMention(CODA_RANDOM_ID_GENERATOR_CONTRACT,
						Arrays.<ConverterArgumentExpression>asList(
								ConverterRDFLiteralArgument.fromString(xRole),
								ConverterMapArgument.fromNodesMap(args)));

				logger.debug("Going to execute a CODA converter");

				IRI resource = codaCore.executeURIConverter(converterMention);
				return resource;
			} finally {
				RDF4JRepositoryUtils.releaseConnection(conn, repo);
			}
		} catch (ComponentProvisioningException | ConverterException | PropertyNotFoundException e) {
			logger.debug("An exceprtion occuring during the generation of a URI", e);
			throw new URIGenerationException(e);
		} finally {
			codaCore.setRepositoryConnection(null); // necessary because connection handling is external to
													// CODA
			codaCore.stopAndClose();
		}
	}

}
