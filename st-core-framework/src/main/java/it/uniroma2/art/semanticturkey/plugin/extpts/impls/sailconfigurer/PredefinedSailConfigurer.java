package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;

import it.uniroma2.art.semanticturkey.plugin.extpts.SailConfigurer;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.AbstractGraphDBConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.GraphDBFreeConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.PredefinedSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.vocabulary.OWLIM;

/**
 * A component providing the configuration for predefined {@link Sail}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedSailConfigurer implements SailConfigurer {

	private PredefinedSailConfigurerConfiguration config;

	public PredefinedSailConfigurer(PredefinedSailConfigurerConfiguration config) {
		this.config = config;
	}

	@Override
	public SailImplConfig buildSailConfig() {
		SailImplConfig sailConfig;

		if (config instanceof RDF4JSailConfigurerConfiguration) {

			if (config instanceof RDF4JInMemorySailConfigurerConfiguration) {
				MemoryStoreConfig memoryStoreConfig = new MemoryStoreConfig();

				if (config instanceof RDF4JPersistentInMemorySailConfigurerConfiguration) {
					RDF4JPersistentInMemorySailConfigurerConfiguration config2 = (RDF4JPersistentInMemorySailConfigurerConfiguration) config;

					memoryStoreConfig.setPersist(true);
					memoryStoreConfig.setSyncDelay(config2.syncDelay);
				} else {
					memoryStoreConfig.setPersist(false);
				}

				sailConfig = memoryStoreConfig;
			} else if (config instanceof RDF4JNativeSailConfigurerConfiguration) {
				RDF4JNativeSailConfigurerConfiguration config2 = (RDF4JNativeSailConfigurerConfiguration) config;

				NativeStoreConfig nativeStoreConfig = new NativeStoreConfig();
				nativeStoreConfig.setForceSync(config2.forceSync);
				if (!config2.tripleIndexes.isEmpty()) {
					nativeStoreConfig.setTripleIndexes(config2.tripleIndexes);
				}

				sailConfig = nativeStoreConfig;
			} else {
				throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
			}

			RDF4JSailConfigurerConfiguration rdf4jConfig = (RDF4JSailConfigurerConfiguration) config;

			if (rdf4jConfig.rdfsInference) {
				sailConfig = new ForwardChainingRDFSInferencerConfig(sailConfig);
			}

			if (rdf4jConfig.directTypeInference) {
				sailConfig = new DirectTypeHierarchyInferencerConfig(sailConfig);
			}
		} else if (config instanceof AbstractGraphDBConfigurerConfiguration) {
			AbstractGraphDBConfigurerConfiguration config2 = (AbstractGraphDBConfigurerConfiguration) config;

			Resource implNode = SimpleValueFactory.getInstance().createBNode();
			// @formatter:off
			Model model = new ModelBuilder().subject(implNode)
												.add(OWLIM.BASE_URL, config2.baseURL)
												.add(OWLIM.DEFAULT_NS, config2.defaultNS)
												.add(OWLIM.ENTITY_INDEX_SIZE, "" + config2.entityIndexSize)
												.add(OWLIM.ENTITY_ID_SIZE, "" + config2.entityIdSize)
												.add(OWLIM.IMPORTS, config2.imports)
												.add(OWLIM.RULE_SET, config2.ruleSet)
												.add(OWLIM.STORAGE_FOLDER, config2.storageFolder)
												.add(OWLIM.ENABLE_CONTEXT_INDEX, Boolean.toString(config2.enableContextIndex))
												.add(OWLIM.ENABLE_PREDICATE_LIST, Boolean.toString(config2.enablePredicateList))
												.add(OWLIM.IN_MEMORY_LITERAL_PROPERTIES, Boolean.toString(config2.inMemoryLiteralProperties))
												.add(OWLIM.ENABLE_LITERAL_INDEX, Boolean.toString(config2.enableLiteralIndex))
												.add(OWLIM.CHECK_FOR_INCONSISTENCIES, Boolean.toString(config2.checkForInconsistencies))
												.add(OWLIM.DISABLE_SAME_AS, Boolean.toString(config2.disableSameAs))
												.add(OWLIM.QUERY_TIMEOUT, "" + config2.queryTimeout)
												.add(OWLIM.QUERY_LIMIT_RESULTS, "" + config2.queryLimitResults)
												.add(OWLIM.THROW_QUERY_EVALUATION_EXCEPTION_ON_TIMEOUT, Boolean.toString(config2.throwQueryEvaluationExceptionOnTimeout))
												.add(OWLIM.READ_ONLY, Boolean.toString(config2.readOnly))
											.build();
			// @formatter:on
			ModelBasedSailImplConfig graphdbConfig = new ModelBasedSailImplConfig();

			graphdbConfig.parse(model, implNode);
			
			if (config instanceof GraphDBFreeConfigurerConfiguration) {
				graphdbConfig.setType("graphdb:FreeSail");
			} else {
				throw new IllegalArgumentException(
						"Could not recognize GraphDB Sail Type from config object: " + config.getClass());
			}

			sailConfig = graphdbConfig;
		} else {
			throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
		}

		return sailConfig;
	}

}
