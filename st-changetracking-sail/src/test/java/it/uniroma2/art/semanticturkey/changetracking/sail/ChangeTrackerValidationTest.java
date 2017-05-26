package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Set;

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

import static org.junit.Assert.*;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;

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

		try (RepositoryConnection conn = dataRepo.getConnection()) {
			conn.add(plato, RDF.TYPE, FOAF.PERSON, graphA);
		}
		
		printRepositories();

		Set<Resource> appendedForValidation = Repositories.get(historyRepo, (conn) -> {
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

		try (RepositoryConnection conn = dataRepo.getConnection()) {
			conn.remove(plato, RDF.TYPE, FOAF.PERSON, graphA);
		}
		
		Set<Resource> appendedForValidation2 = Repositories.get(historyRepo, (conn) -> {
			return QueryResults
					.asModel(conn.getStatements(null, RDF.TYPE, CHANGELOG.COMMIT, VALIDATION_GRAPH))
					.subjects();
		});

		assertTrue(appendedForValidation2.size() == 2);

		IRI validatableRemoval = (IRI)Sets.difference(appendedForValidation2, appendedForValidation).iterator().next();
		
		printRepositories();

		
		System.out.println();
		System.out.println("=======================");
		System.out.println("Accept " + validatableAddition.getLocalName() +":");
		System.out.println("- :plato rdf:type foaf:Person :graphA");
		System.out.println("=======================");
		System.out.println();
		
		try (RepositoryConnection conn = dataRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableAddition,
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}
		
		try (RepositoryConnection conn = dataRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableRemoval,
					CHANGETRACKER.VALIDATION);
			conn.commit();
		}


		printRepositories();

	}

	protected void printRepositories() throws RepositoryException, UnknownTransactionStateException {
		System.out.println();
		System.out.println("--- Data repo ---");
		System.out.println();

		Repositories.consume(dataRepo, conn -> {
			conn.export(Rio.createWriter(RDFFormat.NQUADS, System.out));
		});

		System.out.println();
		System.out.println("--- History repo ---");
		System.out.println();

		Repositories.consume(historyRepo, conn -> {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TRIG, System.out);
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			conn.export(rdfWriter);
		});

		System.out.println();
	}

}
