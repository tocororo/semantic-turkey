package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerConfig;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerImpl;
import it.uniroma2.art.semanticturkey.ontology.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;

/**
 * A {@link TestRule} for setting up a fresh new Semantic Turkey Data directory.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STEnviroment implements TestRule {
	public static final String DEFAULT_BASE_DIR_NAME = "target/test-base";
	public static final String DEFAULT_STDATA_DIR_NAME = "test-stdata";
	public static final String DEFAULT_STCONFIG_NAME = "stconfig.properties";

	public static final String DEFAULT_CORE_REPO_ID = "core";
	public static final String DEFAULT_SUPPORT_REPO_ID = "support";

	public static final String DEFAULT_BASE_URI = "http://example.org/";
	public static final String DEFAULT_HISTORY_GRAPH = "http://example.org/history";
	public static final String DEFAULT_VALIDATION_GRAPH = "http://example.org/validation";

	private File baseDir;
	private String stDataDirName;
	private String coreRepoID;
	private String supportRepoID;

	private String baseURI;
	private String historyGraph;
	private String validationGraph;

	private boolean requiresValidation;
	private LocalRepositoryManager repositoryManager;
	private Repository coreRepo;
	private Repository supportRepo;
	private String stConfigName;
	private OntologyManagerImpl ontologyManager;

	public STEnviroment() {
		this.baseDir = new File(DEFAULT_BASE_DIR_NAME);
		this.stConfigName = DEFAULT_STCONFIG_NAME;
		this.stDataDirName = DEFAULT_STDATA_DIR_NAME;
		this.coreRepoID = DEFAULT_CORE_REPO_ID;
		this.supportRepoID = DEFAULT_SUPPORT_REPO_ID;
		this.baseURI = DEFAULT_BASE_URI;
		this.historyGraph = DEFAULT_HISTORY_GRAPH;
		this.validationGraph = DEFAULT_VALIDATION_GRAPH;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				tearUp(base, description);
				try {
					base.evaluate();
				} finally {
					tearDown(base, description);
				}
			}
		};

	}

	private void tearUp(Statement base, Description description)
			throws FileNotFoundException, IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		// Ensures that the base directory is a fresh new empty directory
		if (baseDir.exists()) {
			FileUtil.deltree(baseDir);
		}
		baseDir.mkdirs();

		// Creates an empty STData directory
		File testSTDataDir = new File(baseDir, stDataDirName);
		testSTDataDir.mkdirs();

		// Sets up the repository manager
		repositoryManager = new LocalRepositoryManager(testSTDataDir);
		repositoryManager.initialize();

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig(supportRepoID, new SailRepositoryConfig(new MemoryStoreConfig())));

		supportRepo = repositoryManager.getRepository(supportRepoID);
		Repositories.consume(supportRepo, conn -> {
			conn.setNamespace(CHANGELOG.PREFIX, CHANGELOG.NAMESPACE);
			conn.setNamespace(PROV.PREFIX, PROV.NAMESPACE);
		});

		ChangeTrackerConfig trackerConfig = new ChangeTrackerConfig(new MemoryStoreConfig());
		trackerConfig.setSupportRepositoryID(supportRepoID);
		trackerConfig.setMetadataNS(ModelUtilities.createDefaultNamespaceFromBaseURI(historyGraph));
		// trackerConfig.setInteractiveNotifications(true);

		requiresValidation = detectValidatonRequirement(base, description);

		if (requiresValidation) {
			trackerConfig.setValidationEnabled(true);
			trackerConfig.setValidationGraph(SimpleValueFactory.getInstance().createIRI(validationGraph));
		} else {
			trackerConfig.setValidationEnabled(false);
		}

		trackerConfig.setHistoryGraph(SimpleValueFactory.getInstance().createIRI(historyGraph));

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig(coreRepoID, new SailRepositoryConfig(trackerConfig)));
		coreRepo = repositoryManager.getRepository(coreRepoID);

		// Sets up the ontology mirror
		File ontologiesMirrorLocation = new File(testSTDataDir, "ontologiesMirror");
		ontologiesMirrorLocation.mkdirs();
		File ontologiesMirrorFile = new File(testSTDataDir, "OntologiesMirror.properties");
		ontologiesMirrorFile.createNewFile();
		File testStConfig = new File(baseDir, stConfigName);
		try (PrintWriter writer = new PrintWriter(new FileWriter(testStConfig))) {
			writer.println("data.dir=" + testSTDataDir.getPath().replaceAll("\\\\", "/"));
			writer.println(
					"ontologiesMirrorLocation=" + ontologiesMirrorLocation.getPath().replaceAll("\\\\", "/"));
		}
		Config.initialize(testStConfig);

		Method setOntologiesMirrorRegistry = OntologiesMirror.class
				.getDeclaredMethod("setOntologiesMirrorRegistry", File.class);
		setOntologiesMirrorRegistry.setAccessible(true);
		setOntologiesMirrorRegistry.invoke(null, ontologiesMirrorFile);

		ontologyManager = new OntologyManagerImpl(coreRepo, requiresValidation);
		ontologyManager.initializeMappingsPersistence(new NSPrefixMappings(testSTDataDir, true));
		ontologyManager.setBaseURI(baseURI);
	}

	private boolean detectValidatonRequirement(Statement base, Description description) {
		Method testMethod = Arrays.stream(description.getTestClass().getDeclaredMethods())
				.filter(m -> m.getName().equals(description.getMethodName())).findAny()
				.orElseThrow(() -> new IllegalStateException("Could not determine test method"));
		return testMethod.isAnnotationPresent(RequiresValidation.class);
	}

	private void tearDown(Statement base, Description description) {
		if (repositoryManager != null) {
			repositoryManager.shutDown();
		}
	}

	public Repository getCoreRepo() {
		return coreRepo;
	}

	public Repository getSupportRepo() {
		return supportRepo;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public boolean isValidationRequired() {
		return requiresValidation;
	}

	public OntologyManagerImpl getOntologyManager() {
		return ontologyManager;
	}

	public void cacheOntology(InputStream ontoIS, String ontoBaseURI, String cacheName)
			throws IOException, FileNotFoundException {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(cacheName);
		mirFile.getFile().createNewFile();
		try (OutputStream os = new FileOutputStream(mirFile.getFile())) {
			IOUtils.copy(ontoIS, os);
		}
		OntologiesMirror.addCachedOntologyEntry(ontoBaseURI, mirFile);
	}
}
