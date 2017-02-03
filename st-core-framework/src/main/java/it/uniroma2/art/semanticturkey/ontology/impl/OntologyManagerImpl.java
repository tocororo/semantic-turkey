package it.uniroma2.art.semanticturkey.ontology.impl;

import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromLocalFile;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromOntologyMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWeb;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWebToMirror;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.query.QueryResults;
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
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportMethod;
import it.uniroma2.art.semanticturkey.ontology.ImportModality;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;

public class OntologyManagerImpl implements OntologyManager {

	private static final Logger logger = LoggerFactory.getLogger(OntologyManagerImpl.class);

	private Repository repository;
	private NSPrefixMappings nsPrefixMappings;
	private volatile String baseURI;

	private Map<IRI, ImportStatus> importsStatusMap;

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
				conn.add(physicalMirrorFile, baseURI,
						RDFFormat
								.matchFileName(physicalMirrorFile.getName(),
										RDFParserRegistry.getInstance()
												.getKeys())
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
			RDFFormat rdfFormat) throws MalformedURLException, ModelUpdateException, RDF4JException,
					OntologyManagerException {
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

		// try to download the ontology
		notifiedAddedOntologyImport(fromWebToMirror, baseURI, sourceURL, mirFile, modality,
				updateImportStatement);

		// if the download was achieved, import the ontology in the model
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


	}

	private void notifiedAddedOntologyImport(ImportMethod fromlocalfile, String baseURI2,
			String fromLocalFilePath, MirroredOntologyFile mirFile, ImportModality modality,
			boolean updateImportStatement) {

	}

	@Override
	public void declareApplicationOntology(IRI ont, boolean ng, boolean ns) {
	}

	@Override
	public void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getBaseURI() {
		return baseURI;
	}

	@Override
	public void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public ImportStatus getImportStatus(String baseURI) {
		return new ImportStatus(ImportStatus.Values.NG, null); // TODO
	}

	@Override
	public Map<String, String> getNSPrefixMappings(boolean explicit) throws ModelAccessException {
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
		return false;
	}

	@Override
	public boolean isSupportOntNamespace(String ns) {
		return false;
	}

	@Override
	public void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException {
		// TODO Auto-generated method stub

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
	public void removeOntologyImport(String uri)
			throws IOException, ModelUpdateException, ModelAccessException {
		// TODO Auto-generated method stub

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

		} catch (RDF4JException e) {
			throw new OntologyManagerException(e);
		}
	}

}
