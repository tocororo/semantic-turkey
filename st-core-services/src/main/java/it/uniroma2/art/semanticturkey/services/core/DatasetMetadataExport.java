package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
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
public class DatasetMetadataExport extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(DatasetMetadataExport.class);

	@STServiceOperation
	@Read
	public Collection<RDFFormat> getOutputFormats() throws Exception {
		return RDFWriterRegistry.getInstance().getKeys();
	}

	@STServiceOperation
	public JsonNode getExporterSettings(String exporterId) throws STPropertyAccessException {
		try {
			STProperties settings = PluginManager.getPluginFactory(exporterId)
					.getProjectSettings(getProject());

			ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
			ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
			objectNode.set("type", JsonNodeFactory.instance.textNode(settings.getClass().getName()));
			objectNode.set("shortName", JsonNodeFactory.instance.textNode(settings.getShortName()));
			objectNode.set("parameters", parameters);

			for (String prop : settings.getProperties()) {
				String parDescr = settings.getPropertyDescription(prop);
				ObjectNode newPar = JsonNodeFactory.instance.objectNode();
				parameters.add(newPar);
				newPar.set("name", JsonNodeFactory.instance.textNode(prop));
				newPar.set("description", JsonNodeFactory.instance.textNode(parDescr));
				newPar.set("required",
						JsonNodeFactory.instance.booleanNode(settings.isRequiredProperty(prop)));
				String contentType = settings.getPropertyContentType(prop);
				if (contentType != null)
					newPar.set("type", JsonNodeFactory.instance.textNode(contentType));
				Object parValue = settings.getPropertyValue(prop);
				if (parValue != null) {
					newPar.set("value", JsonNodeFactory.instance.textNode(parValue.toString()));
				}
			}

			return objectNode;
		} catch (PropertyNotFoundException e) {
			throw new STPropertyAccessException(e);
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void setExporterSettings(String exporterId, Map<String, Object> properties)
			throws STPropertyAccessException, STPropertyUpdateException {
		try {
			STProperties settings = PluginManager.getPluginFactory(exporterId)
					.getProjectSettings(getProject());

			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				settings.setPropertyValue(key, value);
			}

			STPropertiesChecker settingsChecker = STPropertiesChecker.getModelConfigurationChecker(settings);
			if (!settingsChecker.isValid()) {
				throw new IllegalArgumentException(
						"Settings not valid: " + settingsChecker.getErrorMessage());
			}
			STPropertiesManager.setProjectSettings(settings, getProject(), exporterId);
		} catch (WrongPropertiesException e) {
			throw new STPropertyAccessException(e);
		}
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