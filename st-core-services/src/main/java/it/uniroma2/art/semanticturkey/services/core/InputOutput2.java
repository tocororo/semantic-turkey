package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.metadata.OntologyImport;

/**
 * This class provides services for input/output.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class InputOutput2 extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(InputOutput2.class);

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
	@PreAuthorize("@auth.isAuthorized('rdf', 'C')")
	public Collection<OntologyImport> loadRDF(MultipartFile inputFile, String baseURI,
			@Optional RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws FileNotFoundException, IOException, UnsupportedRDFFormatException {

		// create a temp file (in karaf data/temp folder) to copy the received file
		File inputServerFile = File.createTempFile("loadRDF", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(inputServerFile);

			if (rdfFormat == null) {
				logger.debug("guessing format from extension of file to be loaded: " + rdfFormat);
				rdfFormat = RDFFormat
						.matchFileName(inputFile.getName(), RDFParserRegistry.getInstance().getKeys())
						.orElseThrow(() -> new IllegalArgumentException(
								"Could not match a parser for file name: " + inputFile.getName()));
			}

			Set<IRI> failedImports = new HashSet<>();

			getProject().getNewOntologyManager().loadOntologyData(inputServerFile, baseURI, rdfFormat,
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