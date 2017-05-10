package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;

/**
 * A {@link NotifyingSailConnection} which is returned by a {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerConnection extends NotifyingSailConnectionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ChangeTracker.class);

	private final SailConnectionListener connectionListener;
	private final ChangeTracker sail;
	private final StagingArea stagingArea;
	private Model connectionLocalGraphManagement;
	private final UpdateHandler updateHandler;

	private Literal startTime;

	public ChangeTrackerConnection(NotifyingSailConnection wrappedCon, ChangeTracker sail) {
		super(wrappedCon);
		this.sail = sail;
		this.stagingArea = new StagingArea();
		this.connectionLocalGraphManagement = null;

		this.connectionListener = new SailConnectionListener() {

			@Override
			public void statementAdded(Statement st) {
				if (shouldTrackStatement(st)) {
					stagingArea.stageAddition(st);
				}
			}

			@Override
			public void statementRemoved(Statement st) {
				if (shouldTrackStatement(st)) {
					stagingArea.stageRemoval(st);
				}
			}

		};
		getWrappedConnection().addConnectionListener(connectionListener);
		updateHandler = new FlagUpdateHandler();
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
		stagingArea.clear();
		updateHandler.clearHandler();
		startTime = currentTimeAsLiteral();
		logger.debug("Transaction Begin / Isolation Level = {}", level);
	}

	@Override
	public void rollback() throws SailException {
		try {
			updateHandler.clearHandler();
			stagingArea.clear();
		} finally {
			super.rollback();
		}
		logger.debug("Transaction Rollback");
	}

	@Override
	public void commit() throws SailException {
		Function<IRI, Function<RepositoryConnection, Function<IRI, Consumer<? super Statement>>>> consumer = commit -> conn -> predicate -> stmt -> {
			Resource stmtRes = conn.getValueFactory().createIRI(sail.metadataNS,
					UUID.randomUUID().toString());

			conn.add(stmtRes, RDF.TYPE, CHANGELOG.QUADRUPLE, sail.metadataGraph);
			conn.add(stmtRes, CHANGELOG.SUBJECT, stmt.getSubject(), sail.metadataGraph);
			conn.add(stmtRes, CHANGELOG.PREDICATE, stmt.getPredicate(), sail.metadataGraph);
			conn.add(stmtRes, CHANGELOG.OBJECT, stmt.getObject(), sail.metadataGraph);

			Resource ctx = stmt.getContext();
			if (ctx == null) {
				ctx = SESAME.NIL;
			}

			conn.add(stmtRes, CHANGELOG.CONTEXT, ctx, sail.metadataGraph);
			conn.add(commit, predicate, stmtRes, sail.metadataGraph);
		};

		// Some triple stores (e.g. GraphDB, at least version 8.0.4) notify triple additions/deletions only
		// during the commit operation. Therefore, we can't be certain that an empty staging area means a
		// read-only transaction.

		// In a read-only connection, just execute the commit
		if (updateHandler.isReadOnly()) {
			super.commit();
			return;
		}

		// If the underlying triple store sends interactive notifications, and the staging area is empty then
		// we shuld commit and return
		if (sail.interactiveNotifications && stagingArea.isEmpty()) {
			super.commit();
			return;
		}

		// However, if some triples have been staged or the triple triple store does not send interactive
		// notifications, then we need to log the operations.

		synchronized (sail) {
			// Prepares data commit (in case of success, it is unlikely that a subsequent commit() fails)
			prepare();

			// Commits the metadata
			IRI commitIRI;
			IRI modifiedTriplesIRI;
			
			Resource previousTip = null;
			Model commitMetadataModel = new LinkedHashModel();
			boolean triplesUnknown = false;
			
			try (RepositoryConnection metaRepoConn = sail.metadataRepo.getConnection()) {
				ValueFactory vf = metaRepoConn.getValueFactory();

				metaRepoConn.begin();
				
				Literal generationTime = currentTimeAsLiteral();
				Literal endTime = currentTimeAsLiteral();

				// Gets the tip of MASTER:
				// - if the history is empty, then there is no MASTER and its associated tip
				// - otherwise, there should be exactly one tip, which is the last successful commit
				List<Statement> headList = QueryResults.asList(metaRepoConn.getStatements(CHANGELOG.MASTER,
						CHANGELOG.TIP, null, sail.metadataGraph));

				if (headList.size() > 1) {
					throw new SailException(
							"Could not commit the changeset metadata, since the tip of MASTER is not unique: "
									+ headList);
				}

				// If the tip of MASTER is defined, check that has status committed. Otherwise, it could be
				// possible that the registered commit has not been effectively applied to the data
				// repository. In this case, subsequent commits are denied, until the consistency between data
				// and metadata is restored.

				if (headList.size() == 1) {
					previousTip = (Resource) headList.iterator().next().getObject();

					boolean previousTipCommitted = metaRepoConn.hasStatement(previousTip, CHANGELOG.STATUS,
							vf.createLiteral("committed"), false, sail.metadataGraph);

					if (!previousTipCommitted) {
						throw new SailException(
								"Could not commit the changeset metadata, since there is a pending commit: "
										+ previousTip);
					}
				}

				commitIRI = vf.createIRI(sail.metadataNS, UUID.randomUUID().toString());

				stagingArea.getCommitMetadataModel().stream().map(st -> {
					Resource s = st.getSubject();
					IRI p = st.getPredicate();
					Value o = st.getObject();

					boolean refactorS = s.equals(CHANGETRACKER.COMMIT_METADATA);
					boolean refactorP = p.equals(CHANGETRACKER.COMMIT_METADATA);
					boolean refactorO = o.equals(CHANGETRACKER.COMMIT_METADATA);

					if (refactorS || refactorP || refactorO) {
						return SimpleValueFactory.getInstance().createStatement(refactorS ? commitIRI : s,
								refactorP ? commitIRI : p, refactorO ? commitIRI : o);
					} else {
						return st;
					}
				}).forEach(commitMetadataModel::add);

				metaRepoConn.add(commitIRI, RDF.TYPE, CHANGELOG.COMMIT, sail.metadataGraph);
				metaRepoConn.add(commitIRI, PROV.STARTED_AT_TIME, startTime, sail.metadataGraph);
				metaRepoConn.add(commitIRI, PROV.ENDED_AT_TIME, endTime, sail.metadataGraph);

				if (!commitMetadataModel.isEmpty()) {
					metaRepoConn.add(commitMetadataModel, sail.metadataGraph);
				}

				if (stagingArea.isEmpty()) {
					triplesUnknown = true;
					metaRepoConn.add(commitIRI, CHANGELOG.STATUS, vf.createLiteral("triples-unknown"),
							sail.metadataGraph);
				} else {
					modifiedTriplesIRI = vf.createIRI(sail.metadataNS, UUID.randomUUID().toString());
					
					metaRepoConn.add(modifiedTriplesIRI, RDF.TYPE, PROV.ENTITY, sail.metadataGraph);
					stagingArea.getAddedStatements().forEach(
							consumer.apply(modifiedTriplesIRI).apply(metaRepoConn).apply(CHANGELOG.ADDED_STATEMENT));
					stagingArea.getRemovedStatements().forEach(
							consumer.apply(modifiedTriplesIRI).apply(metaRepoConn).apply(CHANGELOG.REMOVED_STATEMENT));
					metaRepoConn.add(modifiedTriplesIRI, PROV.WAS_GENERATED_BY, commitIRI, sail.metadataGraph);
					metaRepoConn.add(modifiedTriplesIRI, PROV.GENERATED_AT_TIME, generationTime, sail.metadataGraph);
					metaRepoConn.add(commitIRI, PROV.GENERATED, modifiedTriplesIRI, sail.metadataGraph);
				}

				if (previousTip != null) {
					metaRepoConn.add(commitIRI, CHANGELOG.PARENT_COMMIT, previousTip, sail.metadataGraph);
					metaRepoConn.remove(CHANGELOG.MASTER, CHANGELOG.TIP, previousTip, sail.metadataGraph);
				}
				metaRepoConn.add(CHANGELOG.MASTER, CHANGELOG.TIP, commitIRI, sail.metadataGraph);

				metaRepoConn.commit();
			} catch (RepositoryException e) {
				// It may be the case that metadata have been committed, but for some reason (e.g.
				// disconnection from a remote metadata repo) the transaction status cannot be reported back
				throw new SailException(e);
			}

			// Commits the data
			try {
				super.commit();
			} catch (SailException e) {
				// commit() has failed, so we should undo the history
				removeLastCommit(commitIRI, previousTip, commitMetadataModel, triplesUnknown);
				throw e;
			}

			// Data has been committed. So, mark the MASTER commit as committed

			// If triples were unknown when the commit was first logged, then add that information now
			// (note that in the meantime the triple store should have sent the necessary notifications)

			if (triplesUnknown && stagingArea.isEmpty()) {
				removeLastCommit(commitIRI, previousTip, commitMetadataModel, triplesUnknown);
			} else {
				try (RepositoryConnection metaRepoConn = sail.metadataRepo.getConnection()) {
					ValueFactory vf = metaRepoConn.getValueFactory();
	
					metaRepoConn.begin();
	
					// If triples were unknown when the commit was first logged, then add that information now
					// (note that in the meantime the triple store should have sent the necessary notifications)
	
					if (triplesUnknown) {
						stagingArea.getAddedStatements().forEach(consumer.apply(commitIRI).apply(metaRepoConn)
								.apply(CHANGELOG.ADDED_STATEMENT));
						stagingArea.getRemovedStatements().forEach(consumer.apply(commitIRI)
								.apply(metaRepoConn).apply(CHANGELOG.REMOVED_STATEMENT));
					}
					metaRepoConn.remove(commitIRI, CHANGELOG.STATUS, null, sail.metadataGraph);
					metaRepoConn.add(commitIRI, CHANGELOG.STATUS, vf.createLiteral("committed"),
							sail.metadataGraph);
					metaRepoConn.commit();
				} catch (RepositoryException e) {
					// We would end up with data committed and metadata missing that information.
					// Since we don't known if data have been committed or not, it is safest to make fail
					// subsequent commit attempts.
					throw new SailException(e);
				}
			}

			stagingArea.clear();
			updateHandler.clearHandler();
		}

		// Note that if the MASTER's tip is not marked as committed, we are unsure about whether or not
		// the commit has been applied to the data repo
	}

	protected Literal currentTimeAsLiteral() throws SailException {
		GregorianCalendar calendar = new GregorianCalendar();
		XMLGregorianCalendar currentDateTimeXML;
		try {
			currentDateTimeXML = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
		} catch (DatatypeConfigurationException e) {
			throw new SailException(e);
		}
		return SimpleValueFactory.getInstance().createLiteral(currentDateTimeXML);
	}

	private void removeLastCommit(IRI commitIRI, Resource previousTip, Model commitMetadataModel,
			boolean triplesUnknown) throws SailException {
		try (RepositoryConnection metaRepoConn = sail.metadataRepo.getConnection()) {
			metaRepoConn.begin();

			if (!triplesUnknown) {
				Update triplesRemoveUpdate = metaRepoConn.prepareUpdate(
						"REMOVE { ?quad ?p ?o . } WHERE {?commit <" + CHANGELOG.ADDED_STATEMENT
								+ ">|<" + CHANGELOG.REMOVED_STATEMENT + "> ?quad . }");
				triplesRemoveUpdate.setBinding("commit", commitIRI);
				triplesRemoveUpdate.execute();
			}

			metaRepoConn.remove(commitIRI, null, null, sail.metadataGraph);

			if (!commitMetadataModel.isEmpty()) {
				metaRepoConn.remove(commitMetadataModel, sail.metadataGraph);
			}
			metaRepoConn.remove(CHANGELOG.MASTER, CHANGELOG.TIP, commitIRI, sail.metadataGraph);

			if (previousTip != null) {
				metaRepoConn.add(CHANGELOG.MASTER, CHANGELOG.TIP, previousTip, sail.metadataGraph);
			}

			metaRepoConn.commit();
		} catch (RepositoryException e2) {
			// we might be unable to rollback the metadata. In this case, we could end up with a
			// a commit (the status of which is not "committed" and without a commit in the data
			throw new SailException(e2);
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		Collection<Iteration<? extends Statement, SailException>> iterations = new ArrayList<>(2);
		if (contextList.contains(CHANGETRACKER.STAGED_ADDITIONS)) {
			iterations.add(new CollectionIteration<>(stagingArea.getAddedStatements()));
			contextList.remove(CHANGETRACKER.STAGED_ADDITIONS);
		}
		if (contextList.contains(CHANGETRACKER.STAGED_REMOVALS)) {
			iterations.add(new CollectionIteration<>(stagingArea.getRemovedStatements()));
			contextList.remove(CHANGETRACKER.STAGED_REMOVALS);
		}
		if (contextList.contains(CHANGETRACKER.GRAPH_MANAGEMENT)) {
			iterations.add(new CollectionIteration<>(getGraphManagementModel().filter(subj, pred, obj)));
			contextList.remove(CHANGETRACKER.GRAPH_MANAGEMENT);
		}
		if (contextList.contains(CHANGETRACKER.COMMIT_METADATA)) {
			iterations.add(new CollectionIteration<>(getCommitMetadataModel().filter(subj, pred, obj)));
			contextList.remove(CHANGETRACKER.COMMIT_METADATA);
		}
		if (contextList.size() > 0 || iterations.isEmpty()) {
			Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
			iterations.add(super.getStatements(subj, pred, obj, includeInferred, newContexts));
		}
		return new UnionIteration<>(iterations);
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		if (contextList.contains(CHANGETRACKER.GRAPH_MANAGEMENT)) {
			addToGraphManagementModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.GRAPH_MANAGEMENT);
		}
		if (contextList.contains(CHANGETRACKER.COMMIT_METADATA)) {
			addToCommitMetadataModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.COMMIT_METADATA);
		}

		if (contexts.length == 0 || !contextList.isEmpty()) {
			Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
			try {
				updateHandler.addStatement(subj, pred, obj, newContexts);
				super.addStatement(subj, pred, obj, newContexts);
			} catch (Exception e) {
				updateHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		if (contextList.contains(CHANGETRACKER.GRAPH_MANAGEMENT)) {
			addToGraphManagementModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.GRAPH_MANAGEMENT);
		}
		if (contextList.contains(CHANGETRACKER.COMMIT_METADATA)) {
			addToCommitMetadataModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.COMMIT_METADATA);
		}

		if (contexts.length == 0 || !contextList.isEmpty()) {
			Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
			try {
				updateHandler.addStatement(modify, subj, pred, obj, newContexts);
				super.addStatement(modify, subj, pred, obj, newContexts);
			} catch (Exception e) {
				updateHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		if (contextList.contains(CHANGETRACKER.GRAPH_MANAGEMENT)) {
			removeFromGraphManagementModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.GRAPH_MANAGEMENT);
		}
		if (contextList.contains(CHANGETRACKER.COMMIT_METADATA)) {
			removeFromCommitMetadataModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.COMMIT_METADATA);
		}

		if (contexts.length == 0 || !contextList.isEmpty()) {
			Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
			try {
				updateHandler.removeStatements(subj, pred, obj, newContexts);
				super.removeStatements(subj, pred, obj, newContexts);
			} catch (Exception e) {
				updateHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		if (contextList.contains(CHANGETRACKER.GRAPH_MANAGEMENT)) {
			removeFromGraphManagementModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.GRAPH_MANAGEMENT);
		}
		if (contextList.contains(CHANGETRACKER.COMMIT_METADATA)) {
			removeFromCommitMetadataModel(subj, pred, obj);
			contextList.remove(CHANGETRACKER.COMMIT_METADATA);
		}

		if (contexts.length == 0 || !contextList.isEmpty()) {
			Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
			try {
				updateHandler.removeStatement(modify, subj, pred, obj, newContexts);
				super.removeStatement(modify, subj, pred, obj, newContexts);
			} catch (Exception e) {
				updateHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		updateHandler.clear(contexts);
		try {
			super.clear(contexts);
		} catch (Exception e) {
			updateHandler.recordCorruption();
			throw e;
		}
	}

	@Override
	public void clearNamespaces() throws SailException {
		updateHandler.clearNamespaces();
		try {
			super.clearNamespaces();
		} catch (Exception e) {
			updateHandler.recordCorruption();
			throw e;
		}
	}

	@Override
	public void removeNamespace(String prefix) throws SailException {
		updateHandler.removeNamespace(prefix);
		try {
			super.removeNamespace(prefix);
		} catch (Exception e) {
			updateHandler.recordCorruption();
			throw e;
		}
	}

	private void removeFromGraphManagementModel(Resource subj, IRI pred, Value obj) {
		if (connectionLocalGraphManagement == null) {
			connectionLocalGraphManagement = new LinkedHashModel(sail.graphManagement);
		}

		connectionLocalGraphManagement.remove(subj, pred, obj);
	}

	private void addToGraphManagementModel(Resource subj, IRI pred, Value obj) {
		if (connectionLocalGraphManagement == null) {
			connectionLocalGraphManagement = new LinkedHashModel(sail.graphManagement);
		}

		connectionLocalGraphManagement.add(subj, pred, obj);
	}

	private Model getGraphManagementModel() {
		if (connectionLocalGraphManagement != null) {
			return connectionLocalGraphManagement;
		} else {
			return sail.graphManagement;
		}
	}

	protected boolean shouldTrackStatement(Statement st) {
		Model model = getGraphManagementModel();

		Resource context = st.getContext();

		if (context == null) {
			context = SESAME.NIL;
		}

		// A context is included iff it is pulled in by graph inclusions but thrown out by graph exclusions

		if (model.contains(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, context)
				|| model.contains(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH,
						SESAME.WILDCARD)
				|| model.filter(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.INCLUDE_GRAPH, null)
						.isEmpty()) {

			if (model.contains(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, context) || model
					.contains(CHANGETRACKER.GRAPH_MANAGEMENT, CHANGETRACKER.EXCLUDE_GRAPH, SESAME.WILDCARD)) {
				return false;
			}

			return true;
		}

		return false;
	}

	private Model getCommitMetadataModel() {
		return stagingArea.getCommitMetadataModel();
	}

	private void addToCommitMetadataModel(Resource subj, IRI pred, Value obj) {
		stagingArea.getCommitMetadataModel().add(subj, pred, obj);
	}

	private void removeFromCommitMetadataModel(Resource subj, IRI pred, Value obj) {
		stagingArea.getCommitMetadataModel().remove(subj, pred, obj);
	}
}
