package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import org.eclipse.rdf4j.model.IRI;

/**
 * This class represents the context of execution of an {@link ReformattingExporter}. An object of this class is not
 * thread-safe, and it shouldn't be stored for later reuse.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface ExporterContext {
	/**
	 * Returns the lexicalization model of the source
	 * @return the lexicalization model of the source
	 */
	IRI getLexicalizationModel();
}
