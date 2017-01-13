package it.uniroma2.art.semanticturkey.services.core;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for exporting the data managed by a project .
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Export extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Export.class);

	@STServiceOperation
	@Read
	public void export(PluginSpecification[] filters) throws Exception {
		if (filters.length == 0) {
			// No filter has been specified. Then, just dump the data without creating a working copy
			dumpRepository(getManagedConnection());
		} else {
			// Source repository (i.e. the repository associated with the current project)
			RepositoryConnection sourceRepositoryConnection = getManagedConnection();

			// Creates a working copy of the source repository (in-memory, without inference)
			Repository workingRepository = new SailRepository(new MemoryStore());
			try {
				workingRepository.initialize();
				
				try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
					// Copies the source repository to the working repository
					sourceRepositoryConnection.export(new RDFInserter(workingRepositoryConnection));

					// Applies each filter
					for (PluginSpecification filterSpec : filters) {
						ExportFilter exportFilter = (ExportFilter) filterSpec.instatiatePlugin();
						exportFilter.filter(sourceRepositoryConnection, workingRepositoryConnection,
								getWorkingGraph());
					}

					// Dumps the working repository (i.e. the filtered repository)
					dumpRepository(workingRepositoryConnection);
				}
			} finally {
				workingRepository.shutDown();
			}
		}
	}

	private void dumpRepository(RepositoryConnection filteredRepositoryConnection) {
		// TODO: currently, only dump the data to the console. Actual download must be implemented!!!!
		filteredRepositoryConnection.export(Rio.createWriter(RDFFormat.TURTLE, System.out));
	}
};