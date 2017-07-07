package it.uniroma2.art.semanticturkey.validation;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * Utility class about validation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ValidationUtilities {
	private static final Logger logger = LoggerFactory.getLogger(ValidationUtilities.class);

	/**
	 * Disables validation (if enabled) on the given connection to the core repository of the provided
	 * project.
	 * 
	 * @param project
	 * @param conn
	 */
	public static void disableValidationIfEnabled(Project project, RepositoryConnection conn) {

		logger.debug("Disable validation on connection: " + conn.toString());

		if (project.getRepository() != conn.getRepository()) {
			throw new IllegalArgumentException(
					"Could not disable validation for a connection to anything but the core repository of the project");
		}

		if (project.isValidationEnabled()) {
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED, BooleanLiteral.FALSE,
					CHANGETRACKER.VALIDATION);
			conn.prepareBooleanQuery("ASK {}").evaluate(); // perform a dummy query to flush the possibly
															// cached
															// operation
		}
	}

	/**
	 * Returns the add graph if validation is enabled
	 * 
	 * @param validationEnabled
	 * @param graph
	 * @return
	 */
	public static IRI getAddGraphIfValidatonEnabled(boolean validationEnabled, IRI graph) {
		if (validationEnabled) {
			return (IRI) VALIDATION.stagingAddGraph(graph);
		} else {
			return graph;
		}
	}

	/**
	 * Returns the remove graph if validation is enabled
	 * 
	 * @param validationEnabled
	 * @param graph
	 * @return
	 */
	public static Resource getRemoveGraphIfValidatonEnabled(boolean validationEnabled, IRI graph) {
		if (validationEnabled) {
			return (IRI) VALIDATION.stagingRemoveGraph(graph);
		} else {
			return graph;
		}
	}

}
