package it.uniroma2.art.semanticturkey.changetracking.sail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.uniroma2.art.semanticturkey.changetracking.model.HistoryRepositories;
import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerConfig;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;

/**
 * Test class for {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerTest extends NotifyingSailWrapper {

	private static final String HISTORY_REPO_ID = "test-history";
	private static final String HISTORY_NS = "http://example.org/history#";
	private static final IRI HISTORY_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://example.org/history");

	private LocalRepositoryManager repositoryManager;

	private Repository dataRepo;
	private Repository historyRepo;

	private static final Namespace ns = new SimpleNamespace("ex", "http://example.org/");

	private static final String TEST_REPOSITORY_MANAGER_BASE = "target/test-repositories/";

	private static final IRI graphA;
	private static final IRI socrates;
	private static final IRI plato;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		graphA = vf.createIRI(ns.getName(), "graph-A");
		socrates = vf.createIRI(ns.getName(), "socrates");
		plato = vf.createIRI(ns.getName(), "plato");
	}

	@Before
	public void setup() {
		File baseRepoManagerDir = new File(TEST_REPOSITORY_MANAGER_BASE);
		if (baseRepoManagerDir.exists()) {
			FileUtil.deltree(baseRepoManagerDir);
		}
		baseRepoManagerDir.mkdirs();

		repositoryManager = new LocalRepositoryManager(baseRepoManagerDir);
		repositoryManager.initialize();

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig(HISTORY_REPO_ID, new SailRepositoryConfig(new NativeStoreConfig())));

		historyRepo = repositoryManager.getRepository(HISTORY_REPO_ID);
		RepositoryRegistry.getInstance().addRepository(HISTORY_REPO_ID, historyRepo);

		ChangeTrackerConfig trackerConfig = new ChangeTrackerConfig(new NativeStoreConfig());
		trackerConfig.setHistoryRepositoryID(HISTORY_REPO_ID);
		trackerConfig.setHistoryNS(HISTORY_NS);
		trackerConfig.setHistoryGraph(HISTORY_GRAPH);

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig("test-data", new SailRepositoryConfig(trackerConfig)));
		dataRepo = repositoryManager.getRepository("test-data");

	}

	@After
	public void teardown() {
		repositoryManager.shutDown();
	}

	@Test
	public void testSingleCommit() {

		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		Model expectedAdditions = new LinkedHashModel();
		expectedAdditions.add(socrates, RDF.TYPE, FOAF.PERSON);

		Model expectedRemovals = new LinkedHashModel();

		Repositories.consume(dataRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.add(socrates, RDF.TYPE, FOAF.PERSON);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions));
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals));
		});

		assertFalse(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		Repositories.consume(historyRepo, conn -> {
			Optional<Resource> tipHolder = HistoryRepositories.getTip(conn, HISTORY_GRAPH);

			assertTrue(tipHolder.isPresent());

			Resource tip = tipHolder.get();

			assertTrue(tip instanceof IRI);

			assertTrue(tip.stringValue().startsWith(HISTORY_NS));

			Model actualAdditions = QueryResults
					.asModel(HistoryRepositories.getAddedStaments(conn, tip, HISTORY_GRAPH));
			Model actualRemovals = QueryResults
					.asModel(HistoryRepositories.getRemovedStaments(conn, tip, HISTORY_GRAPH));
			
			assertTrue(Models.isomorphic(expectedAdditions, actualAdditions));
			assertTrue(Models.isomorphic(expectedRemovals, actualRemovals));
		});

	}

	@Test
	public void testSelfCancellingUpdates() {

		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		Model expectedAdditions = new LinkedHashModel();
		Model expectedRemovals = new LinkedHashModel();

		Repositories.consume(dataRepo, conn -> {

			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.add(socrates, RDF.TYPE, FOAF.PERSON);
			conn.remove(socrates, RDF.TYPE, FOAF.PERSON);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions));
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals));
		});

		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));
	}

	@Test
	public void testUneffectiveDeletion() {
		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		Model expectedAdditions = new LinkedHashModel();
		Model expectedRemovals = new LinkedHashModel();

		Repositories.consume(dataRepo, conn -> {

			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.remove(socrates, RDF.TYPE, FOAF.PERSON);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions));
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals));

		});

		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));
	}

	
	@Test
	public void testTwoCommits() {

		assertTrue(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		// --- first commit --//
		
		Model expectedAdditions1 = new LinkedHashModel();
		expectedAdditions1.add(socrates, RDF.TYPE, FOAF.PERSON);

		Model expectedRemovals1 = new LinkedHashModel();

		Repositories.consume(dataRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.add(socrates, RDF.TYPE, FOAF.PERSON);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions1));
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals1));
		});

		assertFalse(0 == Repositories.get(historyRepo, historyConn -> historyConn.size()));

		Resource firstCommit = Repositories.get(historyRepo, conn -> {
			Optional<Resource> tipHolder = HistoryRepositories.getTip(conn, HISTORY_GRAPH);

			assertTrue(tipHolder.isPresent());

			Resource tip = tipHolder.get();

			assertTrue(tip instanceof IRI);

			assertTrue(tip.stringValue().startsWith(HISTORY_NS));

			Model actualAdditions = QueryResults
					.asModel(HistoryRepositories.getAddedStaments(conn, tip, HISTORY_GRAPH));
			Model actualRemovals = QueryResults
					.asModel(HistoryRepositories.getRemovedStaments(conn, tip, HISTORY_GRAPH));
			
			assertTrue(Models.isomorphic(expectedAdditions1, actualAdditions));
			assertTrue(Models.isomorphic(expectedRemovals1, actualRemovals));
			
			return tip;
		});
		
		// --- second commit --- //
		
		Model expectedAdditions2 = new LinkedHashModel();
		Model expectedRemovals2 = new LinkedHashModel();
		expectedRemovals2.add(socrates, RDF.TYPE, FOAF.PERSON);

		Repositories.consume(dataRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.remove(socrates, RDF.TYPE, FOAF.PERSON);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions2));
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals2));
		});

		assertEquals(Long.valueOf(0), (Long)Repositories.get(dataRepo, conn -> conn.size()));

		Resource secondCommit = Repositories.get(historyRepo, conn -> {
			Optional<Resource> tipHolder = HistoryRepositories.getTip(conn, HISTORY_GRAPH);

			assertTrue(tipHolder.isPresent());

			Resource tip = tipHolder.get();
			
			assertNotEquals(firstCommit, tip);

			assertTrue(tip instanceof IRI);

			assertTrue(tip.stringValue().startsWith(HISTORY_NS));

			Model actualAdditions = QueryResults
					.asModel(HistoryRepositories.getAddedStaments(conn, tip, HISTORY_GRAPH));
			Model actualRemovals = QueryResults
					.asModel(HistoryRepositories.getRemovedStaments(conn, tip, HISTORY_GRAPH));
			
			assertTrue(Models.isomorphic(expectedAdditions2, actualAdditions));
			assertTrue(Models.isomorphic(expectedRemovals2, actualRemovals));
			
			return tip;
		});
		
		Repositories.consume(historyRepo, conn  -> {
			Optional<Resource> parentHolder = HistoryRepositories.getParent(conn, secondCommit, HISTORY_GRAPH);
			
			assertTrue(parentHolder.isPresent());
			assertEquals(firstCommit, parentHolder.get());
		});

	}

}
