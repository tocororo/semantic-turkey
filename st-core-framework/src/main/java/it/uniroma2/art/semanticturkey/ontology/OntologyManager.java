package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;

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

	// Base URI management

	/**
	 * Returns the base URI of the managed ontology
	 * 
	 * @return
	 */
	String getBaseURI();

	/**
	 * Sets the base URI of the managed ontology
	 * 
	 * @param baseURI
	 */
	void setBaseURI(String baseURI);

	// Namespace prefixes management

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

	/**
	 * Defines a namespace prefix. This method shouldn't be invoked explicitly by client code.
	 * 
	 * @param prefix
	 * @param namespace
	 * @throws NSPrefixMappingUpdateException
	 */
	void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException;

	/**
	 * Removes all prefix declarations for the supplied <code>namespace</code>
	 * 
	 * @param namespace
	 * @throws NSPrefixMappingUpdateException
	 */
	void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException;

	// Add an ontology import from various sources (local file, mirror, web)

	/**
	 * Imports an ontology from a local file, and copies it to the ontology mirror.
	 * 
	 * @param baseURI
	 * @param fromLocalFilePath
	 * @param toLocalFile
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 */
	void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException;

	/**
	 * Imports an ontology from the ontology mirror
	 * 
	 * @param baseURI
	 * @param mirFileString
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromMirror(String baseURI, String mirFileString,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Imports an ontology from the web
	 * 
	 * @param baseUriToBeImported
	 * @param url
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromWeb(String baseUriToBeImported, String url, @Nullable RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Imports an ontology from the web and copies it to the ontology mirror
	 * 
	 * @param baseURI
	 * @param sourceURL
	 * @param toLocalFile
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Removes an ontology import
	 * 
	 * @param uriToBeRemoved
	 * @throws IOException
	 */
	void removeOntologyImport(String uriToBeRemoved)
			throws IOException;

	// Ontology import status

	/**
	 * Returns the status of an ontology import. The only relevant information is
	 * {@link ImportStatus#getValue()}
	 * 
	 * @param baseURI
	 * @return
	 */
	ImportStatus getImportStatus(String baseURI);

	// Recover failed imports

	/**
	 * Downloads an ontology that is a failed import from the web
	 * 
	 * @param baseURI
	 * @param altURL
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws IOException
	 */
	void downloadImportedOntologyFromWeb(String baseURI, String altURL,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ImportManagementException, RDF4JException,
			IOException;

	/**
	 * Downloads an ontology that is a failed import from the web to the ontology mirror
	 * 
	 * @param baseURI
	 * @param altURL
	 * @param toLocalFile
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws ImportManagementException, RDF4JException, MalformedURLException,
			IOException;

	/**
	 * Retrieves an ontology that is a failed import from a local file and copies it to the ontology mirror
	 * 
	 * @param baseURI
	 * @param fromLocalFilePath
	 * @param toLocalFile
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws IOException
	 */
	void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ImportManagementException, RDF4JException,
			IOException;

	// Application/Support ontologies management

	/**
	 * can be used by ST extensions to declare use of application ontologies for supporting their
	 * functionalities<br/>
	 * if <code>ng</code> is <b>true</b>, this prevents triples in the namedgraph of this ontology to be shown
	 * (when ST is in <code>user</code> mode)<br/>
	 * if <code>ns</code> is <b>true</b>, this prevents triples having a predicate with namespace matching the
	 * name of ontology <code>ont</code> to be shown (again, when ST is in <code>user</code> mode)<br/>
	 * One of the two boolean arguments must obviously be true, otherwise this declaration has no effect
	 * 
	 * @param ont
	 * @param ng
	 * @param ns
	 */
	void declareApplicationOntology(IRI iri, boolean b, boolean c);

	/**
	 * Checks whether <code>ns</code> is an application ontology
	 * 
	 * @param ns
	 * @return
	 */
	boolean isApplicationOntNamespace(String ns);

	/**
	 * Checks whether <code>ns</code> is a support ontology
	 * 
	 * @param ns
	 * @return
	 */
	boolean isSupportOntNamespace(String ns);

	// Load/Save data

	/**
	 * Adds RDF data directly to the ontology being edited (i.e. it is not a read-only import of an external
	 * ontology that the working ontology depends on, but a mass add of RDF triples to the main graph of the
	 * working ontology)
	 * 
	 * @param inputFile
	 *            the RDF file from which RDF data is being loaded
	 * @param baseURI
	 *            the baseURI to be used when importing relative names from loaded RDF data
	 * @param format
	 * @param graph
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RDF4JException
	 */
	public void loadOntologyData(File inputFile, String baseURI, RDFFormat format, Resource graph,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws FileNotFoundException, IOException, RDF4JException;

	/**
	 * Clear the managed ontology
	 */
	public void clearData() throws RDF4JException;

	/**
	 * Copies an ontology to the ontology mirror.
	 * 
	 * @param baseURI
	 * @param toLocalFile
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, RDF4JException, OntologyManagerException;

	// Management operations

	/**
	 * Initializes mapping persistence
	 * 
	 * @param nsPrefixMappingsPersistence
	 */
	void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappingsPersistence);

	/**
	 * Returns the underlying {@link Repository}
	 * 
	 * @return
	 */
	Repository getRepository();

	/**
	 * Starts the ontology manager
	 * 
	 * @param baseURI
	 * @param repoDir
	 * @param repoConfig
	 * @throws RDF4JException
	 */
	void startOntModel(String baseURI, File repoDir, RepositoryConfig repoConfig) throws RDF4JException;

}