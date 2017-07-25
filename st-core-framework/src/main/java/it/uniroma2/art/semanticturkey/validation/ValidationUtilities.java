package it.uniroma2.art.semanticturkey.validation;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;

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
	 * @param conn
	 * @param consumer
	 * @throws Y
	 */
	public static <Y extends Exception> void executeWithoutValidation(RepositoryConnection conn,
			ThrowingConsumer<RepositoryConnection, Y> consumer) throws Y {

		logger.debug("Disable validation on connection: " + conn.toString());

		boolean validationEnabled = isValidationEnabled(conn);

		logger.debug("Is validation enabled: " + validationEnabled);

		if (validationEnabled) {
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED, BooleanLiteral.FALSE,
					CHANGETRACKER.VALIDATION);
			conn.prepareBooleanQuery("ASK {}").evaluate(); // perform a dummy query to flush the possibly
															// cached operation
		}

		try {
			consumer.accept(conn);
		} finally {
			if (validationEnabled) {
				conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED, BooleanLiteral.TRUE,
						CHANGETRACKER.VALIDATION);
				conn.prepareBooleanQuery("ASK {}").evaluate(); // perform a dummy query to flush the possibly
																// cached operation
			}
		}
	}

	/**
	 * Determines whether validation is enabled on the provided connection
	 * 
	 * @param conn
	 * @return
	 */
	public static boolean isValidationEnabled(RepositoryConnection conn) {
		Optional<Value> enablmentHodler = Models
				.object(QueryResults.asModel(conn.getStatements(CHANGETRACKER.VALIDATION,
						CHANGETRACKER.ENABLED, null, CHANGETRACKER.VALIDATION)));
		if (enablmentHodler.isPresent()) {
			return enablmentHodler.get().equals(BooleanLiteral.TRUE);
		} else {
			return false;
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

	public static interface ThrowingConsumer<X, Y extends Exception> {
		void accept(X input) throws Y;
	}
}
