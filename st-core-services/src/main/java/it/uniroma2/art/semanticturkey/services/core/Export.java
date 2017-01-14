package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.export.FilteringStep;

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
	public Collection<AnnotatedValue<org.eclipse.rdf4j.model.Resource>> getGraphNames() throws Exception {
		return Iterations.stream(getManagedConnection().getContextIDs())
				.map(AnnotatedValue<org.eclipse.rdf4j.model.Resource>::new).collect(toList());
	}

	@STServiceOperation
	@Read
	public Collection<RDFFormat> getOutputFormats() throws Exception {
		return RDFWriterRegistry.getInstance().getKeys();
	}

	@STServiceOperation
	@Read
	public void export(HttpServletResponse oRes,
			@Optional(defaultValue = "") org.eclipse.rdf4j.model.IRI[] graphs, FilteringStep[] filteringSteps,
			@Optional(defaultValue = "TRIG") RDFFormat outputFormat) throws Exception {
		if (filteringSteps.length == 0) {
			// No filter has been specified. Then, just dump the data without creating a working copy
			dumpRepository(oRes, getManagedConnection(), graphs, outputFormat);
		} else {
			// Translates numeric graph references to graph names
			IRI[][] step2graphs = computeGraphsForStep(graphs, filteringSteps);

			// Source repository (i.e. the repository associated with the current project)
			RepositoryConnection sourceRepositoryConnection = getManagedConnection();

			// Creates a working copy of the source repository (in-memory, without inference)
			Repository workingRepository = new SailRepository(new MemoryStore());
			try {
				workingRepository.initialize();

				try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
					// Copies the relevant graphs from the source repository to the working repository
					sourceRepositoryConnection.export(new RDFInserter(workingRepositoryConnection), graphs);

					// Applies each filter
					for (int i = 0; i < filteringSteps.length; i++) {
						FilteringStep filteringStep = filteringSteps[i];
						PluginSpecification filterSpec = filteringStep.getFilter();
						ExportFilter exportFilter = (ExportFilter) filterSpec.instatiatePlugin();
						exportFilter.filter(sourceRepositoryConnection, workingRepositoryConnection,
								step2graphs[i]);
					}

					// Dumps the working repository (i.e. the filtered repository)
					dumpRepository(oRes, workingRepositoryConnection, graphs, outputFormat);
				}
			} finally {
				workingRepository.shutDown();
			}
		}
	}

	private IRI[][] computeGraphsForStep(IRI[] graphs, FilteringStep[] filteringSteps)
			throws IllegalArgumentException {
		IRI[][] step2graphs = new IRI[filteringSteps.length][];
		for (int i = 0; i < filteringSteps.length; i++) {
			int[] graphRefs = filteringSteps[i].getGraphs();

			step2graphs[i] = new IRI[graphRefs.length];

			for (int j = 0; j < graphRefs.length; j++) {
				int ref = graphRefs[j];

				if (ref < 0 || ref >= graphs.length) {
					throw new IllegalArgumentException(
							"Graph reference " + ref + " in filtering step " + i + " is not valid");
				}

				step2graphs[i][j] = graphs[ref];
			}
		}

		return step2graphs;
	}

	private void dumpRepository(HttpServletResponse oRes, RepositoryConnection filteredRepositoryConnection,
			Resource[] graphs, RDFFormat outputFormat) throws IOException {
		File tempServerFile = File.createTempFile("save", "." + outputFormat.getDefaultFileExtension());
		try {
			try (OutputStream tempServerFileStream = new FileOutputStream(tempServerFile)) {
				filteredRepositoryConnection.export(Rio.createWriter(outputFormat, tempServerFileStream),
						graphs);
			}

			oRes.setHeader("Content-Disposition",
					"attachment; filename=save." + outputFormat.getDefaultFileExtension());
			oRes.setContentType(outputFormat.getDefaultMIMEType());
			oRes.setContentLength((int) tempServerFile.length());
			
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}
};