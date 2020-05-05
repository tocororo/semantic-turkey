package it.uniroma2.art.semanticturkey.trivialinference.sail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

/**
 * Test class for {@link TrivialInferencer}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class TrivialInferencerTest extends NotifyingSailWrapper {
	protected Repository repo;

	public static IRI g = SimpleValueFactory.getInstance().createIRI("http://example.org/");

	@Before
	public abstract void setup();

	@After
	public void teardown() {
		repo.shutDown();
	}

	@Test
	public void testDefintionOfSymmetricPropertyBeforeUsage() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI friend = vf.createIRI("http://example.org/friend");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(friend, RDF.TYPE, OWL.SYMMETRICPROPERTY, g);
		}

		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(john, friend, philipp, g);
			conn.commit();
		}

		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements, CoreMatchers.hasItem(vf.createStatement(philipp, friend, john, g)));
	}

	@Test
	public void testDefintionOfSymmetricPropertyTogetherWithUsage() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI friend = vf.createIRI("http://example.org/friend");
		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(friend, RDF.TYPE, OWL.SYMMETRICPROPERTY, g);
			conn.add(john, friend, philipp, g);
			conn.commit();
		}

		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(philipp, friend, john, g))));
	}

	@Test
	public void testDefintionOfInversePropertyBeforeUsage() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI parent = vf.createIRI("http://example.org/parent");
		IRI child = vf.createIRI("http://example.org/child");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(parent, OWL.INVERSEOF, child, g);
		}

		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(john, parent, philipp, g);
			conn.commit();
		}

		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements, CoreMatchers.hasItem(vf.createStatement(philipp, child, john, g)));
	}

	@Test
	public void testDefintionOfInversePropertyTogetherWithUsage() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI parent = vf.createIRI("http://example.org/parent");
		IRI child = vf.createIRI("http://example.org/child");
		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(parent, OWL.INVERSEOF, child, g);
			conn.add(john, parent, philipp, g);
			conn.commit();
		}

		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(philipp, child, john, g))));
	}

	@Test
	public void testDefintionOfSymmetricPropertyBeforeDeletion() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI friend = vf.createIRI("http://example.org/friend");

		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		System.out.println("add instances");
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(john, friend, philipp, g);
			conn.add(philipp, friend, john, g);
			conn.commit();
		}

		System.out.println("add symmetric definition");
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(friend, RDF.TYPE, OWL.SYMMETRICPROPERTY, g);
		}

		Model preconditionStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(preconditionStatements)));

		assertThat(preconditionStatements, CoreMatchers.hasItem(vf.createStatement(john, friend, philipp, g)));
		assertThat(preconditionStatements,
				CoreMatchers.hasItem(vf.createStatement(philipp, friend, john, g)));

		System.out.println("remove statement");
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.remove(john, friend, philipp, g);
			conn.commit();
		}

		System.out.println("test final status");
		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(philipp, friend, john, g))));
		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(john, friend, philipp, g))));
	}

	@Test
	public void testDefintionOfInversePropertyBeforeDeletion() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI parent = vf.createIRI("http://example.org/parent");
		IRI child = vf.createIRI("http://example.org/child");

		IRI john = vf.createIRI("http://example.org/john");
		IRI philipp = vf.createIRI("http://example.org/philipp");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(john, parent, philipp, g);
			conn.add(philipp, child, john, g);
			conn.commit();
		}

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(parent, OWL.INVERSEOF, child, g);
		}

		Model preconditionStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(preconditionStatements)));

		assertThat(preconditionStatements, CoreMatchers.hasItem(vf.createStatement(philipp, child, john, g)));
		assertThat(preconditionStatements,
				CoreMatchers.hasItem(vf.createStatement(john, parent, philipp, g)));

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.remove(john, parent, philipp, g);
			conn.commit();
		}

		Model actualStatements = new LinkedHashModel();
		Repositories.consume(repo, conn -> conn.export(new StatementCollector(actualStatements)));

		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(philipp, child, john, g))));
		assertThat(actualStatements,
				CoreMatchers.not(CoreMatchers.hasItem(vf.createStatement(john, parent, philipp, g))));

	}

}
