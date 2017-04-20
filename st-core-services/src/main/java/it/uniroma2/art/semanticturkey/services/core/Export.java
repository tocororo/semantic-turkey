package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.QueryResults;
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
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.export.FilteringPipeline;
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
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public Collection<AnnotatedValue<org.eclipse.rdf4j.model.Resource>> getNamedGraphs() throws Exception {
		return Iterations.stream(getManagedConnection().getContextIDs())
				.map(AnnotatedValue<org.eclipse.rdf4j.model.Resource>::new).collect(toList());
	}

	@STServiceOperation
	@Read
	public Collection<RDFFormat> getOutputFormats() throws Exception {
		return RDFWriterRegistry.getInstance().getKeys();
	}

	/**
	 * Exports the content of the currently used project.
	 * 
	 * @param oRes
	 * @param graphs
	 *            the graphs to be exported. An empty array means all graphs the name of which is an IRI
	 * @param filteringSteps
	 *            a JSON string representing an array of {@link FilteringStep}. Each filter is applied to a
	 *            subset of the exported graphs. No graph means every exported graph
	 * @param outputFormat
	 *            the output format. If it does not support graphs, the exported graph are merged into a
	 *            single graph
	 * @param force
	 *            <code>true</code> tells the service to proceed despite the presence of triples in the null
	 *            context or in graphs named by blank nodes. Otherwise, under this conditions the service
	 *            would fail, so that available information is not silently ignored
	 * @throws Exception
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public void export(HttpServletResponse oRes, @Optional(defaultValue = "") IRI[] graphs,
			@Optional(defaultValue = "[]") FilteringPipeline filteringPipeline,
			@Optional(defaultValue = "TRIG") RDFFormat outputFormat,
			@Optional(defaultValue = "false") boolean force) throws Exception {

		Set<Resource> sourceGraphs = QueryResults.asSet(getManagedConnection().getContextIDs());

		if (!force) {
			if (getManagedConnection().size((Resource) null) != 0) {
				throw new IllegalArgumentException(
						"The null graph contains triples that will not be exported. You can force the export, to ignore this issue.");
			}

			if (sourceGraphs.stream().anyMatch(BNode.class::isInstance)) {
				throw new IllegalArgumentException(
						"Some graphs that are associated with bnodes will not be exported. You can force the export, to ignore this issue.");
			}
		}

		// if no graph is provided, then export of graphs the name of which is a URI
		if (graphs.length == 0) {
			graphs = sourceGraphs.stream().filter(IRI.class::isInstance).map(IRI.class::cast)
					.toArray(IRI[]::new);
		}

		FilteringStep[] filteringSteps = filteringPipeline.getSteps();

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
					// Copies all graphs from the source repository to the working repository
					sourceRepositoryConnection.export(new RDFInserter(workingRepositoryConnection));

					// Applies each filter
					for (int i = 0; i < filteringSteps.length; i++) {
						IRI[] stepGraphs = step2graphs[i];
						FilteringStep filteringStep = filteringSteps[i];
						PluginSpecification filterSpec = filteringStep.getFilter();
						ExportFilter exportFilter = (ExportFilter) filterSpec.instatiatePlugin();
						exportFilter.filter(sourceRepositoryConnection, workingRepositoryConnection,
								stepGraphs);
					}
					// Dumps the working repository (i.e. the filtered repository)
					dumpRepository(oRes, workingRepositoryConnection, graphs, outputFormat);
				}
			} finally {
				workingRepository.shutDown();
			}
		}
	}

	/**
	 * Returns the graph upon which a filter should operate.
	 * 
	 * @param graphs
	 * @param filteringSteps
	 * @return
	 * @throws IllegalArgumentException
	 */
	private IRI[][] computeGraphsForStep(IRI[] graphs, FilteringStep[] filteringSteps)
			throws IllegalArgumentException {
		Set<IRI> exportedGraphs = new HashSet<>();
		exportedGraphs.addAll(Arrays.asList(graphs));

		IRI[][] step2graphs = new IRI[filteringSteps.length][];
		for (int i = 0; i < filteringSteps.length; i++) {
			IRI[] stepGraphs = filteringSteps[i].getGraphs();

			if (stepGraphs == null || stepGraphs.length == 0) {
				step2graphs[i] = new IRI[graphs.length];
				System.arraycopy(graphs, 0, step2graphs[i], 0, graphs.length);
			} else {
				step2graphs[i] = new IRI[stepGraphs.length];

				for (int j = 0; j < stepGraphs.length; j++) {
					IRI g = stepGraphs[j];

					if (!exportedGraphs.contains(g)) {
						throw new IllegalArgumentException(
								"Filtered graph " + g + " was not exported");
					}

					step2graphs[i][j] = g;
				}
			}
		}

		return step2graphs;
	}

	/**
	 * Dumps the provided graphs to the servlet response. It is assumed that the array of graphs has already
	 * been expanded, so an empty array is interpreted as a <i>no graph</i>.
	 * 
	 * @param oRes
	 * @param filteredRepositoryConnection
	 * @param expandedGraphs
	 * @param outputFormat
	 * @throws IOException
	 */
	private void dumpRepository(HttpServletResponse oRes, RepositoryConnection filteredRepositoryConnection,
			Resource[] expandedGraphs, RDFFormat outputFormat) throws IOException {
		File tempServerFile = File.createTempFile("save", "." + outputFormat.getDefaultFileExtension());
		try {
			if (expandedGraphs.length != 0) {
				try (OutputStream tempServerFileStream = new FileOutputStream(tempServerFile)) {
					filteredRepositoryConnection.export(Rio.createWriter(outputFormat, tempServerFileStream),
							expandedGraphs);
				}
			} else {
				Rio.write(new LinkedHashModel(), new FileOutputStream(tempServerFile), outputFormat);
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