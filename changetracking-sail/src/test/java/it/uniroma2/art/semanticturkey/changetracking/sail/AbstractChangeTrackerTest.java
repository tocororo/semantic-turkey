package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.springframework.core.annotation.AnnotationUtils;

import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerConfig;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import junit.framework.AssertionFailedError;

/**
 * Abstract base class of tests for {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractChangeTrackerTest {

	protected static final String HISTORY_REPO_ID = "test-history";
	protected static final String HISTORY_NS = "http://example.org/history#";
	protected static final IRI HISTORY_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://example.org/history");
	protected static final IRI VALIDATION_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://example.org/validation");
	protected static final IRI BLACKLIST_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://example.org/blacklist");

	protected LocalRepositoryManager repositoryManager;

	protected Repository coreRepo;
	protected Repository supportRepo;

	protected static final Namespace ns = new SimpleNamespace("ex", "http://example.org/");

	protected static final String TEST_REPOSITORY_MANAGER_BASE = "target/test-repositories/";

	protected static final IRI graphA;
	protected static final IRI graphB;
	protected static final IRI socrates;
	protected static final IRI plato;
	protected static final boolean PRINT_HISTORY = true;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		graphA = vf.createIRI(ns.getName(), "graph-A");
		graphB = vf.createIRI(ns.getName(), "graph-B");
		socrates = vf.createIRI(ns.getName(), "socrates");
		plato = vf.createIRI(ns.getName(), "plato");
	}

	protected boolean requiresValidation;
	protected boolean requiresBlacklisting;
	protected boolean requiresHistory;

	@Rule
	public TestWatcher testWatcher = new TestWatcher() {

		protected void starting(org.junit.runner.Description description) {
			// it seems that using JUnitParamsRunner it is no longer possible to obtain annotations from the
			// description parameter
			String methodName = description.getMethodName();
			// strip the parameters from executions of parameterized tests
			int parIndex = methodName.indexOf("(");
			if (parIndex != -1) {
				methodName = methodName.substring(0, parIndex);
			}

			String methodNameCopyForLambda = methodName;
			Method testMethod = Arrays.stream(description.getTestClass().getMethods())
					.filter(m -> m.getName().equals(methodNameCopyForLambda)).findAny()
					.orElseThrow(() -> new AssertionFailedError("Unable to find the test method: "
							+ description.getMethodName() + " in test class: " + description.getTestClass()));

			RequiresValidation validationAnnot = AnnotationUtils.findAnnotation(testMethod,
					RequiresValidation.class);

			if (validationAnnot != null) {
				requiresValidation = true;
				requiresBlacklisting = validationAnnot.blacklisting();
			}

			DoesNotWantHistory omitHistoryAnnot = AnnotationUtils.findAnnotation(testMethod,
					DoesNotWantHistory.class);

			requiresHistory = (omitHistoryAnnot == null);
		};
	};

	@Before
	public void setup() {
		File baseRepoManagerDir = new File(TEST_REPOSITORY_MANAGER_BASE);
		if (baseRepoManagerDir.exists()) {
			FileUtil.deltree(baseRepoManagerDir);
		}
		baseRepoManagerDir.mkdirs();

		repositoryManager = new LocalRepositoryManager(baseRepoManagerDir);
		repositoryManager.init();

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig(HISTORY_REPO_ID, new SailRepositoryConfig(new NativeStoreConfig())));

		supportRepo = repositoryManager.getRepository(HISTORY_REPO_ID);
		Repositories.consume(supportRepo, conn -> {
			conn.setNamespace(CHANGELOG.PREFIX, CHANGELOG.NAMESPACE);
			conn.setNamespace(PROV.PREFIX, PROV.NAMESPACE);
		});
		ChangeTrackerConfig trackerConfig = new ChangeTrackerConfig(new NativeStoreConfig());
		trackerConfig.setSupportRepositoryID(HISTORY_REPO_ID);
		trackerConfig.setMetadataNS(HISTORY_NS);
		// trackerConfig.setInteractiveNotifications(true);

		if (requiresValidation) {
			trackerConfig.setValidationEnabled(true);
			trackerConfig.setValidationGraph(VALIDATION_GRAPH);
			if (requiresBlacklisting) {
				trackerConfig.setBlacklistingEnabled(true);
				trackerConfig.setBlacklistGraph(BLACKLIST_GRAPH);
			}
		} else {
			trackerConfig.setValidationEnabled(false);
		}

		if (requiresHistory) {
			trackerConfig.setHistoryEnabled(true);
			trackerConfig.setHistoryGraph(HISTORY_GRAPH);
		} else {
			trackerConfig.setHistoryEnabled(false);
		}

		repositoryManager.addRepositoryConfig(
				new RepositoryConfig("test-data", new SailRepositoryConfig(trackerConfig)));
		coreRepo = repositoryManager.getRepository("test-data");

	}

	@After
	public void teardown() {
		repositoryManager.shutDown();
	}

	protected void printRepositories() throws RepositoryException, UnknownTransactionStateException {
		System.out.println();
		System.out.println("--- Data repo ---");
		System.out.println();

		Repositories.consume(coreRepo, conn -> {
			conn.export(Rio.createWriter(RDFFormat.NQUADS, System.out));
		});

		System.out.println();
		System.out.println("--- Support repo ---");
		System.out.println();

		Repositories.consume(supportRepo, conn -> {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TRIG, System.out);
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			conn.export(rdfWriter);
		});

		System.out.println();
	}

}
