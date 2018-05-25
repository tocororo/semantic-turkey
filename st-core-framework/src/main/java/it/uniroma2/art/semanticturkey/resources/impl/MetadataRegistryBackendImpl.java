/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2014.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */
package it.uniroma2.art.semanticturkey.resources.impl;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.stream.FactoryConfigurationError;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.ontology.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.resources.CatalogRecord;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.MetadataDiscoveryException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryCreationException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryIntializationException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;
import it.uniroma2.art.semanticturkey.vocabulary.METADATAREGISTRY;

/**
 * 
 * An implementation of {@link MetadataRegistryBackend}.
 * 
 */
public class MetadataRegistryBackendImpl implements MetadataRegistryBackend {

	private static final String METADATA_REGISTRY_DIRECTORY = "metadataRegistry";
	private static final String METADATA_REGISTRY_FILE = "catalog.ttl";
	private static final RDFFormat CATALOG_FORMAT = RDFFormat.TURTLE;

	public static final String DEFAULTNS = "http://semanticturkey.uniroma2.it/metadataregistry/";

	private File registryDirectory;
	private File catalogFile;
	private SailRepository metadataRegistry;
	private MediationFramework mediationFramework;

	/**
	 * Constructs a {@link MetadataRegistryBackendImpl} whose base is {@link Config#getDataDir()}.
	 * 
	 * @param mediationFramework
	 * @throws MetadataRegistryCreationException
	 */
	public MetadataRegistryBackendImpl(MediationFramework mediationFramework)
			throws MetadataRegistryCreationException {
		this(Config.getDataDir(), mediationFramework);
	}

	/**
	 * Constructs a {@link MetadataRegistryBackendImpl} based on the file (if any)
	 * <code>$baseDir/metadataRegistry/catalog.ttl</code>.
	 * 
	 * @param baseDir
	 * @param mediationFramework
	 * @throws MetadataRegistryCreationException
	 */
	public MetadataRegistryBackendImpl(File baseDir, MediationFramework mediationFramework)
			throws MetadataRegistryCreationException {
		try {
			this.registryDirectory = new File(baseDir, METADATA_REGISTRY_DIRECTORY);
			this.catalogFile = new File(registryDirectory, METADATA_REGISTRY_FILE);
			this.registryDirectory.mkdirs();

			if (!this.registryDirectory.exists()) {
				throw new MetadataRegistryCreationException(
						"Cannot create the folder hierarchy associated with the metadata registry");
			}

			this.mediationFramework = mediationFramework;
		} catch (FactoryConfigurationError e) {
			throw new MetadataRegistryCreationException(e);
		}
	}

	/**
	 * Initializes the metadata registry.
	 * 
	 * @throws MetadataRegistryIntializationException
	 */
	@PostConstruct
	public void initialize() throws MetadataRegistryIntializationException {
		metadataRegistry = new SailRepository(new MemoryStore());
		metadataRegistry.initialize();

		try (RepositoryConnection conn = metadataRegistry.getConnection()) {
			conn.setNamespace(DCAT.NS.getPrefix(), DCAT.NS.getName());
			conn.setNamespace(VOID.NS.getPrefix(), VOID.NS.getName());
			conn.setNamespace(DCTERMS.NS.getPrefix(), DCTERMS.NS.getName());
			conn.setNamespace(XMLSchema.NS.getPrefix(), XMLSchema.NS.getName());
			conn.setNamespace(FOAF.NS.getPrefix(), FOAF.NS.getName());
			conn.setNamespace(OWL.NS.getPrefix(), OWL.NS.getName());
			conn.setNamespace(METADATAREGISTRY.NS.getPrefix(), METADATAREGISTRY.NS.getName());
			conn.setNamespace("", DEFAULTNS);

			if (catalogFile.exists()) {
				try {
					conn.add(catalogFile, null, CATALOG_FORMAT);
				} catch (RDFParseException | RepositoryException | IOException e) {
					throw new MetadataRegistryIntializationException(e);
				}
			}
		}
	}

	/**
	 * Releases the resources consumed by the metadata registry
	 */
	@PreDestroy
	public void destroy() {
		if (metadataRegistry != null) {
			metadataRegistry.shutDown();
		}
	}

	private void saveToFile(RepositoryConnection conn) throws IOException {
		RDFWriterFactory rdfWriterFactory = RDFWriterRegistry.getInstance().get(CATALOG_FORMAT)
				.orElseThrow(() -> new IllegalStateException(
						"Unable to locate factory of the writer for " + CATALOG_FORMAT.getName()));
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(catalogFile),
				StandardCharsets.UTF_8)) {
			RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
			rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
			conn.export(rdfWriter);
		}
	}

	protected void checkNotLocallyDefined(RepositoryConnection conn, @Nullable IRI resource)
			throws IllegalArgumentException {
		if (resource == null)
			return;

		if (conn.hasStatement(resource, null, null, false)) {
			throw new IllegalArgumentException(
					"Resource already defined: " + NTriplesUtil.toNTriplesString(resource));
		}
	}

	protected void checkLocallyDefined(RepositoryConnection conn, @Nullable IRI resource)
			throws IllegalArgumentException {
		if (resource == null)
			return;

		if (!conn.hasStatement(resource, null, null, false)) {
			throw new IllegalArgumentException(
					"Resource not already defined: " + NTriplesUtil.toNTriplesString(resource));
		}
	}

	@Override
	public RepositoryConnection getConnection() {
		RepositoryConnection conn = metadataRegistry.getConnection();
		conn.setIsolationLevel(IsolationLevels.SERIALIZABLE);
		return conn;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#addDataset(org.eclipse.rdf4j.model.IRI,
	 * java.lang.String, java.lang.String, java.lang.Boolean, org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public synchronized IRI addDataset(@Nullable IRI dataset, String uriSpace, @Nullable String title,
			@Nullable Boolean dereferenceable, @Nullable IRI sparqlEndpoint)
			throws IllegalArgumentException, MetadataRegistryWritingException {
		try (RepositoryConnection conn = getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			checkNotLocallyDefined(conn, dataset);

			Update update = conn.prepareUpdate(
			// @formatter:off
				" PREFIX dcat: <http://www.w3.org/ns/dcat#>                                  \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                \n" +
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                  \n" +
				" PREFIX void: <http://rdfs.org/ns/void#>                                    \n" +
				" PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdreg#>                \n" +
				"                                                                            \n" +
				" INSERT {                                                                   \n" +
				"   ?catalog a dcat:Catalog ;                                                \n" +
				"     dcat:dataset ?dataset ;                                                \n" +
				"     dcat:record ?record .                                                  \n" +
				" 	                                                                         \n" +
				"   ?record a dcat:CatalogRecord ;                                           \n" +
				"     dcterms:issued ?now ;                                                  \n" +
				"     foaf:primaryTopic ?dataset .                                           \n" +
				" 		                                                                     \n" +
				"   ?dataset a void:Dataset ;                                                \n" +
				"     void:uriSpace ?uriSpace ;                                              \n" +
				"     dcterms:title ?title ;                                                 \n" +
				"     void:sparqlEndpoint ?sparqlEndpoint ;                                  \n" +
				"     mdreg:dereferenciationSystem ?dereferenciationSystem .                 \n" +
				" }                                                                          \n" +
				" WHERE {                                                                    \n" +
				"   OPTIONAL {                                                               \n" +
				"     ?catalogT a dcat:Catalog .                                             \n" +
				"   }                                                                        \n" +
				"   BIND(IF(BOUND(?catalogT), ?catalogT, ?catalogExt) as ?catalog)           \n" +
				"   BIND(NOW() AS ?now)                                                      \n" +
				" }                                                                          \n"
				// @formatter:on
			);

			update.setBinding("dataset",
					dataset != null ? dataset : vf.createIRI(DEFAULTNS, UUID.randomUUID().toString()));

			IRI record = vf.createIRI(DEFAULTNS, UUID.randomUUID().toString());
			update.setBinding("record", record);

			update.setBinding("uriSpace", vf.createLiteral(uriSpace));
			if (title != null) {
				update.setBinding("title", vf.createLiteral(title));
			}
			if (dereferenceable != null) {
				update.setBinding("dereferenciationSystem",
						dereferenceable ? METADATAREGISTRY.STANDARD_DEREFERENCIATION
								: METADATAREGISTRY.NO_DEREFERENCIATION);
			}
			if (sparqlEndpoint != null) {
				update.setBinding("sparqlEndpoint", sparqlEndpoint);
			}

			update.setBinding("catalogExt", vf.createIRI(DEFAULTNS + UUID.randomUUID().toString()));

			update.execute();

			try {
				saveToFile(conn);
			} catch (IOException e) {
				throw new MetadataRegistryWritingException(e);
			}

			return record;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#addDatasetVersion(org.eclipse.rdf4j.model.IRI,
	 * org.eclipse.rdf4j.model.IRI, java.lang.String)
	 */
	@Override
	public synchronized void addDatasetVersion(IRI catalogRecord, @Nullable IRI dataset, String versionInfo)
			throws IllegalArgumentException, IOException {
		try (RepositoryConnection conn = getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			checkLocallyDefined(conn, catalogRecord);
			checkNotLocallyDefined(conn, dataset);

			BooleanQuery constraintQuery = conn.prepareBooleanQuery(
			// @formatter:off
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>             \n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>          \n" +
                "                                                       \n" +
				" ASK {                                                 \n" +
				"   ?record foaf:topic [                                \n" +
				"     owl:versionInfo ?versionInfo                      \n" +
				"   ]                                                   \n" +
				" }                                                     \n"
				// @formatter:on
			);
			constraintQuery.setIncludeInferred(false);
			constraintQuery.setBinding("record", catalogRecord);
			constraintQuery.setBinding("versionInfo", vf.createLiteral(versionInfo));
			if (constraintQuery.evaluate()) {
				throw new IllegalArgumentException(
						"Catalog record already contains this version: " + versionInfo);
			}

			Update update = conn.prepareUpdate(
			// @formatter:off
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                \n" +
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                  \n" +
				" PREFIX void: <http://rdfs.org/ns/void#>                                    \n" +
				"                                                                            \n" +
				" DELETE {                                                                   \n" +
				"   ?record dcterms:modified ?oldModified .                                  \n" +
				" }                                                                          \n" +
				" INSERT {                                                                   \n" +
				"   ?record dcterms:modified ?now ;                                          \n" +
				"     foaf:topic ?dataset .                                                  \n" +
				" 		                                                                     \n" +
				"   ?dataset a void:Dataset ;                                                \n" +
				"     owl:versionInfo ?versionInfo .                                         \n" +
				" }                                                                          \n" +
				" WHERE {                                                                    \n" +
				"   OPTIONAL { ?record dcterms:modified ?oldModified . }                     \n" +
				"   BIND(NOW() AS ?now)                                                      \n" +
				" }                                                                          \n"
				// @formatter:on
			);
			update.setBinding("record", catalogRecord);
			update.setBinding("dataset",
					dataset != null ? dataset : vf.createIRI(DEFAULTNS, UUID.randomUUID().toString()));
			update.setBinding("versionInfo", vf.createLiteral(versionInfo));
			update.execute();

			saveToFile(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#setDereferenciability(org.eclipse.rdf4j.model.IRI,
	 * java.lang.Boolean)
	 */
	@Override
	public synchronized void setDereferenciability(IRI dataset, @Nullable Boolean value)
			throws IllegalArgumentException, IOException {
		try (RepositoryConnection conn = getConnection()) {
			checkLocallyDefined(conn, dataset);
			Update update = conn.prepareUpdate(
			// @formatter:off
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
				" PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
				" PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdreg#>                     \n" +
				"                                                                                 \n" +
				" DELETE {                                                                        \n" +
				"   ?record dcterms:modified ?oldModified .                                       \n" +
				"   ?dataset mdreg:dereferenciationSystem ?oldDereferenciationSystem .            \n" +
				" }                                                                               \n" +
				" INSERT {                                                                        \n" +
				"   ?record dcterms:modified ?now .                                               \n" +
				"   ?dataset mdreg:dereferenciationSystem ?dereferenciationSystem .               \n" + 
				" }                                                                               \n" +
				" WHERE {                                                                         \n" +
				"   ?record foaf:primaryTopic | foaf:topic ?dataset .                             \n" +
				"   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
				"   OPTIONAL { ?dataset mdreg:dereferenciationSystem ?oldDereferenciationSystem .}\n" +
				"   BIND(NOW() AS ?now)                                                           \n" +
				" }                                                                               \n"
				// @formatter:on
			);
			update.setBinding("dataset", dataset);
			if (value != null) {
				update.setBinding("dereferenciationSystem", value ? METADATAREGISTRY.STANDARD_DEREFERENCIATION
						: METADATAREGISTRY.NO_DEREFERENCIATION);
			}
			update.execute();

			saveToFile(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#setSPARQLEndpoint(org.eclipse.rdf4j.model.IRI,
	 * org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public synchronized void setSPARQLEndpoint(IRI dataset, @Nullable IRI endpoint)
			throws IllegalArgumentException, IOException {
		try (RepositoryConnection conn = getConnection()) {
			checkLocallyDefined(conn, dataset);
			Update update = conn.prepareUpdate(
			// @formatter:off
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
				" PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
				"                                                                                 \n" +
				" DELETE {                                                                        \n" +
				"   ?record dcterms:modified ?oldModified .                                       \n" +
				"   ?dataset void:sparqlEndpoint ?oldEndpoint .                                   \n" +
				" }                                                                               \n" +
				" INSERT {                                                                        \n" +
				"   ?record dcterms:modified ?now .                                               \n" +
				"   ?dataset void:sparqlEndpoint ?endpoint .                                      \n" + 
				" }                                                                               \n" +
				" WHERE {                                                                         \n" +
				"   ?record foaf:primaryTopic | foaf:topic ?dataset .                             \n" +
				"   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
				"   OPTIONAL { ?dataset void:sparqlEndpoint ?oldEndpoint }                        \n" +
				"   BIND(NOW() AS ?now)                                                           \n" +
				" }                                                                               \n"
				// @formatter:on
			);
			update.setBinding("dataset", dataset);
			if (endpoint != null) {
				update.setBinding("endpoint", endpoint);
			}
			update.execute();

			saveToFile(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#getCatalogRecords()
	 */
	@Override
	public synchronized Collection<CatalogRecord> getCatalogRecords() {
		try (RepositoryConnection conn = getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(
			// @formatter:off
				" PREFIX dcat: <http://www.w3.org/ns/dcat#>                                           \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                         \n" +
				" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                           \n" +
				" PREFIX void: <http://rdfs.org/ns/void#>                                             \n" +
				" PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdreg#>                         \n" +
                "                                                                                     \n" +
				" SELECT * {                                                                          \n" +
				"   {SELECT ?record ?recordIssued ?recordModified WHERE {                             \n" +
				"     ?catalog a dcat:Catalog ;                                                       \n" +
				"       dcat:record ?record .                                                         \n" +
                "                                                                                     \n" +
				"     ?record dcterms:issued ?recordIssued .                                          \n" +
				"     OPTIONAL { ?record dcterms:modified ?recordModified . }                         \n" +
				"                                                                                     \n" +
				"     FILTER EXISTS {                                                                 \n" +
				"       ?record foaf:primaryTopic [] .                                                \n" +
				"     }                                                                               \n" +
				"   }}                                                                                \n" +
				"                                                                                     \n" +
				"   {                                                                                 \n" +
				"     ?record foaf:primaryTopic ?dataset .                                            \n" +
				" 	  BIND(foaf:primaryTopic as ?datasetRole)                                         \n" +
				"   } UNION {                                                                         \n" +
				"     ?record foaf:topic ?dataset .                                                   \n" +
				" 	  BIND(foaf:topic as ?datasetRole)                                                \n" +
				"   }                                                                                 \n" +
				"   OPTIONAL {                                                                        \n" +
				"     ?dataset void:uriSpace ?datasetUriSpace.                                        \n" +
				"   }                                                                                 \n" +
				"   OPTIONAL {                                                                        \n" +
				" 	?dataset dcterms:title ?datasetTitle .                                            \n" +
				"   }                                                                                 \n" +
				"   OPTIONAL {                                                                        \n" +
				" 	?dataset mdreg:dereferenciationSystem ?datasetDereferenciationSystem .            \n" +
				"   }                                                                                 \n" +
				"   OPTIONAL {                                                                        \n" +
				" 	?dataset void:sparqlEndpoint ?datasetSPARQLEndpoint .                             \n" +
				"   }                                                                                 \n" +
				"   OPTIONAL {                                                                        \n" +
				" 	?dataset owl:versionInfo ?datasetVersionInfo .                                    \n" +
				"   }                                                                                 \n" +
				"                                                                                     \n" +
				" }                                                                                   \n"
				// @formatter:on
			);
			query.setIncludeInferred(false);

			Map<IRI, DatasetMetadata> record2primary = new HashMap<>();
			Map<IRI, GregorianCalendar> record2issued = new HashMap<>();
			Map<IRI, GregorianCalendar> record2modified = new HashMap<>();
			Multimap<IRI, DatasetMetadata> record2version = HashMultimap.create();

			Set<IRI> records = new HashSet<>();

			try (TupleQueryResult results = query.evaluate()) {
				while (results.hasNext()) {
					BindingSet bs = results.next();

					IRI record = (IRI) bs.getValue("record");

					records.add(record);

					if (!record2issued.containsKey(record)) {
						record2issued.put(record, ((Literal) bs.getValue("recordIssued")).calendarValue()
								.toGregorianCalendar());
					}

					if (!record2modified.containsKey(record) && bs.hasBinding("recordModified")) {
						record2modified.put(record, ((Literal) bs.getValue("recordModified")).calendarValue()
								.toGregorianCalendar());
					}

					Value datasetRole = bs.getValue("datasetRole");

					boolean primary;

					if (FOAF.PRIMARY_TOPIC.equals(datasetRole)) {
						primary = true;
					} else {
						primary = false;
					}

					DatasetMetadata datasetMetadata = bindingset2datasetmetadata(bs);

					if (primary) {
						if (!datasetMetadata.getUriSpace().isPresent()
								|| datasetMetadata.getVersionInfo().isPresent()) {
							continue;
						}
						record2primary.put(record, datasetMetadata);
					} else {
						if (!datasetMetadata.getVersionInfo().isPresent()) {
							continue;
						}
						record2version.put(record, datasetMetadata);
					}
				}
			}
			return records.stream().filter(r -> record2issued.containsKey(r) && record2primary.containsKey(r))
					.map(r -> new CatalogRecord(r, record2issued.get(r), record2modified.get(r),
							record2primary.get(r),
							record2version.get(r).stream()
									.sorted(Comparator.comparing(
											(DatasetMetadata meta) -> meta.getVersionInfo().orElse(null)))
									.collect(toList())))
					.collect(toList());
		}
	}

	@Override
	public DatasetMetadata getDatasetMetadata(IRI dataset) {
		return new DatasetMetadata(
				SimpleValueFactory.getInstance()
						.createIRI("http://aims.fao.org/aos/agrovoc/void.ttl#Agrovoc"),
				"http://aims.fao.org/aos/agrovoc/", "Agrovoc", METADATAREGISTRY.STANDARD_DEREFERENCIATION,
				SimpleValueFactory.getInstance()
						.createIRI("http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc"),
				null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#findDatasetForResource(org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public synchronized DatasetMetadata findDatasetForResource(IRI iriResource) {
		// -------------------------------------------------------------------------------------------
		// The following resolution strategy might be subject to ambiguity in some rare circumstances

		try (RepositoryConnection conn = getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			TupleQuery query = conn.prepareTupleQuery(
			// @formatter:off
					" PREFIX dcat: <http://www.w3.org/ns/dcat#>                                           \n" +
					" PREFIX dcterms: <http://purl.org/dc/terms/>                                         \n" +
					" PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                           \n" +
					" PREFIX void: <http://rdfs.org/ns/void#>                                             \n" +
					" PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdreg#>                         \n" +
	                "                                                                                     \n" +
					" SELECT * {                                                                          \n" +
					"   ?dataset void:uriSpace ?datasetUriSpace.                                          \n" +
					"   OPTIONAL {                                                                        \n" +
					" 	?dataset dcterms:title ?datasetTitle .                                            \n" +
					"   }                                                                                 \n" +
					"   OPTIONAL {                                                                        \n" +
					" 	?dataset mdreg:dereferenciationSystem ?datasetDereferenciationSystem .            \n" +
					"   }                                                                                 \n" +
					"   OPTIONAL {                                                                        \n" +
					" 	?dataset void:sparqlEndpoint ?datasetSPARQLEndpoint .                             \n" +
					"   }                                                                                 \n" +
					"   OPTIONAL {                                                                        \n" +
					" 	?dataset owl:versionInfo ?datasetVersionInfo .                                    \n" +
					"   }                                                                                 \n" +
					"                                                                                     \n" +
					" }                                                                                   \n" +
					" LIMIT 1                                                                             \n"
					// @formatter:on
			);
			query.setIncludeInferred(false);

			// -----------------------------------------
			// Case 1: The provided URI is the base URI

			query.setBinding("datasetUriSpace", vf.createLiteral(iriResource.stringValue()));
			BindingSet bs = QueryResults.singleResult(query.evaluate());

			if (bs != null) {
				return bindingset2datasetmetadata(bs);
			}

			// ------------------------------------------
			// Case 2: The namespace is the base URI
			// e.g., [http://example.org/]Person

			String namespace = iriResource.getNamespace();

			query.setBinding("datasetUriSpace", vf.createLiteral(namespace));
			bs = QueryResults.singleResult(query.evaluate());

			if (bs != null) {
				return bindingset2datasetmetadata(bs);
			}

			// --------------------------------------------
			// Case 2: The namespace is the base URI + "#"
			// e.g., [http://example.org]#Person

			if (namespace.endsWith("#")) {
				query.setBinding("datasetUriSpace",
						vf.createLiteral(namespace.substring(0, namespace.length() - 1)));

				bs = QueryResults.singleResult(query.evaluate());

				if (bs != null) {
					return bindingset2datasetmetadata(bs);
				}
			} else {
				return null;
			}
		}

		return null;
	}

	private DatasetMetadata bindingset2datasetmetadata(BindingSet bs) {
		IRI dataset = (IRI) bs.getValue("dataset");

		@Nullable
		String uriSpace = Optional.ofNullable(bs.getValue("datasetUriSpace")).map(Value::stringValue)
				.orElse(null);
		@Nullable
		String title = Optional.ofNullable(bs.getValue("datasetTitle")).map(Value::stringValue).orElse(null);
		@Nullable
		IRI dereferenciationSystem = Optional.ofNullable(bs.getValue("datasetDereferenciationSystem"))
				.map(IRI.class::cast).orElse(null);
		@Nullable
		IRI sparqlEndpoint = Optional.ofNullable(bs.getValue("datasetSPARQLEndpoint")).map(IRI.class::cast)
				.orElse(null);
		@Nullable
		String versionInfo = Optional.ofNullable(bs.getValue("datasetVersionInfo")).map(Value::stringValue)
				.orElse(null);

		return new DatasetMetadata(dataset, uriSpace, title, dereferenciationSystem, sparqlEndpoint,
				versionInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#discoverDataset(org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public IRI discoverDataset(IRI iri) throws MetadataDiscoveryException {
		try {

			Boolean datasetDeferenceable = null;

			// Dereferences the provided IRI
			RDFLoader rdfLoader = RDF4JUtilities.createRobustRDFLoader();
			Model statements = new LinkedHashModel();
			Model voidStatements = null;

			try {
				rdfLoader.load(new URL(iri.stringValue()), null, null, new StatementCollector(statements));
			} catch (IOException | RDFParseException | RDFHandlerException e) {
				// swallow exception
			}

			// If the data returned from the IRI contains statements about such IRI, then we assume that the
			// dataset is dereferenceable
			if (statements.contains(iri, null, null)) {
				datasetDeferenceable = true;
			}

			// Try to determine the dataset resource

			// First attempt: look at the property void:inDataset

			@Nullable
			IRI voidDataset = Models.getPropertyIRI(statements, iri, VOID.IN_DATASET).orElse(null);

			Literal datasetTitle = null;
			IRI datasetSPARQLEndpoint = null;
			String datasetUriSpace = null;

			// Second Attempt: the provided IRI is the void:Dataset
			if (voidDataset == null && statements.contains(iri, RDF.TYPE, VOID.DATASET)) {
				voidDataset = iri;
				voidStatements = statements;
				// possible dereferenceablity of the dataset description tells nothing about the
				// dereferenceability of data
				datasetDeferenceable = null;
			}

			// Third Attempt: the IRI contains an ontology declaration
			if (voidDataset == null) {
				IRI candidateDataset = Models.subjectIRI(statements.filter(null, RDF.TYPE, OWL.ONTOLOGY))
						.orElse(null);
				if (candidateDataset != null) {
					voidDataset = candidateDataset;
					voidStatements = statements;
					datasetDeferenceable = true;
					datasetUriSpace = ModelUtilities
							.createDefaultNamespaceFromBaseURI(candidateDataset.stringValue());
				}
			}

			if (datasetUriSpace == null && Boolean.TRUE.equals(datasetDeferenceable)) {
				datasetUriSpace = iri.getNamespace();
			}

			if (voidStatements == null) {
				voidStatements = new LinkedHashModel();

				URL voidDownloadAddress;

				if (voidDataset == null) {

					if (datasetUriSpace != null) {
						voidDownloadAddress = new URL(datasetUriSpace + "void.ttl");
					} else {
						voidDownloadAddress = new URL(
								iri.stringValue().endsWith("/") ? iri.stringValue() + "void.ttl"
										: iri.stringValue() + "/void.ttl");
					}
				} else {
					voidDownloadAddress = new URL(voidDataset.stringValue());
				}

				// Try to download the void:Dataset
				try {
					rdfLoader.load(voidDownloadAddress, null, null, new StatementCollector(voidStatements));
				} catch (IOException | RDFParseException | RDFHandlerException e) {
					// swallow exception
				}
			}

			if (voidDataset == null) {
				if (datasetUriSpace != null) {
					voidDataset = Models
							.subjectIRI(voidStatements.filter(null, VOID.URI_SPACE,
									SimpleValueFactory.getInstance().createLiteral(datasetUriSpace)))
							.orElse(null);
				} else {
					voidDataset = voidStatements.filter(null, VOID.URI_SPACE, null).stream().filter(stmt -> {
						String candidateUriSpace = stmt.getObject().stringValue();

						if (iri.stringValue().startsWith(candidateUriSpace)) {
							return true;
						}

						if (candidateUriSpace.endsWith("/") || candidateUriSpace.endsWith("#")) {
							if (candidateUriSpace.substring(0, candidateUriSpace.length() - 1)
									.equals(iri.stringValue())) {
								return true;
							}
						}

						return false;
					}).map(Statement::getSubject).filter(IRI.class::isInstance).map(IRI.class::cast).findAny()
							.orElse(null);
				}
			}

			if (voidDataset != null) {
				if (voidStatements.contains(voidDataset, null, null)) {
					datasetTitle = Models.getPropertyLiteral(voidStatements, voidDataset, DCTERMS.TITLE)
							.orElse(datasetTitle);
					if (datasetTitle == null) {
						datasetTitle = Models.getPropertyLiteral(voidStatements, voidDataset, DC.TITLE)
								.orElse(datasetTitle);
					}
					datasetSPARQLEndpoint = Models
							.getPropertyIRI(voidStatements, voidDataset, VOID.SPARQL_ENDPOINT)
							.orElse(datasetSPARQLEndpoint);
					datasetUriSpace = Models.getPropertyString(voidStatements, voidDataset, VOID.URI_SPACE)
							.orElse(datasetUriSpace);
				}
			}

			if (datasetDeferenceable == null && datasetSPARQLEndpoint != null && datasetUriSpace != null) {
				SPARQLRepository sparqlRepository = new SPARQLRepository(datasetSPARQLEndpoint.stringValue());
				sparqlRepository.initialize();
				try {
					try (RepositoryConnection conn = sparqlRepository.getConnection()) {
						TupleQuery query = conn.prepareTupleQuery(
						// @formatter:off
							"SELECT ?resource {\n" +
							"  ?resource ?p ?o .\n" +
							"  FILTER(isIRI(?resource))\n" +
							"  FILTER(STRSTARTS(STR(?resource), ?datasetUriSpace))\n" +
							"}\n" +
							"LIMIT 1\n"
							// @formatter:on
						);
						query.setBinding("datasetUriSpace",
								SimpleValueFactory.getInstance().createLiteral(datasetUriSpace));
						List<BindingSet> querySolutions = QueryResults.asList(query.evaluate());

						if (!querySolutions.isEmpty()) {
							IRI exampleResource = (IRI) querySolutions.iterator().next().getValue("resource");
							Model exampleResourceStatements = new LinkedHashModel();
							try {
								rdfLoader.load(new URL(exampleResource.stringValue()), null, null,
										new StatementCollector(exampleResourceStatements));
							} catch (IOException | RDFParseException | RDFHandlerException e) {
								// swallow exception
							}

							datasetDeferenceable = exampleResourceStatements.contains(exampleResource, null,
									null);
						}

					}

				} finally {
					sparqlRepository.shutDown();
				}
			}

			if (datasetUriSpace != null) {
				return addDataset(voidDataset, datasetUriSpace,
						datasetTitle != null ? datasetTitle.stringValue() : null, datasetDeferenceable,
						datasetSPARQLEndpoint);
			} else {
				throw new MetadataDiscoveryException(
						"Could not discover a dataset from " + RenderUtils.toSPARQL(iri));
			}
		} catch (Exception e) {
			throw new MetadataDiscoveryException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.resources.M#assessLexicalizationModel(org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public void assessLexicalizationModel(IRI dataset) throws AssessmentException {
		try (RepositoryConnection metadataConn = metadataRegistry.getConnection()) {
			mediationFramework.assessLexicalizationModel(metadataConn, dataset);
		}
	}

}
