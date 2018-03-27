package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.base.InterceptingRepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertTrue;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.tx.ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor;

/**
 * Abstract base class of tests for implementation of {@link RDFTransformer}. To test a specific
 * implementation, it is sufficient to write a concrete subclass having test methods each invoking
 * {@link #executeTest()}. The latter would discover from the class path (in the same package) a number of
 * resources describing the specific test case:
 * <ul>
 * <li><i>{testclass}-{testmethod}-settings.xl</i>: settings of the test</li>
 * <li><i>{testclass}-{testname}-data.trig</i>: input data in TriG format (base URI is {@value #BASE_URI})
 * </li>
 * <li><i>{testclass}-{testname}-expected.trig</i>: the expected model in TriG format</li>
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractRDFTransformerTest {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private SailRepository sourceRepository;
	private SailRepository workingRepository;

	@Before
	public void setup() {
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
	 * <li><i>{testclass}-{testmethod}-settings.xl</i>: settings of the test</li>
	 * <li><i>{testclass}-{testname}-data.trig</i>: input data in TriG format (base URI is {@value #BASE_URI})
	 * </li>
	 * <li><i>{testclass}-{testname}-expected.trig</i>: the expected model in TriG format</li>
	 * </ul>
	 * 
	 * @throws Exception
	 */
	protected void executeTest() throws Exception {
		try (RepositoryConnection sourceRepositoryConnection = sourceRepository.getConnection()) {
			try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
				String testName = Thread.currentThread().getStackTrace()[2].getMethodName();

				// Retrieves the settings
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

				Document settings = docBuilder
						.parse(Objects
								.requireNonNull(
										this.getClass()
												.getResourceAsStream(this.getClass().getSimpleName() + "-"
														+ testName + "-settings.xml"),
										"missing settings for test " + testName));

				Element testCaseElement = settings.getDocumentElement();

				String baseURI = Objects
						.requireNonNull(testCaseElement.getElementsByTagName("BaseURI").item(0),
								"missing element <BaseURI>")
						.getTextContent();

				IRI[] graphs = Optional.ofNullable(testCaseElement.getElementsByTagName("Graphs").item(0))
						.map(graphsElement -> {
							NodeList nodeList = ((Element) graphsElement).getElementsByTagName("Graph");
							ArrayList<IRI> rv = new ArrayList<>(nodeList.getLength());
							for (int i = 0; i < nodeList.getLength(); i++) {
								rv.add(vf.createIRI(nodeList.item(i).getTextContent()));
							}
							return rv.toArray(new IRI[rv.size()]);
						}).orElseGet(() -> new IRI[0]);

				String factoryID = Objects
						.requireNonNull(testCaseElement.getElementsByTagName("FactoryID").item(0),
								"missing element <FactoryID>")
						.getTextContent();

				@SuppressWarnings("unchecked")
				ExtensionFactory<? extends RDFTransformer> factory = (ExtensionFactory<? extends RDFTransformer>) Class
						.forName(factoryID).newInstance();

				Element configElement = Optional
						.ofNullable(testCaseElement.getElementsByTagName("Config").item(0))
						.map(Element.class::cast).orElse(null);

				String configType;
				Element paramsElement = null;

				if (configElement != null) {
					configType = Optional.ofNullable(configElement.getElementsByTagName("ConfigType").item(0))
							.map(n -> ((Element) n).getTextContent()).orElse(null);
					paramsElement = Optional.ofNullable(configElement.getElementsByTagName("Params").item(0))
							.map(Element.class::cast).orElse(null);
				} else {
					configType = null;
				}

				Configuration config;

				RDFTransformer exportFilter;

				if (configType == null) {
					exportFilter = ((NonConfigurableExtensionFactory<? extends RDFTransformer>) factory)
							.createInstance();
				} else {
					@SuppressWarnings("unchecked")
					ConfigurableExtensionFactory<? extends RDFTransformer, Configuration> configurableFactory = ((ConfigurableExtensionFactory<? extends RDFTransformer, Configuration>) factory);
					config = configurableFactory.getConfigurations().stream()
							.filter(c -> c.getClass().getName().equals(configType)).findAny()
							.orElseThrow(() -> new IllegalArgumentException(
									"Unknown configuration type: " + configType));

					if (paramsElement != null) {
						NodeList childNodes = paramsElement.getChildNodes();
						for (int i = 0; i < childNodes.getLength(); i++) {
							Node child = childNodes.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								Element childElement = (Element) child;
								config.setPropertyValue(childElement.getTagName(),
										childElement.getTextContent());
							}
						}
					}

					exportFilter = configurableFactory.createInstance(config);
				}

				// Feed the source repository
				sourceRepositoryConnection
						.add(Objects
								.requireNonNull(
										this.getClass()
												.getResourceAsStream(this.getClass().getSimpleName() + "-"
														+ testName + "-data.trig"),
										"missing input data for test " + testName),
								baseURI, RDFFormat.TRIG);

				// Get the expected model
				Model expectedModel = Rio
						.parse(Objects
								.requireNonNull(
										this.getClass()
												.getResourceAsStream(this.getClass().getSimpleName() + "-"
														+ testName + "-expected.trig"),
										"missing input data for test " + testName),
								baseURI, RDFFormat.TRIG);

				// Copy the source repository to the working repository
				sourceRepositoryConnection.export(new RDFInserter(workingRepositoryConnection));

				// Read-only wrapper around the source repository
				InterceptingRepositoryConnectionWrapper readOnlySourceRepositoryConnection = new InterceptingRepositoryConnectionWrapper(
						sourceRepository, sourceRepositoryConnection);
				readOnlySourceRepositoryConnection.addRepositoryConnectionInterceptor(
						new ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor());

				// Execute the RDF Transformer (the fact that we pass a read-only wrapper around the source
				// repository
				// should detect any attempt by the filter to modify the source repository)
				exportFilter.transform(readOnlySourceRepositoryConnection, workingRepositoryConnection,
						graphs);

				printWorkingRepository(testName, workingRepositoryConnection);

				// Asserts that the working repository matches the expected model

				assertTrue(Models.isomorphic(expectedModel,
						QueryResults.asModel(workingRepositoryConnection.getStatements(null, null, null))));
			}
		}
	}

	private void printWorkingRepository(String testName, RepositoryConnection workingRepositoryConnection) {
		if (isPrintEnabled(testName)) {
			System.out.println("Test name: " + testName);
			RDFWriter writer = Rio.createWriter(RDFFormat.NQUADS, System.out);
			workingRepositoryConnection.export(writer);
			System.out.println();
			System.out.println();
		}
	}

	protected boolean isPrintEnabled(String testName) {
		return false;
	}
}
