package it.uniroma2.art.semanticturkey.ontology.impl;

import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromLocalFile;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromOntologyMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWeb;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWebToMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.toOntologyMirror;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.models.PrefixMapping;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportMethod;
import it.uniroma2.art.semanticturkey.ontology.ImportModality;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.utilities.Utilities;

/**
 * Native implementation of {@link OntologyManager} for RDF4J.
 * 
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OntologyManagerImpl implements OntologyManager {

	private static final Logger logger = LoggerFactory.getLogger(OntologyManagerImpl.class);

	private Repository repository;
	private NSPrefixMappings nsPrefixMappings;
	private volatile String baseURI;

	private Map<IRI, ImportStatus> importsStatusMap;
	private Map<ImportModality, Set<IRI>> importModalityMap;

	private Set<IRI> applicationOntologiesNG;
	private Set<String> applicationOntologiesNamespace;
	private Set<IRI> supportOntologiesNG;
	private Set<String> supportOntologiesNamespace;

	public OntologyManagerImpl() {
		// initializes user, application and support ontology sets
		applicationOntologiesNG = new HashSet<>();
		applicationOntologiesNamespace = new HashSet<>();
		supportOntologiesNG = new HashSet<>();
		supportOntologiesNamespace = new HashSet<>();

		importModalityMap = new HashMap<>();
		importModalityMap.put(ImportModality.APPLICATION, applicationOntologiesNG);
		importModalityMap.put(ImportModality.SUPPORT, supportOntologiesNG);

	}

	@Override
	public void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		addOntologyImportFromLocalFile(baseURI, fromLocalFilePath, toLocalFile, ImportModality.USER, true);
	}

	public void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			ImportModality modality, boolean updateImportStatement)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		logger.debug("adding: " + baseURI + " from localfile: " + fromLocalFilePath + " to Mirror: "
				+ toLocalFile);
		File inputFile = new File(fromLocalFilePath);
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				conn.add(inputFile, baseURI,
						RDFFormat
								.matchFileName(inputFile.getName(), RDFParserRegistry.getInstance().getKeys())
								.orElseThrow(() -> new OntologyManagerException(
										"Could not match a parser for file name: " + inputFile.getName())),
						conn.getValueFactory().createIRI(baseURI));
			}
		} catch (Exception e) {
			if (!updateImportStatement) {
				importsStatusMap.put(SimpleValueFactory.getInstance().createIRI(baseURI),
						ImportStatus.createFailedStatus(e.getMessage()));
			} else
				throw new OntologyManagerException(e);
		}
		notifiedAddedOntologyImport(fromLocalFile, baseURI, fromLocalFilePath, mirFile, modality,
				updateImportStatement);
	}

	@Override
	public void addOntologyImportFromMirror(String baseURI, String mirFileString)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		addOntologyImportFromMirror(baseURI, mirFileString, ImportModality.USER, true);
	}

	public void addOntologyImportFromMirror(String baseURI, String mirFileString, ImportModality modality,
			boolean updateImportStatement)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(mirFileString);
		File physicalMirrorFile = new File(mirFile.getAbsolutePath());
		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				conn.add(physicalMirrorFile, baseURI, RDFFormat
						.matchFileName(physicalMirrorFile.getName(),
								RDFParserRegistry.getInstance().getKeys())
						.orElseThrow(() -> new OntologyManagerException(
								"Could not match a parser for file name: " + physicalMirrorFile.getName())),
						conn.getValueFactory().createIRI(baseURI));
			}
		} catch (Exception e) {
			if (!updateImportStatement) {
				importsStatusMap.put(SimpleValueFactory.getInstance().createIRI(baseURI),
						ImportStatus.createFailedStatus(e.getMessage()));
			} else
				throw new OntologyManagerException(e);
		}
		notifiedAddedOntologyImport(fromOntologyMirror, baseURI, null, mirFile, modality,
				updateImportStatement);

	}

	@Override
	public void addOntologyImportFromWeb(String baseURI, String sourceURL, RDFFormat rdfFormat)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		addOntologyImportFromWeb(baseURI, sourceURL, rdfFormat, ImportModality.USER, true);
	}

	public void addOntologyImportFromWeb(String baseURI, String sourceURL, RDFFormat rdfFormat,
			ImportModality modality, boolean updateImportStatement)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				conn.add(new URL(sourceURL), baseURI, rdfFormat, conn.getValueFactory().createIRI(baseURI));
			}
		} catch (Exception e) {
			if (!updateImportStatement) {
				importsStatusMap.put(SimpleValueFactory.getInstance().createIRI(baseURI),
						ImportStatus.createFailedStatus(e.getMessage()));
			} else
				throw new OntologyManagerException(e);
		}
		notifiedAddedOntologyImport(fromWeb, baseURI, sourceURL, null, modality, updateImportStatement);
	}

	@Override
	public void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException {
		addOntologyImportFromWebToMirror(baseURI, sourceURL, toLocalFile, rdfFormat, ImportModality.USER,
				true);
	}

	public void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat, ImportModality modality, boolean updateImportStatement)
			throws MalformedURLException, RDF4JException, OntologyManagerException {

		// first of all, try to download the ontology in the mirror file in the format specified by the user
		// (or inferred from the extention of the file). Then, if this download was done without any problem,
		// import the ontology

		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		RDFFormat guessedFormat = RDFFormat
				.matchFileName(mirFile.getLocalName(), RDFParserRegistry.getInstance().getKeys())
				.orElse(null);

		if (rdfFormat != null) {
			// check it the input rdfFormat is compliant with the file extension
			if (guessedFormat == null || rdfFormat != guessedFormat) {
				// change the file extention according to the input RDFFormat
				String newLocalFile = toLocalFile + "." + rdfFormat.getDefaultFileExtension();
				mirFile = new MirroredOntologyFile(newLocalFile);
			}
		} else {
			if (guessedFormat == null) {
				String newLocalFile = toLocalFile + "." + RDFFormat.RDFXML.getDefaultFileExtension();
				mirFile = new MirroredOntologyFile(newLocalFile);
			}
		}

		try {
			// try to download the ontology
			notifiedAddedOntologyImport(fromWebToMirror, baseURI, sourceURL, mirFile, modality,
					updateImportStatement);

			try (RepositoryConnection conn = repository.getConnection()) {
				// if the download was achieved, import the ontology in the model

				conn.add(new URL(sourceURL), baseURI, rdfFormat, conn.getValueFactory().createIRI(baseURI));
			}
		} catch (Exception e) {
			if (!updateImportStatement) {
				importsStatusMap.put(SimpleValueFactory.getInstance().createIRI(baseURI),
						ImportStatus.createFailedStatus(e.getMessage()));
			} else
				throw new OntologyManagerException(e);
		}

	}

	private void notifiedAddedOntologyImport(ImportMethod method, String baseURI, String sourcePath,
			OntFile localFile, ImportModality mod, boolean updateImportStatement)
			throws OntologyManagerException {
		logger.debug("notifying added ontology import with method: " + method + " with baseuri: " + baseURI
				+ " sourcePath: " + sourcePath + " localFile: " + localFile + " importModality: " + mod
				+ ", thought for updating the import status: " + updateImportStatement);
		try (RepositoryConnection conn = repository.getConnection()) {
			try {
				IRI ont = conn.getValueFactory().createIRI(baseURI);

				// ***************************
				// Checking that the imported ontology has exactly the same URI used to import it. This may
				// happen
				// when the given URI is a successful URL for retrieving the ontology but it is not the URI of
				// the ontology

				Set<IRI> declOnts = QueryResults
						.asModel(conn.getStatements(null, RDF.TYPE, OWL.ONTOLOGY, false, ont)).subjects()
						.stream().filter(IRI.class::isInstance).map(IRI.class::cast).collect(toSet());

				// the import ont does not contain the declaration of itself as an ont and it contains at
				// least
				// one declaration (probably its own one)
				if (!declOnts.contains(ont) && !declOnts.isEmpty()) {
					// extracting the real baseURI of the imported ontology
					IRI realURI = declOnts.iterator().next();
					// checking that the realURI has not already been imported, by checking the existence of
					// its
					// NG in the current data
					if (conn.hasStatement(null, null, null, false, realURI)) {
						// if realURI is already imported, then remove the data imported in the wrong URI
						conn.clear(ont);
						// and throw an exception
						throw new OntologyManagerException("the real URI for the imported ontology: " + ont
								+ " is actually: " + realURI + " which, however, has already been imported");
					} else {
						// we have to move imported data to the correct baseuri
						Update update = conn.prepareUpdate("MOVE GRAPH " + RenderUtils.toSPARQL(ont)
								+ " TO GRAPH " + RenderUtils.toSPARQL(realURI));
						update.execute();
						ont = realURI;
						baseURI = realURI.stringValue();
					}
				}

				// ***************************

				if (method == fromWebToMirror) {
					Utilities.downloadRDF(new URL(sourcePath), localFile.getAbsolutePath());
				} else if (method == fromLocalFile)
					Utilities.copy(sourcePath, localFile.getAbsolutePath());

				if (method == fromWebToMirror || method == fromLocalFile)
					OntologiesMirror.addCachedOntologyEntry(baseURI, (MirroredOntologyFile) localFile);

				if (method == fromWebToMirror || method == fromLocalFile || method == fromOntologyMirror) {
					logger.debug(
							"setting : " + baseURI + " import status to \"local\" on file: " + localFile);
					importsStatusMap.put(ont, new ImportStatus(ImportStatus.Values.LOCAL, localFile));
				} else if (method == fromWeb) {
					logger.debug("setting : " + baseURI + " import status to \"WEB\" on file: " + localFile);
					importsStatusMap.put(ont, new ImportStatus(ImportStatus.Values.WEB, localFile));
				} else
					throw new OntologyManagerException("the addImport method invoked, identified by id: "
							+ method + " has not been recognized");

				// if the import is explicitly asked by the user, then the import statement is explicitly
				// added to
				// the ontology
				if (updateImportStatement) {
					logger.debug("adding import statement for uri: " + baseURI);
					IRI projBaseURI = conn.getValueFactory().createIRI(getBaseURI());
					conn.add(projBaseURI, OWL.IMPORTS, conn.getValueFactory().createIRI(baseURI),
							projBaseURI);
				}

				// // updates the related import set with the loaded ontology
				// refreshedOntologies.put(mod, ont);
				// logger.debug("import set for: " + mod + " updated: " + importModalityMap.get(mod));
				//
				// // recursively load imported ontologies
				// logger.debug("refreshing the import situation after adding new ontology: " + ont);
				// refreshImportsForOntology(ont, mod);

				recoverImportsForOntology(conn, ont, mod);

				if (updateImportStatement) {
					// if updateImportStatement==true then it is an explicit request from the user so in this
					// way
					// we wait before all the cascade of imports has been resolved (which is invoked through
					// recoverOntology, having updateImportStatement==false), but then guess missing prefixes
					// just one time
					logger.debug("updating prefixes: " + baseURI);
					guessMissingPrefixes(conn);
				}

			} catch (MalformedURLException e) {
				throw new OntologyManagerException(e.getMessage() + " is not a valid URL");
			} catch (java.net.UnknownHostException e) {
				throw new OntologyManagerException(
						e.getMessage() + " is not resident on a host known by your DNS");
			} catch (IOException e) {
				throw new OntologyManagerException(e.getMessage() + " is not reachable");
			} catch (RDF4JException e) {
				throw new OntologyManagerException(e);
			}
		}
	}

	private void recoverImportsForOntology(RepositoryConnection conn, IRI ont, ImportModality mod)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		for (IRI importedOnt : QueryResults.stream(conn.getStatements(ont, OWL.IMPORTS, null, ont))
				.map(Statement::getObject).filter(IRI.class::isInstance).map(IRI.class::cast)
				.collect(toSet())) {
			recoverOntology(conn, importedOnt, mod);
		}
	}

	private void recoverOntology(RepositoryConnection conn, IRI importedOntology, ImportModality mod)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		logger.debug("recovering ontology: " + importedOntology);
		String baseURI = importedOntology.stringValue();
		String mirroredOntologyEntry = OntologiesMirror.getMirroredOntologyEntry(baseURI);
		// I could nest the following 4 conditions and make them more compact, but I prefer to keep 'em
		// separate as they are. See also comments on the importStatus variable

		// if a cached mirror file is available for the importedBaseURI,
		// but the ontology is NOT loaded in a named graph in the quad store
		// STRANGE SITUATION: it should never happen, because you add the import statement
		// only after you successfully managed to add the named graph to the quad store
		if (!availableNG(conn, importedOntology)) {
			if (mirroredOntologyEntry != null) {
				logger.debug("MIRROR & NO_NG for graph: " + importedOntology);
				addOntologyImportFromMirror(baseURI, mirroredOntologyEntry, mod, false);
			} else {
				logger.debug("NO_MIRROR & NO_NG for graph: " + importedOntology);
				addOntologyImportFromWeb(baseURI, baseURI, null, mod, false); // TODO if I save alternative
			}
		} else {
			importsStatusMap.putIfAbsent(conn.getValueFactory().createIRI(baseURI),
					new ImportStatus(ImportStatus.Values.NG, null));
		}
	}

	/**
	 * tells if named graph <code>ont</code> is present in the current ontology
	 * 
	 * @param ont
	 * @return
	 * @throws RDF4JException
	 */
	protected boolean availableNG(RepositoryConnection conn, IRI ont) throws RDF4JException {
		return QueryResults.stream(conn.getContextIDs()).anyMatch(ont::equals);
	}

	private void guessMissingPrefixes(RepositoryConnection conn) throws RDF4JException {
		// ARTNamespaceIterator namespaceIt = model.listNamespaces();
		// while (namespaceIt.streamOpen()) {
		// String ns = namespaceIt.getNext().getName();
		// guessMissingPrefix(ns);
		// }
		// namespaceIt.close();

		for (IRI userOnt : getOntologyImports(conn)) {
			String ns = ModelUtilities.createDefaultNamespaceFromBaseURI(userOnt.stringValue());
			guessMissingPrefix(conn, ns);
		}
	}

	private void guessMissingPrefix(RepositoryConnection conn, String ns) throws RDF4JException {
		logger.debug("checking namespace: " + ns + " for missing prefix");
		if (QueryResults.stream(conn.getNamespaces()).noneMatch(nsObj -> nsObj.getName().equals(ns))) {
			String guessedPrefix = ModelUtilities.guessPrefix(ns);
			conn.setNamespace(guessedPrefix, ns);
			logger.debug("namespace: " + ns
					+ " was missing from mapping table, guessed and added new prefix: " + guessedPrefix);
		}
	}

	/**
	 * gets the set of ontologies imported by the user
	 * 
	 * @return
	 * @throws ModelAccessException
	 */
	public Collection<IRI> getOntologyImports(RepositoryConnection conn) throws RDF4JException {
		return getDeclaredImports(conn, ImportModality.USER);
	}

	/**
	 * <p>
	 * retrieves the list of imports for the given {@link ImportModality}<br/>
	 * note that these are imports declared (it is not assured that they have been imported successfully)
	 * </p>
	 * <p>
	 * for example, <code>getImportSet(ImportModality.USER)</code> retrieves the set of all ontology imports
	 * set by the user
	 * </p>
	 * 
	 * @param conn
	 * 
	 * @param mod
	 * @return
	 * @throws ModelAccessException
	 */
	public Collection<IRI> getDeclaredImports(RepositoryConnection conn, ImportModality mod)
			throws RDF4JException {
		if (mod == ImportModality.USER) {
			IRI baseURI = conn.getValueFactory().createIRI(getBaseURI());
			return QueryResults.stream(conn.getStatements(baseURI, OWL.IMPORTS, null))
					.map(Statement::getObject).filter(IRI.class::isInstance).map(IRI.class::cast)
					.collect(toSet());
		} else
			return importModalityMap.get(mod);
	}

	@Override
	public void declareApplicationOntology(IRI ont, boolean ng, boolean ns) {
		if (ng) {
			applicationOntologiesNG.add(ont);
		}

		if (ns) {
			applicationOntologiesNamespace
					.add(ModelUtilities.createDefaultNamespaceFromBaseURI(ont.stringValue()));
		}
	}

	@Override
	public void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws ModelUpdateException, ImportManagementException, RDF4JException, IOException {
		checkImportFailed(baseURI);

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(new URL(baseURI), baseURI, null, conn.getValueFactory().createIRI(baseURI));
			getImportedOntology(fromWeb, baseURI, altURL, null, null);
		}
	}

	private void checkImportFailed(String baseURI) throws ImportManagementException {
		ImportStatus impStatus = importsStatusMap.get(baseURI);
		if (impStatus == null)
			throw new ImportManagementException("the import for: " + baseURI
					+ " should be stored inside the import status map, while it is not");
		if (impStatus.getValue() != ImportStatus.Values.FAILED)
			throw new ImportManagementException("the import for: " + baseURI
					+ " should be a FAILED import for this request to make sense, while it is not");
	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException, RDF4JException, MalformedURLException,
			IOException {
		checkImportFailed(baseURI);
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(new URL(baseURI), baseURI, null, conn.getValueFactory().createIRI(baseURI));
			getImportedOntology(fromWebToMirror, baseURI, altURL, null, mirFile);
		}
	}

	@Override
	public String getBaseURI() {
		return baseURI;
	}

	@Override
	public void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws ModelUpdateException, ImportManagementException, RDF4JException, IOException {
		checkImportFailed(baseURI);
		File inputFile = new File(fromLocalFilePath);
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(inputFile, baseURI,
					RDFFormat.matchFileName(inputFile.getName(), RDFParserRegistry.getInstance().getKeys())
							.orElseThrow(() -> new OntologyManagerException(
									"Could not match a parser for file name: " + inputFile.getName())),
					conn.getValueFactory().createIRI(baseURI));
			getImportedOntology(fromLocalFile, baseURI, null, fromLocalFilePath, mirFile);
		}
	}

	@Override
	public ImportStatus getImportStatus(String baseURI) {
		return importsStatusMap.get(SimpleValueFactory.getInstance().createIRI(baseURI));
	}

	@Override
	public Map<String, String> getNSPrefixMappings(boolean explicit) throws OntologyManagerException {
		try {
			if (explicit) {
				return nsPrefixMappings.getNSPrefixMappingTable();
			} else {
				return Repositories.get(repository, conn -> {
					return Namespaces.asMap(QueryResults.asSet(conn.getNamespaces()));
				});
			}
		} catch (RDF4JException e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappings)
			throws ModelUpdateException, ModelAccessException {
		this.nsPrefixMappings = nsPrefixMappings;
		// owlModel nsPrefixMapping regeneration from persistenceNSPrefixMapping
		Map<String, String> nsPrefixMapTable = nsPrefixMappings.getNSPrefixMappingTable();
		Set<Map.Entry<String, String>> mapEntries = nsPrefixMapTable.entrySet();
		for (Map.Entry<String, String> entry : mapEntries) {
			setNsPrefix(entry.getValue(), entry.getKey(), true);
		}

	}

	@Override
	public boolean isApplicationOntNamespace(String ns) {
		return applicationOntologiesNamespace.contains(ns);
	}

	@Override
	public boolean isSupportOntNamespace(String ns) {
		return supportOntologiesNamespace.contains(ns);
	}

	@Override
	public void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException, OntologyManagerException {
		ImportStatus impStatus = importsStatusMap.get(SimpleValueFactory.getInstance().createIRI(baseURI));
		OntFile tempFile;

		if (impStatus == null)
			throw new ImportManagementException("the import for: " + baseURI
					+ " should be stored inside the import status map, while it is not");
		if (impStatus.getValue() == ImportStatus.Values.FAILED) // this is different from the one in
			// "checkImportFailed"
			throw new ImportManagementException("the import for: " + baseURI
					+ " is a FAILED import, so you should not be allowed to mirror this ontology");

		tempFile = impStatus.getCacheFile();
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		logger.debug("saving data for Mirroring Ontology:\nbaseURI: " + baseURI + "\ntempFile: " + tempFile
				+ "\nmirFile: " + mirFile);

		getImportedOntology(toOntologyMirror, baseURI, baseURI, null, mirFile);
	}

	private void getImportedOntology(ImportMethod method, String baseURI, String altURL,
			String fromLocalFilePath, OntFile mirror_cacheFile)
			throws OntologyManagerException, RDF4JException {

		ImportStatus.Values statusBeingSet = ImportStatus.Values.UNASSIGNED;
		try {

			if (method == fromWebToMirror || method == toOntologyMirror) { // with previous RepositoryManager,
				// WEB used local tempFiles and
				// was also on the check here
				Utilities.downloadRDF(new URL(altURL), mirror_cacheFile.getAbsolutePath());
			} else if (method == fromLocalFile) // wrt previous RepositoryManager, toOntologyMirror has been
				// moved to previous check, because ontologies are downloaded
				// from their original site
				Utilities.copy(fromLocalFilePath, mirror_cacheFile.getAbsolutePath());

			if (method == fromWebToMirror || method == fromLocalFile || method == toOntologyMirror) {
				OntologiesMirror.addCachedOntologyEntry(baseURI, (MirroredOntologyFile) mirror_cacheFile);
				statusBeingSet = ImportStatus.Values.LOCAL;
			} else if (method == fromWeb) {
				statusBeingSet = ImportStatus.Values.WEB;
			}

			ImportStatus impStatus = importsStatusMap
					.get(SimpleValueFactory.getInstance().createIRI(baseURI));
			if (impStatus == null)
				importsStatusMap.put(SimpleValueFactory.getInstance().createIRI(baseURI),
						new ImportStatus(statusBeingSet, mirror_cacheFile));
			else
				impStatus.setValue(statusBeingSet, mirror_cacheFile);

		} catch (IOException e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void removeNSPrefixMapping(String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		try {
			nsPrefixMappings.removeNSPrefixMapping(namespace);
			Repositories.consume(repository, conn -> {
				Set<String> prefixesToDelete = QueryResults.stream(conn.getNamespaces())
						.filter(ns -> ns.getName().equals(namespace)).map(Namespace::getPrefix)
						.collect(toSet());
				for (String prefix : prefixesToDelete) {
					conn.removeNamespace(prefix);
				}
			});
		} catch (RDF4JException e) {
			throw new NSPrefixMappingUpdateException(e);
		}
	}

	@Override
	public void removeOntologyImport(String uriToBeRemoved)
			throws IOException, ModelUpdateException, ModelAccessException {
		removeOntologyImport(uriToBeRemoved, ImportModality.USER);
	}

	public void removeOntologyImport(String uriToBeRemoved, ImportModality mod)
			throws IOException, ModelUpdateException, ModelAccessException {
		try (RepositoryConnection conn = repository.getConnection()) {
			IRI ont = conn.getValueFactory().createIRI(uriToBeRemoved);

			Set<IRI> toBeRemovedOntologies = computeImportsClosure(conn, ont);
			logger.debug("transitive closure of imports to be removed: " + toBeRemovedOntologies);

			// removes the ontology from the import set
			logger.debug("removing import declaration for ontology: " + ont + ". Modality: " + mod);
			removeDeclaredImport(conn, ont, mod);

			Set<IRI> toBeSavedOntologies = computeImportsClosure(conn, ImportModality.getModalities());
			logger.debug("transitive closure of other imports: " + toBeSavedOntologies);

			toBeRemovedOntologies.removeAll(toBeSavedOntologies);
			logger.debug("computed difference between the two sets: " + toBeRemovedOntologies);

			// deletes ontology content and its entry from the input status only if this ontology is not
			// imported
			// by any other modality

			// we need to check this in advance because if it's equal to zero, then we cannot pass the empty
			// array to clearRDF (see below), which means "all named graphs"
			int numOntToBeRemoved = toBeRemovedOntologies.size();
			if (numOntToBeRemoved != 0) {
				// deletes the content of the imported ontologies
				logger.debug("clearing all RDF data associated to named graphs: " + toBeRemovedOntologies);
				conn.clear(toBeRemovedOntologies.toArray(new IRI[toBeRemovedOntologies.size()]));

				for (IRI remOnt : toBeRemovedOntologies) {
					// deletes the entry from the importStatusMap
					importsStatusMap.remove(remOnt);
				}
			}
		}
	}

	private Set<IRI> computeImportsClosure(RepositoryConnection conn, Set<ImportModality> modalities) {
		Set<IRI> importClosure = new HashSet<IRI>();
		logger.debug("computing global import closure on modalities: " + modalities);
		for (ImportModality otherMod : modalities) {
			logger.debug("checking declared " + otherMod + " imports for establishing import closure");
			for (IRI ont : getDeclaredImports(conn, otherMod)) {
				logger.debug("\timport: " + ont);
				importClosure.addAll(computeImportsClosure(conn, ont));
			}
		}
		return importClosure;
	}

	private void removeDeclaredImport(RepositoryConnection conn, IRI ont, ImportModality mod) {
		if (mod == ImportModality.USER) {
			IRI baseURIResource = conn.getValueFactory().createIRI(getBaseURI());
			conn.remove(baseURIResource, OWL.IMPORTS, ont, baseURIResource);
		} else {
			importModalityMap.get(mod).remove(ont);
		}
	}

	private Set<IRI> computeImportsClosure(RepositoryConnection conn, IRI ont) {
		logger.debug("computing imports closure for import: " + ont);
		TupleQuery query = conn
				.prepareTupleQuery("SELECT DISTINCT ?x {?ont <http://www.w3.org/2002/07/owl#imports>* ?x }");
		query.setBinding("ont", ont);
		return QueryResults.stream(query.evaluate()).map(bs -> bs.getValue("x")).filter(IRI.class::isInstance)
				.map(IRI.class::cast).collect(toSet());
	}

	@Override
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * a wrapper around OWLART {@link PrefixMapping} with an additional <code>overwrite</code> parameter
	 * which, if false, makes the method ignore calls if the namespace-prefix mapping already exists. If true,
	 * it still does not overwrite if the old and new values are the same
	 * 
	 * @param namespace
	 * @param prefix
	 * @param overwrite
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 */
	private void setNsPrefix(String namespace, String prefix, boolean overwrite)
			throws ModelAccessException, ModelUpdateException {
		Repositories.consume(repository, conn -> {
			String oldPrefix = QueryResults.stream(conn.getNamespaces())
					.filter(ns -> ns.getName().equals(namespace)).findAny().map(Namespace::getPrefix)
					.orElse(null);
			if ((oldPrefix == null) || (overwrite && !oldPrefix.equals(prefix))) {
				conn.setNamespace(prefix, namespace);
			}
		});
	}

	// TODO Pay attention this is not being invoked by any method! check metadata ones!
	public void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		Repositories.consume(repository, conn -> conn.setNamespace(prefix, namespace));
		nsPrefixMappings.setNSPrefixMapping(namespace, prefix);
	}

	@Override
	public void startOntModel(String baseURI, File repoDir, RepositoryConfig supportRepoConfig)
			throws OntologyManagerException {
		try {
			SailRepositoryFactory repoFactory = new SailRepositoryFactory();
			repository = repoFactory.getRepository(supportRepoConfig.getRepositoryImplConfig());
			repository.setDataDir(repoDir);
			this.baseURI = baseURI;
			repository.initialize();

			importsStatusMap = new HashMap<>();

			try (RepositoryConnection conn = repository.getConnection()) {
				for (ImportModality mod : Arrays.asList(ImportModality.SUPPORT, ImportModality.APPLICATION,
						ImportModality.USER)) {
					logger.debug("refreshing " + mod + " imports:");
					for (IRI ont : getDeclaredImports(conn, mod)) {
						logger.debug("\timport: " + ont);
						recoverOntology(conn, ont, mod);
					}
				}
				guessMissingPrefixes(conn);
			}
		} catch (MalformedURLException | RDF4JException e) {
			throw new OntologyManagerException(e);
		}
	}

}
