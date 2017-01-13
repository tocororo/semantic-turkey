package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.base.InterceptingRepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.tx.ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor;

/**
  * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
  */
public class SPARQLExportFilterTest {

	public static final String BASE_URI = "http://example.org/";

	private SPARQLExportFilterFactory factory;

	private SailRepository sourceRepository;
	private SailRepository workingRepository;

	@Before
	public void setup() {
		factory = new SPARQLExportFilterFactory();

		sourceRepository = new SailRepository(new MemoryStore());
		sourceRepository.initialize();

		workingRepository = new SailRepository(new MemoryStore());
		workingRepository.initialize();
	}

	@After
	public void teardown() {
		try {
			if (sourceRepository != null) {
				sourceRepository.shutDown();
			}
		} finally {
			if (workingRepository != null) {
				workingRepository.shutDown();
			}
		}
	}

	/**
	 * Executes a test identified by caller method name. The test will try to find within the resources the
	 * following files:
	 * <ul>
	 * <li><i>{testname}-data.trig</i>: input data in TriG format (base URI is {@value #BASE_URI})</li>
	 * <li><i>{testname}-filter.txt</i>: a SPARQL update implementing the filter</li>
	 * <li><i>{testname}-expected.trig</i>: the expected model in TriG format</li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	protected void executeTest() throws Exception {
		try (RepositoryConnection sourceRepositoryConnection = sourceRepository.getConnection()) {
			try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
				String testName = Thread.currentThread().getStackTrace()[2].getMethodName();

				// Feed the source repository
				sourceRepositoryConnection.add(
						Objects.requireNonNull(this.getClass().getResourceAsStream(testName + "-data.trig"),
								"missing input data for test " + testName),
						BASE_URI, RDFFormat.TRIG);

				// Get the expected model
				Model expectedModel = Rio.parse(Objects.requireNonNull(
						this.getClass().getResourceAsStream(testName + "-expected.trig"),
						"missing input data for test " + testName), BASE_URI, RDFFormat.TRIG);

				// Copy the source repository to the working repository
				sourceRepositoryConnection.export(new RDFInserter(workingRepositoryConnection));

				// Read-only wrapper around the source repository
				InterceptingRepositoryConnectionWrapper readOnlySourceRepositoryConnection = new InterceptingRepositoryConnectionWrapper(
						sourceRepository, sourceRepositoryConnection);
				readOnlySourceRepositoryConnection.addRepositoryConnectionInterceptor(
						new ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor());

				// Get the SPARQL Update
				String filter = IOUtils.toString(
						Objects.requireNonNull(this.getClass().getResourceAsStream(testName + "-filter.txt"),
								"missing SPARQL filter for test " + testName));

				// Instantiate a configured export filter
				PluginConfiguration pluginConfiguration = factory.createDefaultPluginConfiguration();
				pluginConfiguration.setParameter("filter", filter);

				SPARQLExportFilter exportFilter = factory.createInstance(pluginConfiguration);

				// Execute the export (the fact that we pass a read-only wrapper around the source repository
				// should detect any attempt by the filter to modify the source repository)
				exportFilter.filter(readOnlySourceRepositoryConnection, workingRepositoryConnection,
						SimpleValueFactory.getInstance().createIRI(BASE_URI));

				// // Uncomment the following lines to print the working repo after the filter
				// System.out.println("Test name: " + testName);
				// RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, System.out);
				// workingRepositoryConnection.export(writer);

				// Asserts that the working repository matches the expected model
				
				assertTrue(Models.isomorphic(expectedModel,
						QueryResults.asModel(workingRepositoryConnection.getStatements(null, null, null))));
			}
		}
	}

	@Test
	public void testSPARQLExport1() throws Exception {
		executeTest();
	}

}
