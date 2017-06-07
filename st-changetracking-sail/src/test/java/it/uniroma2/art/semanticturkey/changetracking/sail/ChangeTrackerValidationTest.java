package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertTrue;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;

/**
 * Test class for {@link ChangeTracker} focusing on validation.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerValidationTest extends AbstractChangeTrackerTest {

	@Test
	@RequiresValidation
	public void testValidation() {
		System.out.println();
		System.out.println("=======================");
		System.out.println("+ :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.add(plato, RDF.TYPE, FOAF.PERSON, graphA);
		}

		printRepositories();

		Set<Resource> appendedForValidation = Repositories.get(supportRepo, (conn) -> {
			return QueryResults
					.asModel(conn.getStatements(null, RDF.TYPE, CHANGELOG.COMMIT, VALIDATION_GRAPH))
					.subjects();
		});

		assertTrue(appendedForValidation.size() == 1);

		IRI validatableAddition = (IRI) appendedForValidation.iterator().next();

		System.out.println();
		System.out.println("=======================");
		System.out.println("- :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.remove(plato, RDF.TYPE, FOAF.PERSON, graphA);
		}

		Set<Resource> appendedForValidation2 = Repositories.get(supportRepo, (conn) -> {
			return QueryResults
					.asModel(conn.getStatements(null, RDF.TYPE, CHANGELOG.COMMIT, VALIDATION_GRAPH))
					.subjects();
		});

		assertTrue(appendedForValidation2.size() == 2);

		IRI validatableRemoval = (IRI) Sets.difference(appendedForValidation2, appendedForValidation)
				.iterator().next();

		printRepositories();

		System.out.println();
		System.out.println("=======================");
		System.out.println("Accept " + validatableAddition.getLocalName() + ":");
		System.out.println("- :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableAddition,
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableRemoval,
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}

		printRepositories();

	}

	@Test
	@RequiresValidation
	public void testValidation2() {
		System.out.println();
		System.out.println("=======================");
		System.out.println("+ :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.add(plato, RDF.TYPE, FOAF.PERSON, graphA);
		}

		printRepositories();

		System.out.println();
		System.out.println("=======================");
		System.out.println("+ :socrates rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.add(socrates, RDF.TYPE, FOAF.PERSON, graphA);
		}

		printRepositories();

		List<Resource> validatableAddition = Repositories.tupleQuery(supportRepo,
				"SELECT ?commit {?commit a <http://semanticturkey.uniroma2.it/ns/changelog#Commit> ; <http://www.w3.org/ns/prov#endedAtTime> ?endTime} ORDER BY ?endTime",
				qr -> QueryResults.stream(qr).map(bs -> (Resource) bs.getValue("commit"))
						.collect(Collectors.toList()));

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			assertTrue(conn.size(graphA) == 0);
			assertTrue(conn.size(VALIDATION.stagingAddGraph(graphA)) == 2);
			assertTrue(conn.size(VALIDATION.stagingRemoveGraph(graphA)) == 0);
		}
		assertTrue(validatableAddition.size() == 2);
		
		System.out.println();
		System.out.println("=======================");
		System.out.println("Reject " + validatableAddition.get(1) + ":");
		System.out.println("+ :socrates rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT, validatableAddition.get(1),
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}
		
		printRepositories();
		
		System.out.println();
		System.out.println("=======================");
		System.out.println("Reject " + validatableAddition.get(0) + ":");
		System.out.println("+ :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT, validatableAddition.get(0),
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}
		
		printRepositories();
		

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			assertTrue(conn.size() == 0);
		}
	
		try (RepositoryConnection conn = supportRepo.getConnection()) {
			assertTrue(conn.size() == 0);
		}
	}

	protected void printRepositories() throws RepositoryException, UnknownTransactionStateException {
		System.out.println();
		System.out.println("--- Data repo ---");
		System.out.println();

		Repositories.consume(coreRepo, conn -> {
			conn.export(Rio.createWriter(RDFFormat.NQUADS, System.out));
		});

		System.out.println();
		System.out.println("--- History repo ---");
		System.out.println();

		Repositories.consume(supportRepo, conn -> {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TRIG, System.out);
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			conn.export(rdfWriter);
		});

		System.out.println();
	}

}
