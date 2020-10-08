package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.FormattedResourceSource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RDFReporter;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Source;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingException;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingExporter;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * This class provides services for exporting the data managed by a project .
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Export extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Export.class);

	@Autowired
	private ExtensionPointManager exptManager;

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public Collection<AnnotatedValue<org.eclipse.rdf4j.model.Resource>> getNamedGraphs() throws Exception {
		return Iterations.stream(getManagedConnection().getContextIDs()).distinct()
				.map(AnnotatedValue<org.eclipse.rdf4j.model.Resource>::new).collect(toList());
	}

	/**
	 * Gets {@link RDFFormat}s for which a writer is available
	 * 
	 * @return
	 * @throws Exception
	 */
	@STServiceOperation
	@Read
	public Collection<RDFFormat> getOutputFormats() throws Exception {
		return RDF4JUtilities.getOutputFormats();
	}

	/**
	 * Returns formats accepted by a {@link ReformattingExporter}.
	 * 
	 * @param reformattingExporterID
	 * @return
	 */
	@STServiceOperation
	@Read
	public List<DataFormat> getExportFormats(String reformattingExporterID) {
		ExtensionFactory<?> extensionPoint = exptManager.getExtension(reformattingExporterID);

		if (extensionPoint instanceof FormatCapabilityProvider) {
			return ((FormatCapabilityProvider) extensionPoint).getFormats();
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Exports the content of the currently used project. The RDF data can be transformed using a
	 * {@link TransformationPipeline}, and optionally reformatted to a (usually non-RDF) format by means of a
	 * {@link ReformattingExporter}. The response of this operation is the actual data, if no
	 * {@code deployerSpec} is provided; otherwise, the standard response of a void service operation is
	 * written to the output stream, while the data are deployed somewhere else.
	 * 
	 * @param oRes
	 * @param graphs
	 *            the graphs to be exported. An empty array means all graphs the name of which is an IRI
	 * @param filteringPipeline
	 *            a JSON string representing an array of {@link TransformationStep}. Each filter is applied to
	 *            a subset of the exported graphs. No graph means every exported graph
	 * @param includeInferred
	 *            tells if inferred triples should be included
	 * @param outputFormat
	 *            the output format. If it does not support graphs, the exported graph are merged into a
	 *            single graph
	 * @param force
	 *            <code>true</code> tells the service to proceed despite the presence of triples in the null
	 *            context or in graphs named by blank nodes. Otherwise, under this conditions the service
	 *            would fail, so that available information is not silently ignored
	 * @param reformattingExporterSpec
	 *            an optional {@link ReformattingExporter} that reformats the data to a (usually non-RDF)
	 *            format
	 * @param deployerSpec
	 *            an optional {@link Deployer} to export the data somewhere instead of simply downloading it
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public void export(HttpServletResponse oRes, @Optional(defaultValue = "") IRI[] graphs,
			@Optional(defaultValue = "[]") TransformationPipeline filteringPipeline,
			@Optional(defaultValue = "false") boolean includeInferred, @Optional String outputFormat,
			@Optional(defaultValue = "false") boolean force,
			@Optional PluginSpecification reformattingExporterSpec,
			@Optional PluginSpecification deployerSpec) throws Exception {

		exportHelper(exptManager, stServiceContext, oRes, getManagedConnection(), graphs, filteringPipeline,
				includeInferred, outputFormat, force, deployerSpec, reformattingExporterSpec);
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'R')")
	public void dataDump(HttpServletResponse oRes, @Optional(defaultValue = "Turtle") RDFFormat format)
			throws Exception {
		exportHelper(exptManager, stServiceContext, oRes, getManagedConnection(),
				new IRI[] { (IRI) getWorkingGraph() }, new TransformationPipeline(new TransformationStep[0]),
				false, format.getName(), true, null, null);
	}

	public static void exportHelper(ExtensionPointManager exptManager, STServiceContext stServiceContext,
			HttpServletResponse oRes, RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			TransformationPipeline filteringPipeline, boolean includeInferred, String outputFormat,
			boolean force, @Nullable PluginSpecification deployerSpec,
			@Nullable PluginSpecification reformattingExporterSpec)
			throws IOException, WrongPropertiesException,
			ExportPreconditionViolationException, IllegalArgumentException, STPropertyAccessException,
			InvalidConfigurationException, NoSuchExtensionException, ReformattingException {
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

		TransformationStep[] filteringSteps = filteringPipeline.getSteps();

		if (filteringSteps.length == 0) {
			// No filter has been specified. Then, just dump the data without creating a working copy
			formatAndThenDownloadOrDeploy(exptManager, stServiceContext, oRes, graphs, includeInferred,
					outputFormat, deployerSpec, sourceRepositoryConnection, reformattingExporterSpec);
		} else {
			// Translates numeric graph references to graph names
			IRI[][] step2graphs = computeGraphsForStep(graphs, filteringSteps);

			// Creates a working copy of the source repository (in-memory, without inference)
			Repository workingRepository = new SailRepository(new MemoryStore());
			try {
				workingRepository.init();

				try (RepositoryConnection workingRepositoryConnection = workingRepository.getConnection()) {
					// Copies all graphs from the source repository to the working repository (including the
					// null context, when inference is enabled)
					sourceRepositoryConnection.exportStatements(null, null, null, includeInferred,
							new RDFInserter(workingRepositoryConnection));

					// Applies each filter
					for (int i = 0; i < filteringSteps.length; i++) {
						IRI[] stepGraphs = step2graphs[i];
						TransformationStep filteringStep = filteringSteps[i];
						PluginSpecification filterSpec = filteringStep.getFilter();
						RDFTransformer transformer = exptManager.instantiateExtension(RDFTransformer.class,
								filterSpec);
						transformer.transform(sourceRepositoryConnection, workingRepositoryConnection,
								stepGraphs);
					}

					formatAndThenDownloadOrDeploy(exptManager, stServiceContext, oRes, graphs,
							includeInferred, outputFormat, deployerSpec, workingRepositoryConnection,
							reformattingExporterSpec);
				}
			} finally {
				workingRepository.shutDown();
			}
		}
	}

	/**
	 * Depending on whether {@code deployerSpec} is non {@code null}, deploys the data or downloads it
	 * 
	 * @param exptManager
	 * @param oRes
	 * @param graphs
	 * @param includeInferred
	 * @param outputFormat
	 * @param deployerSpec
	 * @param workingRepositoryConnection
	 * @param reformattingExporterSpec
	 * @throws IllegalArgumentException
	 * @throws NoSuchExtensionException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 * @throws IOException
	 * @throws ReformattingException
	 */
	public static void formatAndThenDownloadOrDeploy(ExtensionPointManager exptManager,
			STServiceContext stServiceContext, HttpServletResponse oRes, IRI[] graphs,
			boolean includeInferred, String outputFormat, @Nullable PluginSpecification deployerSpec,
			RepositoryConnection workingRepositoryConnection,
			@Nullable PluginSpecification reformattingExporterSpec)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, IOException, ReformattingException {

		// apply the given reformatting export, or if no deployer was specified force serialization to RDF

		ReformattingExporter reformattingExporter;
		if (reformattingExporterSpec != null) {
			reformattingExporter = exptManager.instantiateExtension(ReformattingExporter.class,
					reformattingExporterSpec);
		} else if (deployerSpec == null) {
			reformattingExporter = exptManager.instantiateExtension(ReformattingExporter.class,
					new PluginSpecification(
							"it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporter",
							"it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporterConfiguration",
							null, JsonNodeFactory.instance.objectNode()));
		} else {
			reformattingExporter = null;
		}

		try (ClosableFormattedResource formattedResource = reformattingExporter == null ? null
				: reformattingExporter.export(workingRepositoryConnection, graphs, outputFormat,
						() -> stServiceContext.getProject().getLexicalizationModel())) {

			Source source;
			if (formattedResource != null) {
				source = new FormattedResourceSource(formattedResource);
			} else {
				source = new RepositorySource(workingRepositoryConnection, graphs);
			}

			if (deployerSpec != null) {
				Deployer deployer = exptManager.instantiateExtension(Deployer.class, deployerSpec);
				deployer.deploy(source);
				String responseString = ServletUtilities.getService()
						.createReplyResponse(stServiceContext.getRequest().getServiceMethod(),
								RepliesStatus.ok, SerializationType.json)
						.getResponseContent();
				byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
				oRes.setHeader("Content-Type", "application/json;charset=UTF-8");
				oRes.setHeader("Content-Length", Integer.toString(responseBytes.length));
				try (OutputStream os = oRes.getOutputStream()) {
					os.write(responseBytes, 0, responseBytes.length);
					os.flush();
				}
			} else {
				// Dumps the working repository (i.e. the filtered repository)
				write2requestResponse(oRes, formattedResource); // if there is no deployer, then data are
																// always reformatted
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
	private static IRI[][] computeGraphsForStep(IRI[] graphs, TransformationStep[] filteringSteps)
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
	 * Writes the provided <em>formatted resource</em> to the output stream of the <em>HTTP response</em>
	 * 
	 * @param oRes
	 * @param source
	 * @throws IOException
	 */
	public static void write2requestResponse(HttpServletResponse oRes, ClosableFormattedResource source)
			throws IOException {
		oRes.setHeader("Content-Disposition",
				"attachment; filename=save." + source.getDefaultFileExtension());
		oRes.setContentType(source.getMIMEType());
		source.writeTo(oRes.getOutputStream());
	}

	/**
	 * Writes the provided <em>formatted resource</em> to the output stream of the <em>HTTP response</em>
	 * 
	 * @param oRes
	 * @param source
	 * @throws IOException
	 */
	public static void write2requestResponse(HttpServletResponse oRes, RDFReporter source,
			RDFFormat outputFormat) throws IOException {
		oRes.setHeader("Content-Disposition",
				"attachment; filename=save." + outputFormat.getDefaultFileExtension());
		oRes.setContentType(outputFormat.getDefaultMIMEType());
		source.export(Rio.createWriter(outputFormat, oRes.getOutputStream()));
	}
}