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
import org.eclipse.rdf4j.model.Model;
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
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
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
 * This class provides services for exporting metadata about the dataset associated with the current project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class DatasetMetadataExport extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(DatasetMetadataExport.class);

	@STServiceOperation
	@Read
	public Collection<RDFFormat> getOutputFormats() throws Exception {
		return RDFWriterRegistry.getInstance().getKeys();
	}

	/**
	 * Exports the metadata about the dataset associated with the currently used project.
	 * 
	 * @param oRes
	 * @param exporterSpecification
	 *            a specification of the {@link DatasetMetadataExporter} to be used
	 * @param outputFormat
	 *            the output format. If it does not support graphs, the exported graph are merged into a
	 *            single graph
	 * @throws UnloadablePluginConfigurationException
	 * @throws UnsupportedPluginConfigurationException
	 * @throws BadConfigurationException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws DatasetMetadataExporterException
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public void export(HttpServletResponse oRes, PluginSpecification exporterSpecification,
			@Optional(defaultValue = "TURTLE") RDFFormat outputFormat)
			throws ClassNotFoundException, BadConfigurationException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, IOException, DatasetMetadataExporterException {

		exporterSpecification.expandDefaults();
		DatasetMetadataExporter exporter = (DatasetMetadataExporter) exporterSpecification.instatiatePlugin();

		Model metadata = exporter.produceDatasetMetadata(getProject(), getManagedConnection(),
				(IRI) getWorkingGraph());

		dumpRepository(oRes, metadata, outputFormat);
	}

	/**
	 * Dumps the provided metadata to the servlet response.
	 * 
	 * @param oRes
	 * @param metadata
	 * @param outputFormat
	 * @throws IOException
	 */
	private void dumpRepository(HttpServletResponse oRes, Model metadata, RDFFormat outputFormat)
			throws IOException {
		File tempServerFile = File.createTempFile("save", "." + outputFormat.getDefaultFileExtension());
		try {
			Rio.write(metadata, new FileOutputStream(tempServerFile), outputFormat);

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