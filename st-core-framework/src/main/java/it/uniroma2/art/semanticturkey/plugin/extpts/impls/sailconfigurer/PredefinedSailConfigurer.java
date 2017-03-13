package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;

import it.uniroma2.art.semanticturkey.plugin.extpts.SailConfigurer;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.PredefinedSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf.RDF4JSailConfigurerConfiguration;

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
		} else {
			throw new IllegalArgumentException("Unsupported config class: " + config.getClass());
		}
		
		return sailConfig;
	}

}
