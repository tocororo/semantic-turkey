package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolverClient;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;

/**
 * A {@link NotifyingSail} keeping track of changes to an underlying {@code Sail}. Each commit containing at
 * least one relevant update is recorded into the <i>support repository</i>. The representation of the history
 * conforms to the vocabulary {@link it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG}.
 * <p>
 * A change is recorded only if it is an effective update to the underlying data: i.e. either adding a triple
 * that wasn't already assert, or removing a previously asserted triples. Self-canceling operations are
 * ignored as well.
 * <p>
 * The client of this <code>Sail</code> can manage the tracking system, by reading/writing appropriate
 * contexts: these operations are intercepted by this Sail, so that they are not executed against the
 * underlying data. Currently, only <code>addStatement(..)</code>, <code>removeStatements(...)</code> and
 * <code>getStatements(..)</code> are supported. In particular, SPARQL Queries cannot be used to read the
 * special-purpose contexts defined by this <code>Sail</code>.
 * <p>
 * The contexts {@link CHANGETRACKER#STAGED_ADDITIONS} and {@link CHANGETRACKER#STAGED_REMOVALS} can be used
 * to list the triples being staged for addition and removal, respectively.
 * <p>
 * The context {@link CHANGETRACKER#GRAPH_MANAGEMENT} contains the description of a homonymous resource, which
 * is associated with the graphs to include and exclude, via the properties
 * {@link CHANGETRACKER#INCLUDE_GRAPH} and {@link CHANGETRACKER#EXCLUDE_GRAPH}, respectively. An update is
 * recorded into the history only if its context is a graph such that it satisfies the inclusion criterion and
 * it does not satisfy the exclusion criterion. An empty set of included graphs is equivalent to include all
 * graphs, while an empty set of excluded graphs is equivalent to not rejecting any graph. The resource
 * {@link CHANGELOG#NULL} (formerly {@link SESAME#NIL}) represents the <code>null</code> context, while
 * {@link SESAME#WILDCARD} is another mechanism to specify all graphs.
 * <p>
 * By default, the history ignores the <code>null</code> context and includes other contexts. Excluding the
 * <code>null</code> context is a simple mechanism to ignore inferred triples.
 * <p>
 * It is possible to write additional metadata about a commit through the context
 * {@link CHANGETRACKER#COMMIT_METADATA}. Since the client doesn't know which resource will represent the
 * commit in the history repository, the client may identify the commit via the IRI
 * {@link CHANGETRACKER#COMMIT_METADATA}: when the commit metadata is written into the history, this
 * identifier will be replace with the actual identifier of the commit.
 * <p>
 * When validation is enabled, write operations are first logged inside the validation graph (in the support
 * repository) and applied to the staging graphs (in the core repository).
 * <p>
 * To accept/reject a previous transaction, it is sufficient to write in the context
 * {@link CHANGETRACKER#VALIDATION}, as follows:
 * {@code conn.add(commitIRI, CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT | CHANGETRACKER.ACCEPT, CHANGETRACKER.VALIDATION)}
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTracker extends NotifyingSailWrapper implements RepositoryResolverClient {
	protected static final Logger logger = LoggerFactory.getLogger(ChangeTracker.class);

	public static final Optional<Boolean> OPTIONAL_TRUE = Optional.of(true);
	public static final Optional<Boolean> OPTIONAL_FALSE = Optional.of(false);
	public static final String PROPERTIES = "changetracker.properties";
	protected static final Properties props;
	protected static final String PROP_VERSION = "version";
	protected static final String version;
	protected final String supportRepoId;
	protected final String serverURL;
	protected final String metadataNS;
	protected final IRI historyGraph;
	protected final IRI validationGraph;
	protected final IRI blacklistGraph;
	protected Repository supportRepo;
	protected final Model graphManagement;
	protected final boolean historyEnabled;
	protected final boolean validationEnabled;
	protected final boolean blacklistEnabled;
	protected final Optional<Boolean> interactiveNotifications;
	private Function<String, Repository> repositoryResolver;

	static {
		props = new Properties();
		try {
			props.load(ChangeTracker.class.getResourceAsStream(PROPERTIES));
			version = props.getProperty(PROP_VERSION);

			if (version == null || version.equals("") || version.contains("$"))
				throw new IllegalStateException("Wrong version number detected: " + version);
		} catch (IOException e) {
			logger.error(
					"Exception preventing initialization of class " + ChangeTracker.class.getSimpleName(), e);
			throw new IllegalStateException(e);
		}
	}

	public static String getVersion() {
		return version;
	}

	public ChangeTracker(/* @Nullable */ String serverURL, String supportRepoId, String metadataNS,
			IRI historyGraph, Set<IRI> includeGraph, Set<IRI> excludeGraph, boolean historyEnabled,
			boolean validationEnabled, Optional<Boolean> interactiveNotifications,
			/* @Nullable */ IRI validationGraph, boolean blacklistEnabled,
			/* @Nullable */ IRI blacklistGraph) {
		this.serverURL = serverURL;
		this.supportRepoId = supportRepoId;
		this.metadataNS = metadataNS;
		this.historyGraph = historyGraph;
		this.graphManagement = new LinkedHashModel();
		this.historyEnabled = historyEnabled;
		this.validationEnabled = validationEnabled;
		this.validationGraph = validationGraph;
		this.blacklistGraph = blacklistGraph;
		this.interactiveNotifications = interactiveNotifications;

		includeGraph.forEach(
				g -> graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, g));
		excludeGraph.forEach(
				g -> graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, g));
		if (historyGraph != null) {
			graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.HISTORY_GRAPH, historyGraph);
		}

		if (validationGraph != null) {
			graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.VALIDATION_GRAPH,
					validationGraph);
		}

		this.blacklistEnabled = blacklistEnabled;
		if (blacklistGraph != null) {
			graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.BLACKLIST_GRAPH,
					blacklistGraph);
		}
	}

	public void setRepositoryResolver(RepositoryResolver resolver) {
		this.repositoryResolver = resolver::getRepository;
	}

	public void setRepositoryResolver(org.eclipse.rdf4j.repository.sail.config.RepositoryResolver resolver) {
		this.repositoryResolver = resolver::getRepository;
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();

		if (serverURL != null) {
			supportRepo = new HTTPRepository(serverURL, supportRepoId);
		} else {
			supportRepo = new RepositoryWrapper(repositoryResolver.apply(supportRepoId)) {
				@Override
				public void shutDown() throws RepositoryException {
					// Ignore shutdown of the referenced repository
				}
			};
		}
	}

	@Override
	public void shutDown() throws SailException {
		try {
			if (supportRepo != null) {
				supportRepo.shutDown();
				supportRepo = null;
			}
		} finally {
			super.shutDown();
		}
	}

	@Override
	public ChangeTrackerConnection getConnection() throws SailException {
		logger.debug("Obtaining new connection");
		NotifyingSailConnection delegate = super.getConnection();
		ChangeTrackerConnection connection = new ChangeTrackerConnection(delegate, this);
		return connection;
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		IsolationLevel isolationLevel = super.getDefaultIsolationLevel();

		if (interactiveNotifications.equals(OPTIONAL_FALSE)) {
			return isolationLevel;
		} else {
			if (isolationLevel.isCompatibleWith(IsolationLevels.SERIALIZABLE)) {
				return isolationLevel;
			} else {
				return MoreObjects.firstNonNull(
						IsolationLevels.getCompatibleIsolationLevel(IsolationLevels.SERIALIZABLE,
								super.getSupportedIsolationLevels()),
						IsolationLevels.SERIALIZABLE);
			}
		}
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		List<IsolationLevel> supportedByDelegate = super.getSupportedIsolationLevels();

		if (interactiveNotifications.equals(OPTIONAL_FALSE)) {
			return supportedByDelegate;
		} else {
			return supportedByDelegate.stream()
					.filter(level -> level.isCompatibleWith(IsolationLevels.SERIALIZABLE))
					.collect(Collectors.toList());
		}
	}

}