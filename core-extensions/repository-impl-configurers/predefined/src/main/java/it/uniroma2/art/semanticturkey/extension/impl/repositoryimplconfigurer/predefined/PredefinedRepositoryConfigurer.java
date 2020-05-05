package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;

import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.vocabulary.OWLIM;

/**
 * A component providing the configuration for predefined {@link Sail}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryConfigurer implements RepositoryImplConfigurer {

	private PredefinedConfiguration config;

	public PredefinedRepositoryConfigurer(PredefinedConfiguration config) {
		this.config = config;
	}

	@Override
	public RepositoryImplConfig buildRepositoryImplConfig(
			@Nullable Function<SailImplConfig, SailImplConfig> backendDecorator) {
		RepositoryImplConfig repositoryImplConfig;

		if (config instanceof RDF4JSailConfiguration) {
			SailImplConfig sailImplConfig;

			if (config instanceof RDF4JInMemorySailConfiguration) {
				MemoryStoreConfig memoryStoreConfig = new MemoryStoreConfig();

				if (config instanceof RDF4JPersistentInMemorySailConfiguration) {
					RDF4JPersistentInMemorySailConfiguration config2 = (RDF4JPersistentInMemorySailConfiguration) config;

					memoryStoreConfig.setPersist(true);
					memoryStoreConfig.setSyncDelay(config2.syncDelay);
				} else {
					memoryStoreConfig.setPersist(false);
				}

				sailImplConfig = memoryStoreConfig;
			} else if (config instanceof RDF4JNativeSailConfiguration) {
				RDF4JNativeSailConfiguration config2 = (RDF4JNativeSailConfiguration) config;

				NativeStoreConfig nativeStoreConfig = new NativeStoreConfig();
				nativeStoreConfig.setForceSync(config2.forceSync);
				if (!config2.tripleIndexes.isEmpty()) {
					nativeStoreConfig.setTripleIndexes(config2.tripleIndexes);
				}

				sailImplConfig = nativeStoreConfig;
			} else {
				throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
			}

			RDF4JSailConfiguration rdf4jConfig = (RDF4JSailConfiguration) config;

			switch (rdf4jConfig.inferencer) {
			case RDF4JSailConfiguration.NONE_INFERENCER:
				break; // do nothing
			case RDF4JSailConfiguration.FORWARD_CHAINING_RDFS_INFERENCER:
				sailImplConfig = new ForwardChainingRDFSInferencerConfig(sailImplConfig);
				break;
			case RDF4JSailConfiguration.SCHEMA_CACHING_RDFS_INFERENCER:
				sailImplConfig = new SchemaCachingRDFSInferencerConfig(sailImplConfig);
				break;
			default:
				throw new IllegalArgumentException("Unsupported inferencer: " + rdf4jConfig.inferencer);
			}

			if (rdf4jConfig.directTypeInference) {
				sailImplConfig = new DirectTypeHierarchyInferencerConfig(sailImplConfig);
			}

			if (backendDecorator != null) {
				sailImplConfig = backendDecorator.apply(sailImplConfig);
			}

			repositoryImplConfig = new SailRepositoryConfig(sailImplConfig);
		} else if (config instanceof AbstractGraphDBConfiguration) {
			AbstractGraphDBConfiguration config2 = (AbstractGraphDBConfiguration) config;

			Resource implNode = SimpleValueFactory.getInstance().createBNode();
			// @formatter:off
			Model model = new ModelBuilder().subject(implNode)
												.add(OWLIM.BASE_URL, config2.baseURL)
												.add(OWLIM.DEFAULT_NS, config2.defaultNS)
												.add(OWLIM.ENTITY_INDEX_SIZE, "" + config2.entityIndexSize)
												.add(OWLIM.ENTITY_ID_SIZE, "" + config2.entityIdSize)
												.add(OWLIM.IMPORTS, config2.imports)
												.add(OWLIM.RULESET, config2.ruleset)
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

			SailImplConfig sailImplConfig = graphdbConfig;

			boolean decorationApplied = false;
			if (backendDecorator != null) {
				SailImplConfig sailImplConfig2 = backendDecorator.apply(sailImplConfig);

				if (sailImplConfig2 != null && sailImplConfig2 != sailImplConfig) {
					decorationApplied = true;
				}

				sailImplConfig = sailImplConfig2;
			}

			String gdbRepoType;
			if (config instanceof GraphDBFreeConfiguration) {
				graphdbConfig.setType("graphdb:FreeSail");
				gdbRepoType = "graphdb:FreeSailRepository";
			} else if (config instanceof GraphDBSEConfiguration) {
				graphdbConfig.setType("owlim:Sail");
				gdbRepoType = "owlim:MonitorRepository";
			} else {
				throw new IllegalArgumentException(
						"Could not recognize GraphDB Sail Type from config object: " + config.getClass());
			}

			if (decorationApplied) {
				gdbRepoType = SailRepositoryFactory.REPOSITORY_TYPE;
			}

			SailRepositoryConfig repositoryImplConfigTemp = new SailRepositoryConfig(sailImplConfig);
			repositoryImplConfigTemp.setType(gdbRepoType);
			repositoryImplConfig = repositoryImplConfigTemp;
		} else {
			throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
		}

		return repositoryImplConfig;
	}

}
