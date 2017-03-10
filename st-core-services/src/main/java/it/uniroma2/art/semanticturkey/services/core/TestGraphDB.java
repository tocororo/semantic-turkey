package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import it.uniroma2.art.semanticturkey.changetracking.sail.RepositoryRegistry;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class TestGraphDB extends STServiceAdapter {
	private static Logger logger = LoggerFactory.getLogger(TestGraphDB.class);

	public static final String CORE_CONFIG_FILE = "graphdb-configWithValidation.ttl";
	public static final IRI CTX = SimpleValueFactory.getInstance().createIRI("http://example.org/ns/ctx#");

	@STServiceOperation
	public void test() throws RDFParseException, RDFHandlerException, IOException {
		File tempDir = Files.createTempDir();
		
		RepositoryManager repositoryManager = new LocalRepositoryManager(tempDir);
		repositoryManager.initialize();
		try {
			repositoryManager.addRepositoryConfig(new RepositoryConfig("support-repo",
					new SailRepositoryConfig(new MemoryStoreConfig(true, 500L))));
			Repository supportRepository = repositoryManager.getRepository("support-repo");
			RepositoryRegistry.getInstance().addRepository("support-repo", supportRepository);
			// Instantiate a repository graph model
			TreeModel graph = new TreeModel();

			// Read repository configuration file
			InputStream config = TestGraphDB.class.getResourceAsStream(CORE_CONFIG_FILE);
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			rdfParser.setRDFHandler(new StatementCollector(graph));
			rdfParser.parse(config, RepositoryConfigSchema.NAMESPACE);
			config.close();

			// Retrieve the repository node as a resource
			Resource repositoryNode = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
					RepositoryConfigSchema.REPOSITORY);

			// Create a repository configuration object and add it to the repositoryManager
			RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
			repositoryManager.addRepositoryConfig(repositoryConfig);

			// Get the repository from repository manager, note the repository id set in configuration .ttl
			// file
			Repository repository = repositoryManager.getRepository("graphdb-repo");

			// Open a connection to this repository
			try (RepositoryConnection conn = repository.getConnection()) {
				conn.begin();
				conn.add(conn.getValueFactory().createIRI("http://example.org/Person"), RDF.TYPE, OWL.CLASS,
						CTX);
//				conn.remove(conn.getValueFactory().createIRI("http://example.org/Person"), null, null, CTX);
				conn.commit();
				conn.export(Rio.createWriter(RDFFormat.NTRIPLES, System.out));
			}

			System.out.println("----- CHANGE TRACKING -----");
			try (RepositoryConnection conn = supportRepository.getConnection()) {
				conn.export(Rio.createWriter(RDFFormat.NTRIPLES, System.out));
			}
		} finally {
			repositoryManager.shutDown();
		}

	}
}