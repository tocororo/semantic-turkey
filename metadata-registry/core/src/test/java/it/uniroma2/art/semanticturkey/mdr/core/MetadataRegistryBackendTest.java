package it.uniroma2.art.semanticturkey.mdr.core;

import java.io.File;
import java.util.Collection;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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
				null,
				false);
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
					null,
					false);
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
					null,
					false);
		}

		try (RepositoryConnection con = metadataRegistryBackend.getConnection()) {
			con.export(Rio.createWriter(RDFFormat.TURTLE, System.out).set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true).set(BasicWriterSettings.BASE_DIRECTIVE, true));
		}

		Collection<CatalogRecord2> rootDatasets = metadataRegistryBackend.listRootDatasets();
		System.out.println("Root datasets");
		System.out.println(rootDatasets);

	}

	@Test
	@Ignore("ignored since it requires access to external resources that could be not available")
	public void testDiscoverAgrovocFromResource() throws MetadataDiscoveryException, NoSuchDatasetMetadataException, MetadataRegistryStateException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://aims.fao.org/aos/agrovoc/c_12332"));

		CatalogRecord2 catalogRecord = metadataRegistryBackend.getCatalogRecordMetadata(catalogRecordIRI);
		DatasetMetadata agrovocDataset = metadataRegistryBackend.getDatasetMetadata(catalogRecord.getDataset().getIdentity());
		Collection<LexicalizationSetMetadata> agrovocLexicalizationSets = metadataRegistryBackend
				.getEmbeddedLexicalizationSets(agrovocDataset.getIdentity());

		assertAgrovocDataset(agrovocDataset);
		assertAgrovocLexicalizationSets(agrovocLexicalizationSets);
	}

	@Test
	@Ignore("ignored since it requires access to external resources that could be not available")
	public void testDiscoverAgrovocFromVoID() throws MetadataDiscoveryException, NoSuchDatasetMetadataException, MetadataRegistryStateException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(SimpleValueFactory.getInstance()
				.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"));

		CatalogRecord2 catalogRecord = metadataRegistryBackend.getCatalogRecordMetadata(catalogRecordIRI);
		DatasetMetadata agrovocDataset = metadataRegistryBackend.getDatasetMetadata(catalogRecord.getDataset().getIdentity());
		Collection<LexicalizationSetMetadata> agrovocLexicalizationSets = metadataRegistryBackend
				.getEmbeddedLexicalizationSets(agrovocDataset.getIdentity());

		assertAgrovocDataset(agrovocDataset);
		assertAgrovocLexicalizationSets(agrovocLexicalizationSets);
	}

	public static void assertAgrovocDataset(DatasetMetadata agrovocDataset) throws AssertionError {
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
	@Ignore("ignored since it requires access to external resources that could be not available")
	public void testDiscoverFOAFFromResource() throws MetadataDiscoveryException, NoSuchDatasetMetadataException, MetadataRegistryStateException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1/Person"));

		CatalogRecord2 catalogRecord = metadataRegistryBackend.getCatalogRecordMetadata(catalogRecordIRI);
		DatasetMetadata foafDataset = metadataRegistryBackend.getDatasetMetadata(catalogRecord.getDataset().getIdentity());
		assertFOAFDataset(foafDataset);
	}

	@Test
	@Ignore("ignored since it requires access to external resources that could be not available")
	public void testDiscoverFOAFFromURISpace() throws MetadataDiscoveryException, NoSuchDatasetMetadataException, MetadataRegistryStateException {
		IRI catalogRecordIRI = metadataRegistryBackend
				.discoverDataset(SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1/"));

		CatalogRecord2 catalogRecord = metadataRegistryBackend.getCatalogRecordMetadata(catalogRecordIRI);
		DatasetMetadata foafDataset = metadataRegistryBackend.getDatasetMetadata(catalogRecord.getDataset().getIdentity());
		assertFOAFDataset(foafDataset);
	}

	@Test
	@Ignore("ignored since it requires access to external resources that could be not available")
	public void testDiscoverFOAFFromBaseURIWithoutEndingSlash() throws MetadataDiscoveryException, NoSuchDatasetMetadataException, MetadataRegistryStateException {
		IRI catalogRecordIRI = metadataRegistryBackend
				.discoverDataset(SimpleValueFactory.getInstance().createIRI("http://xmlns.com/foaf/0.1"));

		CatalogRecord2 catalogRecord = metadataRegistryBackend.getCatalogRecordMetadata(catalogRecordIRI);
		DatasetMetadata foafDataset = metadataRegistryBackend.getDatasetMetadata(catalogRecord.getDataset().getIdentity());
		assertFOAFDataset(foafDataset);
	}

	public static void assertFOAFDataset(DatasetMetadata foafDataset) throws AssertionError {
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
		Distribution distribution = new Distribution(
				null,
				METADATAREGISTRY.SPARQL_ENDPOINT,
				Values.iri("http://dbpedia.org/sparql"),
				null);
		IRI dbpediaDataset = metadataRegistryBackend.createConcreteDataset(
				"DBPedia_LOD",
				"http://dbpedia.org/resource/",
				Values.literal("DBpedia", "en"),
				Values.literal("DBpedia", "en"),
				true,
				distribution,
				null,
				false);

		metadataRegistryBackend.discoverLexicalizationSets(dbpediaDataset);

		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			conn.export(
					RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).orElse(null).getWriter(System.out));
		}

	}

	@Test
	public void testAssessmentOfAgrovocLexicalizationModel()
			throws IllegalArgumentException, MetadataRegistryWritingException, AssessmentException {
		Distribution distribution = new Distribution(
				null,
				METADATAREGISTRY.SPARQL_ENDPOINT,
				Values.iri("http://agrovoc.fao.org/sparql"),
				null);
		IRI dbpediaDataset = metadataRegistryBackend.createConcreteDataset(
				"AGROVOC_LOD",
				"http://aims.fao.org/aos/agrovoc/",
				Values.literal("Agrovoc", "en"),
				Values.literal("Agrovoc", "en"),
				true,
				distribution,
				null,
				false);

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
		Distribution agrovocDistribution = new Distribution(Values.iri("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"), METADATAREGISTRY.SPARQL_ENDPOINT, Values.iri("http://agrovoc.fao.org/sparql"), null);
		IRI agrovocDataset = metadataRegistryBackend.createConcreteDataset("agrovoc", "http://aims.fao.org/aos/agrovoc/",
				Values.literal("Agrovoc DatasetSpecification"), null, true, agrovocDistribution, null, false);


		Distribution eurovocDistribution = new Distribution(Values.iri("http://eurovoc.europa.eu/void.ttl#EuroVoc"), METADATAREGISTRY.SPARQL_ENDPOINT, null, null);
		metadataRegistryBackend.createConcreteDataset("eurovoc", "http://eurovoc.europa.eu/",
				Values.literal("EuroVoc DatasetSpecification"), null, true, eurovocDistribution, null, false);

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
