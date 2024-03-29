package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.ontology.ImportModality;
import it.uniroma2.art.semanticturkey.ontology.InvalidPrefixException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.OntologyImport;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.ontology.PrefixNotDefinedException;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;

/**
 * This class provides services for manipulating metadata associated with a project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class Metadata extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Metadata.class);

	// Base uri management

	/**
	 * Returns the base URI associated with the project
	 * 
	 * @return
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('pm(project, baseuri)', 'R')") temporarily disabled (maybe not
	// required)
	public String getBaseURI() {
		return getOntologyManager().getBaseURI();
	}

	// Namespace prefixes management

	/**
	 * Returns the default namespace associated with the project
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	// @PreAuthorize("@auth.isAuthorized('pm(project, defnamespace)', 'R')") temporarily disabled (maybe not
	// required)
	public String getDefaultNamespace() {
		return getManagedConnection().getNamespace("");
	}

	/**
	 * Sets the default namespace associated with the project
	 * 
	 * @param namespace
	 * @throws ProjectUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, defnamespace)', 'U')")
	// @Write Project.setDefaultNamespace uses an internal/dedicate connection
	public void setDefaultNamespace(String namespace) throws ProjectUpdateException {
		getProject().setDefaultNamespace(namespace);
	}

	/**
	 * Returns the prefix declarations in the managed ontology.
	 *
	 * @param explicit
	 * @return
	 * @throws OntologyManagerException
	 */
	@STServiceOperation
	public Collection<PrefixMapping> getNamespaceMappings(@Optional(defaultValue = "false") boolean explicit) throws OntologyManagerException {
		Map<String, String> allMappings = getOntologyManager().getNSPrefixMappings(explicit);
		Map<String, String> explicitMappings = getOntologyManager().getNSPrefixMappings(true);

		return allMappings.entrySet().stream().map(entry -> new PrefixMapping(entry.getKey(),
				entry.getValue(), explicitMappings.containsKey(entry.getKey()))).sorted(Comparator.comparing(PrefixMapping::getPrefix)).collect(toList());
	}

	/**
	 * Expands a qname into its full form, by substituting a namespace for its prefix.
	 * 
	 * @param qname
	 * @return
	 * @throws IllegalArgumentException
	 *             if the provided qname has problems such as missing colon or undefined prefix
	 * @throws PrefixNotDefinedException
	 */
	@STServiceOperation
	@Read
	public String expandQName(String qname) throws IllegalArgumentException, PrefixNotDefinedException {
		String[] parts = qname.split(":");

		if (parts.length == 0) {
			throw new IllegalArgumentException("The provided value does not contain a colon: " + qname);
		}

		if (parts.length == 1) {
			return getManagedConnection().getNamespace("") + qname;
		} else {
			String prefix = parts[0];

			String ns = getManagedConnection().getNamespace(prefix);

			if (ns == null) {
				throw new PrefixNotDefinedException(prefix);
			}

			return ns + parts[1];
		}
	}

	/**
	 * Defines a namespace prefix
	 * 
	 * @param prefix
	 * @param namespace
	 * @throws NSPrefixMappingUpdateException
	 * @throws InvalidPrefixException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, prefixMapping)', 'U')")
	public void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, InvalidPrefixException {
		if (!ModelUtilities.isPrefixSyntValid(prefix)) {
			throw new InvalidPrefixException(prefix);
		}
		getOntologyManager().setNSPrefixMapping(prefix, namespace);
	}

	/**
	 * Changes the namespace mapping for the loaded ontology. Since there is no evidence that any ontology API
	 * will ever use this (there is typically only a setNamespaceMapping method) we have not included a
	 * changeNamespaceMapping in the API and consequently we delegate here setNamespaceMapping. Should this
	 * situation change, this method will require a proper implementation.
	 * 
	 * @throws InvalidPrefixException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, prefixMapping)', 'U')")
	public void changeNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, InvalidPrefixException {
		if (!ModelUtilities.isPrefixSyntValid(prefix)) {
			throw new InvalidPrefixException(prefix);
		}
		getOntologyManager().setNSPrefixMapping(prefix, namespace);
	}

	/**
	 * Removes all prefix declarations for the supplied <code>namespace</code>
	 * 
	 * @param namespace
	 * @param checkOnlyExplicit
	 * @throws NSPrefixMappingUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, prefixMapping)', 'D')")
	public void removeNSPrefixMapping(String namespace,
			@Optional(defaultValue = "true") boolean checkOnlyExplicit)
			throws NSPrefixMappingUpdateException {
		getOntologyManager().removeNSPrefixMapping(namespace, checkOnlyExplicit);
	}

	/**
	 * Represents an individual prefix mapping.
	 *
	 */
	public static class PrefixMapping {
		private final String prefix;
		private final String namespace;
		private final boolean explicit;

		public PrefixMapping(String prefix, String namespace, boolean explicit) {
			this.prefix = prefix;
			this.namespace = namespace;
			this.explicit = explicit;
		}

		/**
		 * Returns the prefix
		 * 
		 * @return
		 */
		public String getPrefix() {
			return prefix;
		}

		/**
		 * Returns the namespace
		 * 
		 * @return
		 */
		public String getNamespace() {
			return namespace;
		}

		/**
		 * Tells whether this prefix mapping was explicitly set through the {@link OntologyManager}
		 * 
		 * @return
		 */
		public boolean isExplicit() {
			return explicit;
		}
	}

	// Graph management

	/**
	 * Returns the named graphs in the managed ontology.
	 * 
	 * @return
	 * @throws RepositoryException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'R')")
	public Collection<AnnotatedValue<org.eclipse.rdf4j.model.Resource>> getNamedGraphs()
			throws RepositoryException {
		return Iterations.stream(getManagedConnection().getContextIDs())
				.map(AnnotatedValue<org.eclipse.rdf4j.model.Resource>::new).collect(toList());
	}

	// Import management

	/**
	 * Returns the hierarchy formed by the imports. Cyclic imports are identified and reported in the response
	 */
	@STServiceOperation
	@Read
	// @PreAuthorize("@auth.isAuthorized('rdf(import)', 'R')") temporarily disabled (maybe not required)
	public Collection<it.uniroma2.art.semanticturkey.ontology.OntologyImport> getImports()
			throws RepositoryException {
		return getOntologyManager().getUserOntologyImportTree(getManagedConnection());
	}

	/**
	 * Imports an ontology from a local file, and copies it to the ontology mirror
	 * 
	 * @param baseURI
	 * @param localFile
	 * @param mirrorFile
	 * @param transitiveImportAllowance
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'C')")
	public Collection<OntologyImport> addFromLocalFile(@Optional String baseURI, MultipartFile localFile,
			@Optional String mirrorFile, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, OntologyManagerException, IOException {
		Set<IRI> failedImports = new HashSet<>();
		File inputServerFile = File.createTempFile("addFromLocalFile", localFile.getOriginalFilename());
		if (baseURI == null) {
			baseURI = inputServerFile.toURI().toString();
		}
		try {
			localFile.transferTo(inputServerFile);
			getOntologyManager().addOntologyImportFromLocalFile(getManagedConnection(), baseURI,
					ImportModality.USER, inputServerFile.getPath(), mirrorFile, transitiveImportAllowance,
					failedImports);

			return OntologyImport.fromImportFailures(failedImports);
		} finally {
			inputServerFile.delete();
		}
	}

	/**
	 * Imports an ontology from the ontology mirror
	 * 
	 * @param baseURI
	 * @param mirrorFile
	 * @param transitiveImportAllowance
	 * 
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws OntologyManagerException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'C')")
	public Collection<OntologyImport> addFromMirror(String baseURI, String mirrorFile,
			TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		Set<IRI> failedImports = new HashSet<>();

		getOntologyManager().addOntologyImportFromMirror(getManagedConnection(), baseURI, ImportModality.USER,
				mirrorFile, transitiveImportAllowance, failedImports);

		return OntologyImport.fromImportFailures(failedImports);
	}

	/**
	 * Imports an ontology from the web
	 * 
	 * @param baseURI
	 * @param altUrl
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * 
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws OntologyManagerException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'C')")
	@Write
	public Collection<OntologyImport> addFromWeb(String baseURI, @Optional String altUrl,
			@Optional RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		String url = altUrl != null ? altUrl : baseURI;

		Set<IRI> failedImports = new HashSet<>();

		getOntologyManager().addOntologyImportFromWeb(getManagedConnection(), baseURI, ImportModality.USER,
				url, rdfFormat, transitiveImportAllowance, failedImports);

		return OntologyImport.fromImportFailures(failedImports);
	}

	/**
	 * Imports an ontology from the web and copies it to the ontology mirror
	 * 
	 * @param baseURI
	 * @param altUrl
	 * @param mirrorFile
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * 
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws OntologyManagerException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public Collection<OntologyImport> addFromWebToMirror(String baseURI, @Optional String altUrl,
			String mirrorFile, @Optional RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		String url = altUrl != null ? altUrl : baseURI;

		Set<IRI> failedImports = new HashSet<>();

		getOntologyManager().addOntologyImportFromWebToMirror(getManagedConnection(), baseURI,
				ImportModality.USER, url, mirrorFile, rdfFormat, transitiveImportAllowance, failedImports);

		return OntologyImport.fromImportFailures(failedImports);
	}

	/**
	 * Removes an ontology import
	 * 
	 * @param baseURI
	 * @throws RDF4JException
	 * @throws OntologyManagerException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'D')")
	public void removeImport(String baseURI) throws RDF4JException, OntologyManagerException, IOException {
		getOntologyManager().removeOntologyImport(getManagedConnection(), baseURI);
	}

	// Failed imports recovery

	/**
	 * Downloads an ontology that is a failed import from the web
	 * 
	 * @param baseURI
	 * @param altUrl
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @return
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(import)', 'C')")
	public Collection<OntologyImport> downloadFromWeb(String baseURI, @Optional String altUrl,
			@Optional RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, ImportManagementException, IOException {
		String url = altUrl != null ? altUrl : baseURI;

		Set<IRI> failedImports = new HashSet<>();

		getOntologyManager().downloadImportedOntologyFromWeb(getManagedConnection(), baseURI, url, rdfFormat,
				transitiveImportAllowance, failedImports);

		return OntologyImport.fromImportFailures(failedImports);
	}

	/**
	 * Downloads an ontology that is a failed import from the web to the ontology mirror
	 * 
	 * @param baseURI
	 * @param altUrl
	 * @param mirrorFile
	 * @param rdfFormat
	 * @param transitiveImportAllowance
	 * @return
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('sys(ontologymirror)', 'C')")
	public Collection<OntologyImport> downloadFromWebToMirror(String baseURI, @Optional String altUrl,
			String mirrorFile, @Optional RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, ImportManagementException, IOException {
		String url = altUrl != null ? altUrl : baseURI;

		Set<IRI> failedImports = new HashSet<>();

		getOntologyManager().downloadImportedOntologyFromWebToMirror(getManagedConnection(), baseURI, url,
				mirrorFile, rdfFormat, transitiveImportAllowance, failedImports);

		return OntologyImport.fromImportFailures(failedImports);
	}

	/**
	 * Retrieves an ontology that is a failed import from a local file and copies it to the ontology mirror
	 * 
	 * @param baseURI
	 * @param localFile
	 * @param mirrorFile
	 * @param transitiveImportAllowance
	 * @return
	 * @throws RDF4JException
	 * @throws MalformedURLException
	 * @throws ImportManagementException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public Collection<OntologyImport> getFromLocalFile(String baseURI, MultipartFile localFile,
			@Optional String mirrorFile, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws RDF4JException, MalformedURLException, ImportManagementException, IOException {
		Set<IRI> failedImports = new HashSet<>();
		File inputServerFile = File.createTempFile("getFromLocalFile", localFile.getOriginalFilename());
		try {
			localFile.transferTo(inputServerFile);
			getOntologyManager().getImportedOntologyFromLocalFile(getManagedConnection(), baseURI,
					inputServerFile.getPath(), mirrorFile, transitiveImportAllowance, failedImports);

			return OntologyImport.fromImportFailures(failedImports);
		} finally {
			inputServerFile.delete();
		}
	}

	protected OntologyManager getOntologyManager() {
		return getProject().getOntologyManager();
	}
}
