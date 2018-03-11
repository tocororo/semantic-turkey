package it.uniroma2.art.semanticturkey.ontology;

import static java.util.stream.Collectors.toSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import it.uniroma2.art.semanticturkey.ontology.ImportStatus.Values;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * Test class for {@link OntologyManager}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OntologyManagerTest {

	@Rule
	public STEnviroment stEnv = new STEnviroment();

	/**
	 * In this test case, we first import the ontology <http://www.w3.org/2000/01/rdf-schema#>, and then we
	 * expect a failure if one attempts subsequently to import <http://www.w3.org/2000/01/rdf-schema>
	 * (forgetting the trailing hash character).
	 * 
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws OntologyManagerException
	 */
	@Test
	public void testImport1() throws RDF4JException, MalformedURLException, OntologyManagerException {
		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn,
						"http://www.w3.org/2000/01/rdf-schema#", ImportModality.USER,
						OntologyManager.class.getResource("rdf-schema.rdf").toString(), null,
						transitiveImportAllowance, failedImports);
			});
		}

		try {
			try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
				ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
					Set<IRI> failedImports = new HashSet<>();
					TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
					stEnv.getOntologyManager().addOntologyImportFromWeb(conn,
							"http://www.w3.org/2000/01/rdf-schema", ImportModality.USER,
							OntologyManager.class.getResource("rdf-schema.rdf").toString(), null,
							transitiveImportAllowance, failedImports);
					assertThat(failedImports, empty());
				});
			}
		} catch (OntologyManagerException e) {
			if (e.getMessage().startsWith("Ontology already imported")) {
				return; // exits from the tests successfully
			}
		}

		fail("Missing expected OntologyManagerException");
	}

	/**
	 * In this test case, we first import <http://www.w3.org/2000/01/rdf-schema#>, and then import
	 * <http://www.w3.org/2002/07/owl>, which in turn imports <http://www.w3.org/2000/01/rdf-schema>
	 * (forgetting the trailing hash character). We expect that the system does not reimport the the RDFS
	 * ontology.
	 * 
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws OntologyManagerException
	 */
	@Test
	public void testImport2() throws RDF4JException, MalformedURLException, OntologyManagerException {
		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn,
						"http://www.w3.org/2000/01/rdf-schema#", ImportModality.USER,
						OntologyManager.class.getResource("rdf-schema.rdf").toString(), null,
						transitiveImportAllowance, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asSet(conn.getContextIDs()), containsInAnyOrder(
					vf.createIRI(stEnv.getBaseURI()), vf.createIRI("http://www.w3.org/2000/01/rdf-schema#")));
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
						ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(), null,
						transitiveImportAllowance, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asSet(conn.getContextIDs()),
					containsInAnyOrder(vf.createIRI(stEnv.getBaseURI()),
							vf.createIRI("http://www.w3.org/2000/01/rdf-schema#"),
							vf.createIRI("http://www.w3.org/2002/07/owl")));
		}

	}

	/**
	 * In this test case, we add <http://www.w3.org/2000/01/rdf-schema#> to a named graph without the
	 * terminating #, and then import <http://example.org/ontImportingRDFSWithFragment>, which in turn imports
	 * <http://www.w3.org/2000/01/rdf-schema#>.We expect that the system does not reimport the the RDFS
	 * ontology.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws IOException
	 */
	@Test
	public void testImport2B() throws RDF4JException, OntologyManagerException, IOException {
		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				conn.add(OntologyManager.class.getResource("rdf-schema.rdf"),
						"http://www.w3.org/2000/01/rdf-schema", RDFFormat.RDFXML,
						conn.getValueFactory().createIRI("http://www.w3.org/2000/01/rdf-schema"));
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asSet(conn.getContextIDs()),
					containsInAnyOrder(vf.createIRI("http://www.w3.org/2000/01/rdf-schema")));
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn,
						"http://example.org/ontImportingRDFSWithFragment", ImportModality.USER,
						OntologyManager.class.getResource("ontImportingRDFSWithFragment.ttl").toString(),
						null, transitiveImportAllowance, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asSet(conn.getContextIDs()),
					containsInAnyOrder(vf.createIRI(stEnv.getBaseURI()),
							vf.createIRI("http://www.w3.org/2000/01/rdf-schema"),
							vf.createIRI("http://example.org/ontImportingRDFSWithFragment")));
		}

	}

	/**
	 * In this test case, we first store <http://www.w3.org/2000/01/rdf-schema#> in the ontologies manager,
	 * and then import <http://www.w3.org/2002/07/owl>, which in turn imports
	 * <http://www.w3.org/2000/01/rdf-schema> (forgetting the trailing hash character). We expect that the
	 * system successfully import the dependent ontology using the ontologies mirror.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@Test
	public void testImport3()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {
		stEnv.cacheOntology(OntologyManager.class.getResourceAsStream("rdf-schema.rdf"),
				"http://www.w3.org/2000/01/rdf-schema#", "rdfs.rdf");
		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(stEnv.isValidationRequired(), conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				TransitiveImportMethodAllowance transitiveImportAllowance = TransitiveImportMethodAllowance.mirror;
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
						ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(), null,
						transitiveImportAllowance, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asSet(conn.getContextIDs()),
					containsInAnyOrder(vf.createIRI(stEnv.getBaseURI()),
							vf.createIRI("http://www.w3.org/2000/01/rdf-schema#"),
							vf.createIRI("http://www.w3.org/2002/07/owl")));
		}

	}

	/**
	 * In this test case, we first cache the ontology <http://example.org/B> and then import
	 * <http://example.org/A>, which in turn imports the cached ontology.
	 * 
	 * Using validation, we expect that everything is stores in the staging add graph.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	@RequiresValidation
	public void testImport4()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		stEnv.cacheOntology(OntologyManager.class.getResourceAsStream("ontB.ttl"), "http://example.org/B",
				"ontB.ttl");

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://example.org/A",
					ImportModality.USER, OntologyManager.class.getResource("ontA.ttl").toString(),
					RDFFormat.TURTLE, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asList(conn.getContextIDs()), containsInAnyOrder(
					ValidationUtilities.getAddGraphIfValidatonEnabled(true, vf.createIRI(stEnv.getBaseURI())),
					ValidationUtilities.getAddGraphIfValidatonEnabled(true,
							vf.createIRI("http://example.org/A")),
					ValidationUtilities.getAddGraphIfValidatonEnabled(true,
							vf.createIRI("http://example.org/B"))));
		}

	}

	/**
	 * In this test case we use validation, and then we do the following:
	 * <ol>
	 * <li>we import ontology <http://example.org/B>, temporarily disabling validation (no use of the stage
	 * add graph)</li>
	 * <li>we import ontology <http://example.org/A>, which in turns depends on the former. We expect that
	 * ontology <http://example.org/B> is not staged for addition</li>
	 * <li>we then attempt to remove the explicit import of ontology <http://example.org/B>. This operation
	 * should be disallowed, because the corresponding graph would be kept alive only by staged ontology
	 * imports</li>
	 * </ol>
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	@RequiresValidation
	public void testImport5()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(true, conn, c_ -> {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://example.org/B",
						ImportModality.USER, OntologyManager.class.getResource("ontB.ttl").toString(),
						RDFFormat.TURTLE, TransitiveImportMethodAllowance.mirror, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asList(conn.getContextIDs()), containsInAnyOrder(
					vf.createIRI(stEnv.getBaseURI()), vf.createIRI("http://example.org/B")));
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://example.org/A",
					ImportModality.USER, OntologyManager.class.getResource("ontA.ttl").toString(),
					RDFFormat.TURTLE, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			assertThat(QueryResults.asList(conn.getContextIDs()),
					containsInAnyOrder(vf.createIRI(stEnv.getBaseURI()),
							ValidationUtilities.getAddGraphIfValidatonEnabled(true,
									vf.createIRI(stEnv.getBaseURI())),
							ValidationUtilities.getAddGraphIfValidatonEnabled(true,
									vf.createIRI("http://example.org/A")),
							vf.createIRI("http://example.org/B")));
		}

		try {
			try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().removeOntologyImport(conn, "http://example.org/B");
				assertThat(failedImports, empty());
			}
			fail("Missing expected OntologyManagerException exception");
		} catch (OntologyManagerException e) {
			if (!e.getMessage().startsWith("Could not delete ontology import")) {
				throw e;
			}
		}

	}

	/**
	 * Similar to {@link #testImport5()}, but now the ontologies are RDFS and OWL. The reason is that OWL
	 * imports RDFS without indicating the trailing <code>#</code>, which occurs in the URI of the RDFS
	 * ontology.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	@RequiresValidation
	public void testImport6()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(true, conn, c_ -> {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn,
						"http://www.w3.org/2000/01/rdf-schema#", ImportModality.USER,
						OntologyManager.class.getResource("rdf-schema.rdf").toString(), RDFFormat.RDFXML,
						TransitiveImportMethodAllowance.mirror, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
					ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(),
					RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try {
			try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().removeOntologyImport(conn,
						"http://www.w3.org/2000/01/rdf-schema#");
				assertThat(failedImports, empty());
			}
			fail("Missing expected OntologyManagerException exception");
		} catch (OntologyManagerException e) {
			if (!e.getMessage().startsWith("Could not delete ontology import")) {
				throw e;
			}
		}

	}

	/**
	 * Similar to {@link #testImport6()}, but now OWL is imported (and validated) before RDFS. Check that it
	 * is still impossible to remove the import to OWL.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	@RequiresValidation
	public void testImport7()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		stEnv.cacheOntology(OntologyManager.class.getResourceAsStream("rdf-schema.rdf"),
				"http://www.w3.org/2000/01/rdf-schema#", "rdf-schema.rdf");

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(true, conn, c_ -> {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
						ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(),
						RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
				assertThat(failedImports, empty());
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2000/01/rdf-schema#",
					ImportModality.USER, OntologyManager.class.getResource("rdf-schema.rdf").toString(),
					RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try {
			try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
				stEnv.getOntologyManager().removeOntologyImport(conn, "http://www.w3.org/2002/07/owl");
			}
			fail("Missing expected OntologyManagerException exception");
		} catch (OntologyManagerException e) {
			if (!e.getMessage().startsWith("Could not delete ontology import")) {
				throw e;
			}
		}

	}

	/**
	 * Similar to {@link #testImport6()}, but now OWL is imported (and validated) before RDFS. Check that it
	 * is still impossible to remove the import to OWL.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	public void testImport8()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		stEnv.cacheOntology(OntologyManager.class.getResourceAsStream("rdf-schema.rdf"),
				"http://www.w3.org/2000/01/rdf-schema#", "rdf-schema.rdf");

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
					ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(),
					RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			assertThat(
					stEnv.getOntologyManager()
							.getImportStatus(conn, "http://www.w3.org/2000/01/rdf-schema", false).getValue(),
					is(Values.FAILED));
			assertThat(
					stEnv.getOntologyManager()
							.getImportStatus(conn, "http://www.w3.org/2000/01/rdf-schema", true).getValue(),
					is(Values.OK));
		}

	}

	/**
	 * In this test case, we first import the OWL ontology as a support ontology, and then attempt to import
	 * it again as a user ontology. In the latter step, we append a redundand hash character to the URL.
	 * 
	 * Both imports should succeeds, and in the latter case the {@code owl:imports} statement should use the
	 * ontology URI without the trailing hash character.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	public void testImport9()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
					ImportModality.SUPPORT, OntologyManager.class.getResource("owl.rdf").toString(),
					RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, containsInAnyOrder(
					conn.getValueFactory().createIRI("http://www.w3.org/2000/01/rdf-schema")));
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			assertThat(QueryResults.stream(conn
					.getStatements(conn.getValueFactory().createIRI(stEnv.getBaseURI()), OWL.IMPORTS, null))
					.map(Statement::getObject).collect(toSet()), empty());
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Set<IRI> failedImports = new HashSet<>();
			stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl#",
					ImportModality.USER, OntologyManager.class.getResource("owl.rdf").toString(),
					RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
			assertThat(failedImports, empty());
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			assertThat(
					QueryResults
							.stream(conn.getStatements(conn.getValueFactory().createIRI(stEnv.getBaseURI()),
									OWL.IMPORTS, null))
							.map(Statement::getObject).collect(toSet()),
					containsInAnyOrder(conn.getValueFactory().createIRI("http://www.w3.org/2002/07/owl")));
		}

	}

	/**
	 * Tests that staged removed imports are not duplicated.
	 * 
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	@RequiresValidation
	public void testImport10()
			throws RDF4JException, OntologyManagerException, FileNotFoundException, IOException {

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			ValidationUtilities.executeWithoutValidation(true, conn, _c -> {
				Set<IRI> failedImports = new HashSet<>();
				stEnv.getOntologyManager().addOntologyImportFromWeb(conn, "http://www.w3.org/2002/07/owl",
						ImportModality.SUPPORT, OntologyManager.class.getResource("owl.rdf").toString(),
						RDFFormat.RDFXML, TransitiveImportMethodAllowance.mirror, failedImports);
				assertThat(failedImports, containsInAnyOrder(
						conn.getValueFactory().createIRI("http://www.w3.org/2000/01/rdf-schema")));
			});
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			stEnv.getOntologyManager().removeOntologyImport(conn, "http://www.w3.org/2002/07/owl");
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			stEnv.getOntologyManager().removeOntologyImport(conn, "http://www.w3.org/2002/07/owl");
		}

		try (RepositoryConnection conn = stEnv.getCoreRepo().getConnection()) {
			Collection<OntologyImport> directImports = stEnv.getOntologyManager()
					.getUserOntologyImportTree(conn);

			assertThat(directImports, hasSize(1));

			OntologyImport ontologyImport = directImports.iterator().next();
			assertThat(ontologyImport.getOntology(),
					equalTo(SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl")));
			assertThat(ontologyImport.getStatus(), equalTo(OntologyImport.Statuses.STAGED_REMOVAL));
		}

	}
}
