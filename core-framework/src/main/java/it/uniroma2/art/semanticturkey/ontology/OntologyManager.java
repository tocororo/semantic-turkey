package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;

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
	void setNSPrefixMapping(String prefix, String namespace) throws NSPrefixMappingUpdateException;

	/**
	 * Removes all prefix declarations for the supplied <code>namespace</code>
	 * 
	 * @param namespace
	 * @param checkOnlyExplicit
	 * @throws NSPrefixMappingUpdateException
	 */
	void removeNSPrefixMapping(String namespace, boolean checkOnlyExplicit) throws NSPrefixMappingUpdateException;

	// Add an ontology import from various sources (local file, mirror, web)

	/**
	 * Imports an ontology from a local file, and optionally copies it to the ontology mirror.
	 * 
	 * @param conn
	 * @param baseURI
	 * @param modality
	 * @param fromLocalFilePath
	 * @param toLocalFile
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 */
	void addOntologyImportFromLocalFile(RepositoryConnection conn, @Nullable String baseURI, ImportModality modality,
			String fromLocalFilePath, @Nullable String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException;

	/**
	 * Imports an ontology from the ontology mirror
	 * 
	 * @param conn
	 * @param baseURI
	 * @param modality
	 * @param mirFileString
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromMirror(RepositoryConnection conn, String baseURI, ImportModality modality,
			String mirFileString, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Imports an ontology from the web
	 *
	 * @param conn
	 * @param baseUriToBeImported
	 * @param modality
	 * @param url
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromWeb(RepositoryConnection conn, String baseUriToBeImported,
			ImportModality modality, String url, @Nullable RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Imports an ontology from the web and copies it to the ontology mirror
	 * 
	 * @param conn
	 * @param baseURI
	 * @param modality
	 *            TODO
	 * @param sourceURL
	 * @param toLocalFile
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 */
	void addOntologyImportFromWebToMirror(RepositoryConnection conn, String baseURI, ImportModality modality,
			String sourceURL, String toLocalFile, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException;

	/**
	 * Returns a hierarchical representation of user ontology Imports.
	 * 
	 * @param conn
	 * @return
	 */
	Collection<OntologyImport> getUserOntologyImportTree(RepositoryConnection conn);

	/**
	 * Removes an ontology import
	 * 
	 * @param conn
	 * @param uriToBeRemoved
	 * @throws IOException
	 */
	void removeOntologyImport(RepositoryConnection conn, String uriToBeRemoved) throws IOException;

	// Ontology import status

	/**
	 * Returns the status of an ontology import. It is possible to indicate whether comparisons should be
	 * based on canonical names (i.e. stripping terminating #).The only relevant information is
	 * {@link ImportStatus#getValue()}
	 * 
	 * @param baseURI
	 * @param canonicalComparison
	 * 
	 * @return
	 */
	ImportStatus getImportStatus(RepositoryConnection conn, String baseURI, boolean canonicalComparison);

	// Recover failed imports

	/**
	 * Downloads an ontology that is a failed import from the web
	 * 
	 * @param conn
	 * @param baseURI
	 * @param altURL
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws IOException
	 */
	void downloadImportedOntologyFromWeb(RepositoryConnection conn, String baseURI, String altURL,
			RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports)
			throws MalformedURLException, ImportManagementException, RDF4JException, IOException;

	/**
	 * Downloads an ontology that is a failed import from the web to the ontology mirror
	 * 
	 * @param conn
	 * @param baseURI
	 * @param altURL
	 * @param toLocalFile
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws ImportManagementException
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	void downloadImportedOntologyFromWebToMirror(RepositoryConnection conn, String baseURI, String altURL,
			String toLocalFile, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws ImportManagementException, RDF4JException, MalformedURLException, IOException;

	/**
	 * Retrieves an ontology that is a failed import from a local file and copies it to the ontology mirror
	 * 
	 * @param conn
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
	void getImportedOntologyFromLocalFile(RepositoryConnection conn, String baseURI, @Nullable String fromLocalFilePath,
			String toLocalFile, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports)
			throws MalformedURLException, ImportManagementException, RDF4JException, IOException;

	// Application/Support ontologies management

	/**
	 * It can be used by ST extensions to declare use of application ontologies for supporting their
	 * functionalities. The boolean parameters can be used to declare different facets of an application
	 * ontology. If no parameter is set to <code>, this declaration has no effect.
	
	 * @param declareImport if <code>true</code>, declares an import with modality
	 *            {@link ImportModality#APPLICATION}.
	 * 
	 * @param ng
	 *            if <b>true</b>, this prevents triples in the namedgraph of this ontology to be shown (when
	 *            ST is in <code>user</code> mode)
	 * @param ns
	 *            if <b>true</b>, this prevents triples having a predicate with namespace matching the name of
	 *            ontology <code>ont</code> to be shown (again, when ST is in <code>user</code> mode)
	 * 
	 * @param ont
	 * @param ng
	 * @param ns
	 */
	void declareApplicationOntology(IRI ont, boolean declareImport, boolean ng, boolean ns);

	/**
	 * Checks whether <code>ns</code> is an application ontology
	 * 
	 * @param ns
	 * @return
	 */
	boolean isApplicationOntNamespace(String ns);

	/**
	 * It can be used to declare use of an ontology, the presence of which is required by the system to work
	 * properly. As an example, core modeling vocabularies are usually declared as such. The boolean
	 * parameters can be used to declare different facets of a support ontology. If no parameter is set to
	 * <code>, this declaration has no effect.
	
	 * @param declareImport if <code>true</code>, declares an import with modality
	 *            {@link ImportModality#SUPPORT}.
	 * 
	 * @param ng
	 *            if <b>true</b>, this prevents triples in the namedgraph of this ontology to be shown (when
	 *            ST is in <code>user</code> mode)
	 * @param ns
	 *            if <b>true</b>, this prevents triples having a predicate with namespace matching the name of
	 *            ontology <code>ont</code> to be shown (again, when ST is in <code>user</code> mode)
	 * 
	 * @param ont
	 * @param ng
	 * @param ns
	 */
	void declareSupportOntology(IRI ont, boolean declareImport, boolean ng, boolean ns);

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
	 * @param conn
	 *            a connection to the managed ontology
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
	public void loadOntologyData(RepositoryConnection conn, File inputFile, String baseURI, RDFFormat format,
			Resource graph, TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws FileNotFoundException, IOException, RDF4JException;

	/**
	 * Returns an {@link RDFHandler} that adds the data to the ontology being edited (i.e. it is not a
	 * read-only import of an external ontology that the working ontology depends on, but a mass add of RDF
	 * triples to the main graph of the working ontology)
	 * 
	 * @param conn
	 *            a connection to the managed ontology
	 * @param baseURI
	 *            the baseURI to be used when importing relative names from loaded RDF data
	 * @param graph
	 * @param transitiveImportAllowance
	 * @param failedImports
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RDF4JException
	 */
	public RDFHandler getRDFHandlerForLoadData(RepositoryConnection conn, String baseURI, Resource graph,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports);

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

	public static IRI computeCanonicalURI(IRI iri) {
		if (iri.stringValue().endsWith("#")) {
			return SimpleValueFactory.getInstance()
					.createIRI(iri.stringValue().substring(0, iri.stringValue().length() - 1));
		} else {
			return iri;
		}
	}

	public static List<String> computeURIVariants(String baseURI) {
		List<String> baseURIVariants = new ArrayList<>();
		baseURIVariants.add(baseURI);
		if (baseURI.endsWith("#")) {
			baseURIVariants.add(baseURI.substring(0, baseURI.length() - 1));
		} else if (!baseURI.endsWith("/")) {
			baseURIVariants.add(baseURI + "#");
		}
		return baseURIVariants;
	}

	public static List<IRI> computeURIVariants(IRI baseURI) {
		List<IRI> baseURIVariants = new ArrayList<>();
		baseURIVariants.add(baseURI);

		String stringValue = baseURI.stringValue();

		if (stringValue.endsWith("#")) {
			baseURIVariants.add(SimpleValueFactory.getInstance()
					.createIRI(stringValue.substring(0, stringValue.length() - 1)));
		} else if (!stringValue.endsWith("/")) {
			baseURIVariants.add(SimpleValueFactory.getInstance().createIRI(stringValue + "#"));
		}
		return baseURIVariants;
	}

}