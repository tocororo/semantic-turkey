package it.uniroma2.art.semanticturkey.mdr.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.DCAT3FRAGMENT;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.STMETADATAREGISTRY;
import org.apache.commons.io.FileUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.google.common.base.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFrameworkRule;
import it.uniroma2.art.semanticturkey.mdr.core.impl.MetadataRegistryBackendImpl;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY;

/**
 * A test suite for {@link MetadataRegistryBackend}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class MetadataRegistryBackendTest {

	protected final static String TEST_REGISTRY_BASE = "target/testbase/mdreg";

	private static final IRI SKOSXL_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2008/05/skos-xl");

	protected MetadataRegistryBackendImpl metadataRegistryBackend;
	protected ValueFactory vf;

	public final OsgiContext osgiContext = new OsgiContext();

	public final MediationFrameworkRule mediationFrameworkRule = new MediationFrameworkRule(osgiContext);

	@Rule
	public final RuleChain ruleChain = RuleChain.outerRule(osgiContext).around(mediationFrameworkRule);

	@Before
	public void setup() throws Exception {

		File baseDir = new File(TEST_REGISTRY_BASE);
		baseDir.mkdirs();
		FileUtils.cleanDirectory(baseDir);

		metadataRegistryBackend = new MetadataRegistryBackendImpl(baseDir,
				mediationFrameworkRule.getObject());
		metadataRegistryBackend.initialize(); // invoke @PostConstruct

		vf = SimpleValueFactory.getInstance();
	}

	@After
	public void teardown() {
		metadataRegistryBackend.destroy(); // invoke @PreDestroy
	}

	@Test
	public void testCreateDataset() throws MetadataRegistryWritingException {
		Distribution distribution = new Distribution(
				null,
				METADATAREGISTRY.SPARQL_ENDPOINT,
				Values.iri("https://agrovoc.fao.org/sparql"),
				null);

		metadataRegistryBackend.createConcreteDataset(
				"AGROVOC",
				"http://aims.fao.org/aos/agrovoc#",
				Values.literal("AGROVOC multilingual thesaurus", "en"),
				Values.literal("The AGROVOC thesaurus contains more than 38 000 concepts in 39 languages covering topics related to food, nutrition, agriculture, fisheries, forestry, environment and other related domains", "en"),
				true,
				distribution,
				null
		);
		try (RepositoryConnection con = metadataRegistryBackend.getConnection()) {
			con.export(Rio.createWriter(RDFFormat.TURTLE, System.out));
		}

	}

	@Test
	public void testCreateDatasetsForAGROVOC() throws MetadataRegistryWritingException {
		// AGROVOC Online SPARQL Endpoint
		{
			Distribution distribution = new Distribution(
					null,
					METADATAREGISTRY.SPARQL_ENDPOINT,
					Values.iri("https://agrovoc.fao.org/sparql"),
					null);
			metadataRegistryBackend.createConcreteDataset(
					"AGROVOC_LOD",
					"http://aims.fao.org/aos/agrovoc#",
					Values.literal("AGROVOC multilingual thesaurus", "en"),
					Values.literal("The AGROVOC thesaurus contains more than 38 000 concepts in 39 languages covering topics related to food, nutrition, agriculture, fisheries, forestry, environment and other related domains", "en"),
					true,
					distribution,
					null
			);
		}

		// AGROVOC master development project
		{
			Distribution distribution = new Distribution(
					null,
					STMETADATAREGISTRY.PROJECT,
					null,
					"AGROVOC_core");
			metadataRegistryBackend.createConcreteDataset(
					"AGROVOC_PROJECT",
					"http://aims.fao.org/aos/agrovoc#",
					Values.literal("AGROVOC multilingual thesaurus", "en"),
					Values.literal("The AGROVOC thesaurus contains more than 38 000 concepts in 39 languages covering topics related to food, nutrition, agriculture, fisheries, forestry, environment and other related domains", "en"),
					true,
					distribution,
					null
			);
		}

		try (RepositoryConnection con = metadataRegistryBackend.getConnection()) {
			con.export(Rio.createWriter(RDFFormat.TURTLE, System.out).set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true).set(BasicWriterSettings.BASE_DIRECTIVE, true));
		}

		Collection<CatalogRecord2> rootDatasets = metadataRegistryBackend.listRootDatasets();
		System.out.println("Root datasets");
		System.out.println(rootDatasets);

	}


	@Ignore
	@Test
	public void test() throws IllegalArgumentException, MetadataRegistryWritingException, IOException,
			NoSuchDatasetMetadataException, MetadataRegistryStateException {

		// Adds the Agrovoc DatasetSpecification

		IRI agrovocCatalogRecordIRI = metadataRegistryBackend.addDataset(
				vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"),
				"http://aims.fao.org/aos/agrovoc/", "Agrovoc", true,
				vf.createIRI("http://agrovoc.fao.org/sparql"));

		// Tests that the dataset is available via the catalog record

		Collection<CatalogRecord> records = metadataRegistryBackend.getCatalogRecords();

		assertThat(records, hasSize(1));

		CatalogRecord agrovocCatalogRecord = records.iterator().next();

		assertThat(agrovocCatalogRecord.getIdentity(), equalTo(agrovocCatalogRecordIRI));
		assertThat(agrovocCatalogRecord.getIssued(), is(notNullValue()));
		assertThat(agrovocCatalogRecord.getModified(), is(nullValue()));

		DatasetMetadata agrovocDataset = agrovocCatalogRecord.getAbstractDataset();

		assertAgrovocDataset(agrovocDataset);

		assertThat(agrovocDataset.getVersionInfo().isPresent(), is(false));

		assertThat(agrovocCatalogRecord.getVersions(), is(empty()));

		// Tests that the dataset is available via direct lookup

		agrovocDataset = metadataRegistryBackend
				.getDatasetMetadata(vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"));

		assertAgrovocDataset(agrovocDataset);

		// Adds a fictional version of the Agrovoc dataset

		metadataRegistryBackend.addDatasetVersion(agrovocCatalogRecordIRI, null, "999.99");

		records = metadataRegistryBackend.getCatalogRecords();

		assertThat(records, hasSize(1));

		agrovocCatalogRecord = records.iterator().next();

		assertThat(agrovocCatalogRecord.getModified(), is(notNullValue()));

		agrovocDataset = agrovocCatalogRecord.getAbstractDataset();
		List<DatasetMetadata> agrovocVersions = agrovocCatalogRecord.getVersions();

		assertThat(agrovocVersions, hasSize(1));

		DatasetMetadata agrovoc999_99 = agrovocVersions.iterator().next();

		assertThat(agrovoc999_99.getVersionInfo().orElseThrow(() -> new AssertionError("Empty optional")),
				is(equalTo("999.99")));

		// Adds an embedded lexicalization set for Agrovoc

		metadataRegistryBackend.addEmbeddedLexicalizationSet(agrovocDataset.getIdentity(), null, null,
				SKOSXL_LEXICALIZATION_MODEL, "en", BigInteger.valueOf(1000), BigInteger.valueOf(2000),
				BigInteger.valueOf(2000), BigDecimal.valueOf(1), BigDecimal.valueOf(2));

		Collection<LexicalizationSetMetadata> agrovocEmbeddedLexicalizationSets = metadataRegistryBackend
				.getEmbeddedLexicalizationSets(agrovocDataset.getIdentity());

		assertThat(agrovocEmbeddedLexicalizationSets, hasSize(1));

		LexicalizationSetMetadata agrovocLexicalizatioSet = agrovocEmbeddedLexicalizationSets.iterator()
				.next();

		assertThat(agrovocLexicalizatioSet.getReferenceDataset(), equalTo(agrovocDataset.getIdentity()));
		assertFalse(agrovocLexicalizatioSet.getLexiconDataset().isPresent());
		assertThat(agrovocLexicalizatioSet.getLexicalizationModel(), equalTo(SKOSXL_LEXICALIZATION_MODEL));
		assertThat(agrovocLexicalizatioSet.getLanguage(), equalTo("en"));

		assertThat(agrovocLexicalizatioSet.getReferences().get(), equalTo(BigInteger.valueOf(1000)));
		assertThat(agrovocLexicalizatioSet.getReferences().get(), equalTo(BigInteger.valueOf(1000)));
		assertThat(agrovocLexicalizatioSet.getLexicalEntries().get(), equalTo(BigInteger.valueOf(2000)));
		assertThat(agrovocLexicalizatioSet.getLexicalizations().get(), equalTo(BigInteger.valueOf(2000)));
		assertThat(agrovocLexicalizatioSet.getPercentage().get(), equalTo(BigDecimal.valueOf(1)));
		assertThat(agrovocLexicalizatioSet.getAvgNumOfLexicalizations().get(),
				equalTo(BigDecimal.valueOf(2)));

	}

	@Test
	public void testDiscoverAgrovocFromResource() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://aims.fao.org/aos/agrovoc/c_12332"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		Collection<LexicalizationSetMetadata> agrovocLexicalizationSets = metadataRegistryBackend
				.getEmbeddedLexicalizationSets(agrovocDataset.getIdentity());
		assertAgrovocDataset(agrovocDataset);
		assertAgrovocLexicalizationSets(agrovocLexicalizationSets);
	}

	@Test
	public void testDiscoverAgrovocFromVoID() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(SimpleValueFactory.getInstance()
				.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		Collection<LexicalizationSetMetadata> agrovocLexicalizationSets = metadataRegistryBackend
				.getEmbeddedLexicalizationSets(agrovocDataset.getIdentity());

		assertAgrovocDataset(agrovocDataset);
		assertAgrovocLexicalizationSets(agrovocLexicalizationSets);
	}

	public static void assertAgrovocDataset(DatasetMetadata agrovocDataset) throws AssertionError {
		assertThat(agrovocDataset.getIdentity(), equalTo(SimpleValueFactory.getInstance()
				.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc")));
		assertThat(agrovocDataset.getUriSpace().orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo("http://aims.fao.org/aos/agrovoc/"));
		assertThat(agrovocDataset.getTitle().orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo("Agrovoc"));
		assertThat(
				agrovocDataset.getDereferenciationSystem()
						.orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo(METADATAREGISTRY.STANDARD_DEREFERENCIATION));
		assertThat(
				agrovocDataset.getSparqlEndpointMetadata()
						.orElseThrow(() -> new AssertionError("Empty optional")).getEndpoint(),
				equalTo(SimpleValueFactory.getInstance().createIRI("http://agrovoc.fao.org/sparql")));
	}

	private void assertAgrovocLexicalizationSets(
			Collection<LexicalizationSetMetadata> agrovocLexicalizationSets) {
		assertThat(agrovocLexicalizationSets, hasSize(Matchers.greaterThan(20)));
		LexicalizationSetMetadata lexicalizationSet = agrovocLexicalizationSets.iterator().next();

		assertThat(lexicalizationSet.getLexicalizationModel(), equalTo(SKOSXL_LEXICALIZATION_MODEL));
	}

	@Test
	public void testDiscoverFOAFFromResource() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1/Person"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertFOAFDataset(agrovocDataset);
	}

	@Test
	public void testDiscoverFOAFFromURISpace() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend
				.discoverDataset(SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1/"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertFOAFDataset(agrovocDataset);
	}

	@Test
	public void testDiscoverFOAFFromBaseURIWithoutEndingSlash() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend
				.discoverDataset(SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertFOAFDataset(agrovocDataset);

	}

	public static void assertFOAFDataset(DatasetMetadata foafDataset) throws AssertionError {
		assertThat(foafDataset.getIdentity(),
				equalTo(SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1/")));
		assertThat(foafDataset.getUriSpace().orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo("http://xmlns.com/foaf/0.1/"));
		assertThat(foafDataset.getTitle().orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo("Friend of a Friend (FOAF) vocabulary"));
		assertThat(
				foafDataset.getDereferenciationSystem()
						.orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo(METADATAREGISTRY.STANDARD_DEREFERENCIATION));
		assertFalse(foafDataset.getSparqlEndpoint().isPresent());
	}

	@Test
	public void testAssessmentOfDBPediaLexicalizationModel()
			throws IllegalArgumentException, MetadataRegistryWritingException, AssessmentException {
		IRI dbpediaDataset = vf.createIRI("http://dbpedia.org/void/Dataset");
		metadataRegistryBackend.addDataset(dbpediaDataset, "http://dbpedia.org/resource/", "DBpedia", true,
				vf.createIRI("http://dbpedia.org/sparql"));

		metadataRegistryBackend.discoverLexicalizationSets(dbpediaDataset);

		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			conn.export(
					RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).orElse(null).getWriter(System.out));
		}

	}

	@Test
	public void testAssessmentOfAgrovocLexicalizationModel()
			throws IllegalArgumentException, MetadataRegistryWritingException, AssessmentException {
		IRI agrovocDataset = vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc");
		metadataRegistryBackend.addDataset(agrovocDataset, "http://aims.fao.org/aos/agrovoc/", "Agrovoc",
				true, vf.createIRI("http://agrovoc.fao.org/sparql"));

		metadataRegistryBackend.discoverLexicalizationSets(agrovocDataset);

		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			conn.export(
					RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).orElse(null).getWriter(System.out));
		}

	}

	@Test
	public void testEmbeddedLinksets() throws IllegalArgumentException, MetadataRegistryWritingException {

		/*
		 * @formatter:off
		 * 
		 * In this test case:
		 * - AGROVOC contains linksets to EuroVoc, using a purpose minted IRI for the dataset as void:objectsTarget
		 * - EuroVoc is registered in the metadata registry with its own IRI
		 * 
		 * The goals are:
		 * - have the target of the linksets being unified to the dataset in the metadata registry.
		 * - merge the two datasets to the same dataset (summing the triples count)
		 * 
		 * @formatter:on
		 */
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI agrovocDataset = vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc");
		metadataRegistryBackend.addDataset(agrovocDataset, "http://aims.fao.org/aos/agrovoc/",
				"Agrovoc DatasetSpecification", true, vf.createIRI("http://agrovoc.fao.org/sparql"));

		IRI eurovocDataset = vf.createIRI("http://eurovoc.europa.eu/void.ttl#EuroVoc");
		metadataRegistryBackend.addDataset(eurovocDataset, "http://eurovoc.europa.eu/", "EuroVoc DatasetSpecification",
				true, null);

		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			Update update = conn.prepareUpdate(
			//@formatter:off
				"PREFIX void: <http://rdfs.org/ns/void#>\n" +
				"INSERT DATA {\n" +
				"  <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc>\n" +
				"    void:subset <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc-EuroVocLinkset1> ;\n" +
				"    void:subset <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc-EuroVocLinkset2> ;\n" +
				"  .\n" +
				"  <http://aims.fao.org/aos/agrovoc/void.ttl#EuroVoc>\n" +
				"    a void:Dataset ;\n" +
				"    void:uriSpace \"http://eurovoc.europa.eu/\";\n" +
				"  .\n" +

				"  <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc-EuroVocLinkset1>" +
				"    a void:Linkset;\n" + 
				"    void:linkPredicate <http://www.w3.org/2004/02/skos/core#exactMatch>;\n" + 
				"    void:objectsTarget <http://aims.fao.org/aos/agrovoc/void.ttl#EuroVoc>;\n" + 
				"    void:subjectsTarget <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc>;\n" + 
				"    void:triples 1000;\n" +
				"  .\n" +
				"  <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc-EuroVocLinkset2>" +
				"    a void:Linkset;\n" + 
				"    void:linkPredicate <http://www.w3.org/2004/02/skos/core#closeMatch>;\n" + 
				"    void:objectsTarget <http://aims.fao.org/aos/agrovoc/void.ttl#EuroVoc>;\n" + 
				"    void:subjectsTarget <http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc>;\n" + 
				"    void:triples 500;\n" +
				"  .\n" +
				"}\n"
				//@formatter:on
			);
			update.execute();
		}

		Collection<LinksetMetadata> linksetsMetadata = metadataRegistryBackend
				.getEmbeddedLinksets(agrovocDataset, 0, true);
		linksetsMetadata.forEach(System.out::println);

	}

}
