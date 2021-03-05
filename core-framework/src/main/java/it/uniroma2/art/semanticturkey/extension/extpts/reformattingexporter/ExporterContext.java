package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import it.uniroma2.art.semanticturkey.project.Project;
import org.eclipse.rdf4j.model.IRI;

/**
 * This class represents the context of execution of an {@link ReformattingExporter}. An object of this class is not
 * thread-safe, and it shouldn't be stored for later reuse.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface ExporterContext {

	/**
	 * Returns the Project of the soruce
	 * @return the Project of the source
	 */
	Project getProject();
}
