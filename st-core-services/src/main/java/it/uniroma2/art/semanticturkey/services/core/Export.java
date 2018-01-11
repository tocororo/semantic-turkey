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
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.collect.Lists;

import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
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
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public void export(HttpServletResponse oRes, @Optional(defaultValue = "") IRI[] graphs,
			@Optional(defaultValue = "[]") FilteringPipeline filteringPipeline,
			@Optional(defaultValue = "false") boolean includeInferred,
			@Optional(defaultValue = "TRIG") RDFFormat outputFormat,
			@Optional(defaultValue = "false") boolean force) throws Exception {

		exportHelper(oRes, getManagedConnection(), graphs, filteringPipeline, includeInferred, outputFormat,
				force);
	}

	public static void exportHelper(HttpServletResponse oRes, RepositoryConnection sourceRepositoryConnection,
			IRI[] graphs, FilteringPipeline filteringPipeline, boolean includeInferred,
			RDFFormat outputFormat, boolean force) throws IOException, ClassNotFoundException,
			UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			WrongPropertiesException, ExportPreconditionViolationException {
		Set<Resource> sourceGraphs = QueryResults.asSet(sourceRepositoryConnection.getContextIDs());

		if (!force) {
			if (sourceRepositoryConnection.size((Resource) null) != 0) {
				throw new ExportPreconditionViolationException(
						"The null graph contains triples that will not be exported. You can force the export, to ignore this issue.");
			}

			if (sourceGraphs.stream().anyMatch(BNode.class::isInstance)) {
				throw new ExportPreconditionViolationException(
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
			dumpRepository(oRes, sourceRepositoryConnection, graphs, includeInferred, outputFormat);
		} else {
			// Translates numeric graph references to graph names
			IRI[][] step2graphs = computeGraphsForStep(graphs, filteringSteps);

			// Creates a working copy of the source repository (in-memory, without inference)
			Repository workingRepository = new SailRepository(new MemoryStore());
			try {
				workingRepository.initialize();

				try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
					// Copies all graphs from the source repository to the working repository (including the
					// null context, when inference is enabled)
					sourceRepositoryConnection.exportStatements(null, null, null, includeInferred,
							new RDFInserter(workingRepositoryConnection));

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
					dumpRepository(oRes, workingRepositoryConnection, graphs, includeInferred, outputFormat);
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
	private static IRI[][] computeGraphsForStep(IRI[] graphs, FilteringStep[] filteringSteps)
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
						throw new IllegalArgumentException("Filtered graph " + g + " was not exported");
					}

					step2graphs[i][j] = g;
				}
			}
		}

		return step2graphs;
	}

	/**
	 * An object able to report to an {@link RDFHandler}.
	 *
	 */
	@FunctionalInterface
	public interface RDFReporter {
		void export(RDFHandler handler);

		static RDFReporter fromRepositoryConnection(RepositoryConnection conn, Resource subj, IRI pred,
				Value obj, boolean includeInferred, Resource... contexts) {
			return handler -> conn.exportStatements(subj, pred, obj, includeInferred, handler, contexts);
		}

		static RDFReporter fromGraphQueryResult(GraphQueryResult result) {
			return handler -> QueryResults.report(result, handler);
		}

		RDFReporter EmptyReporter = new RDFReporter() {

			@Override
			public void export(RDFHandler handler) {
				handler.startRDF();
				handler.endRDF();
			}
		};

	}

	/**
	 * Dumps the provided graphs to the servlet response. It is assumed that the array of graphs has already
	 * been expanded, so an empty array is interpreted as a <i>no graph</i>. Additionally, if
	 * {@code includeNullContext} is {@code true}, then also the {@code null} context is copied.
	 * 
	 * @param oRes
	 * @param filteredRepositoryConnection
	 * @param expandedGraphs
	 * @param includeNullContext
	 * @param outputFormat
	 * @throws IOException
	 */
	private static void dumpRepository(HttpServletResponse oRes,
			RepositoryConnection filteredRepositoryConnection, Resource[] expandedGraphs,
			boolean includeNullContext, RDFFormat outputFormat) throws IOException {
		if (expandedGraphs.length != 0) {
			Resource[] outputGraphs;
			if (includeNullContext) {
				List<Resource> tempList = Lists.newArrayList(expandedGraphs);
				tempList.add(null);
				outputGraphs = tempList.toArray(new Resource[tempList.size()]);
			} else {
				outputGraphs = expandedGraphs;
			}
			report2requestResponse(oRes, RDFReporter.fromRepositoryConnection(filteredRepositoryConnection,
					null, null, null, includeNullContext, outputGraphs), outputFormat);
		} else { // the filtered repoitory is empty
			report2requestResponse(oRes, RDFReporter.EmptyReporter, outputFormat);
		}

	}

	/**
	 * Reports RDF data to the request output stream
	 * 
	 * @param oRes
	 * @param rdfReporter
	 * @param outputFormat
	 * @throws IOException
	 */
	public static void report2requestResponse(HttpServletResponse oRes, RDFReporter rdfReporter,
			RDFFormat outputFormat) throws IOException {
		File tempServerFile = File.createTempFile("save", "." + outputFormat.getDefaultFileExtension());
		try {
			try (OutputStream tempServerFileStream = new FileOutputStream(tempServerFile)) {
				rdfReporter.export(Rio.createWriter(outputFormat, tempServerFileStream));
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