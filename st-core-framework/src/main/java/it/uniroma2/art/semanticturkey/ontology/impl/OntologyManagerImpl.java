package it.uniroma2.art.semanticturkey.ontology.impl;

import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromLocalFile;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromOntologyMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWeb;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWebToMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.toOntologyMirror;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportAndOntologyMismatchException;
import it.uniroma2.art.semanticturkey.ontology.ImportMethod;
import it.uniroma2.art.semanticturkey.ontology.ImportModality;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus.Values;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.ontology.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.utilities.IterationSpy;
import it.uniroma2.art.semanticturkey.utilities.OptionalUtils;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * Native implementation of {@link OntologyManager} for RDF4J.
 * 
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OntologyManagerImpl implements OntologyManager {

	private static final Logger logger = LoggerFactory.getLogger(OntologyManagerImpl.class);

	private volatile Repository repository;
	private volatile NSPrefixMappings nsPrefixMappings;
	private volatile String baseURI;

	private final Map<ImportModality, Set<IRI>> importModalityMap;

	private final Set<IRI> applicationOntologiesNG;
	private final Set<String> applicationOntologiesNamespace;
	private final Set<IRI> supportOntologiesNG;
	private final Set<String> supportOntologiesNamespace;

	private boolean validationEnabled;

	public OntologyManagerImpl(Repository repository, boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
		// initializes user, application and support ontology sets
		applicationOntologiesNG = new HashSet<>();
		applicationOntologiesNamespace = new ConcurrentSkipListSet<>();
		supportOntologiesNG = new HashSet<>();
		supportOntologiesNamespace = new ConcurrentSkipListSet<>();

		importModalityMap = new HashMap<>();
		importModalityMap.put(ImportModality.APPLICATION, applicationOntologiesNG);
		importModalityMap.put(ImportModality.SUPPORT, supportOntologiesNG);

		this.repository = repository;
	}

	@Override
	public String getBaseURI() {
		return baseURI;
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
	public void addOntologyImportFromLocalFile(RepositoryConnection conn, String baseURI,
			String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromLocalFile(baseURI, fromLocalFilePath, toLocalFile, ImportModality.USER, true,
					conn, transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromLocalFile(e.getRealOnt().stringValue(), fromLocalFilePath, toLocalFile,
					ImportModality.USER, true, conn, transitiveImportAllowance, failedImports,
					new HashSet<>());
		}
	}

	public void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			ImportModality modality, boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {

		logger.debug("adding: " + baseURI + " from localfile: " + fromLocalFilePath + " to Mirror: "
				+ toLocalFile);
		File inputFile = new File(fromLocalFilePath);
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		try {
			checkOntologyNotExplicitlyImported(conn, baseURI);

			RDFFormat rdfFormat = Rio.getParserFormatForFileName(inputFile.getName())
					.orElseThrow(() -> new OntologyManagerException(
							"Could not match a parser for file name: " + inputFile.getName()));
			RDFParser parser = RDFParserRegistry.getInstance().get(rdfFormat).map(RDFParserFactory::getParser)
					.orElseThrow(() -> new OntologyManagerException("Unspported RDF Data Format"));
			Model capturedOntologyMetadata = new LinkedHashModel();
			InputStream in = new FileInputStream(inputFile);
			IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
					baseURI, in, parser, capturedOntologyMetadata);

			conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

			notifiedAddedOntologyImport(fromLocalFile, baseURI, fromLocalFilePath, mirFile, modality,
					updateImportStatement, conn, capturedOntologyMetadata, transitiveImportAllowance,
					failedImports, importedOntologies);
		} catch (OntologyManagerException e) {
			throw e;
		} catch (Exception e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void addOntologyImportFromMirror(RepositoryConnection conn, String baseURI, String mirFileString,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromMirror(baseURI, mirFileString, ImportModality.USER, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromMirror(e.getRealOnt().stringValue(), mirFileString, ImportModality.USER,
					true, conn, transitiveImportAllowance, failedImports, new HashSet<>());
		}
	}

	public void addOntologyImportFromMirror(String baseURI, String mirFileString, ImportModality modality,
			boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(mirFileString);
		File physicalMirrorFile = new File(mirFile.getAbsolutePath());
		try {
			checkOntologyNotExplicitlyImported(conn, baseURI);

			RDFFormat rdfFormat = Rio.getParserFormatForFileName(physicalMirrorFile.getName())
					.orElseThrow(() -> new OntologyManagerException(
							"Could not match a parser for file name: " + physicalMirrorFile.getName()));
			RDFParser parser = RDFParserRegistry.getInstance().get(rdfFormat).map(RDFParserFactory::getParser)
					.orElseThrow(() -> new OntologyManagerException("Unspported RDF Data Format"));
			Model capturedOntologyMetadata = new LinkedHashModel();
			InputStream in = new FileInputStream(physicalMirrorFile);
			IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
					baseURI, in, parser, capturedOntologyMetadata);

			conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

			notifiedAddedOntologyImport(fromOntologyMirror, baseURI, null, mirFile, modality,
					updateImportStatement, conn, capturedOntologyMetadata, transitiveImportAllowance,
					failedImports, importedOntologies);
		} catch (OntologyManagerException e) {
			throw e;
		} catch (Exception e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void addOntologyImportFromWeb(RepositoryConnection conn, String baseURI, String sourceURL,
			RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromWeb(baseURI, sourceURL, rdfFormat, ImportModality.USER, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromWeb(e.getRealOnt().stringValue(), sourceURL, rdfFormat, ImportModality.USER,
					true, conn, transitiveImportAllowance, failedImports, new HashSet<>());
		}
	}

	public void addOntologyImportFromWeb(String baseURI, String sourceURL, RDFFormat rdfFormat,
			ImportModality modality, boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {
		try {
			checkOntologyNotExplicitlyImported(conn, baseURI);

			if (baseURI == null) {
				baseURI = sourceURL;
			}

			Model caputeredImports = new LinkedHashModel();
			IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createURLImportsSpyingIteration(
					baseURI, sourceURL, rdfFormat, caputeredImports);
			conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));
			notifiedAddedOntologyImport(fromWeb, baseURI, sourceURL, null, modality, updateImportStatement,
					conn, caputeredImports, transitiveImportAllowance, failedImports, importedOntologies);
		} catch (OntologyManagerException e) {
			throw e;
		} catch (Exception e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void addOntologyImportFromWebToMirror(RepositoryConnection conn, String baseURI, String sourceURL,
			String toLocalFile, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromWebToMirror(baseURI, sourceURL, toLocalFile, rdfFormat, ImportModality.USER,
					true, conn, transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromWebToMirror(e.getRealOnt().stringValue(), sourceURL, toLocalFile, rdfFormat,
					ImportModality.USER, true, conn, transitiveImportAllowance, failedImports,
					new HashSet<>());
		}
	}

	public void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat, ImportModality modality, boolean updateImportStatement,
			RepositoryConnection conn, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports, Set<IRI> importedOntologies) throws OntologyManagerException {

		// first of all, try to download the ontology in the mirror file in the format specified by the user
		// (or inferred from the extention of the file). Then, if this download was done without any problem,
		// import the ontology

		MirroredOntologyFile mirFile = getMirFile(toLocalFile, rdfFormat);

		try {
			checkOntologyNotExplicitlyImported(conn, baseURI);

			Model capturedOntologyMetadata = new LinkedHashModel();
			// try to download the ontology
			notifiedAddedOntologyImport(fromWebToMirror, baseURI, sourceURL, mirFile, modality,
					updateImportStatement, conn, capturedOntologyMetadata, transitiveImportAllowance,
					failedImports, importedOntologies);

			// if the download was achieved, import the ontology in the model

			IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createURLImportsSpyingIteration(
					baseURI, sourceURL, rdfFormat, capturedOntologyMetadata);
			conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

			// this time sends a notification with the spied ontology imports (but change the import method
			// so that it won't be created another mirror entry)

			notifiedAddedOntologyImport(ImportMethod.fromWeb, baseURI, sourceURL, mirFile, modality,
					updateImportStatement, conn, capturedOntologyMetadata, transitiveImportAllowance,
					failedImports, importedOntologies);

		} catch (OntologyManagerException e) {
			throw e;
		} catch (Exception e) {
			throw new OntologyManagerException(e);
		}
	}

	protected MirroredOntologyFile getMirFile(String toLocalFile, RDFFormat rdfFormat) {
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		RDFFormat guessedFormat = RDFFormat
				.matchFileName(mirFile.getLocalName(), RDFParserRegistry.getInstance().getKeys())
				.orElse(null);

		if (rdfFormat != null) {
			// check it the input rdfFormat is compliant with the file extension
			if (guessedFormat == null || rdfFormat != guessedFormat) {
				// change the file extension according to the input RDFFormat
				String newLocalFile = toLocalFile + "." + rdfFormat.getDefaultFileExtension();
				mirFile = new MirroredOntologyFile(newLocalFile);
			}
		} else {
			if (guessedFormat == null) {
				String newLocalFile = toLocalFile + "." + RDFFormat.RDFXML.getDefaultFileExtension();
				mirFile = new MirroredOntologyFile(newLocalFile);
			}
		}
		return mirFile;
	}

	protected IterationSpy<Statement, QueryEvaluationException> createURLImportsSpyingIteration(
			String baseURI, String sourceURL, RDFFormat rdfFormat, Model caputeredImports)
			throws MalformedURLException, IOException, UnsupportedRDFormatException,
			OntologyManagerException {
		URL url = new URL(sourceURL);
		URLConnection con = url.openConnection();

		// Set appropriate Accept headers
		if (rdfFormat != null) {
			for (String mimeType : rdfFormat.getMIMETypes()) {
				con.addRequestProperty("Accept", mimeType);
			}
		} else {
			Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
			List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, true, null);
			for (String acceptParam : acceptParams) {
				con.addRequestProperty("Accept", acceptParam);
			}
		}

		InputStream in = con.getInputStream();

		if (rdfFormat == null) {
			// Try to determine the data's MIME type
			String mimeType = con.getContentType();
			int semiColonIdx = mimeType.indexOf(';');
			if (semiColonIdx >= 0) {
				mimeType = mimeType.substring(0, semiColonIdx).trim();
			}
			rdfFormat = OptionalUtils
					.firstPresent(Rio.getParserFormatForMIMEType(mimeType),
							Rio.getParserFormatForFileName(url.getPath()))
					.orElseThrow(Rio.unsupportedFormat(mimeType));
		}

		RDFParser parser = RDFParserRegistry.getInstance().get(rdfFormat).map(RDFParserFactory::getParser)
				.orElseThrow(() -> new OntologyManagerException("Unspported RDF Data Format"));

		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
				baseURI, in, parser, caputeredImports);
		return spyConcurrentIter;
	}

	protected IterationSpy<Statement, QueryEvaluationException> createImportsSpyingIteration(String baseURI,
			InputStream in, RDFParser parser, Model caputeredImports) {
		BackgroundGraphResult concurrentIter = new BackgroundGraphResult(parser, in, null, baseURI);
		new Thread(concurrentIter).start();
		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = new IterationSpy<>(
				concurrentIter,
				stmt -> stmt.getPredicate().equals(OWL.IMPORTS)
						|| (stmt.getPredicate().equals(RDF.TYPE) && stmt.getObject().equals(OWL.ONTOLOGY)),
				stmt -> caputeredImports.add(stmt));
		return spyConcurrentIter;
	}

	private void notifiedAddedOntologyImport(ImportMethod method, String baseURI, String sourcePath,
			OntFile localFile, ImportModality mod, boolean updateImportStatement, RepositoryConnection conn,
			Model capturedOntologyMetadata, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports, Set<IRI> importedOntologies) throws OntologyManagerException {
		logger.debug("notifying added ontology import with method: " + method + " with baseuri: " + baseURI
				+ " sourcePath: " + sourcePath + " localFile: " + localFile + " importModality: " + mod
				+ ", thought for updating the import status: " + updateImportStatement);
		try {
			IRI ont = conn.getValueFactory().createIRI(baseURI);

			// ***************************
			// Checking that the imported ontology has exactly the same URI used to import it. This may
			// happen when the given URI is a successful URL for retrieving the ontology but it is not the URI
			// of the ontology

			Set<IRI> declOnts = Models
					.subjectIRIs(capturedOntologyMetadata.filter(null, RDF.TYPE, OWL.ONTOLOGY));
			// the import ont does not contain the declaration of itself as an ont and it contains at
			// least one declaration (probably its own one)
			if (!declOnts.contains(ont) && !declOnts.isEmpty()) {
				// extracting the real baseURI of the imported ontology
				IRI realURI = declOnts.iterator().next();
				// checking that the realURI has not already been imported, by checking the existence of
				// its NG in the current data
				if (getImportStatus(conn, realURI.stringValue()).getValue() != ImportStatus.Values.FAILED) {
					// if realURI is already imported, then remove the data imported in the wrong URI
					conn.clear(ValidationUtilities.getAddGraphIfValidatonEnabled(validationEnabled, ont));
					// and throw an exception
					throw new OntologyManagerException("the real URI for the imported ontology: " + ont
							+ " is actually: " + realURI + " which, however, has already been imported");
				} else {
					// Only if updateImportStatement is true, renamed the ontology graph
					// Otherwise, the inconsistency between owl:imports and the named graph would be
					// interpreted as a failed import
					if (updateImportStatement) {
						// we have to move imported data to the correct baseuri
						conn.clear(ValidationUtilities.getAddGraphIfValidatonEnabled(validationEnabled, ont));

						// Throws a mismatch action so that the caller can retry the import with the real URI
						throw new ImportAndOntologyMismatchException(ont, realURI);
					}
				}
			}

			// ***************************

			if (method == fromWebToMirror) {
				Utilities.downloadRDF(new URL(sourcePath), localFile.getAbsolutePath());
			} else if (method == fromLocalFile)
				Utilities.copy(sourcePath, localFile.getAbsolutePath());

			if (method == fromWebToMirror || method == fromLocalFile)
				OntologiesMirror.addCachedOntologyEntry(baseURI, (MirroredOntologyFile) localFile);

			// if the import is explicitly asked by the user, then the import statement is explicitly
			// added to
			// the ontology
			if (updateImportStatement) {
				logger.debug("adding import statement for uri: " + baseURI);
				IRI projBaseURI = conn.getValueFactory().createIRI(getBaseURI());
				conn.add(projBaseURI, OWL.IMPORTS, conn.getValueFactory().createIRI(baseURI), projBaseURI);
			}

			// updates the related import set with the loaded ontology
			// refreshedOntologies.put(mod, ont);
			// logger.debug("import set for: " + mod + " updated: " + importModalityMap.get(mod));
			//
			// // recursively load imported ontologies
			// logger.debug("refreshing the import situation after adding new ontology: " + ont);
			// refreshImportsForOntology(ont, mod);

			importedOntologies.add(ont);

			recoverImportsForOntology(conn, ont, mod, capturedOntologyMetadata, transitiveImportAllowance,
					failedImports, importedOntologies);

			if (updateImportStatement) {
				// if updateImportStatement==true then it is an explicit request from the user so in this
				// way
				// we wait before all the cascade of imports has been resolved (which is invoked through
				// recoverOntology, having updateImportStatement==false), but then guess missing prefixes
				// just one time
				logger.debug("updating prefixes: " + baseURI);
				guessMissingPrefixes(conn, importedOntologies);
			}

		} catch (MalformedURLException e) {
			throw new OntologyManagerException(e.getMessage() + " is not a valid URL", e);
		} catch (java.net.UnknownHostException e) {
			throw new OntologyManagerException(
					e.getMessage() + " is not resident on a host known by your DNS", e);
		} catch (IOException e) {
			throw new OntologyManagerException(e.getMessage() + " is not reachable", e);
		} catch (RDF4JException e) {
			throw new OntologyManagerException(e);
		}
	}

	private void recoverImportsForOntology(RepositoryConnection conn, IRI ont, ImportModality mod,
			Model capturedOntologyMetadata, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports, Set<IRI> importedOntologies)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		for (IRI importedOnt : Models.objectIRIs(capturedOntologyMetadata.filter(ont, OWL.IMPORTS, null))) {
			recoverOntology(conn, importedOnt, mod, transitiveImportAllowance, failedImports,
					importedOntologies);
		}
	}

	private void recoverOntology(RepositoryConnection conn, IRI importedOntology, ImportModality mod,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies)
			throws RDF4JException, MalformedURLException, OntologyManagerException {
		logger.debug("recovering ontology: " + importedOntology);
		String baseURI = importedOntology.stringValue();
		// String mirroredOntologyEntry = OntologiesMirror.getMirroredOntologyEntry(baseURI);
		// // I could nest the following 4 conditions and make them more compact, but I prefer to keep 'em
		// // separate as they are. See also comments on the importStatus variable
		//
		// // if a cached mirror file is available for the importedBaseURI,
		// // but the ontology is NOT loaded in a named graph in the quad store
		// // STRANGE SITUATION: it should never happen, because you add the import statement
		// // only after you successfully managed to add the named graph to the quad store
		// if (!availableNG(conn, importedOntology)) {
		// if (mirroredOntologyEntry != null) {
		// logger.debug("MIRROR & NO_NG for graph: " + importedOntology);
		// addOntologyImportFromMirror(baseURI, mirroredOntologyEntry, mod, false);
		// } else {
		// logger.debug("NO_MIRROR & NO_NG for graph: " + importedOntology);
		// }
		// } else {
		// importsStatusMap.putIfAbsent(conn.getValueFactory().createIRI(baseURI),
		// new ImportStatus(ImportStatus.Values.NG, null));
		// }

		Values importStatus = getImportStatus(conn, baseURI, importedOntologies).getValue();

		boolean ontologyImported = false;

		if (importStatus == Values.FAILED) {
			for (ImportMethod method : transitiveImportAllowance.getAllowedMethods()) {
				try {
					if (method == ImportMethod.fromWeb) {
						addOntologyImportFromWeb(baseURI, baseURI, null, mod, false, conn,
								transitiveImportAllowance, failedImports, importedOntologies);
					} else if (method == ImportMethod.fromOntologyMirror) {
						String mirrorEntry = OntologiesMirror.getMirroredOntologyEntry(baseURI);
						if (mirrorEntry != null) {
							addOntologyImportFromMirror(baseURI, mirrorEntry, mod, false, conn,
									transitiveImportAllowance, failedImports, importedOntologies);
						}
					}

					ontologyImported = true;
					break;
				} catch (OntologyManagerException e) {
					e.printStackTrace();
					// Swallow exception, and tries with the next method
				}
			}
		} else if (importStatus == Values.OK) {
			ontologyImported = true;
		}
		if (ontologyImported) {
			failedImports.remove(conn.getValueFactory().createIRI(baseURI));
		} else {
			failedImports.add(conn.getValueFactory().createIRI(baseURI));
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

	private void guessMissingPrefixes(RepositoryConnection conn, Set<IRI> importedOntologies)
			throws RDF4JException {
		// ARTNamespaceIterator namespaceIt = model.listNamespaces();
		// while (namespaceIt.streamOpen()) {
		// String ns = namespaceIt.getNext().getName();
		// guessMissingPrefix(ns);
		// }
		// namespaceIt.close();

		for (IRI userOnt : importedOntologies) {
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

	@Override
	public void removeOntologyImport(RepositoryConnection conn, String uriToBeRemoved) throws IOException {
		removeOntologyImport(conn, uriToBeRemoved, ImportModality.USER);
	}

	public void removeOntologyImport(RepositoryConnection conn, String uriToBeRemoved, ImportModality mod)
			throws IOException {
		IRI ont = conn.getValueFactory().createIRI(uriToBeRemoved);

		if (getImportStatus(conn, uriToBeRemoved).getValue() != Values.OK) {
			throw new OntologyManagerException(
					"Could not remove import to ontology which is not persistently imported: "
							+ uriToBeRemoved);
		}

		Set<IRI> toBeRemovedOntologies = computeImportsClosure(conn, ont);
		logger.debug("transitive closure of imports to be removed: " + toBeRemovedOntologies);

		// removes the ontology from the import set
		logger.debug("removing import declaration for ontology: " + ont + ". Modality: " + mod);
		removeDeclaredImport(conn, ont, mod);

		Set<IRI> toBeSavedOntologies = computeImportsClosure(conn, ImportModality.getModalities(), ont);
		logger.debug("transitive closure of other imports: " + toBeSavedOntologies);

		toBeRemovedOntologies.removeAll(toBeSavedOntologies);
		logger.debug("computed difference between the two sets: " + toBeRemovedOntologies);

		// deletes ontology content and its entry from the input status only if this ontology is not
		// imported
		// by any other modality

		IRI baseURIasIRI = SimpleValueFactory.getInstance().createIRI(getBaseURI());
		Statement removedImport = SimpleValueFactory.getInstance().createStatement(baseURIasIRI, OWL.IMPORTS,
				ont, baseURIasIRI);
		// Here we check that an ontology to be saved is not exclusively imported by staged ontologies
		for (IRI ontToBeSaved : toBeSavedOntologies) {
			Model liveImportsForOnt = new LinkedHashModel(
					QueryResults.stream(conn.getStatements(null, OWL.IMPORTS, ontToBeSaved, false))
							.filter(stmt -> !toBeRemovedOntologies.contains(stmt.getSubject()))
							.filter(stmt -> !removedImport.equals(stmt)).collect(Collectors.toList()));

			if (liveImportsForOnt.stream()
					.filter(stmt -> !VALIDATION.isAddGraph(stmt.getContext())
							&& !VALIDATION.isRemoveGraph(stmt.getContext()))
					.allMatch(stmt -> liveImportsForOnt.contains(stmt.getSubject(), stmt.getPredicate(),
							stmt.getObject(), VALIDATION.stagingAddGraph(stmt.getContext()),
							VALIDATION.stagingRemoveGraph(stmt.getContext())))) {
				throw new OntologyManagerException(
						"Could not delete ontology import, because that operation would produce graphs only kept live by staged graphs");
			}
		}

		// we need to check this in advance because if it's equal to zero, then we cannot pass the empty
		// array to clearRDF (see below), which means "all named graphs"
		int numOntToBeRemoved = toBeRemovedOntologies.size();
		if (numOntToBeRemoved != 0) {
			// deletes the content of the imported ontologies
			logger.debug("clearing all RDF data associated to named graphs: " + toBeRemovedOntologies);
			conn.clear(toBeRemovedOntologies.toArray(new IRI[toBeRemovedOntologies.size()]));
		}
	}

	@Override
	public ImportStatus getImportStatus(RepositoryConnection conn, String baseURI) {
		return getImportStatus(conn, baseURI, Collections.emptySet());
	}

	private ImportStatus getImportStatus(RepositoryConnection conn, String baseURI,
			Set<IRI> importedOntologies) {
		IRI ont = SimpleValueFactory.getInstance().createIRI(baseURI);

		if (validationEnabled) {
			if (conn.hasStatement(null, null, null, false,
					ValidationUtilities.getAddGraphIfValidatonEnabled(true, ont))) {
				return new ImportStatus(ImportStatus.Values.STAGED_ADDITION, null);
			}

			if (conn.hasStatement(null, null, null, false,
					ValidationUtilities.getRemoveGraphIfValidatonEnabled(true, ont))) {
				return new ImportStatus(ImportStatus.Values.STAGED_REMOVAL, null);
			}
		}

		if (importedOntologies.contains(ont) || QueryResults.asSet(conn.getContextIDs()).contains(ont)) {
			return new ImportStatus(ImportStatus.Values.OK, null);
		}

		return new ImportStatus(ImportStatus.Values.FAILED, null);
	}

	private void checkOntologyNotExplicitlyImported(RepositoryConnection conn, String baseURI)
			throws OntologyManagerException {
		if (getDeclaredImports(conn, ImportModality.USER, false)
				.contains(SimpleValueFactory.getInstance().createIRI(baseURI))) {
			throw new OntologyManagerException("Ontology already imported explicitly: " + baseURI);
		}
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
	 * @param mod
	 * @param excludeStagedRemovals
	 * @return
	 */
	public Collection<IRI> getDeclaredImports(RepositoryConnection conn, ImportModality mod,
			boolean excludeStagedRemovals) throws RDF4JException {
		if (mod == ImportModality.USER) {
			IRI baseURI = conn.getValueFactory().createIRI(getBaseURI());
			Model importStatements = QueryResults.asModel(conn.getStatements(baseURI, OWL.IMPORTS, null));

			Set<IRI> importedOntologies = Models.objectIRIs(importStatements);

			if (excludeStagedRemovals) {
				Set<IRI> stagedRemovals = Models.objectIRIs(
						importStatements.filter(null, null, null, VALIDATION.stagingRemoveGraph(baseURI)));
				importedOntologies.removeAll(stagedRemovals);
			}

			return importedOntologies;
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

	private void checkImportFailed(RepositoryConnection conn, String baseURI)
			throws ImportManagementException, RepositoryException {

		if (getImportStatus(conn, baseURI, Collections.emptySet()).getValue() != ImportStatus.Values.FAILED) {
			throw new ImportManagementException("the import for: " + baseURI
					+ " should be a FAILED import for this request to make sense, while it is not");
		}
	}

	@Override
	public void downloadImportedOntologyFromWeb(RepositoryConnection conn, String baseURI, String altURL,
			RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws ImportManagementException, RDF4JException, IOException {
		checkImportFailed(conn, baseURI);

		getImportedOntology(fromWeb, baseURI, altURL, null, null);

		// TODO: check whether ImportModality.USER is correct

		Model capturedOntologyMetadata = new LinkedHashModel();
		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createURLImportsSpyingIteration(
				baseURI, altURL, rdfFormat, capturedOntologyMetadata);
		conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

		notifiedAddedOntologyImport(fromWeb, baseURI, altURL, null, ImportModality.USER, false, conn,
				capturedOntologyMetadata, transitiveImportAllowance, failedImports, new HashSet<>());
	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(RepositoryConnection conn, String baseURI,
			String altURL, String toLocalFile, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws ImportManagementException, RDF4JException, MalformedURLException, IOException {
		
		MirroredOntologyFile mirFile = getMirFile(toLocalFile, rdfFormat);

		checkImportFailed(conn, baseURI);

		Model capturedOntologyMetadata = new LinkedHashModel();

		// TODO: check whether ImportModality.USER is correct

		// try to download the ontology
		notifiedAddedOntologyImport(fromWebToMirror, baseURI, altURL, mirFile, ImportModality.USER, false,
				conn, capturedOntologyMetadata, transitiveImportAllowance, failedImports, new HashSet<>());

		// if the download was achieved, import the ontology in the model

		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createURLImportsSpyingIteration(
				baseURI, altURL, rdfFormat, capturedOntologyMetadata);
		conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

		// this time sends a notification with the spied ontology imports (but change the import method
		// so that it won't be created another mirror entry)

		notifiedAddedOntologyImport(fromWebToMirror, baseURI, altURL, mirFile, ImportModality.USER, false,
				conn, capturedOntologyMetadata, transitiveImportAllowance, failedImports, new HashSet<>());
	}

	@Override
	public void getImportedOntologyFromLocalFile(RepositoryConnection conn, String baseURI,
			String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws ImportManagementException, RDF4JException, IOException {
		File inputFile = new File(fromLocalFilePath);
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);

		checkImportFailed(conn, baseURI);

		RDFFormat rdfFormat = Rio.getParserFormatForFileName(inputFile.getName())
				.orElseThrow(() -> new OntologyManagerException(
						"Could not match a parser for file name: " + inputFile.getName()));
		RDFParser parser = RDFParserRegistry.getInstance().get(rdfFormat).map(RDFParserFactory::getParser)
				.orElseThrow(() -> new OntologyManagerException("Unspported RDF Data Format"));
		Model capturedOntologyMetadata = new LinkedHashModel();
		InputStream in = new FileInputStream(inputFile);
		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
				baseURI, in, parser, capturedOntologyMetadata);

		conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI));

		Model caputedImports = new LinkedHashModel();
		notifiedAddedOntologyImport(fromLocalFile, baseURI, fromLocalFilePath, mirFile, ImportModality.USER,
				false, conn, caputedImports, transitiveImportAllowance, failedImports, new HashSet<>());
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
			throws ImportManagementException, OntologyManagerException {
		try (RepositoryConnection conn = repository.getConnection()) {
			if (!conn.hasStatement(null, null, null, false, conn.getValueFactory().createIRI(baseURI))) {
				throw new OntologyManagerException("Could not mirror an ontology not loaded: " + baseURI);
			}
		}
		MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);
		logger.debug("saving data for Mirroring Ontology:\nbaseURI: " + baseURI + "\ntempFile: " + mirFile);

		getImportedOntology(toOntologyMirror, baseURI, baseURI, null, mirFile);
	}

	@Override
	public void clearData() throws RDF4JException {
		logger.debug("clearing RDF:\nontology dir = " + Resources.getSemTurkeyDataDir());
		try {
			logger.debug("clearing namespace prefixes");
			if (nsPrefixMappings != null) {// this check is only needed because of the ugly
											// startOntologyData()
				// implementation in SaveToStoreProject.java which activates clearRepository to clean all
				// eventually left persistence files when loading a save-to-store project
				// in that case, nsPrefixMappings is still not loaded in the OntManager because it will be
				// initialized once the loadTriples() method invocation will be concluded
				nsPrefixMappings.clearNSPrefixMappings();
			}
		} catch (NSPrefixMappingUpdateException e) {
			throw new RepositoryException(e);
		}
		if (repository == null) {
			logger.debug("owlModel not active: no need to clear RDF data");
		} else {
			try (RepositoryConnection conn = repository.getConnection()) {
				conn.clear();
			}
			logger.debug("RDF Data cleared");
		}
	}

	private void getImportedOntology(ImportMethod method, String baseURI, String altURL,
			String fromLocalFilePath, OntFile mirror_cacheFile)
			throws OntologyManagerException, RDF4JException {

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
			}
		} catch (IOException e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException {
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

	private Set<IRI> computeImportsClosure(RepositoryConnection conn, Set<ImportModality> modalities,
			@Nullable IRI justRemovedOnt) {
		Set<IRI> importClosure = new HashSet<IRI>();
		logger.debug("computing global import closure on modalities: " + modalities);
		for (ImportModality otherMod : modalities) {
			logger.debug("checking declared " + otherMod + " imports for establishing import closure");
			for (IRI ont : getDeclaredImports(conn, otherMod, true)) {
				if (Objects.equals(justRemovedOnt, ont))
					continue;
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
		GraphQuery query = conn
				.prepareGraphQuery("CONSTRUCT WHERE {?ont <http://www.w3.org/2002/07/owl#imports>* ?x }");
		query.setBinding("ont", ont);
		return Models.objectIRIs(QueryResults.asModel(query.evaluate()));
	}

	@Override
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * a wrapper around the RDF4J's prefix mapping with an additional <code>overwrite</code> parameter which,
	 * if false, makes the method ignore calls if the namespace-prefix mapping already exists. If true, it
	 * still does not overwrite if the old and new values are the same
	 * 
	 * @param namespace
	 * @param prefix
	 * @param overwrite
	 */
	private void setNsPrefix(String namespace, String prefix, boolean overwrite) {
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
	public void setNSPrefixMapping(String prefix, String namespace) throws NSPrefixMappingUpdateException {
		Repositories.consume(repository, conn -> conn.setNamespace(prefix, namespace));
		nsPrefixMappings.setNSPrefixMapping(namespace, prefix);
	}

	@Override
	public void loadOntologyData(RepositoryConnection conn, File inputFile, String baseURI, RDFFormat format,
			Resource graph, TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws FileNotFoundException, IOException, RDF4JException {
		if (format == null) {
			format = Rio.getParserFormatForFileName(inputFile.getName())
					.orElseThrow(() -> new OntologyManagerException(
							"Could not match a parser for file name: " + inputFile.getName()));
		}
		RDFParser parser = RDFParserRegistry.getInstance().get(format).map(RDFParserFactory::getParser)
				.orElseThrow(() -> new OntologyManagerException("Unspported RDF Data Format"));
		Model capturedOntologyMetadata = new LinkedHashModel();
		InputStream in = new FileInputStream(inputFile);
		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
				baseURI, in, parser, capturedOntologyMetadata);

		conn.add(spyConcurrentIter, graph);

		recoverImportsForOntology(conn, conn.getValueFactory().createIRI(baseURI), ImportModality.USER,
				capturedOntologyMetadata, transitiveImportAllowance, failedImports, new HashSet<>());
	}

	@Override
	public void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappings) {
		this.nsPrefixMappings = nsPrefixMappings;
		// owlModel nsPrefixMapping regeneration from persistenceNSPrefixMapping
		Map<String, String> nsPrefixMapTable = nsPrefixMappings.getNSPrefixMappingTable();
		Set<Map.Entry<String, String>> mapEntries = nsPrefixMapTable.entrySet();
		for (Map.Entry<String, String> entry : mapEntries) {
			setNsPrefix(entry.getValue(), entry.getKey(), true);
		}

	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public void startOntModel(String baseURI, File repoDir, RepositoryConfig supportRepoConfig)
			throws OntologyManagerException {
		// try {
		// SailRepositoryFactory repoFactory = new SailRepositoryFactory();
		// repository = repoFactory.getRepository(supportRepoConfig.getRepositoryImplConfig());
		// repository.setDataDir(repoDir);
		this.baseURI = baseURI;
		// repository.initialize();
		// } catch (RDF4JException e) {
		// throw new OntologyManagerException(e);
		// }
	}
}
