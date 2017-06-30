package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.metadata.OntologyImport;

/**
 * This class provides services for input/output.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class InputOutput extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(InputOutput.class);

	/**
	 * Adds RDF data directly to the ontology being edited (i.e. it is not a read-only import of an external
	 * ontology that the working ontology depends on, but a mass add of RDF triples to the main graph of the
	 * working ontology)
	 * 
	 * @param inputFile
	 * @param baseURI
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedRDFFormatException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("#validateImplicitly ? @auth.isAuthorized('rdf', 'CV') : @auth.isAuthorized('rdf', 'C')")
	public Collection<OntologyImport> loadRDF(MultipartFile inputFile, String baseURI,
			@Optional RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			@Optional(defaultValue = "false") boolean validateImplicitly)
			throws FileNotFoundException, IOException {

		RepositoryConnection conn = getManagedConnection();

		if (validateImplicitly) {
			if (!getProject().isValidationEnabled()) {
				throw new IllegalArgumentException(
						"Could not validate loaded data implicitly becase validation is disabled");
			}

			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED, BooleanLiteral.FALSE,
					CHANGETRACKER.VALIDATION);
			conn.prepareBooleanQuery("ASK {}").evaluate(); // perform a dummy query to flush the possibly cached
															// operation
		}

		// create a temp file (in karaf data/temp folder) to copy the received file
		File inputServerFile = File.createTempFile("loadRDF", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(inputServerFile);

			if (rdfFormat == null) {
				logger.debug("guessing format from extension of file to be loaded: " + rdfFormat);
				rdfFormat = RDFFormat
						.matchFileName(inputFile.getOriginalFilename(),
								RDFParserRegistry.getInstance().getKeys())
						.orElseThrow(
								() -> new IllegalArgumentException("Could not match a parser for file name: "
										+ inputFile.getOriginalFilename()));
			}

			Set<IRI> failedImports = new HashSet<>();

			getProject().getNewOntologyManager().loadOntologyData(conn, inputServerFile, baseURI, rdfFormat,
					getWorkingGraph(), transitiveImportAllowance, failedImports);
			return OntologyImport.fromImportFailures(failedImports);
		} finally {
			inputServerFile.delete();
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