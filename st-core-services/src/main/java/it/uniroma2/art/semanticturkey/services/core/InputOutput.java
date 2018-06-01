package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.MoreObjects;
import com.google.common.io.Closer;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.FormattedResourceTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.RepositoryTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.RDFLifter;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.ontology.OntologyImport;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * This class provides services for input/output.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class InputOutput extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(InputOutput.class);

	/**
	 * Tries to match the extension of a file name against the list of RDF formats that can be parsed (see:
	 * {@link Rio#getParserFormatForFileName(String)}).
	 * 
	 * @param fileName
	 * @return the name of the matched {@link RDFFormat}, or <code>null</code> if none is found
	 */
	@STServiceOperation
	public String getParserFormatForFileName(String fileName) {
		return Rio.getParserFormatForFileName(fileName).map(RDFFormat::getName).orElse(null);
	}

	/**
	 * Tries to match the extension of a file name against the list of RDF formats that can be written (see:
	 * {@link Rio#getWriterFormatForFileName(String)}).
	 * 
	 * @param fileName
	 * @return the name of the matched {@link RDFFormat}, or <code>null</code> if none is found
	 */
	@STServiceOperation
	public String getWriterFormatForFileName(String fileName) {
		return Rio.getWriterFormatForFileName(fileName).map(RDFFormat::getName).orElse(null);
	}

	/**
	 * Adds RDF data directly to the ontology being edited (i.e. it is not a read-only import of an external
	 * ontology that the working ontology depends on, but a mass add of RDF triples to the main graph of the
	 * working ontology). The data can originate from the body of the request ({@code inputFile} is non-null)
	 * or be fetched from some source, by means of a {@link Loader} ({@code loaderSpec} is non-null). A
	 * {@code Loader} can be associated with an {@link RDFLifter} ({@code rdfLifter} is non-null), when the
	 * data source is not a triple store (interpreted broadly as a source of RDF data). Whatever the source of
	 * the data is, it is possible to setup a {@code transformationPipeline}, to transform the data before
	 * feeding it to the repository. The presence of a transformation pipeline requires the creation of a
	 * temporary repository, where data are first loaded and processed.
	 * 
	 * @param inputFile
	 * @param baseURI
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param loaderSpec
	 * @param rdfLifterSpec
	 * @param transformationPipeline
	 * @param validateImplicitly
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws RDF4JException
	 * @throws LiftingException
	 * @throws UnsupportedRDFFormatException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("#validateImplicitly ? @auth.isAuthorized('rdf', 'CV') : @auth.isAuthorized('rdf', 'C')")
	public Collection<OntologyImport> loadRDF(@Optional MultipartFile inputFile, String baseURI,
			@Optional String rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			@Optional PluginSpecification loaderSpec, @Optional PluginSpecification rdfLifterSpec,
			@Optional(defaultValue = "[]") TransformationPipeline transformationPipeline,
			@Optional(defaultValue = "false") boolean validateImplicitly)
			throws FileNotFoundException, IOException, RDF4JException, IllegalStateException,
			IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, LiftingException {

		RepositoryConnection conn = getManagedConnection();

		if (validateImplicitly) {
			if (!getProject().isValidationEnabled()) {
				throw new IllegalArgumentException(
						"Could not validate loaded data implicitly becase validation is disabled");
			}

			Collection<OntologyImport> rv = new ArrayList<>();
			ValidationUtilities.executeWithoutValidation(
					ValidationUtilities.isValidationEnabled(stServiceContext), conn, (conn2) -> {
						try {
							Collection<OntologyImport> tempRv = loadRDFInternal(inputFile, baseURI, rdfFormat,
									transitiveImportAllowance, conn2, loaderSpec, rdfLifterSpec,
									transformationPipeline);
							rv.addAll(tempRv);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});

			return rv;
		} else {
			return loadRDFInternal(inputFile, baseURI, rdfFormat, transitiveImportAllowance, conn, loaderSpec,
					rdfLifterSpec, transformationPipeline);
		}
	}

	protected Collection<OntologyImport> loadRDFInternal(@Nullable MultipartFile inputFile, String baseURI,
			@Nullable String format, TransitiveImportMethodAllowance transitiveImportAllowance,
			RepositoryConnection conn, @Nullable PluginSpecification loaderSpec,
			PluginSpecification rdfLifterSpec, TransformationPipeline transformationPipeline)
			throws IOException, IllegalStateException, IllegalArgumentException, FileNotFoundException,
			RDF4JException, NoSuchExtensionException, WrongPropertiesException, STPropertyAccessException,
			InvalidConfigurationException, LiftingException {

		Set<IRI> failedImports = new HashSet<>();

		if (inputFile != null && loaderSpec != null) {
			throw new IllegalArgumentException("The request may not provide both input data and a loader");
		}

		try (Closer closer = Closer.create()) {
			// Prepares a temporary repository, if necessary (i.e. a non-empty transformation pipeline has
			// been provided)
			Repository tempRepo;
			RepositoryConnection tempRepoConn;

			if (transformationPipeline.isEmpty()) {
				tempRepo = null;
				tempRepoConn = null;
			} else {
				tempRepo = new SailRepository(new MemoryStore());
				closer.register(tempRepo::shutDown);

				tempRepoConn = tempRepo.getConnection();
				closer.register(tempRepoConn::close);
			}

			RepositoryConnection workingConn = MoreObjects.firstNonNull(tempRepoConn, conn);
			RDFHandler workingRepoInserter;

			if (workingConn != conn) { // ordinary RDFInserter to handle the temporary repository
				RDFInserter tempInserter = new RDFInserter(workingConn);
				tempInserter.enforceContext(getWorkingGraph());
				workingRepoInserter = tempInserter;
			} else { // special RDFHandler provided by the OntologyManger, if directly writing to the
						// repository
				workingRepoInserter = getProject().getNewOntologyManager().getRDFHandlerForLoadData(conn,
						baseURI, getWorkingGraph(), transitiveImportAllowance, failedImports);
			}

			IRI[] graphs = new IRI[] { (IRI) getWorkingGraph() };

			ClosableFormattedResource formattedResource;

			if (loaderSpec != null) {
				Loader loader = exptManager.instantiateExtension(Loader.class, loaderSpec);

				if (rdfLifterSpec != null) { // assume that the loader targets a stream
					FormattedResourceTarget target = new FormattedResourceTarget();
					loader.load(target);
					formattedResource = target.getTargetFormattedResource();
				} else { // assume that the loader targets the repository connection
					formattedResource = null;
					RepositoryTarget target = new RepositoryTarget(workingRepoInserter);
					loader.load(target);
				}
			} else {
				if (rdfLifterSpec == null) {
					rdfLifterSpec = new PluginSpecification(
							"it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer.RDFDeserializingLifter",
							null, null, null);
				}

				// create a temp file (in karaf data/temp folder) to copy the received file
				File inputServerFile = File.createTempFile("loadRDF", inputFile.getOriginalFilename());
				closer.register(inputServerFile::delete);
				inputFile.transferTo(inputServerFile);

				formattedResource = new ClosableFormattedResource(inputServerFile, null, null);
			}

			closer.register(formattedResource);

			// At this point it holds that: formattedResource != null iff rdfLifter != null

			// Applies an RDFLifter, if necessary

			if (rdfLifterSpec != null) {
				RDFLifter rdfLifter = exptManager.instantiateExtension(RDFLifter.class, rdfLifterSpec);
				rdfLifter.lift(formattedResource, format, workingRepoInserter);
			}

			// At this point, data have been loaded to an RDF repository (either destination or temporary, the
			// latter if tehre is a transformation pipeline)

			// Applies a TransformationPipeline

			TransformationStep[] transformationSteps = transformationPipeline.getSteps();

			for (TransformationStep step : transformationSteps) {
				RDFTransformer rdfTrasformer = exptManager.instantiateExtension(RDFTransformer.class,
						step.getFilter());
				rdfTrasformer.transform(conn, workingConn, graphs);
			}

			if (workingConn != conn) {
				RDFHandler rdfInserter = getProject().getNewOntologyManager().getRDFHandlerForLoadData(conn,
						baseURI, getWorkingGraph(), transitiveImportAllowance, failedImports);
				workingConn.export(rdfInserter);
			}

			return OntologyImport.fromImportFailures(failedImports);

		}
	}

	/**
	 * Clear the repository associated with the current project
	 * 
	 * @param inputFile
	 * @param baseURI
	 * @param rdfFormat
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedRDFFormatException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rdf', 'D')")
	public void clearData() throws RDF4JException {
		getProject().getNewOntologyManager().clearData();
	}

};