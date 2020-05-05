package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for exporting metadata about the dataset associated with the current project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class DatasetMetadata extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(DatasetMetadata.class);

	@Autowired
	private ExtensionPointManager exptManager;

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
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws DatasetMetadataExporterException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public void export(HttpServletResponse oRes, PluginSpecification exporterSpecification,
			@Optional(defaultValue = "TURTLE") RDFFormat outputFormat) throws ClassNotFoundException,
			WrongPropertiesException, IOException, STPropertyAccessException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, DatasetMetadataExporterException {

		DatasetMetadataExporter exporter = exptManager.instantiateExtension(DatasetMetadataExporter.class,
				exporterSpecification);

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
			RDFWriter rdfWriter = Rio.createWriter(outputFormat, new FileOutputStream(tempServerFile));
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			Rio.write(metadata, rdfWriter);
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