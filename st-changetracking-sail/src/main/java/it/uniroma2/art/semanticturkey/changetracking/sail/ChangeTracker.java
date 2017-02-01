package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;

/**
 * A {@link NotifyingSail} keeping track of changes to an underlying {@code Sail}. Each commit containing at
 * least one relevant update is recorded into the <i>history repository</i>. The representation of the history
 * conforms to the vocabulary {@link CHANGELOG}.
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
 * {@link SESAME#NIL} represents the <code>null</code> context, while {@link SESAME#WILDCARD} is another
 * mechanism to specify all graphs.
 * <p>
 * By default, the history ignores the <code>null</code> context and includes other contexts. Excluding the
 * <code>null</code> context is a simple mechanism to ignore inferred triples.
 * <p>
 * Each commit in the history is associated by default to its creation date via the property
 * {@link DCTERMS#CREATED}. It is possible to write additional metadata through the context
 * {@link CHANGETRACKER#COMMIT_METADATA}. Since the client doesn't know which resource will represent the
 * commit in the history repository, the client may identify the commit via the IRI
 * {@link CHANGETRACKER#COMMIT_METADATA}: when the commit metadata is written into the history, this
 * identifier will be replace with the actual identifier of the commit. If the client specify a value for the
 * property {@link DCTERMS#CREATED}, then its value won't be computed automatically.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTracker extends NotifyingSailWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ChangeTracker.class);

	final Repository metadataRepo;
	final String metadataNS;
	final IRI metadataGraph;

	final Model graphManagement;

	public ChangeTracker(Repository metadataRepo, String metadataNS, IRI metadataGraph, Set<IRI> includeGraph,
			Set<IRI> excludeGraph) {
		this.metadataRepo = metadataRepo;
		this.metadataNS = metadataNS;
		this.metadataGraph = metadataGraph;
		this.graphManagement = new LinkedHashModel();
		includeGraph.forEach(
				g -> graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, g));
		excludeGraph.forEach(
				g -> graphManagement.add(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, g));
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
	}

	@Override
	public void shutDown() throws SailException {
		super.shutDown();
	}

	@Override
	public ChangeTrackerConnection getConnection() throws SailException {
		logger.debug("Obtaining new connection");
		NotifyingSailConnection delegate = super.getConnection();
		ChangeTrackerConnection connection = new ChangeTrackerConnection(delegate, this);
		return connection;
	}

}