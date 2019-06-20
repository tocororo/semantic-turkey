package it.uniroma2.art.semanticturkey.ontology.impl;

import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromLocalFile;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromOntologyMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWeb;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.fromWebToMirror;
import static it.uniroma2.art.semanticturkey.ontology.ImportMethod.toOntologyMirror;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportAndOntologyMismatchException;
import it.uniroma2.art.semanticturkey.ontology.ImportMethod;
import it.uniroma2.art.semanticturkey.ontology.ImportModality;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus.Values;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyImport;
import it.uniroma2.art.semanticturkey.ontology.OntologyImport.Statuses;
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
import it.uniroma2.art.semanticturkey.utilities.RDFLoaderParser;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities.ThrowingProcedure;

/**
 * Native implementation of {@link OntologyManager} for RDF4J.
 * 
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OntologyManagerImpl implements OntologyManager {

	private static final Logger logger = LoggerFactory.getLogger(OntologyManagerImpl.class);

	private static final Predicate<Statement> ONTOLOGY_METADATA_SPY_PREDICATE = stmt -> stmt.getPredicate()
			.equals(OWL.IMPORTS)
			|| (stmt.getPredicate().equals(RDF.TYPE) && stmt.getObject().equals(OWL.ONTOLOGY))
			|| (stmt.getPredicate().equals(OWL.VERSIONIRI));

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
		applicationOntologiesNG = ConcurrentHashMap.newKeySet();
		applicationOntologiesNamespace = new ConcurrentSkipListSet<>();
		supportOntologiesNG = ConcurrentHashMap.newKeySet();
		supportOntologiesNamespace = new ConcurrentSkipListSet<>();

		importModalityMap = new ConcurrentHashMap<>();
		importModalityMap.put(ImportModality.APPLICATION, ConcurrentHashMap.newKeySet());
		importModalityMap.put(ImportModality.SUPPORT, ConcurrentHashMap.newKeySet());

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

	private void addOntologyImportSkeleton(RepositoryConnection conn, String baseURI,
			boolean updateImportStatement, ImportModality modality, Set<IRI> importedOntologies,
			ThrowingProcedure<Exception> procedure) throws OntologyManagerException {

		@Nullable
		ImmutablePair<IRI, Values> importStatus = OntologyManager.computeURIVariants(baseURI).stream()
				.map(uri -> new ImmutablePair<>(SimpleValueFactory.getInstance().createIRI(uri),
						getImportStatus(conn, uri, importedOntologies).getValue()))
				.filter(p -> !Values.FAILED.equals((p.getRight()))).findFirst().orElse(null);
		if (importStatus != null) {
			if (Values.STAGED_REMOVAL.equals(importStatus.getValue())) {
				throw new OntologyManagerException(
						"Could not import ontology already staged for deletion: " + baseURI);
			} else if (updateImportStatement) {
				checkOntologyNotExplicitlyImported(conn, baseURI, modality);
				String correctedImport = findMatchingImportIRI(baseURI, conn, importStatus.getLeft());
				declareOntologyImport(conn, correctedImport, modality);
				return;
			}
		}

		if (updateImportStatement) {
			checkOntologyNotExplicitlyImported(conn, baseURI, modality);
		}

		if (importStatus == null) {
			try {
				procedure.execute();
			} catch (OntologyManagerException e) {
				throw e;
			} catch (Exception e) {
				throw new OntologyManagerException(e);
			}
		} else {
			if (updateImportStatement) {
				declareOntologyImport(conn, baseURI, modality);
			}
		}
	}

	private String findMatchingImportIRI(String importURI, RepositoryConnection conn, IRI ctx) {
		IRI canonicalImport = OntologyManager
				.computeCanonicalURI(SimpleValueFactory.getInstance().createIRI(importURI));

		for (IRI candidateOnt : Models
				.subjectIRIs(QueryResults.asModel(conn.getStatements(null, RDF.TYPE, OWL.ONTOLOGY, ctx)))) {
			if (OntologyManager.computeCanonicalURI(candidateOnt).equals(canonicalImport)) {
				return candidateOnt.stringValue();
			}
		}

		for (IRI candidateOnt : Models
				.objectIRIs(QueryResults.asModel(conn.getStatements(null, RDF.TYPE, OWL.VERSIONIRI, ctx)))) {
			if (OntologyManager.computeCanonicalURI(candidateOnt).equals(canonicalImport)) {
				return candidateOnt.stringValue();
			}
		}

		return importURI;
	}

	private void declareOntologyImport(RepositoryConnection conn, String ontURI, ImportModality modality) {
		ValueFactory vf = conn.getValueFactory();

		if (modality == ImportModality.USER) {
			IRI baseURI = vf.createIRI(getBaseURI());
			conn.add(baseURI, OWL.IMPORTS, vf.createIRI(ontURI), baseURI);
		} else {
			importModalityMap.get(modality).add(vf.createIRI(ontURI));
		}
	}

	@Override
	public void addOntologyImportFromLocalFile(RepositoryConnection conn, String baseURI,
			ImportModality modality, String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromLocalFile(baseURI, fromLocalFilePath, toLocalFile, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromLocalFile(e.getRealOnt().stringValue(), fromLocalFilePath, toLocalFile,
					modality, true, conn, transitiveImportAllowance, failedImports, new HashSet<>());
		}
	}

	public void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			ImportModality modality, boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {

		logger.debug("adding: " + baseURI + " from localfile: " + fromLocalFilePath + " to Mirror: "
				+ toLocalFile);
		addOntologyImportSkeleton(conn, baseURI, updateImportStatement, modality, importedOntologies, () -> {
			File inputFile = new File(fromLocalFilePath);
			MirroredOntologyFile mirFile = new MirroredOntologyFile(toLocalFile);

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

		});
	}

	@Override
	public void addOntologyImportFromMirror(RepositoryConnection conn, String baseURI,
			ImportModality modality, String mirFileString,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromMirror(baseURI, mirFileString, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromMirror(e.getRealOnt().stringValue(), mirFileString, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		}
	}

	public void addOntologyImportFromMirror(String baseURI, String mirFileString, ImportModality modality,
			boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {

		addOntologyImportSkeleton(conn, baseURI, updateImportStatement, modality, importedOntologies, () -> {
			MirroredOntologyFile mirFile = new MirroredOntologyFile(mirFileString);
			File physicalMirrorFile = new File(mirFile.getAbsolutePath());
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
		});
	}

	@Override
	public void addOntologyImportFromWeb(RepositoryConnection conn, String baseURI, ImportModality modality,
			String sourceURL, RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromWeb(baseURI, sourceURL, rdfFormat, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromWeb(e.getRealOnt().stringValue(), sourceURL, rdfFormat, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		}
	}

	public void addOntologyImportFromWeb(String baseURI, String sourceURL, RDFFormat rdfFormat,
			ImportModality modality, boolean updateImportStatement, RepositoryConnection conn,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies) throws OntologyManagerException {

		String baseURI2 = baseURI != null ? baseURI : sourceURL;

		addOntologyImportSkeleton(conn, baseURI2, updateImportStatement, modality, importedOntologies, () -> {
			Model caputeredImports = new LinkedHashModel();
			IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createURLImportsSpyingIteration(
					baseURI2, sourceURL, rdfFormat, caputeredImports);
			conn.add(spyConcurrentIter, conn.getValueFactory().createIRI(baseURI2));
			notifiedAddedOntologyImport(fromWeb, baseURI2, sourceURL, null, modality, updateImportStatement,
					conn, caputeredImports, transitiveImportAllowance, failedImports, importedOntologies);
		});
	}

	@Override
	public void addOntologyImportFromWebToMirror(RepositoryConnection conn, String baseURI,
			ImportModality modality, String sourceURL, String toLocalFile, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, RDF4JException, OntologyManagerException {
		try {
			addOntologyImportFromWebToMirror(baseURI, sourceURL, toLocalFile, rdfFormat, modality, true, conn,
					transitiveImportAllowance, failedImports, new HashSet<>());
		} catch (ImportAndOntologyMismatchException e) {
			if (!e.getOnt().stringValue().equals(baseURI))
				throw e;

			addOntologyImportFromWebToMirror(e.getRealOnt().stringValue(), sourceURL, toLocalFile, rdfFormat,
					modality, true, conn, transitiveImportAllowance, failedImports, new HashSet<>());
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

		addOntologyImportSkeleton(conn, baseURI, updateImportStatement, modality, importedOntologies, () -> {
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

		});
	}

	@Override
	public Collection<OntologyImport> getUserOntologyImportTree(RepositoryConnection conn) {
		IRI ont = conn.getValueFactory().createIRI(getBaseURI());

		Model importStatements = QueryResults.asModel(conn.getStatements(null, OWL.IMPORTS, null, false));
		Model versionStatements = QueryResults.asModel(conn.getStatements(null, OWL.VERSIONIRI, null, false));
		Set<IRI> importsBranch = new HashSet<>();

		logger.debug("listing ontology imports");
		return getImportsHelper(conn, importStatements, versionStatements, ont, importsBranch);
	}

	private Collection<OntologyImport> getImportsHelper(RepositoryConnection conn, Model importStatements,
			Model versionStatements, IRI ont, Set<IRI> importsBranch) throws RepositoryException {
		Collection<OntologyImport> rv = new ArrayList<>();

		for (Entry<Value, Set<Resource>> importedOnt2graphs : OntologyManager.computeURIVariants(ont).stream()
				.flatMap(ontVariant -> Stream.concat(Stream.of(ontVariant),
						Models.subjectIRIs(versionStatements.filter(null, OWL.VERSIONIRI, ontVariant))
								.stream()))
				.distinct().flatMap(ontVariant -> importStatements.filter(ontVariant, null, null).stream())
				.filter(stmt -> stmt.getObject() instanceof IRI)
				.collect(Collectors.groupingBy(stmt -> stmt.getObject(),
						Collectors.mapping(Statement::getContext, toSet())))
				.entrySet()) {

			IRI importedOnt = (IRI) importedOnt2graphs.getKey();

			logger.debug("\timport: " + importedOnt);

			IRI canonicalImportOnt = OntologyManager.computeCanonicalURI(importedOnt);

			Statuses status;
			Collection<OntologyImport> importsOfImporteddOntology = null;

			if (importsBranch.contains(canonicalImportOnt)) {
				status = Statuses.LOOP;
			} else {
				ImportStatus importStatus;

				Set<Resource> importingCtxs = importedOnt2graphs.getValue();

				if (importingCtxs.stream().anyMatch(VALIDATION::isAddGraph)) {
					importStatus = new ImportStatus(Values.STAGED_ADDITION, null);
				} else if (importingCtxs.stream().anyMatch(VALIDATION::isRemoveGraph)) {
					importStatus = new ImportStatus(Values.STAGED_REMOVAL, null);
				} else {
					importStatus = getImportStatus(conn, importedOnt.stringValue(), true);
				}
				status = OntologyImport.Statuses.fromImportStatus(importStatus);
				Set<IRI> newImportsBranch = new HashSet<>(importsBranch);
				newImportsBranch.add(canonicalImportOnt);

				importsOfImporteddOntology = getImportsHelper(conn, importStatements, versionStatements,
						importedOnt, newImportsBranch);
			}

			OntologyImport importedOntologyElem = new OntologyImport(importedOnt, status,
					importsOfImporteddOntology);
			rv.add(importedOntologyElem);
		}

		return rv;
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
		Thread t = new Thread(concurrentIter);
		t.start();
		return new IterationSpy<>(concurrentIter, ONTOLOGY_METADATA_SPY_PREDICATE,
				stmt -> caputeredImports.add(stmt));
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
			Set<IRI> ontVers = Models.objectIRIs(capturedOntologyMetadata.filter(null, OWL.VERSIONIRI, null));

			// If there is at least one ontology declaration...
			if (!declOnts.isEmpty()) {
				// If the imported ont does not match any ontology declaration nor does it match any version
				// declaration
				if (!declOnts.contains(ont) && !ontVers.contains(ont)) {
					// extracting the real baseURI of the imported ontology
					IRI declaredOntURI = declOnts.iterator().next();

					// extracting, if any, the real version URI of the imported ontology
					Optional<IRI> declaredVersionURI = ontVers.stream().findFirst();

					IRI realURI; // real ontology URI (either ontology URI or version URI)
					boolean redirectable; // tells whether the difference is so small that an implicit
											// redirect is possible (e.g. trailing #)

					// determines the real URI (matching the declared ontology IRI or version IRI)
					if (OntologyManager.computeURIVariants(declaredOntURI.stringValue())
							.contains(ont.stringValue())) {
						realURI = declaredOntURI;
						redirectable = true;
					} else if (declaredVersionURI.isPresent()) {
						IRI realVersionURI2 = declaredVersionURI.get();
						OntologyManager.computeURIVariants(realVersionURI2.stringValue())
								.contains(ont.stringValue());
						realURI = realVersionURI2;
						redirectable = true;
					} else {
						realURI = declaredOntURI;
						redirectable = false;
					}

					// removes data already loaded in the wrong context
					conn.clear(ValidationUtilities.getClearThroughGraphIfValidationEnabled(validationEnabled,
							ont));

					if (getImportStatus(conn, realURI.stringValue(), false)
							.getValue() != ImportStatus.Values.FAILED) { // real ontology is already imported

						if (updateImportStatement) { // if a first-level import, ask for a retry
							throw new ImportAndOntologyMismatchException(ont, realURI);
						} else { // otherwise, return or throw an exception if redirect is not allowed.
									// Indeed, if the imported ont were not redirectable to realURI, the the
									// import would be considered broken
							if (redirectable) {
								importedOntologies.add(ont);
								return;
							} else {
								// and throw an exception
								throw new OntologyManagerException(
										"the real URI for the imported ontology: " + ont + " is actually: "
												+ realURI + " which, however, has already been imported");
							}
						}
					} else { // real ontology is not already imported
						if (updateImportStatement) { // if a first-level import, abort and request a retry
							throw new ImportAndOntologyMismatchException(ont, realURI);
						} else { // otherwise, return or throw an exception if redirect is not allowed.
									// Indeed, if the imported ont were not redirectable to realURI, the the
									// import would be considered broken
							if (redirectable) {
								throw new ImportAndOntologyMismatchException(ont, realURI);
							} else {
								// and throw an exception
								throw new OntologyManagerException("The indirectly imported ontology: " + ont
										+ " has a not matching real URI: " + realURI);
							}
						}
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
			// added to the ontology
			if (updateImportStatement) {
				logger.debug("adding import statement for uri: " + baseURI);
				declareOntologyImport(conn, baseURI, mod);
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

		Values importStatus = OntologyManager.computeURIVariants(baseURI).stream()
				.map(uri -> getImportStatus(conn, uri, importedOntologies))
				.filter(is -> !Values.FAILED.equals(is.getValue())).findFirst().map(ImportStatus::getValue)
				.orElse(Values.FAILED);

		boolean ontologyImported = false;

		if (importStatus == Values.FAILED) {
			for (ImportMethod method : transitiveImportAllowance.getAllowedMethods()) {
				try {
					try {
						doImport(conn, mod, transitiveImportAllowance, failedImports, importedOntologies,
								baseURI, method);
					} catch (ImportAndOntologyMismatchException e) {
						if (e.getOnt().stringValue().equals(baseURI)) {
							doImport(conn, mod, transitiveImportAllowance, failedImports, importedOntologies,
									e.getRealOnt().stringValue(), method);
						}
					}
					ontologyImported = true;
					break;
				} catch (OntologyManagerException e) {
					// e.printStackTrace();
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

	protected void doImport(RepositoryConnection conn, ImportModality mod,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports,
			Set<IRI> importedOntologies, String baseURI, ImportMethod method)
			throws OntologyManagerException {
		if (method == ImportMethod.fromWeb) {
			addOntologyImportFromWeb(baseURI, baseURI, null, mod, false, conn, transitiveImportAllowance,
					failedImports, importedOntologies);
		} else if (method == ImportMethod.fromOntologyMirror) {
			String mirrorEntry = OntologyManager.computeURIVariants(baseURI).stream()
					.map(OntologiesMirror::getMirroredOntologyEntry).filter(Objects::nonNull).findAny()
					.orElse(null);

			if (mirrorEntry != null) {
				addOntologyImportFromMirror(baseURI, mirrorEntry, mod, false, conn, transitiveImportAllowance,
						failedImports, importedOntologies);
			} else {
				throw new OntologyManagerException("Ontology not cached: " + baseURI);
			}
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
			if (conn.getNamespace(guessedPrefix) == null) { // avoid to override the declarion of the guessed
															// prefix
				conn.setNamespace(guessedPrefix, ns);
				logger.debug("namespace: " + ns
						+ " was missing from mapping table, guessed and added new prefix: " + guessedPrefix);
			}
		}
	}

	@Override
	public void removeOntologyImport(RepositoryConnection conn, String uriToBeRemoved) throws IOException {
		removeOntologyImport(conn, uriToBeRemoved, ImportModality.USER);
	}

	public void removeOntologyImport(RepositoryConnection conn, String uriToBeRemoved, ImportModality mod)
			throws IOException {
		IRI ont = conn.getValueFactory().createIRI(uriToBeRemoved);
		IRI canonicalOnt = OntologyManager.computeCanonicalURI(ont);
		IRI canonicalBaseURI = OntologyManager
				.computeCanonicalURI(conn.getValueFactory().createIRI(getBaseURI()));
		if (getImportStatus(conn, uriToBeRemoved, true).getValue() != Values.OK) {
			throw new OntologyManagerException(
					"Could not remove import to ontology which is not persistently imported: "
							+ uriToBeRemoved);
		}

		Model importAxioms = QueryResults.asModel(conn.getStatements(null, OWL.IMPORTS, null));
		Set<IRI> toBeRemovedOntologies = computeImportsClosure(importAxioms, ont);

		logger.debug("transitive closure of imports to be removed: " + toBeRemovedOntologies);

		Set<IRI> toBeSavedOntologies = computeImportsClosure(conn, importAxioms,
				ImportModality.getModalities(), ont, mod);

		logger.debug("transitive closure of other imports: " + toBeSavedOntologies);

		toBeRemovedOntologies.removeAll(toBeSavedOntologies);
		logger.debug("computed difference between the two sets: " + toBeRemovedOntologies);

		// // deletes ontology content and its entry from the input status only if this ontology is not
		// // imported by any other modality
		//
		// IRI baseURIasIRI = SimpleValueFactory.getInstance().createIRI(getBaseURI());
		// Optional<Statement> removedImport = Optional.ofNullable(mod == ImportModality.USER
		// ? SimpleValueFactory.getInstance().createStatement(baseURIasIRI, OWL.IMPORTS, ont,
		// baseURIasIRI)
		// : null);

		// Here we check the saved ontologies are potentially dangling (because of validation)

		for (IRI ontToBeSaved : toBeSavedOntologies) {

			// // If there is an explicit import other than a user import, then there is no problem
			// if (Sets.difference(ImportModality.getModalities(), Collections.singleton(ImportModality.USER))
			// .stream().flatMap(m -> this.getDeclaredImports(conn, m, false).stream())
			// .map(this::OntologyManager.computeCanonicalURI).anyMatch(ontToBeSaved::equals)) {
			// continue;
			// }

			// Otherwise, check that the ontology is not exclusively imported by staged ontologies

			Model savingImports = new LinkedHashModel(importAxioms.stream()
					.filter(stmt -> stmt.getObject() instanceof IRI && OntologyManager
							.computeCanonicalURI((IRI) stmt.getObject()).equals(ontToBeSaved))
					.collect(toList()));
			if (mod == ImportModality.USER) {
				savingImports.removeIf(stmt -> stmt.getSubject() instanceof IRI
						&& OntologyManager.computeCanonicalURI((IRI) stmt.getSubject()).stringValue()
								.equals(getBaseURI())
						&& OntologyManager.computeCanonicalURI((IRI) stmt.getObject()).equals(canonicalOnt));
			}
			for (ImportModality otherMod : ImportModality.getModalities()) {
				if (otherMod != ImportModality.USER) {
					Collection<IRI> imports = getDeclaredImports(conn, otherMod, false);
					if (otherMod == mod) {
						imports.removeIf(
								uri -> OntologyManager.computeCanonicalURI(uri).equals(canonicalOnt));
					}
					imports.stream()
							.filter(uri -> OntologyManager.computeCanonicalURI(uri).equals(canonicalOnt))
							.map(uri -> conn.getValueFactory().createStatement(canonicalBaseURI, OWL.IMPORTS,
									uri, canonicalBaseURI))
							.forEach(savingImports::add);
				}
			}

			// If the ontology to be saved was not imported by an ontology that we are going to remove and it
			// is not equal to the explicit import we are going to remove, then there is no problem
			if (Sets.intersection(savingImports.subjects(), toBeRemovedOntologies).isEmpty()
					&& !canonicalOnt.equals(ontToBeSaved)) {
				continue;
			}

			// Otherwise, check at there is at least a non-staged (for addition/removal) import to the saved
			// ontology

			if (savingImports.stream()
					.filter(stmt -> stmt.getSubject() instanceof IRI && !toBeRemovedOntologies
							.contains(OntologyManager.computeCanonicalURI((IRI) stmt.getSubject())))
					.filter(stmt -> !VALIDATION.isAddGraph(stmt.getContext())
							&& !VALIDATION.isRemoveGraph(stmt.getContext()))
					.allMatch(stmt -> savingImports.contains(stmt.getSubject(), stmt.getPredicate(),
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

		// removes the ontology from the import set
		logger.debug("removing import declaration for ontology: " + ont + ". Modality: " + mod);
		removeDeclaredImport(conn, ont, mod);
	}

	@Override
	public ImportStatus getImportStatus(RepositoryConnection conn, String baseURI,
			boolean canonicalComparison) {
		if (canonicalComparison) {
			return OntologyManager.computeURIVariants(baseURI).stream()
					.map(uri -> getImportStatus(conn, uri, Collections.emptySet()))
					.filter(is -> !Values.FAILED.equals(is.getValue())).findAny()
					.orElse(new ImportStatus(Values.FAILED, null));
		} else {
			return getImportStatus(conn, baseURI, Collections.emptySet());
		}
	}

	private ImportStatus getImportStatus(RepositoryConnection conn, String baseURI,
			Set<IRI> importedOntologies) {

		IRI ont = SimpleValueFactory.getInstance().createIRI(baseURI);
		List<Resource> contexts = QueryResults.asList(conn.getContextIDs());

		if (importedOntologies.contains(ont) || contexts.contains(ont)) {
			return new ImportStatus(ImportStatus.Values.OK, null);
		}

		BooleanQuery ctxQuery = conn.prepareBooleanQuery("ASK { GRAPH ?g { ?s ?p ?o } }");
		ctxQuery.setIncludeInferred(false);

		if (validationEnabled) {
			ctxQuery.setBinding("g", ValidationUtilities.getAddGraphIfValidatonEnabled(true, ont));
			if (ctxQuery.evaluate()) {
				// if (contexts.contains(ValidationUtilities.getAddGraphIfValidatonEnabled(true, ont))) {
				return new ImportStatus(ImportStatus.Values.STAGED_ADDITION, null);
			}

			ctxQuery.setBinding("g", ValidationUtilities.getRemoveGraphIfValidatonEnabled(true, ont));
			if (ctxQuery.evaluate()) {
				// if (contexts.contains(ValidationUtilities.getRemoveGraphIfValidatonEnabled(true, ont))) {
				return new ImportStatus(ImportStatus.Values.STAGED_REMOVAL, null);
			}
		}

		return new ImportStatus(ImportStatus.Values.FAILED, null);
	}

	private void checkOntologyNotExplicitlyImported(RepositoryConnection conn, String baseURI,
			ImportModality modality) throws OntologyManagerException {
		boolean alreadyImported;
		if (modality == ImportModality.USER) {
			if (getDeclaredImports(conn, ImportModality.USER, false).stream()
					.map(OntologyManager::computeCanonicalURI).anyMatch(OntologyManager.computeCanonicalURI(
							SimpleValueFactory.getInstance().createIRI(baseURI))::equals)) {
				alreadyImported = true;
			} else {
				alreadyImported = false;
			}
		} else {
			alreadyImported = importModalityMap.get(modality).stream()
					.map(OntologyManager::computeCanonicalURI).anyMatch(OntologyManager.computeCanonicalURI(
							SimpleValueFactory.getInstance().createIRI(baseURI))::equals);
		}

		if (alreadyImported) {
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
			return new HashSet<>(importModalityMap.get(mod));
	}

	@Override
	public void declareApplicationOntology(IRI ont, boolean declareImport, boolean ng, boolean ns) {
		if (declareImport) {
			importModalityMap.get(ImportModality.APPLICATION).add(ont);
		}
		if (ng) {
			applicationOntologiesNG.add(ont);
		}

		if (ns) {
			applicationOntologiesNamespace
					.add(ModelUtilities.createDefaultNamespaceFromBaseURI(ont.stringValue()));
		}
	}

	@Override
	public void declareSupportOntology(IRI ont, boolean declareImport, boolean ng, boolean ns) {
		if (declareImport) {
			importModalityMap.get(ImportModality.SUPPORT).add(ont);
		}
		if (ng) {
			supportOntologiesNG.add(ont);
		}

		if (ns) {
			supportOntologiesNamespace
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
				removeNamespaceInternal(conn, namespace);
			});
		} catch (RDF4JException e) {
			throw new NSPrefixMappingUpdateException(e);
		}
	}

	private void removeNamespaceInternal(RepositoryConnection conn, String namespace)
			throws RepositoryException {
		Set<String> prefixesToDelete = QueryResults.stream(conn.getNamespaces())
				.filter(ns -> ns.getName().equals(namespace)).map(Namespace::getPrefix).collect(toSet());
		for (String prefix : prefixesToDelete) {
			conn.removeNamespace(prefix);
		}
	}

	/**
	 * Returns all the imported ontologies as canonical URIs (i.e. stripping the terminating hash fragment)
	 * 
	 * @param conn
	 * @param importAxioms
	 * @param modalities
	 * @param justRemovedOnt
	 * @param removedModality
	 * @return
	 */
	private Set<IRI> computeImportsClosure(RepositoryConnection conn, Model importAxioms,
			Set<ImportModality> modalities, @Nullable IRI justRemovedOnt, ImportModality removedModality) {
		Set<IRI> importClosure = new HashSet<IRI>();
		logger.debug("computing global import closure on modalities: " + modalities);

		justRemovedOnt = justRemovedOnt != null ? OntologyManager.computeCanonicalURI(justRemovedOnt) : null;

		for (ImportModality otherMod : modalities) {
			logger.debug("checking declared " + otherMod + " imports for establishing import closure");
			for (IRI ont : getDeclaredImports(conn, otherMod, true)) {
				if (otherMod.equals(removedModality)
						&& Objects.equals(justRemovedOnt, OntologyManager.computeCanonicalURI(ont)))
					continue;
				logger.debug("\timport: " + ont);
				importClosure.addAll(computeImportsClosure(importAxioms, ont));
			}
		}
		return importClosure;
	}

	private Set<IRI> computeImportsClosure(Model importStatements, IRI ont) {
		logger.debug("computing imports closure for import: " + ont);

		Set<IRI> importedOntologiesCollector = new HashSet<>();
		computeImportsClosureHelper(importStatements, ont, importedOntologiesCollector);

		return importedOntologiesCollector;
	}

	private void computeImportsClosureHelper(Model importStatements, IRI ont,
			Set<IRI> importedOntologiesCollector) {
		logger.debug("computing imports closure for import (helper): " + ont);

		IRI canonicalOnt = OntologyManager.computeCanonicalURI(ont);

		if (importedOntologiesCollector.contains(canonicalOnt)) {
			return;
		} else {
			importedOntologiesCollector.add(canonicalOnt);
		}

		for (IRI ontVariant : OntologyManager.computeURIVariants(ont.stringValue()).stream()
				.map(uri -> SimpleValueFactory.getInstance().createIRI(uri)).collect(toSet())) {
			Set<IRI> directImports = Models.objectIRIs(importStatements.filter(ontVariant, OWL.IMPORTS, null))
					.stream().map(OntologyManager::computeCanonicalURI).collect(toSet());

			Set<IRI> ontologiesPendingProcessing = Sets.difference(directImports, importedOntologiesCollector)
					.immutableCopy();
			for (IRI importedOnt : ontologiesPendingProcessing) {
				computeImportsClosureHelper(importStatements, importedOnt, importedOntologiesCollector);
			}
		}
	}

	private void removeDeclaredImport(RepositoryConnection conn, IRI ont, ImportModality mod) {
		if (mod == ImportModality.USER) {
			IRI baseURIResource = conn.getValueFactory().createIRI(getBaseURI());
			conn.remove(baseURIResource, OWL.IMPORTS, ont, baseURIResource);
		} else {
			importModalityMap.get(mod).remove(ont);
		}
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
		Repositories.consume(repository, conn -> {
			removeNamespaceInternal(conn, namespace);
			conn.setNamespace(prefix, namespace);
		});
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
		RDFParser parser = new RDFLoaderParser(format);

		Model capturedOntologyMetadata = new LinkedHashModel();
		InputStream in = new FileInputStream(inputFile);

		IterationSpy<Statement, QueryEvaluationException> spyConcurrentIter = createImportsSpyingIteration(
				baseURI, in, parser, capturedOntologyMetadata);

		conn.add(spyConcurrentIter, graph);

		recoverImportsForOntology(conn, conn.getValueFactory().createIRI(baseURI), ImportModality.USER,
				capturedOntologyMetadata, transitiveImportAllowance, failedImports, new HashSet<>());
	}

	@Override
	public RDFHandler getRDFHandlerForLoadData(RepositoryConnection conn, String baseURI, Resource graph,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports) {
		return new RDFInserter(conn) {

			private Model capturedOntologyMetadata = new LinkedHashModel();
			private Set<String> initialPrefixes = new HashSet<>();
			private Set<String> initialNamespaces = new HashSet<>();
			private Set<String> currentPrefixes = new HashSet<>();
			private Set<String> currentNamespaces = new HashSet<>();

			{
				this.setPreserveBNodeIDs(false);
				this.enforceContext(graph);
			}

			@Override
			public void startRDF() throws RDFHandlerException {
				super.startRDF();
				capturedOntologyMetadata.clear();
				initialPrefixes.clear();
				initialNamespaces.clear();
				currentPrefixes.clear();
				currentNamespaces.clear();
				for (Map.Entry<String, String> entry : getNSPrefixMappings(false).entrySet()) {
					initialPrefixes.add(entry.getKey());
					initialNamespaces.add(entry.getValue());
				}
			}

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				super.handleStatement(st);
				if (ONTOLOGY_METADATA_SPY_PREDICATE.test(st)) {
					capturedOntologyMetadata.add(st);
				}
			}

			@Override
			public void handleNamespace(String prefix, String name) {
				// do not handle prefix declarations that override existing declarations, or that introduce
				// ambiguity. It was not possible to use OntologyManagerImpl::resotreNsPrefixMappings(..)
				// because a subsequent read of the namespace mappings did not include the new prefixes
				if (!initialPrefixes.contains(prefix) && !initialNamespaces.contains(name)
						&& !currentPrefixes.contains(prefix) && !currentNamespaces.contains(name)) {
					super.handleNamespace(prefix, name);
					currentPrefixes.add(prefix);
					currentNamespaces.add(name);
				}
			}

			@Override
			public void endRDF() throws RDFHandlerException, OntologyManagerException {
				try {
					super.endRDF();

					recoverImportsForOntology(conn, conn.getValueFactory().createIRI(baseURI),
							ImportModality.USER, capturedOntologyMetadata, transitiveImportAllowance,
							failedImports, new HashSet<>());

				} catch (RDF4JException | MalformedURLException e) {
					throw new RDFHandlerException(e);
				}
			}
		};
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

	private static void restoreNsPrefixMappings(RepositoryConnection conn,
			Map<String, String> initialPrefixMappings, Map<String, String> newPrefixMappings)
			throws RepositoryException {
		MapDifference<String, String> diff = Maps.difference(newPrefixMappings, initialPrefixMappings);

		for (Entry<String, String> newEntry : diff.entriesOnlyOnLeft().entrySet()) {
			String ns = newEntry.getValue();
			if (initialPrefixMappings.containsValue(ns)) {
				conn.removeNamespace(newEntry.getKey());
			}
		}

		for (Entry<String, ValueDifference<String>> changedEntry : diff.entriesDiffering().entrySet()) {
			conn.setNamespace(changedEntry.getKey(), changedEntry.getValue().rightValue());
		}
	}
}
