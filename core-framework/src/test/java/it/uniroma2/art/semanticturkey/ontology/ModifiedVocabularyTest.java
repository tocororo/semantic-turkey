package it.uniroma2.art.semanticturkey.ontology;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.uniroma2.art.lime.model.vocabulary.AbstractModifiedVocabularyTest;

/**
 * This test class verifies that the cached vocabularies that we modified (e.g. to remove DOCTYPE) are
 * equivalent to the original version.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@RunWith(Parameterized.class)
public class ModifiedVocabularyTest extends AbstractModifiedVocabularyTest {

	@Parameters(name = "{0}")
	public static Collection<String> data() {
		return Arrays.asList("owl.rdf", "lexinfo.owl");
	}


	public ModifiedVocabularyTest(String modifiedVocabName) {
		super(modifiedVocabName);
	}
}
