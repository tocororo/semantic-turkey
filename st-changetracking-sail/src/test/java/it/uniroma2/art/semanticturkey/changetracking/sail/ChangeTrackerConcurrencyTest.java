package it.uniroma2.art.semanticturkey.changetracking.sail;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.uniroma2.art.semanticturkey.changetracking.model.HistoryRepositories;
import junit.framework.AssertionFailedError;

/**
 * Test class for {@link ChangeTracker} dealing with concurrency issues.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerConcurrencyTest extends AbstractChangeTrackerTest {

	@Test
	public void testInterleavedTransactions() {
		try (RepositoryConnection conn1 = coreRepo.getConnection();
				RepositoryConnection conn2 = coreRepo.getConnection()) {
			conn1.begin();
			conn2.begin();

			conn1.add(plato, RDF.TYPE, FOAF.PERSON, graphA);
			conn2.add(plato, RDF.TYPE, FOAF.PERSON, graphA);

			conn1.commit();
			conn2.commit();
			
			throw new AssertionFailedError("A conflict should have been occurred");
		} catch (RepositoryException e) {
			if (e.getCause() instanceof SailConflictException) {
				// Swallow expected exception
			} else {
				throw e;
			}
		}

		try (RepositoryConnection conn = supportRepo.getConnection()) {
			Resource tip = HistoryRepositories.getTip(conn, HISTORY_GRAPH)
					.orElseThrow(() -> new AssertionFailedError("expected one commit"));

			assertEquals(QueryResults.asModel(HistoryRepositories.getAddedStaments(conn, tip, HISTORY_GRAPH)),
					new ModelBuilder().namedGraph(graphA).add(plato, RDF.TYPE, FOAF.PERSON).build());
			assertTrue(QueryResults.asModel(HistoryRepositories.getRemovedStaments(conn, tip, HISTORY_GRAPH))
					.isEmpty());
			assertFalse(HistoryRepositories.getParent(conn, tip, HISTORY_GRAPH).isPresent());
		}
	}
}
