package it.uniroma2.art.semanticturkey.extension.extpts.rdflifter;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;

/**
 * This class represents the context of execution of an {@link RDFLifter}. An object of this class is not
 * thread-safe, and it shouldn't be stored for later reuse.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface LifterContext {
	/**
	 * Returns the lexicalization model of the target
	 * 
	 * @return the lexicalization model of the target
	 */
	IRI getLexicalizationModel();

	/**
	 * Generates an IRI using the target {@link URIGenerator}
	 * 
	 * @param xRole
	 * @param valueMapping
	 * @return
	 * @throws URIGenerationException
	 */
	IRI generateIRI(String xRole, Map<String, Value> valueMapping) throws URIGenerationException;
}
