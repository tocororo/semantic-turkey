package it.uniroma2.art.semanticturkey.trivialinference.sail;

import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;

import it.uniroma2.art.semanticturkey.trivialinference.sail.config.TrivialInferencerConfig;

public class RDF4JTrivialInferencerTest extends TrivialInferencerTest {

	@Override
	public void setup() {
		repo = new SailRepositoryFactory().getRepository(
				new SailRepositoryConfig(new TrivialInferencerConfig(new MemoryStoreConfig())));
		repo.init();
	}

}
