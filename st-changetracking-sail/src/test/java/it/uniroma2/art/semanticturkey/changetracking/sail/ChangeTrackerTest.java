package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import it.uniroma2.art.semanticturkey.changetracking.model.HistoryRepositories;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;

/**
 * Test class for {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerTest extends AbstractChangeTrackerTest {

	protected void testSkeleton(TestCommitStrategy strategy) {
		assertTrue(0 == Repositories.get(supportRepo, historyConn -> historyConn.size()));

		Model expectedAdditions = strategy.expectedAdditions();
		Model expectedRemovals = strategy.expectedRemovals();

		Repositories.consume(coreRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			strategy.doUpdate(conn);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions));
			assertTrue(Models.isomorphic(
					QueryResults.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals));
		});

		if (strategy.shouldIncreaseHistory()) {
			assertFalse(0 == Repositories.get(supportRepo, historyConn -> historyConn.size()));
		} else {
			assertTrue(0 == Repositories.get(supportRepo, historyConn -> historyConn.size()));
		}

		if (strategy.shouldIncreaseData()) {
			assertFalse(0 == Repositories.get(coreRepo, historyConn -> historyConn.size()));
		} else {
			assertTrue(0 == Repositories.get(coreRepo, historyConn -> historyConn.size()));
		}

		if (strategy.shouldIncreaseHistory()) {
			Repositories.consume(supportRepo, conn -> {
				conditionalPrintHistory(conn);
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

				
				Optional<Literal> startTimeHolder = Models.objectLiteral(QueryResults.asModel(conn.getStatements(tip, PROV.STARTED_AT_TIME, null)));
				Optional<Literal> endTimeHolder = Models.objectLiteral(QueryResults.asModel(conn.getStatements(tip, PROV.ENDED_AT_TIME, null)));

				assertTrue(startTimeHolder.isPresent());
				assertTrue(endTimeHolder.isPresent());
				
				Literal startTime = startTimeHolder.get();
				Literal endTime = endTimeHolder.get();

				assertEquals(XMLSchema.DATETIME, startTime.getDatatype());
				assertEquals(XMLSchema.DATETIME, endTime.getDatatype());

				XMLGregorianCalendar startTimeValue = Literals.getCalendarValue(startTime, null);
				XMLGregorianCalendar endTimeValue = Literals.getCalendarValue(endTime, null);
				
				assertTrue(startTimeValue.compare(endTimeValue) < 0);
				
				strategy.checkCommit(conn, tip);
			});
		}
	}

	protected void conditionalPrintHistory(RepositoryConnection historyConn)
			throws UnsupportedRDFormatException, RepositoryException, RDFHandlerException {
		if (PRINT_HISTORY) {
			System.out.println("-------------------");
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TRIG, System.out);
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			historyConn.export(rdfWriter);
			System.out.println("-------------------");
		}
	}

	protected abstract class TestCommitStrategy {

		public abstract Model expectedAdditions();

		public abstract Model expectedRemovals();

		public abstract void doUpdate(RepositoryConnection conn);

		public abstract boolean shouldIncreaseHistory();

		public abstract boolean shouldIncreaseData();

		public void checkCommit(RepositoryConnection historyConn, Resource commit) {
		}
	}

	@Test
	public void testSingleCommit() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().namedGraph(graphA).add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

		});
	}

	@Test
	public void testSelfCancellingUpdates() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public Model expectedAdditions() {
				return new LinkedHashModel();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
				conn.remove(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}

			@Override
			public boolean shouldIncreaseHistory() {
				return false;
			}

			@Override
			public boolean shouldIncreaseData() {
				return false;
			}

		});
	}

	@Test
	public void testUneffectiveDeletion() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public Model expectedAdditions() {
				return new LinkedHashModel();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.remove(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}

			@Override
			public boolean shouldIncreaseHistory() {
				return false;
			}

			@Override
			public boolean shouldIncreaseData() {
				return false;
			}

		});
	}

	@Test
	public void testTwoCommits() {

		assertTrue(0 == Repositories.get(supportRepo, historyConn -> historyConn.size()));

		// --- first commit --//

		Model expectedAdditions1 = new LinkedHashModel();
		expectedAdditions1.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);

		Model expectedRemovals1 = new LinkedHashModel();

		Repositories.consume(coreRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions1));
			assertTrue(Models.isomorphic(
					QueryResults.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals1));
		});

		assertFalse(0 == Repositories.get(supportRepo, historyConn -> historyConn.size()));

		Resource firstCommit = Repositories.get(supportRepo, conn -> {
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
		expectedRemovals2.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);

		Repositories.consume(coreRepo, conn -> {
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					Collections.emptyList());
			assertEquals(
					QueryResults.asList(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					Collections.emptyList());

			conn.remove(socrates, RDF.TYPE, FOAF.PERSON, graphA);

			assertTrue(Models.isomorphic(
					QueryResults
							.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_ADDITIONS)),
					expectedAdditions2));
			assertTrue(Models.isomorphic(
					QueryResults.asModel(conn.getStatements(null, null, null, CHANGETRACKER.STAGED_REMOVALS)),
					expectedRemovals2));
		});

		assertEquals(Long.valueOf(0), (Long) Repositories.get(coreRepo, conn -> conn.size()));

		Resource secondCommit = Repositories.get(supportRepo, conn -> {
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
		
		Repositories.consume(supportRepo, conn -> {
			conditionalPrintHistory(conn);
		});
		
		Repositories.consume(supportRepo, conn -> {
			Optional<Resource> parentHolder = HistoryRepositories.getParent(conn, secondCommit,
					HISTORY_GRAPH);

			assertTrue(parentHolder.isPresent());
			assertEquals(firstCommit, parentHolder.get());
		});
	}

	@Test
	public void testDefaultInclusionsAndInclusions1() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().namedGraph(graphA).add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.add(socrates, RDF.TYPE, FOAF.PERSON);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

		});
	}

	@Test
	public void testDefaultInclusionsAndInclusions2() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().namedGraph(graphA).add(socrates, RDF.TYPE, FOAF.PERSON)
						.namedGraph(graphB).add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphB);
			}

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

		});
	}

	@Test
	public void testReadDefaultInclusionsAndExclusions() {
		Repositories.consume(coreRepo, conn -> {
			Set<Value> defaultInclusions = QueryResults
					.asModel(conn.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH,
							null, CHANGETRACKER.GRAPH_MANAGEMENT))
					.objects();
			Set<Value> defaultExclusions = QueryResults
					.asModel(conn.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH,
							null, CHANGETRACKER.GRAPH_MANAGEMENT))
					.objects();

			assertTrue(defaultInclusions.isEmpty());
			assertTrue(defaultExclusions.equals(Collections.singleton(SESAME.NIL)));
		});
	}

	@Test
	public void testChangeInclusionsAndExclusions1() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().namedGraph(graphA).add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, graphA,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphB);
			}
		});
	}

	@Test
	public void testChangeInclusionsAndExclusions2() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, SESAME.NIL,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(socrates, RDF.TYPE, FOAF.PERSON);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}
		});
	}

	@Test
	public void testChangeInclusionsAndExclusions3() {
		testSkeleton(new TestCommitStrategy() {

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);
				conn.remove(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, null,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, graphA,
						CHANGETRACKER.GRAPH_MANAGEMENT);

				conn.add(socrates, RDF.TYPE, FOAF.PERSON);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}
		});
	}

	@Test
	public void testCommitMetadata1() {
		// @formatter:off
		BNode qualifiedAssociation = SimpleValueFactory.getInstance().createBNode();
		
		Model commitMetadata = new ModelBuilder()
			.setNamespace(CHANGELOG.NS)
			.setNamespace(PROV.NS)
			.namedGraph(CHANGETRACKER.COMMIT_METADATA)
			.subject(CHANGETRACKER.COMMIT_METADATA)
				.add(PROV.USED, SimpleValueFactory.getInstance().createIRI("http://semanticturkey.uniroma2.it/ns/services/SKOS/addConcept"))
				.add(PROV.QUALIFIED_ASSOCIATION, qualifiedAssociation)
			.subject(qualifiedAssociation)
				.add(RDF.TYPE, PROV.ASSOCIATION)
				.add(PROV.HAS_AGENT, SimpleValueFactory.getInstance().createIRI("http://semanticturkey.uniroma2.it/ns/users/TestUser"))
				.add(PROV.HAD_ROLE, SimpleValueFactory.getInstance().createIRI("http://semanticturkey.uniroma2.it/ns/roles/performer"))
			.build();
		// @formatter:on

		testSkeleton(new TestCommitStrategy() {

			@Override
			public boolean shouldIncreaseHistory() {
				return true;
			}

			@Override
			public boolean shouldIncreaseData() {
				return true;
			}

			@Override
			public Model expectedAdditions() {
				return new ModelBuilder().namedGraph(graphA).add(socrates, RDF.TYPE, FOAF.PERSON).build();
			}

			@Override
			public Model expectedRemovals() {
				return new LinkedHashModel();
			}

			@Override
			public void doUpdate(RepositoryConnection conn) {
				conn.add(commitMetadata, CHANGETRACKER.COMMIT_METADATA);
				conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
			}

			@Override
			public void checkCommit(RepositoryConnection historyConn, Resource commit) {
				String metadataCheckQuery = commitMetadata.stream().map(s -> RenderUtils.toSPARQL(s.getSubject()) + " " + RenderUtils.toSPARQL(s.getPredicate()) + " " + RenderUtils.toSPARQL(s.getObject()) + ".").collect(Collectors.joining("\n", "ASK {\n", "\n}\n"));
				metadataCheckQuery = metadataCheckQuery.replace("<http://semanticturkey.uniroma2.it/ns/change-tracker#commit-metadata>", RenderUtils.toSPARQL(commit));
				BooleanQuery metadataCheckQueryObj = historyConn.prepareBooleanQuery(metadataCheckQuery);
				assertTrue(metadataCheckQueryObj.evaluate());
				
					
			}
		});
	}
}
