package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;

/**
 * A class managing high level operations (such as recursive ontology import, gussing namespace-prefix
 * mappings etc..) which are usually not automated by a triple store.
 * <p/>
 * <p>
 * The <b>namespace-prefix mapping</b> has a double management
 * <ul>
 * <li>prefixes for imported ontologies are guessed by means of some heuristics and then directly added to the
 * loaded triple store manager. These "guesses" will be typically shown with a lighter color on user
 * interfaces, denoting their setting being chosen automatically</li>
 * <li>the namespace-prefix mapping APIs in this class allow for the definition of custom mappings. These are
 * persisted on the project associated to this OntologyManager and will be reloaded each time the project is
 * open. These custom mappings will usually be evident in the UI as user-specified mappings</li>
 * </ul>
 * Note that, at the level of this OntologyManager, there is no track of which mappings were already present
 * in the loaded triple store and which ones have been guessed by OntologyManager. Only those explicitly
 * selected by the user are made evident
 * </p>
 * 
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface OntologyManager {

	void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, RDF4JException, ModelUpdateException, OntologyManagerException;

	void addOntologyImportFromMirror(String baseURI, String mirFileString)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void declareApplicationOntology(IRI iri, boolean b, boolean c);

	void downloadImportedOntologyFromWeb(String baseURI, String altURL) throws MalformedURLException,
			ModelUpdateException, ImportManagementException, RDF4JException, IOException;

	void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException, RDF4JException, MalformedURLException,
			IOException;

	/**
	 * Returns the base URI of the managed ontology
	 * 
	 * @return
	 */
	String getBaseURI();

	void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException, RDF4JException,
			IOException;

	ImportStatus getImportStatus(String baseURI);

	/**
	 * Returns the prefixed defined in the managed ontology. The returned <code>Map</code> is indexed by the
	 * prefixes. If the parameter <code>explicit</code> is set to <code>true</code>, then the returned
	 * <code>Map</code> contains only prefixed explicitly set through the <code>OntologyManager</code>.
	 * 
	 * @param explicit
	 * @return
	 * @throws OntologyManagerException
	 */
	Map<String, String> getNSPrefixMappings(boolean explicit) throws OntologyManagerException;

	Repository getRepository();

	void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappingsPersistence)
			throws ModelUpdateException, ModelAccessException;

	boolean isApplicationOntNamespace(String ns);

	boolean isSupportOntNamespace(String ns);

	void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException, ModelUpdateException;

	void removeOntologyImport(String uriToBeRemoved)
			throws IOException, ModelUpdateException, ModelAccessException;

	void setBaseURI(String baseURI);

	void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException;

	void startOntModel(String baseURI, File repoDir, RepositoryConfig repoConfig) throws RDF4JException;
}
