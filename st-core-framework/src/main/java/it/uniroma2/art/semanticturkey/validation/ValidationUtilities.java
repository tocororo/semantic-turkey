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
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;

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
	 * @param validationEnabled
	 * @param conn
	 * @param consumer
	 * @throws Y
	 */
	public static <Y extends Exception> void executeWithoutValidation(boolean validationEnabled,
			RepositoryConnection conn, ThrowingConsumer<RepositoryConnection, Y> consumer) throws Y {

		logger.debug("Disable validation on connection: " + conn.toString());

		logger.debug("Is validation enabled6: " + validationEnabled);

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
		return conn.hasStatement(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED, BooleanLiteral.TRUE, false,
				CHANGETRACKER.VALIDATION);
	}

	/**
	 * Determines whether validation is enabled on the provided service context. It assumes that validation is
	 * never enabled on a repository other than the core one.
	 * 
	 * @param stServiceContext
	 * @return
	 */
	public static boolean isValidationEnabled(STServiceContext stServiceContext) {
		String repositoryId = STServiceContextUtils.getRepostoryId(stServiceContext);

		if (repositoryId.equals(Project.CORE_REPOSITORY)) {
			return stServiceContext.getProject().isValidationEnabled();
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
	 * Returns a graph that indicates to clear staged additions if validation is enabled, or to clear the
	 * already asserted graph
	 * 
	 * @param validationEnabled
	 * @param ont
	 * @return
	 */
	public static Resource getClearThroughGraphIfValidationEnabled(boolean validationEnabled, IRI graph) {
		if (validationEnabled) {
			return (IRI) VALIDATION.clearThroughGraph(graph);
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

	public static interface ThrowingProcedure<Y extends Exception> {
		void execute() throws Y;
	}

}
