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
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;

import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.vocabulary.OWLIM;

/**
 * A component providing the configuration for predefined {@link Sail}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurer implements RepositoryImplConfigurer {

	private PredefinedRepositoryImplConfigurerConfiguration config;

	public PredefinedRepositoryImplConfigurer(PredefinedRepositoryImplConfigurerConfiguration config) {
		this.config = config;
	}

	@Override
	public RepositoryImplConfig buildRepositoryImplConfig(@Nullable Function<SailImplConfig, SailImplConfig> backendDecorator) {
		RepositoryImplConfig repositoryImplConfig;

		if (config instanceof RDF4JSailConfigurerConfiguration) {
			SailImplConfig sailImplConfig;

			if (config instanceof RDF4JInMemorySailConfigurerConfiguration) {
				MemoryStoreConfig memoryStoreConfig = new MemoryStoreConfig();

				if (config instanceof RDF4JPersistentInMemorySailConfigurerConfiguration) {
					RDF4JPersistentInMemorySailConfigurerConfiguration config2 = (RDF4JPersistentInMemorySailConfigurerConfiguration) config;

					memoryStoreConfig.setPersist(true);
					memoryStoreConfig.setSyncDelay(config2.syncDelay);
				} else {
					memoryStoreConfig.setPersist(false);
				}

				sailImplConfig = memoryStoreConfig;
			} else if (config instanceof RDF4JNativeSailConfigurerConfiguration) {
				RDF4JNativeSailConfigurerConfiguration config2 = (RDF4JNativeSailConfigurerConfiguration) config;

				NativeStoreConfig nativeStoreConfig = new NativeStoreConfig();
				nativeStoreConfig.setForceSync(config2.forceSync);
				if (!config2.tripleIndexes.isEmpty()) {
					nativeStoreConfig.setTripleIndexes(config2.tripleIndexes);
				}

				sailImplConfig = nativeStoreConfig;
			} else {
				throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
			}

			if (backendDecorator != null) {
				sailImplConfig = backendDecorator.apply(sailImplConfig);
			}
			
			RDF4JSailConfigurerConfiguration rdf4jConfig = (RDF4JSailConfigurerConfiguration) config;

			if (rdf4jConfig.rdfsInference) {
				sailImplConfig = new ForwardChainingRDFSInferencerConfig(sailImplConfig);
			}

			if (rdf4jConfig.directTypeInference) {
				sailImplConfig = new DirectTypeHierarchyInferencerConfig(sailImplConfig);
			}

			repositoryImplConfig = new SailRepositoryConfig(sailImplConfig);
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
			if (config instanceof GraphDBFreeConfigurerConfiguration) {
				graphdbConfig.setType("graphdb:FreeSail");
				gdbRepoType = "graphdb:FreeSailRepository";
			} else if(config instanceof GraphDBSEConfigurerConfiguration) {
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
