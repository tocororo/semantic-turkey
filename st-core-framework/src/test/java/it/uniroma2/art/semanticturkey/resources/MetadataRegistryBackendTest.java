package it.uniroma2.art.semanticturkey.resources;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import it.uniroma2.art.semanticturkey.vocabulary.METADATAREGISTRY;

/**
 * A test suite for {@link MetadataRegistryBackend}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class MetadataRegistryBackendTest {

	protected final static String TEST_REGISTRY_BASE = "target/testbase/mdreg";

	protected MetadataRegistryBackend metadataRegistryBackend;
	protected ValueFactory vf;

	@Before
	public void setup()
			throws IOException, MetadataRegistryCreationException, MetadataRegistryIntializationException {
		File baseDir = new File(TEST_REGISTRY_BASE);
		baseDir.mkdirs();
		FileUtils.cleanDirectory(baseDir);

		metadataRegistryBackend = new MetadataRegistryBackend(baseDir);
		metadataRegistryBackend.initialize(); // invoke @PostConstruct

		vf = SimpleValueFactory.getInstance();
	}

	@After
	public void teardown() {
		metadataRegistryBackend.destroy(); // invoke @PreDestroy
	}

	@Test
	public void test() throws IllegalArgumentException, MetadataRegistryWritingException, IOException {

		// Adds the Agrovoc Dataset

		IRI agrovocCatalogRecordIRI = metadataRegistryBackend.addDataset(
				vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"),
				"http://aims.fao.org/aos/agrovoc/", "Agrovoc", true,
				vf.createIRI("http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc"));

		Collection<CatalogRecord> records = metadataRegistryBackend.getCatalogRecords();

		assertThat(records, hasSize(1));

		CatalogRecord agrovocCatalogRecord = records.iterator().next();

		assertThat(agrovocCatalogRecord.getIdentity(), equalTo(agrovocCatalogRecordIRI));
		assertThat(agrovocCatalogRecord.getIssued(), is(notNullValue()));
		assertThat(agrovocCatalogRecord.getModified(), is(nullValue()));

		DatasetMetadata agrovocDataset = agrovocCatalogRecord.getAbstractDataset();

		assertThat(agrovocDataset.getIdentity(),
				is(equalTo(vf.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"))));
		assertThat(agrovocDataset.getTitle().orElseThrow(() -> new AssertionError("Empty optional")),
				is(equalTo("Agrovoc")));
		assertThat(agrovocDataset.getUriSpace().orElseThrow(() -> new AssertionError("Empty optional")),
				is(equalTo("http://aims.fao.org/aos/agrovoc/")));
		assertThat(agrovocDataset.getSparqlEndpoint().orElseThrow(() -> new AssertionError("Empty optional")),
				is(equalTo(vf.createIRI("http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc"))));
		assertThat(agrovocDataset.getVersionInfo().isPresent(), is(false));

		assertThat(agrovocCatalogRecord.getVersions(), is(empty()));

		// Add a fictional version of the Agrovoc dataset

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
	}

	@Test
	public void testDiscoverAgrovocFromResource() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://aims.fao.org/aos/agrovoc/c_12332"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertAgrovocDataset(agrovocDataset);
	}

	@Test
	public void testDiscoverAgrovocFromBaseURIWithoutEndingSlash() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://aims.fao.org/aos/agrovoc"));

		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertAgrovocDataset(agrovocDataset);
	}

	@Test
	public void testDiscoverAgrovocFromURISpace() throws MetadataDiscoveryException {
		IRI catalogRecordIRI = metadataRegistryBackend.discoverDataset(
				SimpleValueFactory.getInstance().createIRI("http://aims.fao.org/aos/agrovoc/"));
		CatalogRecord catalogRecord = metadataRegistryBackend.getCatalogRecords().stream()
				.filter(record -> Objects.equal(record.getIdentity(), catalogRecordIRI)).findAny()
				.orElseThrow(() -> new AssertionError("Unable to find the newly created catalog record"));

		DatasetMetadata agrovocDataset = catalogRecord.getAbstractDataset();
		assertAgrovocDataset(agrovocDataset);

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
		assertThat(agrovocDataset.getSparqlEndpoint().orElseThrow(() -> new AssertionError("Empty optional")),
				equalTo(SimpleValueFactory.getInstance()
						.createIRI("http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc")));
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

}
