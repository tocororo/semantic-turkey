package it.uniroma2.art.semanticturkey.services.core.lime;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon.LIME;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LIMEProfilerTest {
	private Repository repo;

	@Before
	public void setup() {
		repo = new SailRepository(new MemoryStore());
		repo.initialize();
	}

	@Test
	public void test1() throws Exception {
		executeTestInternal();
	}

	@Test
	public void test2() throws Exception {
		executeTestInternal();
	}

	@Test
	public void test3() throws Exception {
		executeTestInternal();
	}

	private void executeTestInternal() throws ParserConfigurationException, SAXException, IOException {
		String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document testCaseDoc;
		try (InputStream is = Objects.requireNonNull(
				this.getClass()
						.getResourceAsStream(this.getClass().getSimpleName() + "-" + testName + ".xml"),
				"missing input data for test " + testName)) {
			testCaseDoc = docBuilder.parse(is);
		}

		Element testCaseElement = testCaseDoc.getDocumentElement();
		String baseURI = testCaseElement.getElementsByTagName("BaseURI").item(0).getTextContent();
		IRI lexicalizationModelIRI = SimpleValueFactory.getInstance().createIRI(
				testCaseElement.getElementsByTagName("LexicalizationModel").item(0).getTextContent());

		String inputDatasetText = testCaseElement.getElementsByTagName("InputDataset").item(0)
				.getTextContent();
		String expectedStatisticsText = testCaseElement.getElementsByTagName("ExpectedStatistics").item(0)
				.getTextContent();

		Repositories.consume(repo, repoConn -> {
			RDFParser rdfParser = Rio.createParser(RDFFormat.TRIG);
			rdfParser.setRDFHandler(new RDFInserter(repoConn));
			try {
				rdfParser.parse(new StringReader(inputDatasetText), baseURI);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		Model actualStatistics = new TreeModel();
		try (RepositoryConnection repoConn = repo.getConnection()) {
			Model expectedStatistics = Rio.parse(new StringReader(expectedStatisticsText), baseURI,
					RDFFormat.TRIG);

			LIMEProfiler limeProfiler = new LIMEProfiler();

			IRI referenceDataset = SimpleValueFactory.getInstance()
					.createIRI("http://example.org/void.ttl#Dataset");

			Model datasetProfile = new LinkedHashModel();

			DatasetStatistics datasetStatistics = limeProfiler.profileDataset(repoConn, referenceDataset,
					new SemanticModel(), datasetProfile);
			datasetStatistics.setReferences(2);
			LexicalizationModel lexicalizationModel = new LexicalizationModelRegistry()
					.getLexicalizationModel(lexicalizationModelIRI).get();

			Model aLexicalizationSetStats = limeProfiler.profileLexicalizationSet(repoConn, referenceDataset,
					datasetStatistics, lexicalizationModel);

			actualStatistics.addAll(datasetProfile);
			actualStatistics.addAll(aLexicalizationSetStats);
		}

		System.out.println("-------");
		actualStatistics.setNamespace(LIME.NS);
		actualStatistics.setNamespace(RDFS.NS);
		actualStatistics.setNamespace(XMLSchema.NS);
		Rio.write(actualStatistics, System.out, RDFFormat.TURTLE);
	}

	@After
	public void teardown() {
		if (repo != null) {
			repo.shutDown();
		}
	}
}
