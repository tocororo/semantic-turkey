package it.uniroma2.art.semanticturkey.trivialinference.sail;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;

import com.ontotext.trree.config.OWLIMSailConfig;

import it.uniroma2.art.semanticturkey.trivialinference.sail.config.TrivialInferencerConfig;

public class GDBTrivialInferencerTest extends TrivialInferencerTest {

	@Override
	public void setup() {
		FileUtils.deleteQuietly(new File("storage"));
		OWLIMSailConfig owlimSailConfig = new OWLIMSailConfig();
		repo = new SailRepositoryFactory()
				.getRepository(new SailRepositoryConfig(new TrivialInferencerConfig(owlimSailConfig)));

		repo.init();
	}

}
