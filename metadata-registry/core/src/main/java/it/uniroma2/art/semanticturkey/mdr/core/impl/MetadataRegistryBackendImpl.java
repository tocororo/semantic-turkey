package it.uniroma2.art.semanticturkey.mdr.core.impl;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.stream.FactoryConfigurationError;

import it.uniroma2.art.semanticturkey.mdr.core.AbstractDatasetSpecification;
import it.uniroma2.art.semanticturkey.mdr.core.AbstractDatasetAttachment;
import it.uniroma2.art.semanticturkey.mdr.core.CatalogRecord2;
import it.uniroma2.art.semanticturkey.mdr.core.ConcreteDatasetSpecification;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata2;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetSpecification;
import it.uniroma2.art.semanticturkey.mdr.core.Distribution;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.DCAT3FRAGMENT;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.STMETADATAREGISTRY;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
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
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.mdr.core.LexicalizationSetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.LinksetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.LinksetMetadata.Target;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataDiscoveryException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryCreationException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryIntializationException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.mdr.core.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * An implementation of {@link MetadataRegistryBackend}.
 */
public class MetadataRegistryBackendImpl implements MetadataRegistryBackend {

    private static final IRI ADMS_VERSION_NOTES = Values.iri("http://www.w3.org/ns/adms#versionNotes");
    private static final Logger logger = LoggerFactory.getLogger(MetadataRegistryBackendImpl.class);

    public static final String METADATA_REGISTRY_DIRECTORY = "metadataRegistry";
    public static final String METADATA_REGISTRY_FILE = "catalog.ttl";
    private static final RDFFormat CATALOG_FORMAT = RDFFormat.TURTLE;

    public static final String DEFAULT_BASEURI = "http://semanticturkey.uniroma2.it/metadataregistry/";
    public static final String DEFAULT_NS = DEFAULT_BASEURI;
    public static final String DEFAULT_PROJECT_NS = DEFAULT_NS + "projects/";

    protected File registryDirectory;
    protected File catalogFile;
    protected SailRepository metadataRegistry;
    protected MediationFramework mediationFramework;

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

    @PostConstruct
    @Override
    public void initialize() throws MetadataRegistryIntializationException {
        metadataRegistry = createRepository();
        initializeRepository();
    }

    /**
     * Creates the RDF4J {@link Repository} used to manage the RDF content of the metadata registry. By
     * default, it creates a non-persistent in-memory repository (note that actual persistence as an RDF file
     * is managed by {@link #saveToFile()}). Override this method to use a different repository
     * implementation.
     *
     * @return
     */
    protected SailRepository createRepository() {
        return new SailRepository(new MemoryStore());
    }

    /**
     * Initializes the RDF4J {@link Repository} used to managed the RDF content of the metadata registry.
     *
     * @throws RepositoryException
     * @throws MetadataRegistryIntializationException
     */
    protected void initializeRepository() throws RepositoryException, MetadataRegistryIntializationException {
        metadataRegistry.init();

        try (RepositoryConnection conn = metadataRegistry.getConnection()) {
            conn.setNamespace(DCAT.NS.getPrefix(), DCAT.NS.getName());
            conn.setNamespace(VOID.NS.getPrefix(), VOID.NS.getName());
            conn.setNamespace(DCTERMS.NS.getPrefix(), DCTERMS.NS.getName());
            conn.setNamespace(XSD.NS.getPrefix(), XSD.NS.getName());
            conn.setNamespace(FOAF.NS.getPrefix(), FOAF.NS.getName());
            conn.setNamespace(OWL.NS.getPrefix(), OWL.NS.getName());
            conn.setNamespace(METADATAREGISTRY.NS.getPrefix(), METADATAREGISTRY.NS.getName());
            conn.setNamespace(STMETADATAREGISTRY.NS.getPrefix(), STMETADATAREGISTRY.NS.getName());
            conn.setNamespace(LIME.PREFIX, LIME.NAMESPACE);
            conn.setNamespace("", DEFAULT_BASEURI);

            if (catalogFile.exists()) {
                try {
                    conn.add(catalogFile, null, CATALOG_FORMAT);
                } catch (RDFParseException | RepositoryException | IOException e) {
                    throw new MetadataRegistryIntializationException(e);
                }
            }

            if (!conn.hasStatement(null, RDF.TYPE, DCAT.CATALOG, false)) {
                IRI catalog = conn.getValueFactory().createIRI(conn.getNamespace("") + "catalog");
                conn.add(catalog, RDF.TYPE, DCAT.CATALOG);
            }
        }
    }

    @PreDestroy
    @Override
    public void destroy() {
        if (metadataRegistry != null) {
            metadataRegistry.shutDown();
        }
    }

    /**
     * Saves the content of the (default graph of the) metadata registry to {@link #catalogFile}. Override
     * this to method to change the persistence strategy.
     *
     * @throws MetadataRegistryWritingException
     */
    protected void saveToFile() throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriterFactory rdfWriterFactory = RDFWriterRegistry.getInstance().get(CATALOG_FORMAT)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unable to locate factory of the writer for " + CATALOG_FORMAT.getName()));
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(catalogFile),
                    StandardCharsets.UTF_8)) {
                RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
                rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, true);
                conn.export(rdfWriter, (Resource) null);
            }
        } catch (IOException e) {
            throw new MetadataRegistryWritingException(e);
        }
    }

    public static void main(String[] args) throws URISyntaxException {
        Model model = new LinkedHashModel();
        model.add(Values.iri("http://example.org#pippo"), RDF.TYPE, FOAF.PERSON);
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, System.out, "http://example.org");
        writer.set(BasicWriterSettings.PRETTY_PRINT, true);
        writer.set(BasicWriterSettings.BASE_DIRECTIVE, true);

        Rio.write(model, writer);


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

    @Override
    public synchronized IRI createConcreteDataset(String datasetLocalName, String uriSpace, Literal title, Literal description, Boolean dereferenceable, Distribution distribution, AbstractDatasetAttachment abstractDatasetAttachment, boolean renameOnConflict) throws MetadataRegistryWritingException {
        ConcreteDatasetSpecification concreteDatasetSpecification = new ConcreteDatasetSpecification(null, uriSpace, title, description, dereferenceable, distribution);
        return createDataset(datasetLocalName, concreteDatasetSpecification, abstractDatasetAttachment, null, null, true, renameOnConflict);
    }

    protected synchronized IRI createConcreteDataset(IRI datasetIRI, String uriSpace, Literal title, Literal description, Boolean dereferenceable, Distribution distribution, AbstractDatasetAttachment abstractDatasetAttachment, boolean renameOnConflict) throws MetadataRegistryWritingException {
        ConcreteDatasetSpecification concreteDatasetSpecification = new ConcreteDatasetSpecification(datasetIRI, uriSpace, title, description, dereferenceable, distribution);
        return createDataset(null, concreteDatasetSpecification, abstractDatasetAttachment, null, null, true, renameOnConflict);
    }

    protected synchronized IRI createDataset(String datasetLocalName,
            DatasetSpecification dataset,
            AbstractDatasetAttachment abstractDatasetAttachment,
            BiConsumer<Model, IRI> postOperation, IRI ctx, boolean save, boolean renameOnConflict) throws MetadataRegistryWritingException {
        IRI datasetIRI = dataset.getIdentity();

        try (RepositoryConnection conn = getConnection()) {
            ValueFactory vf = conn.getValueFactory();
            if (datasetLocalName == null) {
                datasetLocalName = UUID.randomUUID().toString();
            }

            String recordLocalName = "record_" + datasetLocalName;
            String distributionLocalName = "distribution_" + datasetLocalName;

            IRI recordIRI;
            IRI distributionIRI = dataset.getDistribution() != null ? dataset.getDistribution().getIdentity() : null;

            boolean recheckNames;
            int nameAppend = 0;
            boolean generateDatasetIRI = (datasetIRI == null);
            boolean generateDistributionIRI = (distributionIRI == null);
            do {
                recheckNames = false;

                if (generateDatasetIRI) {
                    datasetIRI = vf.createIRI(conn.getNamespace(""), datasetLocalName);
                }
                recordIRI = vf.createIRI(conn.getNamespace(""), recordLocalName);

                if (generateDistributionIRI) {
                    distributionIRI = vf.createIRI(conn.getNamespace(""), distributionLocalName);
                }

                try {
                    checkNotLocallyDefined(conn, datasetIRI);
                    checkNotLocallyDefined(conn, recordIRI);
                    checkNotLocallyDefined(conn, distributionIRI);
                } catch (IllegalArgumentException e) {
                    if (StringUtils.startsWith(e.getMessage(), "Resource already defined") && renameOnConflict) {
                        nameAppend += 1;
                        if (nameAppend > 100) {
                            throw e; // no nome than 100 retries
                        }

                        if (nameAppend > 1) { // if this is not the first retry, remove the string _N that has appended previously
                            datasetLocalName = StringUtils.substringBeforeLast(datasetLocalName, "_");
                            recordLocalName = StringUtils.substringBeforeLast(recordLocalName, "_");
                            distributionLocalName = StringUtils.substringBeforeLast(distributionLocalName, "_");
                        }

                        datasetLocalName = datasetLocalName + "_" + nameAppend;
                        recordLocalName = recordLocalName + "_" + nameAppend;
                        distributionLocalName = distributionLocalName + "_" + nameAppend;

                        recheckNames = generateDatasetIRI || generateDistributionIRI;
                    } else {
                        throw e;
                    }
                }
            } while (recheckNames);

            dataset.setIdentity(datasetIRI);

            if (dataset.getDistribution() != null && dataset.getDistribution().getIdentity() == null) {
                dataset.getDistribution().setIdentity(distributionIRI);
            }

            Update update = conn.prepareUpdate("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                    "\n" +
                    "INSERT {\n" +
                    "    ?catalog\n" +
                    "        dcat:record ?record ;\n" +
                    "        dcat:dataset ?dataset\n" +
                    "        .\n" +
                    "        \n" +
                    "    ?record a dcat:CatalogRecord ;\n" +
                    "        dct:issued ?now ;\n" +
                    "        foaf:primaryTopic ?dataset \n" +
                    "        .\n" +
                    "\n" +
                    "}\n" +
                    "WHERE {\n" +
                    "    BIND(NOW() as ?now)\n" +
                    "    ?catalog a dcat:Catalog .\n" +
                    "}");
            update.setBinding("dataset", datasetIRI);
            update.setBinding("record", recordIRI);
            SimpleDataset ds = new SimpleDataset();
            ds.setDefaultInsertGraph(ctx);
            ds.addDefaultRemoveGraph(ctx);
            update.setDataset(ds);
            update.execute();

            Model model = new LinkedHashModel();
            dataset.export(model, vf);
            if (abstractDatasetAttachment != null) {
                abstractDatasetAttachment.export(model, vf, dataset.getIdentity());
            }
            if (postOperation != null) {
                postOperation.accept(model, datasetIRI);
            }
            conn.add(model, ctx);

        }

        if (save) {
            saveToFile();
        }

        return datasetIRI;
    }

    @Override
    public Collection<CatalogRecord2> listRootDatasets() {
        try (RepositoryConnection con = getConnection()) {
            TupleQuery query = con.prepareTupleQuery("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                    "\n" +
                    "SELECT *\n" +
                    "WHERE " +
                    "{\n" +
                    "    ?dataset ^foaf:primaryTopic ?record .\n" +
                    "    FILTER EXISTS {\n" +
                    "        [] dcat:record ?record .\n" +
                    "    " +
                    "}\n" +
                    "    FILTER NOT EXISTS {\n" +
                    "        [] mdr:master|mdr:lod|dcat:hasVersion ?dataset .\n" +
                    "    " +
                    "}\n" +
                    "    {\n" +
                    "    " +
                    "   ?record ?record_p ?record_o .   \n" +
                    "    } UNION " +
                    "{\n" +
                    "        ?dataset ?dataset_p ?dataset_o \n" +
                    "    " +
                    "} UNION {\n" +
                    "        ?dataset (mdr:master|mdr:lod|dcat:hasVersion) / void:uriSpace ?otherURISpace\n" +
                    "    " +
                    "} UNION {\n" +
                    "         ?dataset dcat:distribution/rdf:type ?distributionType\n" +
                    "    } UNION {\n" +
                    "   " +
                    "      ?dataset dcat:distribution/foaf:name ?distributionName\n" +
                    "    } UNION {\n" +
                    "     ?dataset dcat:distribution/void:sparqlEndpoint ?sparqlEndpoint .\n" +
                    "     OPTIONAL { ?sparqlEndpoint ?sparqlEndpoint_p ?sparqlEndpoint_o } .\n" +
                    "    }\n" +
                    "}\n" +
                    "ORDER BY asc(?dataset) asc(?record)\n");
            try (TupleQueryResult result = query.evaluate()) {
                return parseCatalogRecords(result);
            }
        }
    }

    @Override
    public CatalogRecord2 getCatalogRecordMetadata(IRI catalogRecord) {
        try (RepositoryConnection con = getConnection()) {

            checkLocallyDefined(con, catalogRecord);

            TupleQuery query = con.prepareTupleQuery("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                    "\n" +
                    "SELECT *\n" +
                    "WHERE " +
                    "{\n" +
                    "    ?dataset ^foaf:primaryTopic ?record .\n" +
                    "    {\n" +
                    "    " +
                    "   ?record ?record_p ?record_o .   \n" +
                    "    } UNION " +
                    "{\n" +
                    "        ?dataset ?dataset_p ?dataset_o \n" +
                    "    " +
                    "} UNION {\n" +
                    "        ?dataset (mdr:master|mdr:lod|dcat:hasVersion) / void:uriSpace ?otherURISpace\n" +
                    "    " +
                    "} UNION {\n" +
                    "         ?dataset dcat:distribution/rdf:type ?distributionType\n" +
                    "    } UNION {\n" +
                    "         ?dataset dcat:distribution/foaf:name ?distributionName\n" +
                    "} UNION {\n" +
                    "     ?dataset dcat:distribution/void:sparqlEndpoint ?sparqlEndpoint .\n" +
                    "     OPTIONAL { ?sparqlEndpoint ?sparqlEndpoint_p ?sparqlEndpoint_o } .\n" +
                    "    }\n" +
                    "}\n" +
                    "ORDER BY asc(?dataset) asc(?record)");
            query.setBinding("record", catalogRecord);
            try (TupleQueryResult result = query.evaluate()) {
                return parseCatalogRecords(result).stream().findFirst().orElse(null);
            }
        }
    }

    @Override
    public Collection<CatalogRecord2> listConnectedDatasets(IRI abstractDataset) {
        try (RepositoryConnection con = getConnection()) {

            checkLocallyDefined(con, abstractDataset);

            TupleQuery query = con.prepareTupleQuery("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                    "\n" +
                    "SELECT *\n" +
                    "WHERE " +
                    "{\n" +
                    "    ?dataset ^foaf:primaryTopic ?record .\n" +
                    "    FILTER EXISTS {\n" +
                    "        [] dcat:record ?record .\n" +
                    "    " +
                    "}\n" +
                    "    VALUES(?datasetRelProp) {\n" +
                    "        (mdr:master)(mdr:lod)(dcat:hasVersion)\n" +
                    "    " +
                    "}\n" +
                    "    ?abstractDataset ?datasetRelProp ?dataset .\n" +
                    "    {\n" +
                    "       ?record ?record_p ?record_o ." +
                    "   \n" +
                    "    " +
                    "} UNION" +
                    " {\n" +
                    "        ?dataset ?dataset_p ?dataset_o \n" +
                    "    " +
                    "} UNION {\n" +
                    "        ?dataset (mdr:master|mdr:lod|dcat:hasVersion) / void:uriSpace ?otherURISpace\n" +
                    "    " +
                    "} UNION {\n" +
                    "         ?dataset dcat:distribution/rdf:type ?distributionType\n" +
                    "    } UNION {\n" +
                    "         ?dataset dcat:distribution/foaf:name ?distributionName\n" +
                    "    } UNION {\n" +
                    "         ?dataset dcat:distribution/void:sparqlEndpoint ?sparqlEndpoint .\n" +
                    "         OPTIONAL { ?sparqlEndpoint ?sparqlEndpoint_p ?sparqlEndpoint_o } .\n" +
                    "   }\n" +
                    "}\n" +
                    "ORDER BY asc(?dataset) asc(?record)\n");
            query.setBinding("abstractDataset", abstractDataset);
            try (TupleQueryResult result = query.evaluate()) {
                return parseCatalogRecords(result);
            }
        }
    }

    @Override
    public synchronized void connectToAbstractDataset(IRI dataset, AbstractDatasetAttachment abstractDatasetAttachment) throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {

            checkLocallyDefined(conn, dataset);
            checkLocallyDefined(conn, abstractDatasetAttachment.getAbstractDataset());

            Model model = new LinkedHashModel();
            ValueFactory vf = conn.getValueFactory();
            abstractDatasetAttachment.export(model, vf, dataset);
            conn.add(model);
            IRI abstractDatasetIRI = abstractDatasetAttachment.getAbstractDataset();
            updateDatasetRecordTimestamp(conn, abstractDatasetIRI);
        }

        saveToFile();
    }

    @Override
    public synchronized void disconnectFromAbstractDataset(IRI dataset, IRI abstractDataset) throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {

            checkLocallyDefined(conn, dataset);
            checkLocallyDefined(conn, abstractDataset);

            BooleanQuery otherChildCheck = conn.prepareBooleanQuery("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                    "\n" +
                    "ASK {\n" +
                    "  ?abstractDataset mdr:master|mdr:lod|dcat:hasVersion ?otherDataset .\n" +
                    "  FILTER(!sameTerm(?otherDataset, ?dataset)) \n" +
                    "}");

            otherChildCheck.setBinding("dataset", dataset);
            otherChildCheck.setBinding("abstractDataset", abstractDataset);

            if (!otherChildCheck.evaluate()) {
                throw new IllegalArgumentException("Could not disconnect the sole subset of an abstract dataset");
            }

            Update datasetDisconnectionQuery = conn.prepareUpdate("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                    "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                    "\n" +
                    "DELETE {\n" +
                    "  ?abstractDataset mdr:master ?dataset .\n" +
                    "  ?abstractDataset mdr:lod ?dataset .\n" +
                    "  ?abstractDataset dcat:hasVersion ?dataset .\n" +
                    "  ?record dct:modified ?oldModified .\n" +
                    "}\n" +
                    "INSERT {\n" +
                    "  ?record dct:modified ?now .\n" +
                    "}\n" +
                    "WHERE {\n" +
                    "  ?record a dcat:CatalogRecord ;\n" +
                    "    foaf:primaryTopic ?abstractDataset .\n" +
                    "    \n" +
                    "  OPTIONAL {\n" +
                    "    ?record dct:modified ?oldModified\n" +
                    "  " +
                    "}\n" +
                    "  \n" +
                    "  BIND(NOW() as ?now)\n" +
                    "}");

            datasetDisconnectionQuery.setBinding("dataset", dataset);
            datasetDisconnectionQuery.setBinding("abstractDataset", abstractDataset);

            datasetDisconnectionQuery.execute();
        }

        saveToFile();
    }

    protected void updateDatasetRecordTimestamp(RepositoryConnection conn, IRI dataset) {
        Update updateRecordTimestamp = conn.prepareUpdate("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "\n" +
                "DELETE {\n" +
                "  ?record dct:modified ?oldModified \n" +
                "}\n" +
                "INSERT {\n" +
                "  ?record dct:modified ?now \n" +
                "}\n" +
                "WHERE {\n" +
                "    ?record a dcat:CatalogRecord ;\n" +
                "      foaf:primaryTopic ?dataset .\n" +
                "    OPTIONAL {\n" +
                "      ?record dct:modified ?oldModified \n" +
                "    " +
                "}\n" +
                "    BIND(NOW() as ?now)\n" +
                "}");
        updateRecordTimestamp.setBinding("dataset", dataset);
        updateRecordTimestamp.execute();
    }

    protected void updateParentDatasetRecordTimestamp(RepositoryConnection conn, IRI dataset, boolean isRecord) {
        Update updateRecordTimestamp = conn.prepareUpdate("PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                "\n" +
                "DELETE {\n" +
                "  ?record dct:modified ?oldModified \n" +
                "}\n" +
                "INSERT {\n" +
                "  ?record dct:modified ?now \n" +
                "}\n" +
                " WHERE {\n" +
                (isRecord ?
                        "    ?thisRecord foaf:primaryTopic ?dataset . \n"
                        :
                        ""
                ) +
                "    ?parentDataset mdr:master|mdr:lod|dcat:hasVersion ?dataset . \n" +
                "    ?record a dcat:CatalogRecord ;\n" +
                "      foaf:primaryTopic ?parentDataset .\n" +
                "    OPTIONAL {\n" +
                "      ?record dct:modified ?oldModified \n" +
                "    " +
                "}\n" +
                "    BIND(NOW() as ?now)\n" +
                "}");
        if (isRecord) {
            updateRecordTimestamp.setBinding("thisRecord", dataset);
        } else {
            updateRecordTimestamp.setBinding("dataset", dataset);
        }
        updateRecordTimestamp.execute();
    }

    @Override
    public synchronized IRI spawnNewAbstractDataset(IRI dataset1,
            AbstractDatasetAttachment abstractDatasetAttachment1,
            IRI dataset2,
            AbstractDatasetAttachment abstractDatasetAttachment2,
            String datasetLocalName,
            String uriSpace,
            Literal title,
            Literal description,
            Boolean dereferenceable) throws MetadataRegistryWritingException {
        try (RepositoryConnection con = getConnection()) {
            checkLocallyDefined(con, dataset1);
            checkLocallyDefined(con, dataset2);
        }

        AbstractDatasetSpecification abstractDatasetSpecification = new AbstractDatasetSpecification(null, uriSpace, title, description, dereferenceable);
        return createDataset(datasetLocalName, abstractDatasetSpecification, null, (Model model, IRI abstractDatasetIRI) -> {
            abstractDatasetAttachment1.setAbstractDataset(abstractDatasetIRI);
            abstractDatasetAttachment2.setAbstractDataset(abstractDatasetIRI);
            abstractDatasetAttachment1.export(model, SimpleValueFactory.getInstance(), dataset1);
            abstractDatasetAttachment2.export(model, SimpleValueFactory.getInstance(), dataset2);

        }, null, true, false);
    }

    private Collection<CatalogRecord2> parseCatalogRecords(TupleQueryResult result) {
        List<CatalogRecord2> catalogRecordList = new ArrayList<>();
        IRI dataset = null;
        List<BindingSet> bindingSetList = new ArrayList<>();
        boolean done = false;
        while (!done) {
            IRI currentDataset = null;

            BindingSet bs = null;

            if (result.hasNext()) {
                bs = result.next();
                currentDataset = (IRI) bs.getValue("dataset");
            } else {
                done = true;
            }

            if (currentDataset != dataset) {
                if (dataset != null) {
                    catalogRecordList.addAll(parseCatalogRecords(bindingSetList, dataset));
                    bindingSetList.clear();
                }
                dataset = currentDataset;
            }

            if (bs != null) {
                bindingSetList.add(bs);
            }
        }
        return catalogRecordList;
    }

    private Collection<CatalogRecord2> parseCatalogRecords(List<BindingSet> bindingSetList, IRI datasetIRI) {
        String uriSpace = null;
        Set<String> otherURISpaces = new TreeSet<>();
        Map<IRI, Set<IRI>> sparqlEndpoints2Limitations = new HashMap<>();
        IRI dereferenciationSystem = null;
        DatasetMetadata2.SPARQLEndpointMedatadata endpointMetadata = null;
        List<Literal> titles = new ArrayList<>();
        List<Literal> descriptions = new ArrayList<>();
        DatasetMetadata2.DatasetRole datasetRole = DatasetMetadata2.DatasetRole.ROOT;
        DatasetMetadata2.DatasetNature datasetNature = DatasetMetadata2.DatasetNature.ABSTRACT;
        Literal versionInfo = null;
        Literal versionNotes = null;

        List<CatalogRecord2> catalogRecords = new ArrayList<>();
        String distributionName = null;

        for (BindingSet bs : bindingSetList) {
            IRI datasetProperty = (IRI) bs.getValue("dataset_p");
            Value datasetPropertyValue = bs.getValue("dataset_o");

            if (Objects.equals(datasetProperty, VOID.URI_SPACE)) {
                uriSpace = datasetPropertyValue.stringValue();
            } else if (Objects.equals(datasetProperty, DCTERMS.TITLE)) {
                titles.add((Literal) datasetPropertyValue);
            } else if (Objects.equals(datasetProperty, DCTERMS.DESCRIPTION)) {
                descriptions.add((Literal) datasetPropertyValue);
            } else if (Objects.equals(datasetProperty, OWL.VERSIONINFO)) {
                versionInfo = (Literal) datasetPropertyValue;
            } else if (Objects.equals(datasetProperty, ADMS_VERSION_NOTES)) {
                versionNotes = (Literal) datasetPropertyValue;
            } else if (Objects.equals(datasetProperty, METADATAREGISTRY.DEREFERENCIATION_SYSTEM)) {
                dereferenciationSystem = (IRI) datasetPropertyValue;
            }

            Value otherURISpace = bs.getValue("otherURISpace");

            if (otherURISpace != null) {
                otherURISpaces.add(otherURISpace.stringValue());
            }

            IRI datasetRelProp = (IRI) bs.getValue("datasetRelProp");

            if (Objects.equals(datasetRelProp, METADATAREGISTRY.MASTER)) {
                datasetRole = DatasetMetadata2.DatasetRole.MASTER;
            } else if (Objects.equals(datasetRelProp, METADATAREGISTRY.LOD)) {
                datasetRole = DatasetMetadata2.DatasetRole.LOD;
            } else if (Objects.equals(datasetRelProp, DCAT3FRAGMENT.HAS_VERSION)) {
                datasetRole = DatasetMetadata2.DatasetRole.VERSION;
            }

            Value distributionType = bs.getValue("distributionType");

            DatasetMetadata2.DatasetNature newNature = null;

            if (METADATAREGISTRY.SPARQL_ENDPOINT.equals(distributionType)) {
                newNature = DatasetMetadata2.DatasetNature.SPARQL_ENDPOINT;
            } else if (METADATAREGISTRY.RDF4J_HTTP_REPOSITORY.equals(distributionType)) {
                newNature = DatasetMetadata2.DatasetNature.RDF4J_REPOSITORY;
            } else if (METADATAREGISTRY.GRAPHDB_REPOSITORY.equals(distributionType)) {
                newNature = DatasetMetadata2.DatasetNature.GRAPHDB_REPOSITORY;
            } else if (STMETADATAREGISTRY.PROJECT.equals(distributionType)) {
                newNature = DatasetMetadata2.DatasetNature.PROJECT;
            }

            if (newNature != null) {
                if (datasetNature != DatasetMetadata2.DatasetNature.ABSTRACT && newNature != datasetNature) {
                    datasetNature = DatasetMetadata2.DatasetNature.MIX;
                } else {
                    datasetNature = newNature;
                }
            }

            String newDistributionName = Optional.ofNullable(bs.getValue("distributionName"))
                    .map(Value::stringValue)
                    .orElse(null);
            if (newDistributionName != null) {
                distributionName = newDistributionName;
            }

            IRI sparqlEndpoint = (IRI) bs.getValue("sparqlEndpoint");
            if (sparqlEndpoint != null) {
                Set<IRI> limitationsSet = sparqlEndpoints2Limitations.computeIfAbsent(sparqlEndpoint, iri -> new HashSet<>());
                if (Objects.equals(bs.getValue("sparqlEndpoint_p"), METADATAREGISTRY.SPARQL_ENDPOINT_LIMITATION)) {
                    limitationsSet.add((IRI) bs.getValue("sparqlEndpoint_o"));
                }
            }
        }

        otherURISpaces.remove(uriSpace);

        endpointMetadata = sparqlEndpoints2Limitations
                .entrySet()
                .stream()
                .findFirst()
                .map(sparqlEndpointEntry -> new DatasetMetadata2.SPARQLEndpointMedatadata(
                        sparqlEndpointEntry.getKey(), sparqlEndpointEntry.getValue()
                )).orElse(null);

        String projectName;

        if (datasetNature == DatasetMetadata2.DatasetNature.PROJECT) {
            projectName = distributionName;
        } else {
            projectName = null;
        }

        DatasetMetadata2 datasetMetadata = new DatasetMetadata2(
                datasetIRI,
                datasetNature,
                uriSpace,
                otherURISpaces,
                dereferenciationSystem,
                endpointMetadata,
                titles,
                descriptions,
                datasetRole,
                versionInfo,
                versionNotes,
                projectName);

        ZonedDateTime recordIssued = null;
        ZonedDateTime recordModified = null;
        IRI recordIRI = null;

        for (BindingSet bs : bindingSetList) {
            IRI record = (IRI) bs.getValue("record");

            if (record == null) continue;

            if (record != recordIRI) {
                if (recordIRI != null) {
                    CatalogRecord2 catalogRecord = new CatalogRecord2(recordIRI, recordIssued, recordModified, datasetMetadata);
                    catalogRecords.add(catalogRecord);
                }
                recordIRI = record;
                recordIssued = null;
                recordModified = null;
            }
            IRI recordProperty = (IRI) bs.getValue("record_p");
            Value recordPropertyValue = bs.getValue("record_o");

            if (DCTERMS.ISSUED.equals(recordProperty)) {
                recordIssued = Literals.getCalendarValue(recordPropertyValue, null).toGregorianCalendar().toZonedDateTime();
            } else if (DCTERMS.MODIFIED.equals(recordProperty)) {
                recordModified = Literals.getCalendarValue(recordPropertyValue, null).toGregorianCalendar().toZonedDateTime();
            }
        }

        CatalogRecord2 catalogRecord = new CatalogRecord2(recordIRI, recordIssued, recordModified, datasetMetadata);
        catalogRecords.add(catalogRecord);

        return catalogRecords;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend#deleteCatalogRecord(org.eclipse.rdf4j.
     * model.IRI)
     */
    @Override
    public synchronized void deleteCatalogRecord(IRI catalogRecord) throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {

            checkLocallyDefined(conn, catalogRecord);

            updateParentDatasetRecordTimestamp(conn, catalogRecord, true);
            GraphQuery query = conn.prepareGraphQuery(
                    // @formatter:off
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                  \n" +
                            "PREFIX void: <http://rdfs.org/ns/void#>                                    \n" +
                            "PREFIX dcat: <http://www.w3.org/ns/dcat#>									\n" +
                            "	                                                                        \n" +
                            "DESCRIBE * WHERE {                                                         \n" +
                            "  ?catalogRecord foaf:primaryTopic/(dcat:distribution/void:subset*)? ?dataset      \n" +
                            "}                                                                          \n"
                    // @formatter:on
            );
            query.setBinding("catalogRecord", catalogRecord);
            conn.remove(QueryResults.asModel(query.evaluate()));
        }

        saveToFile();
    }

    @Override
    public synchronized void addEmbeddedLexicalizationSet(IRI dataset, @Nullable IRI lexicalizationSet,
            @Nullable IRI lexiconDataset, IRI lexicalizationModel, String language,
            @Nullable BigInteger references, @Nullable BigInteger lexicalEntries,
            @Nullable BigInteger lexicalizations, @Nullable BigDecimal percentage,
            @Nullable BigDecimal avgNumOfLexicalizations) throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            checkLocallyDefined(conn, dataset);
            checkNotLocallyDefined(conn, lexicalizationSet);

            ValueFactory vf = conn.getValueFactory();

            checkLocallyDefined(conn, dataset);
            checkNotLocallyDefined(conn, lexicalizationSet);

            Update update = conn.prepareUpdate(
                    // @formatter:off
                    "PREFIX dcterms: <http://purl.org/dc/terms/>                       \n" +
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>                         \n" +
                            "PREFIX void: <http://rdfs.org/ns/void#>                           \n" +
                            "PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                   \n" +
                            "PREFIX dcat: <http://www.w3.org/ns/dcat#>                         \n" +
                            "                                                                  \n" +
                            "DELETE {                                                          \n" +
                            "  ?record dcterms:modified ?oldModified .                         \n" +
                            "}                                                                 \n" +
                            "INSERT {                                                          \n" +
                            "  ?record dcterms:modified ?now .                                 \n" +
                            "		                                                           \n" +
                            "  ?lexicalizedDataset void:subset ?lexicalizationSet .            \n" +
                            "                                                                  \n" +
                            "  ?lexicalizationSet a lime:LexicalizationSet ;                   \n" +
                            "	lime:referenceDataset ?lexicalizedDataset ;                    \n" +
                            "	lime:lexiconDataset ?lexiconDataset ;                          \n" +
                            "	lime:lexicalizationModel ?lexicalizationModel ;                \n" +
                            "	lime:language ?language ;                                      \n" +
                            "	lime:references ?references ;                                  \n" +
                            "	lime:lexicalEntries ?lexicalEntries ;                          \n" +
                            "	lime:lexicalizations ?lexicalizations ;                        \n" +
                            "	lime:percentage ?percentage ;                                  \n" +
                            "	lime:avgNumOfLexicalizations ?avgNumOfLexicalizations          \n" +
                            "	.                                                              \n" +
                            "}                                                                 \n" +
                            "WHERE {                                                           \n" +
                            "  ?record foaf:primaryTopic ?dataset .                            \n" +
                            "  ?dataset dcat:distribution ?lexicalizedDataset .                \n" +
                            "  OPTIONAL { ?record dcterms:modified ?oldModified . }            \n" +
                            "  BIND(NOW() AS ?now)                                             \n" +
                            "}                                                                 \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            update.setBinding("lexicalizationSet", lexicalizationSet != null ? lexicalizationSet
                    : vf.createIRI(DEFAULT_BASEURI, UUID.randomUUID().toString()));
            if (lexiconDataset != null) {
                update.setBinding("lexiconDataset", lexiconDataset);
            }

            update.setBinding("lexicalizationModel", lexicalizationModel);
            update.setBinding("language", vf.createLiteral(language, XSD.LANGUAGE));

            if (references != null) {
                update.setBinding("references", vf.createLiteral(references));
            }

            if (lexicalEntries != null) {
                update.setBinding("lexicalEntries", vf.createLiteral(lexicalEntries));
            }

            if (lexicalizations != null) {
                update.setBinding("lexicalizations", vf.createLiteral(lexicalizations));
            }

            if (percentage != null) {
                update.setBinding("percentage", vf.createLiteral(percentage));
            }

            if (avgNumOfLexicalizations != null) {
                update.setBinding("avgNumOfLexicalizations", vf.createLiteral(avgNumOfLexicalizations));
            }

            update.execute();
        }

        saveToFile();
    }

    @Override
    public synchronized void deleteEmbeddedLexicalizationSet(IRI lexicalizationSet)
            throws MetadataRegistryWritingException, MetadataRegistryStateException {
        try (RepositoryConnection conn = getConnection()) {

            checkLocallyDefined(conn, lexicalizationSet);

            if (!conn.hasStatement(lexicalizationSet, RDF.TYPE, LIME.LEXICALIZATION_SET, false)) {
                throw new IllegalArgumentException("Not a lexicalization set: " + lexicalizationSet);
            }

            Set<Resource> containerDatsets = QueryResults
                    .asModel(conn.getStatements(null, VOID.SUBSET, lexicalizationSet, false)).subjects();

            if (containerDatsets.isEmpty()) {
                throw new IllegalArgumentException(
                        "Not an embedded lexicalization set: " + lexicalizationSet);
            }

            if (containerDatsets.size() > 1) {
                throw new MetadataRegistryStateException(
                        "Lexicalization set contained in multiple datasets: " + containerDatsets);
            }

            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                     \n" +
                            " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>     \n" +
                            "                                                          \n" +
                            " DELETE {                                                                        \n" +
                            "   ?record dcterms:modified ?oldModified .                                       \n" +
                            "   ?lexicalizationSet ?p1 ?o1 .                                                  \n" +
                            "   ?s2 ?p2 ?lexicalizationSet .                                                  \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            " }                                                                               \n" +
                            " WHERE {    \n" +
                            "   ?lexicalizationSet ^void:subset+/^dcat:distribution/^foaf:primaryTopic ?record .  \n" +
                            "   OPTIONAL {                                                                    \n" +
                            "     ?record dcterms:modified ?oldModified .                                     \n" +
                            "   }                                                                             \n" +
                            "   { ?lexicalizationSet ?p1 ?o1 } UNION { ?s2 ?p2 ?lexicalizationSet}            \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("lexicalizationSet", lexicalizationSet);
            update.execute();
        }

        saveToFile();
    }

    @Override
    public synchronized void addEmbeddedLinkset(IRI dataset, @Nullable IRI linkset, String targetUriSpace, IRI linkPredicate, @Nullable Integer linkCount) throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            ValueFactory vf = conn.getValueFactory();
            checkLocallyDefined(conn, dataset);
            checkLocallyDefined(conn, linkset);

            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                     \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>                                     \n" +
                            " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            "                                                                                 \n" +
                            " DELETE {                                                                        \n" +
                            "   ?record dcterms:modified ?oldModified .                                       \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now . \n" +
                            "   ?linkset a void:Linkset ;\n" +
                            "     void:subjectsTarget ?distribution ;\n" +
                            "     void:objectsTarget ?objectsTarget ;\n" +
                            "     void:linkPredicate ?linkPredicate ;\n" +
                            "     void:triples ?linkCount .\n" +
                            "   \n" +
                            "   ?distribution void:subset ?linkset .\n" +
                            "   \n" +
                            "   ?objectsTarget a void:Dataset ;\n" +
                            "     void:uriSpace ?targetUriSpace .                                           \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset ." +
                            "                                         \n" +
                            "   OPTIONAL {                                                                    \n" +
                            "     ?record dcterms:modified ?oldModified .                                     \n" +
                            "   } \n" +
                            "   ?dataset dcat:distribution ?distribution .                                                                            \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            update.setBinding("linkset", linkset != null ? linkset
                    : vf.createIRI(DEFAULT_BASEURI, UUID.randomUUID().toString()));
            update.setBinding("targetUriSpace", vf.createLiteral(targetUriSpace));
            update.setBinding("objectsTarget", vf.createIRI(DEFAULT_BASEURI, UUID.randomUUID().toString()));
            if (linkPredicate != null) {
                update.setBinding("linkPredicate", linkPredicate);
            }
            if (linkCount != null) {
                update.setBinding("linkCount", vf.createLiteral(linkCount));
            }

            update.execute();
        }

        saveToFile();
    }

    /*
     * (non-Javadoc)
     *
     * @see it.uniroma2.art.semanticturkey.resources.M#setDereferenciability(org.eclipse.rdf4j.model.IRI,
     * java.lang.Boolean)
     */
    @Override
    public synchronized void setDereferenciability(IRI dataset, @Nullable Boolean value)
            throws IllegalArgumentException, MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            checkLocallyDefined(conn, dataset);
            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
                            " PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdr#>                       \n" +
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
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
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
        }

        saveToFile();
    }

    /*
     * (non-Javadoc)
     *
     * @see it.uniroma2.art.semanticturkey.resources.M#setSPARQLEndpoint(org.eclipse.rdf4j.model.IRI,
     * org.eclipse.rdf4j.model.IRI)
     */
    @Override
    public synchronized void setSPARQLEndpoint(IRI dataset, @Nullable IRI endpoint)
            throws IllegalArgumentException, MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            checkLocallyDefined(conn, dataset);
            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                       \n" +
                            " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
                            "                                                                                 \n" +
                            " DELETE {                                                                        \n" +
                            "   ?record dcterms:modified ?oldModified .                                       \n" +
                            "   ?distribution void:sparqlEndpoint ?oldEndpoint .                              \n" +
                            "   ?oldEndpoint ?oldEndpointP ?oldEndpointO .                                    \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            "   ?distribution void:sparqlEndpoint ?endpoint .                                 \n" +
                            "   ?endpoint ?oldEndpointP ?oldEndpointO .                                       \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   ?dataset dcat:distribution ?distribution .                                    \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   OPTIONAL { \n" +
                            "     ?distribution void:sparqlEndpoint ?oldEndpoint \n" +
                            "     OPTIONAL { ?oldEndpoint ?oldEndpointP ?oldEndpointO }\n" +
                            "   }                   \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            if (endpoint != null) {
                update.setBinding("endpoint", endpoint);
            }
            update.execute();
        }

        saveToFile();
    }

    @Override
    public synchronized void setTitle(IRI dataset, Literal title)
            throws IllegalArgumentException, MetadataRegistryWritingException {
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
                            "   ?dataset dcterms:title ?oldTitle .                                            \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            "   ?dataset dcterms:title ?title .                                               \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                         \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   OPTIONAL { \n" +
                            "     ?dataset dcterms:title ?oldTitle\n" +
                            "     FILTER(LANG(?oldTitle) = LANG(?title))" +
                            "   \n" +
                            "   }                                 \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            update.setBinding("title", title);
            update.execute();
        }

        saveToFile();
    }

    @Override
    public void deleteTitle(IRI dataset, Literal title) throws IllegalArgumentException, MetadataRegistryWritingException {
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
                            "   ?dataset dcterms:title ?title .                                               \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   ?dataset dcterms:title ?title\n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            if (title != null) {
                update.setBinding("title", title);
            }
            update.execute();
        }

        saveToFile();
    }

    @Override
    public synchronized void setDescription(IRI dataset, Literal description)
            throws IllegalArgumentException, MetadataRegistryWritingException {
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
                            "   ?dataset dcterms:description ?oldDescription .                                \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            "   ?dataset dcterms:description ?description .                                   \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   OPTIONAL { \n" +
                            "     ?dataset dcterms:description ?oldDescription\n" +
                            "     FILTER(LANG(?oldDescription) = LANG(?description))\n" +
                            "   }                     \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            update.setBinding("description", description);
            update.execute();
        }

        saveToFile();
    }

    @Override
    public void deleteDescription(IRI dataset, Literal description) throws IllegalArgumentException, MetadataRegistryWritingException {
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
                            "   ?dataset dcterms:description ?description .                                   \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   ?dataset dcterms:description ?description\n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("dataset", dataset);
            if (description != null) {
                update.setBinding("description", description);
            }
            update.execute();
        }

        saveToFile();
    }

    @Override
    public Set<IRI> getSPARQLEndpointLimitations(IRI endpoint) {
        try (RepositoryConnection conn = getConnection()) {
            return Models.objectIRIs(QueryResults.asModel(
                    conn.getStatements(endpoint, METADATAREGISTRY.SPARQL_ENDPOINT_LIMITATION, null, false)));

        }
    }

    @Override
    public synchronized void setSPARQLEndpointLimitation(IRI endpoint, IRI limitation)
            throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                       \n" +
                            " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
                            " PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdr#>                       \n" +
                            "                                                                                 \n" +
                            " DELETE {                                                                        \n" +
                            "   ?record dcterms:modified ?oldModified .                                       \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            "   ?endpoint mdreg:sparqlEndpointLimitation ?limitation .                        \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?dataset dcat:distribution ?distribution .                                    \n" +
                            "   ?distribution void:sparqlEndpoint ?endpoint .                                 \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("endpoint", endpoint);
            update.setBinding("limitation", limitation);
            update.execute();
        }

        saveToFile();
    }

    @Override
    public synchronized void removeSPARQLEndpointLimitation(IRI endpoint, IRI limitation)
            throws MetadataRegistryWritingException {
        try (RepositoryConnection conn = getConnection()) {
            Update update = conn.prepareUpdate(
                    // @formatter:off
                    " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                       \n" +
                            " PREFIX dcterms: <http://purl.org/dc/terms/>                                     \n" +
                            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                       \n" +
                            " PREFIX void: <http://rdfs.org/ns/void#>                                         \n" +
                            " PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdr#>                       \n" +
                            "                                                                                 \n" +
                            " DELETE {                                                                        \n" +
                            "   ?record dcterms:modified ?oldModified .                                       \n" +
                            "   ?endpoint mdreg:sparqlEndpointLimitation ?limitation .                        \n" +
                            " }                                                                               \n" +
                            " INSERT {                                                                        \n" +
                            "   ?record dcterms:modified ?now .                                               \n" +
                            " }                                                                               \n" +
                            " WHERE {                                                                         \n" +
                            "   ?dataset dcat:distribution ?distribution .                                    \n" +
                            "   ?distribution void:sparqlEndpoint ?endpoint .                                 \n" +
                            "   ?record foaf:primaryTopic ?dataset .                                          \n" +
                            "   OPTIONAL { ?record dcterms:modified ?oldModified . }                          \n" +
                            "   BIND(NOW() AS ?now)                                                           \n" +
                            " }                                                                               \n"
                    // @formatter:on
            );
            update.setBinding("endpoint", endpoint);
            update.setBinding("limitation", limitation);
            update.execute();
        }

        saveToFile();
    }

    @Override
    public DatasetMetadata getDatasetMetadata(IRI dataset)
            throws NoSuchDatasetMetadataException, MetadataRegistryStateException {
        MapBindingSet bindings = new MapBindingSet();
        bindings.addBinding("dataset", dataset);
        return getUnambiguousDatasetMetadata(bindings)
                .orElseThrow(() -> new NoSuchDatasetMetadataException(bindings));
    }

    @Override
    public Collection<LinksetMetadata> getEmbeddedLinksets(IRI dataset, long treshold, boolean coalesce) {
        try (RepositoryConnection conn = getConnection()) {
            TupleQuery query = conn.prepareTupleQuery(
                    //@formatter:off
                    "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                            "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                            "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                            "PREFIX mdr: <http://semanticturkey.uniroma2.it/ns/mdr#>\n" +
                            "PREFIX stmdr: <http://semanticturkey.uniroma2.it/ns/stmdr#>\n" +
                            "\n" +
                            "SELECT ?linkset ?targetDataset ?targetDatasetUriSpace ?targetDatasetTitle ?registeredTargetDataset ?registeredTargetProjectName ?registeredTargetDatasetTitle ?linkCount ?linkPredicate {\n" +
                            "  ?dataset dcat:distribution ?sourceDataset .\n" +
                            "  ?sourceDataset void:subset ?linkset .\n" +
                            "  ?linkset a void:Linkset ;\n" +
                            "    void:subjectsTarget ?sourceDataset ;\n" +
                            "    void:objectsTarget ?targetDataset\n" +
                            "    .\n" +
                            "  OPTIONAL {\n" +
                            "    ?linkset void:triples ?linkCount .\n" +
                            "  }\n" +
                            "  OPTIONAL {\n" +
                            "    ?linkset void:linkPredicate ?linkPredicate .\n" +
                            "  }\n" +
                            "  OPTIONAL { ?targetDataset dct:title ?targetDatasetTitle . }\n" +
                            "  OPTIONAL { \n" +
                            "    ?targetDataset void:uriSpace ?targetDatasetUriSpace .\n" +
                            "    OPTIONAL {\n" +
                            "      ?record a dcat:CatalogRecord ;\n" +
                            "        foaf:primaryTopic ?registeredTargetDataset .\n" +
                            "      ?registeredTargetDataset void:uriSpace ?targetDatasetUriSpace .\n" +
                            "      FILTER NOT EXISTS {\n" +
                            "        [] mdr:master|mdr:lod|dcat:hasVersion ?registeredTargetDataset .\n" +
                            "      " +
                            "}\n" +
                            "      OPTIONAL {\n" +
                            "    " +
                            "    ?registeredTargetDataset dcat:distribution [\n" +
                            "            a stmdr:Project ;\n" +
                            "            foaf:name ?registeredTargetProjectName\n" +
                            "          ]\n" +
                            "          .\n" +
                            "      }\n" +
                            "      OPTIONAL { ?registeredTargetDataset dct:title ?registeredTargetDatasetTitle . }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}"
                    //@formatter:on
            );
            query.setBinding("dataset", dataset);
            List<BindingSet> queryResults = QueryResults.asList(query.evaluate());

            Map<Value, List<BindingSet>> bindingSetsForLinkset = queryResults.stream()
                    .collect(Collectors.groupingBy(bs -> bs.getValue("linkset")));

            List<LinksetMetadata> rv = new ArrayList<>(bindingSetsForLinkset.keySet().size());

            for (Map.Entry<Value, List<BindingSet>> entry : bindingSetsForLinkset.entrySet()) {
                List<BindingSet> bindingSets = entry.getValue();
                IRI sourceDataset = dataset;
                IRI targetDataset = bindingSets.stream().map(bs -> (IRI) bs.getValue("targetDataset"))
                        .findAny().get();
                Optional<String> targetDatasetUriSpace = bindingSets.stream()
                        .map(bs -> bs.getValue("targetDatasetUriSpace").stringValue())
                        .filter(Objects::nonNull).findAny();

                Optional<Integer> linkCount = bindingSets.stream()
                        .map(bs -> Literals.getIntValue(bs.getValue("linkCount"), 0)).filter(Objects::nonNull)
                        .findAny();
                Optional<IRI> linkPredicate = bindingSets.stream()
                        .map(bs -> (IRI) bs.getValue("linkPredicate")).filter(Objects::nonNull).findAny();

                List<Literal> targetDatasetTitles = bindingSets.stream()
                        .map(bs -> (Literal) bs.getValue("targetDatasetTitle")).filter(Objects::nonNull)
                        .distinct().collect(toList());

                LinksetMetadata.Target targetDatasetDescription = new LinksetMetadata.Target();
                targetDatasetDescription.setDataset(targetDataset);
                targetDatasetDescription.setUriSpace(targetDatasetUriSpace);
                targetDatasetDescription.setTitles(targetDatasetTitles);

                Map<IRI, List<BindingSet>> registeredDatasetsBindingSets = bindingSets.stream()
                        .filter(bs -> bs.hasBinding("registeredTargetDataset"))
                        .collect(Collectors.groupingBy(bs -> (IRI) bs.getValue("registeredTargetDataset")));

                List<Target> registeredTargets = registeredDatasetsBindingSets.entrySet().stream()
                        .map(entry2 -> {
                            List<BindingSet> bss = entry2.getValue();
                            List<Literal> titles = bss.stream()
                                    .map(bs -> (Literal) bs.getValue("registeredTargetDatasetTitle"))
                                    .filter(Objects::nonNull).distinct().collect(toList());
                            Optional<String> projectName = bss.stream()
                                    .map(bs -> bs.getValue("registeredTargetProjectName"))
                                    .filter(Objects::nonNull)
                                    .map(Value::stringValue)
                                    .findAny();

                            Target target = new Target();
                            target.setDataset(entry2.getKey());
                            target.setUriSpace(targetDatasetUriSpace);
                            target.setTitles(titles);
                            projectName.ifPresent(target::setProjectName);

                            return target;
                        }).collect(toList());

                // if the declared target is exactly one of the registered datasets, then leave only that in
                // the list
                Optional<Target> matchingRegisteredTarget = registeredTargets.stream()
                        .filter(t -> Objects.equals(t.getDataset(), targetDataset)).findAny();
                if (matchingRegisteredTarget.isPresent()) {
                    registeredTargets = Collections.singletonList(matchingRegisteredTarget.get());
                }

                LinksetMetadata linksetMetadata = new LinksetMetadata();
                linksetMetadata.setSourceDataset(sourceDataset);
                linksetMetadata.setTargetDataset(targetDatasetDescription);
                linksetMetadata.setRegisteredTargets(registeredTargets);
                linksetMetadata.setLinkCount(linkCount);
                linksetMetadata.setLinkPredicate(linkPredicate);

                if (linkCount.orElse(0) > treshold) {
                    rv.add(linksetMetadata);
                }
            }

            // optional linkset coalescing
            if (coalesce) {
                Map<Pair<IRI, IRI>, List<LinksetMetadata>> groupedLinksets = rv.stream()
                        .collect(Collectors.groupingBy(ls -> ImmutablePair.of(ls.getSourceDataset(),
                                ls.getTargetDataset().getDataset())));

                rv = new ArrayList<>(groupedLinksets.keySet().size());

                for (Entry<Pair<IRI, IRI>, List<LinksetMetadata>> entry : groupedLinksets.entrySet()) {
                    List<LinksetMetadata> linksetsDecriptions = entry.getValue();

                    LinksetMetadata pivotLinksetDescription = linksetsDecriptions.get(0);

                    // clear link predicate if the linksets do not agree
                    if (linksetsDecriptions.stream().map(LinksetMetadata::getLinkPredicate).distinct()
                            .count() != 1) {
                        pivotLinksetDescription.setLinkPredicate(Optional.empty());
                    }

                    // sum the link counts, unless any of them is not specified
                    if (linksetsDecriptions.stream().map(LinksetMetadata::getLinkCount)
                            .allMatch(Optional::isPresent)) {
                        pivotLinksetDescription.setLinkCount(Optional.of(linksetsDecriptions.stream()
                                .map(LinksetMetadata::getLinkCount).mapToInt(Optional::get).sum()));

                    } else {
                        pivotLinksetDescription.setLinkCount(Optional.empty());
                    }

                    rv.add(pivotLinksetDescription);
                }
            }
            return rv;
        }
    }

    public List<DatasetMetadata> listDatasetMetadata(BindingSet bindingSet, int limit) {
        BiFunction<String, String, String> wrapWithOptional = (var,
                pattern) -> bindingSet.hasBinding(var) ? pattern : "OPTIONAL {\n" + pattern + "\n}\n";

        try (RepositoryConnection conn = getConnection()) {
            StringBuilder tupleQuerySb = new StringBuilder(
                    // @formatter:off
                    "PREFIX dcat: <http://www.w3.org/ns/dcat#>                                          \n" +
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                          \n" +
                            "PREFIX dcterms: <http://purl.org/dc/terms/>                                          \n" +
                            "PREFIX void: <http://rdfs.org/ns/void#>                                              \n" +
                            "PREFIX mdreg: <http://semanticturkey.uniroma2.it/ns/mdr#>                          \n" +
                            "PREFIX stmdreg: <http://semanticturkey.uniroma2.it/ns/stmdr#>                          \n" +
                            "PREFIX owl: <http://www.w3.org/2002/07/owl#>                                         \n" +
                            "SELECT ?dataset (MIN(?distribution) as ?distributionT) (MIN(?datasetUriSpace) as ?datasetUriSpaceT) (MIN(?datasetTitle) as ?datasetTitleT) (MIN(?datasetDescription) as ?datasetDescriptionT) \n" +
                            "       (MIN(?datasetDereferenciationSystem) as ?datasetDereferenciationSystemT) \n" +
                            "       (GROUP_CONCAT(CONCAT(STR(?datasetSPARQLEndpoint), \"|_|\", COALESCE(STR(?datasetSPARQLEndpointLimitation), \"-\")); separator=\"|_|\") as ?datasetSPARQLEndpointT) \n" +
                            "       (MIN(?datasetVersionInfo) as ?datasetVersionInfoT) \n" +
                            "       (MIN(?datasetProjectName) as ?datasetProjectNameT) \n" +
                            "WHERE {                                                                              \n");
            // @formatter:on

            List<String> bindingNames = new ArrayList<>(bindingSet.getBindingNames());

            if (!bindingNames.isEmpty()) {
                tupleQuerySb.append("VALUES(\n");
                for (String n : bindingNames) {
                    tupleQuerySb.append(" 	?").append(n).append("\n");
                }
                tupleQuerySb.append(") {\n");
                tupleQuerySb.append(" 	(\n");
                for (String n : bindingNames) {
                    tupleQuerySb.append(" 	").append(RenderUtils.toSPARQL(bindingSet.getValue(n)))
                            .append("\n");
                }
                tupleQuerySb.append(" 	)\n");
                tupleQuerySb.append("}\n");

            }

            tupleQuerySb.append(
                    // @formatter:off
                    "   [] dcat:dataset ?dataset .                                                        \n" +
                            " 	?dataset dcat:distribution ?distribution .                                        \n" +
                            wrapWithOptional.apply("datasetUriSpace",
                                    "     ?dataset void:uriSpace ?datasetUriSpace .                                      \n"
                            ) +
                            wrapWithOptional.apply("datasetTitle",
                                    " 	  ?dataset dcterms:title ?datasetTitle .                                         \n"
                            ) +
                            wrapWithOptional.apply("datasetDescription",
                                    " 	  ?dataset dcterms:description ?datasetDescription .                             \n"
                            ) +

                            wrapWithOptional.apply("datasetDereferenciationSystem",
                                    " 	?dataset mdreg:dereferenciationSystem ?datasetDereferenciationSystem .            \n"
                            ) +
                            wrapWithOptional.apply("datasetSPARQLEndpoint",
                                    " 	    ?distribution void:sparqlEndpoint ?datasetSPARQLEndpoint .                        \n" +
                                            "   OPTIONAL { ?datasetSPARQLEndpoint mdreg:sparqlEndpointLimitation ?datasetSPARQLEndpointLimitation . } \n"
                            ) +
                            wrapWithOptional.apply("datasetProjectName",
                                    " 	    ?distribution foaf:name ?datasetProjectName .                        \n" +
                                            "   FILTER EXISTS { ?distribution a stmdreg:Project . } \n"
                            ) +
                            wrapWithOptional.apply("datasetVersionInfo",
                                    " 	?dataset owl:versionInfo ?datasetVersionInfo .                                    \n"
                            ) +
                            "}                                                                                    \n" +
                            "GROUP BY ?dataset                                                                    \n" +
                            "HAVING BOUND(?dataset)                                                               \n" +
                            (limit > 0 ? "LIMIT " + limit + "\n" : ""));
            // @formatter:on

            TupleQuery datasetQuery = conn.prepareTupleQuery(tupleQuerySb.toString());
            return QueryResults.stream(datasetQuery.evaluate()).map(this::bindingset2datasetmetadata)
                    .collect(toList());
        }
    }

    public Optional<DatasetMetadata> getDatasetMetadata(BindingSet bindingSet) {
        return listDatasetMetadata(bindingSet, 1).stream().findFirst();
    }

    public Optional<DatasetMetadata> getUnambiguousDatasetMetadata(BindingSet bindingSet)
            throws MetadataRegistryStateException {
        List<DatasetMetadata> candidates = listDatasetMetadata(bindingSet, 2);

        if (candidates.size() > 1) {
            throw new MetadataRegistryStateException(
                    "Ambiguous constraints over datasets: " + bindingSet.toString());
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(candidates.iterator().next());
    }

    @Override
    public Collection<LexicalizationSetMetadata> getEmbeddedLexicalizationSets(IRI dataset) {
        try (RepositoryConnection conn = getConnection()) {
            checkLocallyDefined(conn, dataset);

            TupleQuery tupleQuery = conn.prepareTupleQuery(
                    // @formatter:off
                    "PREFIX dcat: <http://www.w3.org/ns/dcat#>                                                     \n" +
                            "PREFIX dcterms: <http://purl.org/dc/terms/>                                                   \n" +
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                                     \n" +
                            "PREFIX void: <http://rdfs.org/ns/void#>                                                       \n" +
                            "PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                                               \n" +
                            "                                                                                              \n" +
                            "SELECT * WHERE {                                                                              \n" +
                            "  ?dataset dcat:distribution ?lexicalizationSetReferenceDataset .                             \n" +
                            "  ?lexicalizationSetReferenceDataset void:subset ?lexicalizationSet .                         \n" +
                            "                                                                                              \n" +
                            "  ?lexicalizationSet a lime:LexicalizationSet .                                               \n" +
                            "  ?lexicalizationSet lime:referenceDataset ?lexicalizationSetReferenceDataset .               \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:lexiconDataset ?lexicalizationSetLexiconDataset                   \n" +
                            "  }                                                                                           \n" +
                            "  ?lexicalizationSet lime:lexicalizationModel ?lexicalizationSetLexicalizationModel .         \n" +
                            "  ?lexicalizationSet lime:language ?lexicalizationSetLanguage .                               \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:references ?lexicalizationSetReferences                           \n" +
                            "  }                                                                                           \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:lexicalEntries ?lexicalizationSetLexicalEntries                   \n" +
                            "  }                                                                                           \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:lexicalizations ?lexicalizationSetLexicalizations                 \n" +
                            "  }                                                                                           \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:percentage ?lexicalizationSetPercentage                           \n" +
                            "  }                                                                                           \n" +
                            "  OPTIONAL {                                                                                  \n" +
                            "    ?lexicalizationSet lime:avgNumOfLexicalizations ?lexicalizationSetAvgNumOfLexicalizations \n" +
                            "  }                                                                                           \n" +
                            "}                                                                                             \n"
                    // @formatter:on
            );
            tupleQuery.setBinding("dataset", dataset);
            return QueryResults.stream(tupleQuery.evaluate()).map(this::bindingset2lexicalizationsetmetadata)
                    .collect(toList());
        }
    }

    protected LexicalizationSetMetadata bindingset2lexicalizationsetmetadata(BindingSet bs) {
        IRI identity = (IRI) bs.getValue("lexicalizationSet");
        IRI referenceDataset = (IRI) bs.getValue("lexicalizationSetReferenceDataset");
        IRI lexiconDataset = (IRI) bs.getValue("lexicalizationSetLexiconDataset");
        IRI lexicalizationModel = (IRI) bs.getValue("lexicalizationSetLexicalizationModel");
        String language = bs.getValue("lexicalizationSetLanguage").stringValue();
        BigInteger references = Optional.ofNullable(bs.getValue("lexicalizationSetReferences"))
                .map(l -> Literals.getIntegerValue(l, null)).orElse(null);
        BigInteger lexicalEntries = Optional.ofNullable(bs.getValue("lexicalizationSetLexicalEntries"))
                .map(l -> Literals.getIntegerValue(l, null)).orElse(null);
        BigInteger lexicalizations = Optional.ofNullable(bs.getValue("lexicalizationSetLexicalizations"))
                .map(l -> Literals.getIntegerValue(l, null)).orElse(null);
        BigDecimal percentage = Optional.ofNullable(bs.getValue("lexicalizationSetPercentage"))
                .map(l -> Literals.getDecimalValue(l, null)).orElse(null);
        BigDecimal avgNumOfLexicalizations = Optional
                .ofNullable(bs.getValue("lexicalizationSetAvgNumOfLexicalizations"))
                .map(l -> Literals.getDecimalValue(l, null)).orElse(null);

        return new LexicalizationSetMetadata(identity, referenceDataset, lexiconDataset, lexicalizationModel,
                language, references, lexicalEntries, lexicalizations, percentage, avgNumOfLexicalizations);
    }

    /*
     * (non-Javadoc)
     *
     * @see it.uniroma2.art.semanticturkey.resources.M#findDatasetForResource(org.eclipse.rdf4j.model.IRI)
     */
    @Override
    public DatasetMetadata findDatasetForResource(IRI iriResource) {
        // -------------------------------------------------------------------------------------------
        // The following resolution strategy might be subject to ambiguity in some rare circumstances

        try (RepositoryConnection conn = getConnection()) {
            ValueFactory vf = conn.getValueFactory();
            Optional<DatasetMetadata> dataset;

            // -----------------------------------------
            // Case 1: The provided URI is the base URI

            MapBindingSet bindings = new MapBindingSet();
            bindings.addBinding("datasetUriSpace", vf.createLiteral(iriResource.stringValue()));
            dataset = getDatasetMetadata(bindings);

            if (dataset.isPresent()) {
                return dataset.get();
            }

            // ------------------------------------------
            // Case 2: The namespace is the base URI
            // e.g., [http://example.org/]Person

            String namespace = iriResource.getNamespace();

            bindings.clear();
            bindings.addBinding("datasetUriSpace", vf.createLiteral(namespace));

            dataset = getDatasetMetadata(bindings);

            if (dataset.isPresent()) {
                return dataset.get();
            }

            // --------------------------------------------
            // Case 2: The namespace is the base URI + "#"
            // e.g., [http://example.org]#Person

            if (namespace.endsWith("#")) {
                bindings.clear();
                bindings.addBinding("datasetUriSpace",
                        vf.createLiteral(namespace.substring(0, namespace.length() - 1)));

                dataset = getDatasetMetadata(bindings);

                if (dataset.isPresent()) {
                    return dataset.get();
                }
            } else {
                return null;
            }
        }

        return null;
    }

    @Override
    public Collection<DatasetMetadata> listDatasetsForResource(IRI iriResource) {
        List<DatasetMetadata> rv = new ArrayList<>();

        // -------------------------------------------------------------------------------------------
        // The following resolution strategy might be subject to ambiguity in some rare circumstances

        try (RepositoryConnection conn = getConnection()) {
            ValueFactory vf = conn.getValueFactory();
            List<DatasetMetadata> dataset;

            // -----------------------------------------
            // Case 1: The provided URI is the base URI

            MapBindingSet bindings = new MapBindingSet();
            bindings.addBinding("datasetUriSpace", vf.createLiteral(iriResource.stringValue()));
            dataset = listDatasetMetadata(bindings, 0);
            rv.addAll(dataset);


            // ------------------------------------------
            // Case 2: The namespace is the base URI
            // e.g., [http://example.org/]Person

            String namespace = iriResource.getNamespace();

            bindings.clear();
            bindings.addBinding("datasetUriSpace", vf.createLiteral(namespace));

            dataset = listDatasetMetadata(bindings, 0);
            rv.addAll(dataset);

            // --------------------------------------------
            // Case 2: The namespace is the base URI + "#"
            // e.g., [http://example.org]#Person

            if (namespace.endsWith("#")) {
                bindings.clear();
                bindings.addBinding("datasetUriSpace",
                        vf.createLiteral(namespace.substring(0, namespace.length() - 1)));

                dataset = listDatasetMetadata(bindings, 0);
                rv.addAll(dataset);
            }
        }

        return rv;
    }

    protected DatasetMetadata bindingset2datasetmetadata(BindingSet bs) {
        IRI dataset = (IRI) bs.getValue("dataset");

        @Nullable
        IRI distribution = (IRI) bs.getValue("distributionT");

        @Nullable
        String uriSpace = Optional.ofNullable(bs.getValue("datasetUriSpaceT")).map(Value::stringValue)
                .filter(s -> !s.isEmpty()).orElse(null);
        @Nullable
        String title = Optional.ofNullable(bs.getValue("datasetTitleT")).map(Value::stringValue)
                .filter(s -> !s.isEmpty()).orElse(null);

        @Nullable
        String description = Optional.ofNullable(bs.getValue("datasetDescriptionT")).map(Value::stringValue)
                .filter(s -> !s.isEmpty()).orElse(null);

        @Nullable
        IRI dereferenciationSystem = Optional.ofNullable(bs.getValue("datasetDereferenciationSystemT"))
                .filter(s -> !s.stringValue().isEmpty()).map(IRI.class::cast).orElse(null);
        @Nullable
        String sparqlEndpointSpec = Optional.ofNullable(bs.getValue("datasetSPARQLEndpointT"))
                .map(Value::stringValue).filter(s -> !s.isEmpty()).orElse(null);

        @Nullable
        String projectName = Optional.ofNullable(bs.getValue("datasetProjectNameT"))
                .map(Value::stringValue).filter(s -> !s.isEmpty()).orElse(null);

        @Nullable
        DatasetMetadata.SPARQLEndpointMedatadata sparqlEndpointMetadata;
        if (sparqlEndpointSpec != null) {
            String[] sparqlEndpointParts = sparqlEndpointSpec.split("\\|_\\|");

            IRI sparqlEndpoint = null;
            Set<IRI> endpointLimitations = new HashSet<>();
            for (int i = 0; i < sparqlEndpointParts.length; i += 2) {
                SimpleValueFactory vf = SimpleValueFactory.getInstance();
                IRI endpoint = vf.createIRI(sparqlEndpointParts[i]);
                String endpointLimitation = sparqlEndpointParts[i + 1];

                if (sparqlEndpoint == null) {
                    sparqlEndpoint = endpoint;
                } else if (!sparqlEndpoint.equals(endpoint)) {
                    continue;
                }

                if (!endpointLimitation.equals("-")) {
                    endpointLimitations.add(vf.createIRI(endpointLimitation));
                }
            }
            sparqlEndpointMetadata = new DatasetMetadata.SPARQLEndpointMedatadata(sparqlEndpoint, endpointLimitations);
        } else {
            sparqlEndpointMetadata = null;
        }

        @Nullable
        String versionInfo = Optional.ofNullable(bs.getValue("datasetVersionInfoT")).map(Value::stringValue)
                .orElse(null);

        return new DatasetMetadata(dataset, distribution, uriSpace, title, description, dereferenciationSystem,
                sparqlEndpointMetadata, versionInfo, projectName);
    }

    protected Pair<DatasetMetadata, Model> discoverDatasetMetadataInternal(IRI iri)
            throws MetadataDiscoveryException {
        try {
            logger.debug("Attempt to discover a dataset from {}", NTriplesUtil.toNTriplesString(iri));

            Boolean datasetDeferenceable = null;

            // Dereferences the provided IRI
            RDFLoader rdfLoader = RDF4JUtilities.createRobustRDFLoader();
            Model statements = new LinkedHashModel();
            Model voidStatements = null;

            logger.debug("About to download data");

            try {
                rdfLoader.load(new URL(iri.stringValue()), null, null, new StatementCollector(statements));
            } catch (IOException | RDFParseException | RDFHandlerException e) {
                logger.debug("Swallowed exception thrown when dereferecing " + NTriplesUtil.toNTriplesString(iri), e);
                // swallow exception
            }

            // If the data returned from the IRI contains statements about such IRI, then we assume that the
            // dataset is dereferenceable
            if (statements.contains(iri, null, null)) {
                datasetDeferenceable = true;
            }

            logger.debug("Was any statement downloaded? {}", datasetDeferenceable);

            // Try to determine the dataset resource

            // First attempt: look at the property void:inDataset

            @Nullable
            IRI voidDataset = Models.getPropertyIRI(statements, iri, VOID.IN_DATASET).orElse(null);

            logger.debug("Reference to VoID dataset: {}", voidDataset);

            Literal datasetTitle = null;
            Literal datasetDescription = null;
            IRI datasetSPARQLEndpoint = null;
            String datasetUriSpace = null;

            // Second Attempt: the provided IRI is the void:DatasetSpecification
            if (voidDataset == null && statements.contains(iri, RDF.TYPE, VOID.DATASET)) {
                logger.debug("The provided IRI is actually a VoID dataset");
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
                    logger.debug("The provided IRI is an OWL ontology");
                    voidDataset = candidateDataset;
                    voidStatements = statements;
                    datasetDeferenceable = true;
                    datasetUriSpace = ModelUtilities
                            .createDefaultNamespaceFromBaseURI(candidateDataset.stringValue());
                }
            }

            if (datasetUriSpace == null && Boolean.TRUE.equals(datasetDeferenceable)) {
                datasetUriSpace = iri.getNamespace();
                logger.debug("Namespace inferred from specific resource: {}", datasetUriSpace);
            }

            if (voidStatements == null) {
                logger.debug("Attempting to retrieve a VoID description");

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

                    logger.debug("Guessed VoID download URL: {}", voidDownloadAddress);
                } else {
                    voidDownloadAddress = new URL(voidDataset.stringValue());
                    logger.debug("using the retrieved VoID download URL: {}", voidDownloadAddress);
                }

                // Try to download the void:DatasetSpecification
                try {
                    logger.debug("Downloading VoID description from URL: {}", voidDownloadAddress);
                    rdfLoader.load(voidDownloadAddress, null, null, new StatementCollector(voidStatements));
                } catch (IOException | RDFParseException | RDFHandlerException e) {
                    // swallow exception
                }
            }

            // Use the VoID metadata (if available) to identify the VoID dataset. This happens when the
            // download URL was guessed

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

                logger.debug("Identified VoID dataset is {}", voidDataset);
            }

            // Use VoID metadata (if available)

            if (voidDataset != null) {

                logger.debug("Extract information held by the VoID dataset");

                if (voidStatements.contains(voidDataset, null, null)) {
                    datasetTitle = Models.getPropertyLiteral(voidStatements, voidDataset, DCTERMS.TITLE)
                            .orElse(datasetTitle);
                    if (datasetTitle == null) {
                        datasetTitle = Models.getPropertyLiteral(voidStatements, voidDataset, DC.TITLE)
                                .orElse(datasetTitle);
                    }
                    datasetDescription = Models
                            .getPropertyLiteral(voidStatements, voidDataset, DCTERMS.DESCRIPTION)
                            .orElse(datasetTitle);
                    if (datasetDescription == null) {
                        datasetDescription = Models
                                .getPropertyLiteral(voidStatements, voidDataset, DC.DESCRIPTION)
                                .orElse(datasetDescription);
                    }

                    datasetSPARQLEndpoint = Models
                            .getPropertyIRI(voidStatements, voidDataset, VOID.SPARQL_ENDPOINT)
                            .orElse(datasetSPARQLEndpoint);
                    datasetUriSpace = Models.getPropertyString(voidStatements, voidDataset, VOID.URI_SPACE)
                            .orElse(datasetUriSpace);
                }

                logger.debug("Title = \"{}\", SPARQL endpoint = \"{}\", URI space = \"{}\"", datasetTitle,
                        datasetSPARQLEndpoint, datasetUriSpace);
            }

            if (datasetDeferenceable == null) {
                logger.debug("Try to find an example resource for testing dereferenciation");

                IRI exampleResource = null;

                if (voidDataset != null) {
                    exampleResource = Models
                            .getPropertyIRI(voidStatements, voidDataset, VOID.EXAMPLE_RESOURCE).orElse(null);
                    logger.debug("Example resource using VoID description: {}", exampleResource);
                }

                if (exampleResource == null && datasetSPARQLEndpoint != null && datasetUriSpace != null) {
                    logger.debug("Attempt to identify an example resource using SPARQL");
                    SPARQLRepository sparqlRepository = new SPARQLRepository(
                            datasetSPARQLEndpoint.stringValue());
                    sparqlRepository.init();
                    try {
                        String exampleResourceQuery =
                                // @formatter:off
                                "SELECT ?resource {\n" +
                                        "  ?resource ?p ?o .\n" +
                                        "  FILTER(isIRI(?resource))\n" +
                                        "  FILTER(STRSTARTS(STR(?resource), ?datasetUriSpace))\n" +
                                        "}\n" +
                                        "LIMIT 1\n";
                        // @formatter:on

                        logger.debug("SPARQL Query to find example resource:\n{}", exampleResourceQuery);
                        try (RepositoryConnection conn = sparqlRepository.getConnection()) {
                            TupleQuery query = conn.prepareTupleQuery(exampleResourceQuery);
                            query.setBinding("datasetUriSpace",
                                    SimpleValueFactory.getInstance().createLiteral(datasetUriSpace));
                            exampleResource = (IRI) QueryResults.asList(query.evaluate()).stream()
                                    .map(bs -> bs.getValue("resource")).findAny().orElse(null);
                            logger.debug("Example resource using SPARQL: {}", exampleResource);
                        }

                    } finally {
                        sparqlRepository.shutDown();
                    }
                }

                if (exampleResource != null) {
                    logger.debug("Attempt to dereference example resource: {}", exampleResource);

                    Model exampleResourceStatements = new LinkedHashModel();
                    try {
                        rdfLoader.load(new URL(exampleResource.stringValue()), null, null,
                                new StatementCollector(exampleResourceStatements));
                    } catch (IOException | RDFParseException | RDFHandlerException e) {
                        // swallow exception
                    }

                    datasetDeferenceable = exampleResourceStatements.contains(exampleResource, null, null);

                    logger.debug("Did dereferenciation succeed? {}", datasetDeferenceable);
                }

            }

            if (datasetUriSpace != null) {
                return ImmutablePair.of(new DatasetMetadata(voidDataset, null, datasetUriSpace,
                        datasetTitle != null ? datasetTitle.stringValue() : null,
                        datasetDescription != null ? datasetDescription.stringValue() : null,
                        METADATAREGISTRY.getDereferenciationSystem(datasetDeferenceable).orElse(null),
                        datasetSPARQLEndpoint != null
                                ? new DatasetMetadata.SPARQLEndpointMedatadata(datasetSPARQLEndpoint,
                                Collections.emptySet())
                                : null,
                        null, null), voidStatements);
            } else {
                throw new MetadataDiscoveryException(
                        "Could not discover a dataset from " + RenderUtils.toSPARQL(iri));
            }

        } catch (MetadataDiscoveryException e) {
            throw e; // pass through the exception without wrapping it further
        } catch (MalformedURLException e) {
            throw new MetadataDiscoveryException(e);
        }
    }

    @Override
    public DatasetMetadata discoverDatasetMetadata(IRI iri) throws MetadataDiscoveryException {
        return discoverDatasetMetadataInternal(iri).getLeft();
    }

    /*
     * (non-Javadoc)
     *
     * @see it.uniroma2.art.semanticturkey.resources.M#discoverDataset(org.eclipse.rdf4j.model.IRI)
     */
    @Override
    public IRI discoverDataset(IRI iri) throws MetadataDiscoveryException {
        Pair<DatasetMetadata, Model> pair = discoverDatasetMetadataInternal(iri);
        DatasetMetadata datasetMetadata = pair.getLeft();
        Model voidStatements = pair.getRight();
        try {
            IRI voidDataset = datasetMetadata.getIdentity();

            Distribution distribution = new Distribution(null, METADATAREGISTRY.SPARQL_ENDPOINT, datasetMetadata.getSparqlEndpoint().orElse(null), null);
            IRI datasetIRI = createConcreteDataset(
                    voidDataset,
                    datasetMetadata.getUriSpace().orElse(null),
                    datasetMetadata.getTitle().map(Values::literal).orElse(null),
                    datasetMetadata.getDescription().map(Values::literal).orElse(null),
                    datasetMetadata.getDereferenciationSystem().map(METADATAREGISTRY.STANDARD_DEREFERENCIATION::equals).orElse(null),
                    distribution,
                    null,
                    false);


            if (voidDataset != null) {
                for (IRI subset : Models.objectIRIs(voidStatements.filter(voidDataset, VOID.SUBSET, null))) {
                    if (voidStatements.contains(subset, RDF.TYPE, LIME.LEXICALIZATION_SET)
                            && voidStatements.contains(subset, LIME.REFERENCE_DATASET, voidDataset)) {
                        IRI lexiconDataset = Models
                                .getPropertyIRI(voidStatements, subset, LIME.LEXICON_DATASET).orElse(null);
                        IRI lexicalizationModel = Models
                                .getPropertyIRI(voidStatements, subset, LIME.LEXICALIZATION_MODEL)
                                .orElse(null);
                        String language = Models.getPropertyString(voidStatements, subset, LIME.LANGUAGE)
                                .orElse(null);
                        BigInteger lexicalEntries = Models
                                .getPropertyLiteral(voidStatements, subset, LIME.LEXICAL_ENTRIES)
                                .map(l -> Literals.getIntegerValue(l, BigInteger.ZERO)).orElse(null);
                        BigInteger references = Models
                                .getPropertyLiteral(voidStatements, subset, LIME.REFERENCES)
                                .map(l -> Literals.getIntegerValue(l, BigInteger.ZERO)).orElse(null);
                        BigInteger lexicalizations = Models
                                .getPropertyLiteral(voidStatements, subset, LIME.LEXICALIZATIONS)
                                .map(l -> Literals.getIntegerValue(l, BigInteger.ZERO)).orElse(null);
                        BigDecimal percentage = Models
                                .getPropertyLiteral(voidStatements, subset, LIME.PERCENTAGE)
                                .map(l -> Literals.getDecimalValue(l, BigDecimal.ZERO)).orElse(null);
                        BigDecimal avgNumOfLexicalizations = Models
                                .getPropertyLiteral(voidStatements, subset, LIME.AVG_NUM_OF_LEXICALIZATIONS)
                                .map(l -> Literals.getDecimalValue(l, BigDecimal.ZERO)).orElse(null);

                        if (language == null || lexicalizationModel == null)
                            continue;

                        addEmbeddedLexicalizationSet(datasetIRI, null, lexiconDataset, lexicalizationModel,
                                language, references, lexicalEntries, lexicalizations, percentage,
                                avgNumOfLexicalizations);
                    }


                    if (voidStatements.contains(subset, RDF.TYPE, VOID.LINKSET)
                            && voidStatements.contains(subset, VOID.SUBJECTS_TARGET, voidDataset)) {

                        Resource objectsTarget = Models.getPropertyResource(voidStatements, subset, VOID.OBJECTS_TARGET).orElse(null);
                        if (ObjectUtils.notEqual(objectsTarget, voidDataset)) {
                            /* Nullable */
                            String targetUriSpace = Models.getPropertyString(voidStatements, objectsTarget, VOID.URI_SPACE).orElse(null);
                            /* Nullable */
                            IRI linkPredicate = Models.getPropertyIRI(voidStatements, subset, VOID.LINK_PREDICATE).orElse(null);
                            /* Nullable */
                            Integer linkCount = Models.getPropertyLiteral(voidStatements, subset, VOID.TRIPLES).map(l -> Literals.getIntValue(l, 0)).filter(v -> v != 0).orElse(null);

                            if (targetUriSpace != null) {
                                addEmbeddedLinkset(datasetIRI, null, targetUriSpace, linkPredicate, linkCount);
                            }
                        }
                    }
                }
            }

            try (RepositoryConnection repConn = getConnection()) {
                return QueryResults.stream(
                                repConn.getStatements(null, FOAF.PRIMARY_TOPIC, datasetIRI, false))
                        .findFirst()
                        .map(s -> (IRI) s.getSubject())
                        .orElseThrow(() -> new IllegalStateException("Could not find catalog record for dataset " + datasetIRI));
            }

        } catch (Exception e) {
            throw new MetadataDiscoveryException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend#discoverLexicalizationSets(org.eclipse
     * .rdf4j.model.IRI)
     */
    @Override
    public synchronized void discoverLexicalizationSets(IRI dataset)
            throws AssessmentException, MetadataRegistryWritingException {

        try (RepositoryConnection metadataConn = metadataRegistry.getConnection()) {
            IRI distribution = QueryResults.stream(metadataConn.getStatements(dataset, DCAT.HAS_DISTRIBUTION, null)).map(s -> (IRI) s.getObject()).findAny().orElseThrow(() -> new IllegalStateException("Could not find distribution for dataset: " + dataset));
            Function<RepositoryConnection, Model> computeDatasetDescription = conn -> QueryResults
                    .asModel(conn.prepareGraphQuery("DESCRIBE * WHERE { " + RenderUtils.toSPARQL(distribution) + " <"
                            + VOID.SUBSET + ">* ?dataset  }").evaluate());

            Model originalDescription = computeDatasetDescription.apply(metadataConn);
            mediationFramework.discoverLexicalizationSets(metadataConn, distribution);
            Model possibilyUpdatedDescription = computeDatasetDescription.apply(metadataConn);

            if (!Models.isomorphic(originalDescription, possibilyUpdatedDescription)) {
                Update updateModificationDate = metadataConn.prepareUpdate(
                        // @formatter:off
                        " PREFIX dcterms: <http://purl.org/dc/terms/>                                \n" +
                                " PREFIX foaf: <http://xmlns.com/foaf/0.1/>                                  \n" +
                                " PREFIX dcat: <http://www.w3.org/ns/dcat#>                                  \n" +
                                "                                                                            \n" +
                                " DELETE {                                                                   \n" +
                                "   ?record dcterms:modified ?oldModified .                                  \n" +
                                " }                                                                          \n" +
                                " INSERT {                                                                   \n" +
                                "   ?record dcterms:modified ?now .                                          \n" +
                                " }                                                                          \n" +
                                " WHERE {                                                                    \n" +
                                "   ?record foaf:topic|foaf:primaryTopic ?dataset ; a dcat:CatalogRecord .   \n" +
                                "   OPTIONAL { ?record dcterms:modified ?oldModified . }                     \n" +
                                "   BIND(NOW() AS ?now)                                                      \n" +
                                " }                                                                          \n"
                        // @formatter:on
                );
                updateModificationDate.setBinding("dataset", dataset);
                updateModificationDate.execute();
            }

        }

        saveToFile();
    }

    @Override
    public synchronized Optional<IRI> getComputedLexicalizationModel(IRI dataset) throws AssessmentException {
        try (RepositoryConnection metadataConn = metadataRegistry.getConnection()) {
            Model profile = extractProfile(dataset);
            return mediationFramework.assessLexicalizationModel(dataset, profile);
        }
    }

    @Override
    public Model extractProfile(IRI dataset) {
        try (RepositoryConnection conn = getConnection()) {
            return QueryResults.asModel(conn.prepareGraphQuery("DESCRIBE * WHERE { "
                    + RenderUtils.toSPARQL(dataset) + " (<" + DCAT.HAS_DISTRIBUTION + ">/<" + VOID.SUBSET + ">*)? ?dataset  }").evaluate());
        }
    }

    public static IRI computeProjectContext(String projectName, ValueFactory vf) {
        return vf.createIRI(UriComponentsBuilder.fromHttpUrl(DEFAULT_BASEURI + "{project}")
                .buildAndExpand(ImmutableMap.of("project", projectName)).toUriString());
    }

    public static Optional<String> computeProjectFromContext(IRI ctx) {
        try {
            if (DEFAULT_BASEURI.equals(ctx.getNamespace())) {
                return Optional.of(URLDecoder.decode(ctx.getLocalName(), StandardCharsets.UTF_8.name()));
            } else {
                return Optional.empty();
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("It shouldn't have happened with UTF-8", e);
        }
    }

    @Override
    public Map<String, Integer> getClassPartitions(IRI dataset) {
        Map<String, Integer> classPartitions = new HashMap<>();

        try (RepositoryConnection conn = getConnection()) {
            checkLocallyDefined(conn, dataset);

            TupleQuery tupleQuery = conn.prepareTupleQuery(
                    // @formatter:off
                    "PREFIX void: <http://rdfs.org/ns/void#>						\n" +
                    "PREFIX dcat: <http://www.w3.org/ns/dcat#>						\n" +
                    "PREFIX voaf: <http://purl.org/vocommons/voaf#>					\n" +
                    "SELECT * WHERE {												\n" +
                    "	?dataset dcat:distribution ?referenceDataset .				\n" +
                    "	?referenceDataset void:entities ?datasetEntities .			\n" +
                    //for SKOS
                    "   OPTIONAL {                                                  \n" +
                    "       ?referenceDataset void:classPartition ?classPartition .	\n" +
                    "       ?classPartition void:class ?cls .						\n" +
                    "       ?classPartition void:entities ?entities .				\n" +
                    "   }                                                           \n" +
                    //for OWL
                    "   OPTIONAL {                                                  \n" +
                    "       ?referenceDataset voaf:classNumber ?classNumber .   	\n" +
                    "       ?referenceDataset voaf:propertyNumber ?propertyNumber .   	\n" +
                    "   }                                                           \n" +
                    "}"
                    // @formatter:on
            );
            tupleQuery.setBinding("dataset", dataset);

            QueryResults.stream(tupleQuery.evaluate()).forEach(bs -> {
                int datasetEntities = Integer.parseInt(((Literal) bs.getValue("datasetEntities")).getLabel());
                //this will be written for each referenceDataset, but it will be simply overwritten each time
                classPartitions.put(dataset.stringValue(), datasetEntities);

                if (bs.hasBinding("classPartition")) {
                    Value cls = bs.getValue("cls");
                    int clsEntities = Integer.parseInt(((Literal) bs.getValue("entities")).getLabel());
                    classPartitions.put(cls.stringValue(), clsEntities);
                }

                if (bs.hasBinding("classNumber")) {
                    int classNumber = Integer.parseInt(((Literal) bs.getValue("classNumber")).getLabel());
                    classPartitions.put("Classes", classNumber);
                    int propertyNumber = Integer.parseInt(((Literal) bs.getValue("propertyNumber")).getLabel());
                    classPartitions.put("Properties", propertyNumber);
                }
            });
            return classPartitions;
        }
    }


}
