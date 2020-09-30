package it.uniroma2.art.semanticturkey.validation;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import it.uniroma2.art.semanticturkey.ontology.RequiresValidation;
import it.uniroma2.art.semanticturkey.ontology.STEnviroment;

public class ValidationUtilsTest {

	@Rule
	public STEnviroment stEnv = new STEnviroment();

	@RequiresValidation
	@Test
	public void testClearData() {
		Repository coreRepo = stEnv.getCoreRepo();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.add(FOAF.PERSON, RDF.TYPE, OWL.CLASS,
					conn.getValueFactory().createIRI("http://xmlns.com/foaf/0.1/"));
		}

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			assertThat(conn.size(), greaterThan(0L));
		}

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			ValidationUtilities.executeWithoutValidation(true, conn, c_ -> {
				c_.clear();
			});
			conn.commit();
		}

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.export(Rio.createWriter(RDFFormat.NQUADS, System.out));
			assertThat(conn.size(), is(0L));
		}

	}

}
