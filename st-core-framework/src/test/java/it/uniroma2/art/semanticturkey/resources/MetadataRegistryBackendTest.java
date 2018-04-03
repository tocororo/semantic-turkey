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

import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
}
