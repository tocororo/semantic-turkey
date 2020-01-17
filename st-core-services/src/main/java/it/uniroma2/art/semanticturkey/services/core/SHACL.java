package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SHACL constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class SHACL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SHACL.class);

	/**
	 * Loads SHACL shapes into the SHACL Shape Graph associated with the contextual project. Existing shapes
	 * are deleted by default, but this behavior can be overridden.
	 * 
	 * @param shapesFile
	 * @param fileFormat
	 * @param clearExisting
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'CU')")
	public void loadShapes(MultipartFile shapesFile, RDFFormat fileFormat,
			@Optional(defaultValue = "false") boolean clearExisting) throws IOException {
		File inputServerFile = File.createTempFile("loadShapes", shapesFile.getOriginalFilename());
		try {
			shapesFile.transferTo(inputServerFile);
			RepositoryConnection con = getManagedConnection();
			if (clearExisting) {
				con.clear(RDF4J.SHACL_SHAPE_GRAPH);
			}
			con.add(inputServerFile, null, fileFormat, RDF4J.SHACL_SHAPE_GRAPH);
		} finally {
			FileUtils.deleteQuietly(inputServerFile);
		}
	}

	/**
	 * Exports the shapes currently stored in the SHACL Shape Graph associated with the contextual project.
	 * The output format is by default pretty printed TURTLE, but this behavior can be overridden. For the
	 * configuration options, please see
	 * {@link it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporter}
	 * 
	 * @param oRes
	 * @param rdfFormat
	 * @param exporterConfiguration
	 * @throws ReformattingException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'R')")
	public void exportShapes(HttpServletResponse oRes, @Optional(defaultValue = "TURTLE") RDFFormat rdfFormat,
			@Optional(defaultValue = "{\"prettyPrint\": true, \"inlineBlankNodes\": true}") ObjectNode exporterConfiguration)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, IOException, ReformattingException {
		ObjectNode exporterConfigurationJson = STPropertiesManager.createObjectMapper()
				.valueToTree(exporterConfiguration);
		PluginSpecification reformattingExporterSpec = new PluginSpecification(
				"it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporter",
				null, null, exporterConfigurationJson);
		Export.formatAndThenDownloadOrDeploy(exptManager, stServiceContext, oRes,
				new IRI[] { RDF4J.SHACL_SHAPE_GRAPH }, false, rdfFormat.getName(), null,
				getManagedConnection(), reformattingExporterSpec);
	}

	/**
	 * Delete existing shapes. This operation clears the SHACL Shape Graph associated with the contextual
	 * project.
	 * 
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'D')")
	public void clearShapes() throws IOException {
		getManagedConnection().clear(RDF4J.SHACL_SHAPE_GRAPH);
	}

}
