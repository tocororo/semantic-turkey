package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.math.BigInteger;
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
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.changetracking.model.HistoryRepositories;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;

/**
 * A {@link NotifyingSailConnection} which is returned by a {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerConnection extends NotifyingSailConnectionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ChangeTracker.class);

	private SailConnectionListener connectionListener;
	private final ChangeTracker sail;
	private final StagingArea stagingArea;
	private Model connectionLocalGraphManagement;

	private final LoggingUpdateHandler validatableOpertionHandler;
	private final UpdateHandler readonlyHandler;

	private boolean validationEnabled;

	private Literal startTime;

	public ChangeTrackerConnection(NotifyingSailConnection wrappedCon, ChangeTracker sail) {
		super(wrappedCon);
		this.sail = sail;
		this.stagingArea = new StagingArea();
		this.connectionLocalGraphManagement = null;

		readonlyHandler = new FlagUpdateHandler();
		validatableOpertionHandler = new LoggingUpdateHandler();
		validationEnabled = sail.validationEnabled;
		if (connectionListener != null) {
			removeConnectionListener(connectionListener);
		}
		initializeListener();
	}

	public void initializeListener() {
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
		addConnectionListener(connectionListener);
	}

	@Override
	public void close() throws SailException {
		try {
			super.close();
		} finally {
			removeConnectionListener(connectionListener);
		}
	}

	@Override
	public void begin() throws SailException {
		begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
		stagingArea.clear();
		readonlyHandler.clearHandler();
		validatableOpertionHandler.clearHandler();
		startTime = currentTimeAsLiteral();
		logger.debug("Transaction Begin / Isolation Level = {}", level);
	}

	@Override
	public void rollback() throws SailException {
		try {
			readonlyHandler.clearHandler();
			validatableOpertionHandler.clearHandler();
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

			conn.add(stmtRes, RDF.TYPE, CHANGELOG.QUADRUPLE, sail.historyGraph);
			conn.add(stmtRes, CHANGELOG.SUBJECT, stmt.getSubject(), sail.historyGraph);
			conn.add(stmtRes, CHANGELOG.PREDICATE, stmt.getPredicate(), sail.historyGraph);
			conn.add(stmtRes, CHANGELOG.OBJECT, stmt.getObject(), sail.historyGraph);

			Resource ctx = stmt.getContext();
			if (ctx == null) {
				ctx = SESAME.NIL;
			}

			conn.add(stmtRes, CHANGELOG.CONTEXT, ctx, sail.historyGraph);
			conn.add(commit, predicate, stmtRes, sail.historyGraph);
		};

		Function<IRI, Function<RepositoryConnection, Function<IRI, Consumer<? super QuadPattern>>>> consumer2 = modifiedTriples -> conn -> predicate -> quad -> {
			Resource stmtRes = conn.getValueFactory().createIRI(sail.metadataNS,
					UUID.randomUUID().toString());

			conn.add(stmtRes, RDF.TYPE, CHANGELOG.QUADRUPLE, sail.validationGraph);
			conn.add(stmtRes, CHANGELOG.SUBJECT, MoreObjects.firstNonNull(quad.getSubject(), SESAME.NIL),
					sail.validationGraph);
			conn.add(stmtRes, CHANGELOG.PREDICATE, MoreObjects.firstNonNull(quad.getPredicate(), SESAME.NIL),
					sail.validationGraph);
			conn.add(stmtRes, CHANGELOG.OBJECT, MoreObjects.firstNonNull(quad.getObject(), SESAME.NIL),
					sail.validationGraph);

			conn.add(stmtRes, CHANGELOG.CONTEXT, MoreObjects.firstNonNull(quad.getContext(), SESAME.NIL),
					sail.validationGraph);
			conn.add(modifiedTriples, predicate, stmtRes, sail.validationGraph);
		};

		synchronized (sail) {
			// Checks if there are requested (validatable) operations to log
			if (!validatableOpertionHandler.isReadOnly()) {
				try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {

					supportRepoConn.begin();

					Model validatableCommitMetadataModel = new LinkedHashModel();

					IRI validatableCommit = supportRepoConn.getValueFactory().createIRI(sail.metadataNS,
							UUID.randomUUID().toString());
					IRI validatableModifiedTriples = supportRepoConn.getValueFactory()
							.createIRI(sail.metadataNS, UUID.randomUUID().toString());
					supportRepoConn.add(validatableCommit, RDF.TYPE, CHANGELOG.COMMIT, sail.validationGraph);
					supportRepoConn.add(validatableCommit, PROV.STARTED_AT_TIME, startTime,
							sail.validationGraph);
					supportRepoConn.add(validatableCommit, PROV.ENDED_AT_TIME, currentTimeAsLiteral(),
							sail.validationGraph);
					supportRepoConn.add(validatableModifiedTriples, RDF.TYPE, PROV.ENTITY,
							sail.validationGraph);
					supportRepoConn.add(validatableCommit, PROV.GENERATED, validatableModifiedTriples,
							sail.validationGraph);
					supportRepoConn.add(validatableModifiedTriples, PROV.WAS_GENERATED_BY, validatableCommit,
							sail.validationGraph);

					generateCommitMetadataModel(validatableCommit, validatableCommitMetadataModel);

					if (!validatableCommitMetadataModel.isEmpty()) {
						supportRepoConn.add(validatableCommitMetadataModel, sail.validationGraph);

					}

					validatableOpertionHandler.getAdditions()
							.forEach(consumer2.apply(validatableModifiedTriples).apply(supportRepoConn)
									.apply(CHANGELOG.ADDED_STATEMENT));
					validatableOpertionHandler.getRemovals()
							.forEach(consumer2.apply(validatableModifiedTriples).apply(supportRepoConn)
									.apply(CHANGELOG.REMOVED_STATEMENT));

					supportRepoConn.commit();
				}
			}
		}
		// Some triple stores (e.g. GraphDB, at least version 8.0.4) notify triple additions/deletions only
		// during the commit operation. Therefore, we can't be certain that an empty staging area means a
		// read-only transaction.

		// In a read-only connection, just execute the commit
		if (readonlyHandler.isReadOnly()) {
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

			Resource previousTip;
			BigInteger revisionNumber = BigInteger.ZERO;
			Model commitMetadataModel = new LinkedHashModel();
			boolean triplesUnknown = false;

			Literal generationTime = currentTimeAsLiteral();
			Literal endTime = currentTimeAsLiteral();

			try (RepositoryConnection supporRepoConn = sail.supportRepo.getConnection()) {
				ValueFactory vf = supporRepoConn.getValueFactory();

				supporRepoConn.begin();

				// Gets the tip of MASTER:
				// - if the history is empty, then there is no MASTER and its associated tip
				// - otherwise, there should be exactly one tip, which is the last successful commit
				List<Statement> headList = QueryResults.asList(supporRepoConn.getStatements(CHANGELOG.MASTER,
						CHANGELOG.TIP, null, sail.historyGraph));

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

					boolean previousTipCommitted = supporRepoConn.hasStatement(previousTip, CHANGELOG.STATUS,
							vf.createLiteral("committed"), false, sail.historyGraph);

					if (!previousTipCommitted) {
						throw new SailException(
								"Could not commit the changeset metadata, since there is a pending commit: "
										+ previousTip);
					}

					BigInteger lastRevisionNumber = Models.objectLiteral(QueryResults.asModel(supporRepoConn
							.getStatements(previousTip, CHANGELOG.REVISION_NUMBER, null, sail.historyGraph)))
							.map(lit -> {
								try {
									return new BigInteger(lit.getLabel());
								} catch (NumberFormatException e) {
									throw new SailException(
											"Current tip has an illegal revision number: " + lit.getLabel());
								}
							}).orElseThrow(() -> new SailException(
									"Current tip does not have a revision number: " + previousTip));

					revisionNumber = lastRevisionNumber.add(BigInteger.ONE);
				} else {
					previousTip = null;
				}

				commitIRI = vf.createIRI(sail.metadataNS, UUID.randomUUID().toString());

				generateCommitMetadataModel(commitIRI, commitMetadataModel);

				supporRepoConn.add(commitIRI, RDF.TYPE, CHANGELOG.COMMIT, sail.historyGraph);
				supporRepoConn.add(commitIRI, PROV.STARTED_AT_TIME, startTime, sail.historyGraph);
				supporRepoConn.add(commitIRI, PROV.ENDED_AT_TIME, endTime, sail.historyGraph);
				supporRepoConn.add(commitIRI, CHANGELOG.REVISION_NUMBER,
						supporRepoConn.getValueFactory().createLiteral(revisionNumber), sail.historyGraph);

				if (!commitMetadataModel.isEmpty()) {
					supporRepoConn.add(commitMetadataModel, sail.historyGraph);
				}

				if (stagingArea.isEmpty()) {
					triplesUnknown = true;
					supporRepoConn.add(commitIRI, CHANGELOG.STATUS, vf.createLiteral("triples-unknown"),
							sail.historyGraph);
				} else {
					recordModifiedTriples(consumer, commitIRI, supporRepoConn, vf, generationTime);
				}

				if (previousTip != null) {
					supporRepoConn.add(commitIRI, CHANGELOG.PARENT_COMMIT, previousTip, sail.historyGraph);
					supporRepoConn.remove(CHANGELOG.MASTER, CHANGELOG.TIP, previousTip, sail.historyGraph);
				}
				supporRepoConn.add(CHANGELOG.MASTER, CHANGELOG.TIP, commitIRI, sail.historyGraph);

				supporRepoConn.commit();
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
				removeLastCommit(sail.historyGraph, commitIRI, previousTip, commitMetadataModel,
						triplesUnknown, true);
				throw e;
			}

			// Data has been committed. So, mark the MASTER commit as committed

			// If triples were unknown when the commit was first logged, then add that information now
			// (note that in the meantime the triple store should have sent the necessary notifications)

			if (triplesUnknown && stagingArea.isEmpty()) {
				removeLastCommit(sail.historyGraph, commitIRI, previousTip, commitMetadataModel,
						triplesUnknown, true);
			} else {
				try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
					ValueFactory vf = supportRepoConn.getValueFactory();

					supportRepoConn.begin();

					// If triples were unknown when the commit was first logged, then add that information now
					// (note that in the meantime the triple store should have sent the necessary
					// notifications)

					if (triplesUnknown) {
						recordModifiedTriples(consumer, commitIRI, supportRepoConn, vf, generationTime);
					}
					supportRepoConn.remove(commitIRI, CHANGELOG.STATUS, null, sail.historyGraph);
					supportRepoConn.add(commitIRI, CHANGELOG.STATUS, vf.createLiteral("committed"),
							sail.historyGraph);
					supportRepoConn.commit();
				} catch (RepositoryException e) {
					// We would end up with data committed and metadata missing that information.
					// Since we don't known if data have been committed or not, it is safest to make fail
					// subsequent commit attempts.
					throw new SailException(e);
				}
			}

			stagingArea.clear();
			readonlyHandler.clearHandler();
			validatableOpertionHandler.clearHandler();
		}

		// Note that if the MASTER's tip is not marked as committed, we are unsure about whether or not
		// the commit has been applied to the data repo
	}

	protected void generateCommitMetadataModel(IRI commitIRI, Collection<? super Statement> model) {
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
		}).forEach(model::add);
	}

	protected void recordModifiedTriples(
			Function<IRI, Function<RepositoryConnection, Function<IRI, Consumer<? super Statement>>>> consumer,
			IRI commitIRI, RepositoryConnection supportRepoConn, ValueFactory vf, Literal generationTime)
			throws RepositoryException {
		IRI modifiedTriplesIRI;
		modifiedTriplesIRI = vf.createIRI(sail.metadataNS, UUID.randomUUID().toString());

		supportRepoConn.add(modifiedTriplesIRI, RDF.TYPE, PROV.ENTITY, sail.historyGraph);
		stagingArea.getAddedStatements().forEach(
				consumer.apply(modifiedTriplesIRI).apply(supportRepoConn).apply(CHANGELOG.ADDED_STATEMENT));
		stagingArea.getRemovedStatements().forEach(
				consumer.apply(modifiedTriplesIRI).apply(supportRepoConn).apply(CHANGELOG.REMOVED_STATEMENT));
		supportRepoConn.add(modifiedTriplesIRI, PROV.WAS_GENERATED_BY, commitIRI, sail.historyGraph);
		supportRepoConn.add(modifiedTriplesIRI, PROV.GENERATED_AT_TIME, generationTime, sail.historyGraph);
		supportRepoConn.add(commitIRI, PROV.GENERATED, modifiedTriplesIRI, sail.historyGraph);
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

	private void removeLastCommit(IRI graph, IRI commitIRI, Resource previousTip, Model commitMetadataModel,
			boolean triplesUnknown, boolean updateTip) throws SailException {
		try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
			supportRepoConn.begin();

			if (!triplesUnknown) {
				Update triplesRemoveUpdate = supportRepoConn.prepareUpdate(
						"DELETE { ?quad ?p ?o . } WHERE {<" + commitIRI + "> <" + PROV.GENERATED
								+ "> ?modifiedTriples . ?modifiedTriples <" + CHANGELOG.ADDED_STATEMENT
								+ ">|<" + CHANGELOG.REMOVED_STATEMENT + "> ?quad . ?quad ?p ?o .}");
				SimpleDataset dataset = new SimpleDataset();
				dataset.addDefaultRemoveGraph(graph);
				dataset.addDefaultGraph(graph);
				triplesRemoveUpdate.setDataset(dataset);
				triplesRemoveUpdate.execute();
			}

			Update modifiedTriplesRemoveUpdate = supportRepoConn
					.prepareUpdate("DELETE { ?modifiedTriples ?p ?o . } WHERE {<" + commitIRI + "> <"
							+ PROV.GENERATED + "> ?modifiedTriples . ?modifiedTriples ?p ?o .}");
			SimpleDataset dataset = new SimpleDataset();
			dataset.addDefaultRemoveGraph(graph);
			dataset.addDefaultGraph(graph);
			modifiedTriplesRemoveUpdate.setDataset(dataset);
			modifiedTriplesRemoveUpdate.execute();

			supportRepoConn.remove(commitIRI, null, null, graph);

			if (!commitMetadataModel.isEmpty()) {
				supportRepoConn.remove(commitMetadataModel, graph);
			}

			if (updateTip) {
				supportRepoConn.remove(CHANGELOG.MASTER, CHANGELOG.TIP, commitIRI, graph);

				if (previousTip != null) {
					supportRepoConn.add(CHANGELOG.MASTER, CHANGELOG.TIP, previousTip, graph);
				}

			}
			supportRepoConn.commit();
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

		if (contextList.contains(CHANGETRACKER.VALIDATION)) {
			handleValidation(subj, pred, obj);
			contextList.remove(CHANGETRACKER.VALIDATION);
		}
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

				if (validationEnabled) {
					validatableOpertionHandler.addStatement(subj, pred, obj, newContexts);
					mangleAddContextsForValidation(newContexts);
				} else {
					readonlyHandler.addStatement(subj, pred, obj, newContexts);
				}

				super.addStatement(subj, pred, obj, newContexts);
			} catch (Exception e) {
				readonlyHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		List<Resource> contextList = new ArrayList<>(contexts.length);
		Arrays.stream(contexts).forEach(c -> contextList.add(c));

		if (contextList.contains(CHANGETRACKER.VALIDATION)) {
			handleValidation(subj, pred, obj);
			contextList.remove(CHANGETRACKER.VALIDATION);
		}
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
				if (validationEnabled) {
					validatableOpertionHandler.addStatement(subj, pred, obj, newContexts);
					mangleAddContextsForValidation(newContexts);
				} else {
					readonlyHandler.addStatement(modify, subj, pred, obj, newContexts);
				}

				super.addStatement(modify, subj, pred, obj, newContexts);
			} catch (Exception e) {
				readonlyHandler.recordCorruption();
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
				if (validationEnabled) {
					validatableOpertionHandler.removeStatements(subj, pred, obj, newContexts);
					mangleRemoveContextsForValidation(newContexts);
					super.addStatement(subj, pred, obj, newContexts);
				} else {
					readonlyHandler.removeStatements(subj, pred, obj, newContexts);
					super.removeStatements(subj, pred, obj, newContexts);
				}
			} catch (Exception e) {
				readonlyHandler.recordCorruption();
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
				if (validationEnabled) {
					validatableOpertionHandler.removeStatement(modify, subj, pred, obj, newContexts);
					mangleRemoveContextsForValidation(newContexts);
					super.addStatement(modify, subj, pred, obj, newContexts);
				} else {
					readonlyHandler.removeStatement(modify, subj, pred, obj, newContexts);
					super.removeStatement(modify, subj, pred, obj, newContexts);
				}
			} catch (Exception e) {
				readonlyHandler.recordCorruption();
				throw e;
			}
		}
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		readonlyHandler.clear(contexts);
		try {
			if (validationEnabled) {
				throw new NotValidatableOperationException("Could not validate clear(Resource...)");
			}
			super.clear(contexts);
		} catch (Exception e) {
			readonlyHandler.recordCorruption();
			throw e;
		}
	}

	@Override
	public void clearNamespaces() throws SailException {
		if (validationEnabled) {
			throw new NotValidatableOperationException("Could not validate clearNamespaces()");
		}

		readonlyHandler.clearNamespaces();
		try {
			super.clearNamespaces();
		} catch (Exception e) {
			readonlyHandler.recordCorruption();
			throw e;
		}
	}

	@Override
	public void removeNamespace(String prefix) throws SailException {
		if (validationEnabled) {
			throw new NotValidatableOperationException("Could not validate removeNamespace(String)");
		}

		readonlyHandler.removeNamespace(prefix);
		try {
			super.removeNamespace(prefix);
		} catch (Exception e) {
			readonlyHandler.recordCorruption();
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

		if (context.stringValue().startsWith(VALIDATION.STAGING_ADD_GRAPH.stringValue())
				|| context.stringValue().startsWith(VALIDATION.STAGING_REMOVE_GRAPH.stringValue())) {
			return false;
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

	private void mangleAddContextsForValidation(Resource[] contexts) {
		if (contexts.length == 0) {
			throw new NotValidatableOperationException(
					"Could not validate operation on the empty set of graphs");
		}

		for (int i = 0; i < contexts.length; i++) {
			contexts[i] = VALIDATION.stagingAddGraph(contexts[i]);
		}
	}

	private void mangleRemoveContextsForValidation(Resource[] contexts) {
		if (contexts.length == 0) {
			throw new NotValidatableOperationException(
					"Could not validate operation on the empty set of graphs");
		}

		for (int i = 0; i < contexts.length; i++) {
			contexts[i] = VALIDATION.stagingRemoveGraph(contexts[i]);
		}
	}

	private void handleValidation(Resource subj, IRI pred, Value obj) throws SailException {
		try {
			validationEnabled = false;
			synchronized (sail) {
				try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
					supportRepoConn.begin();

					if (CHANGETRACKER.ACCEPT.equals(pred)) {
						QueryResults.stream(HistoryRepositories.getAddedStaments(supportRepoConn, (IRI) obj,
								sail.validationGraph)).forEach(s -> {
									addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
											s.getContext());
									removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
											VALIDATION.stagingAddGraph(s.getContext()));
								});
						QueryResults.stream(HistoryRepositories.getRemovedStaments(supportRepoConn, (IRI) obj,
								sail.validationGraph)).forEach(s -> {
									removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
											s.getContext());
									removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
											VALIDATION.stagingRemoveGraph(s.getContext()));
								});
					} else if (CHANGETRACKER.REJECT.equals(pred)) {
						QueryResults.stream(HistoryRepositories.getAddedStaments(supportRepoConn, (IRI) obj,
								sail.validationGraph)).forEach(s -> {
									removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
											VALIDATION.stagingAddGraph(s.getContext()));
								});
						QueryResults.stream(HistoryRepositories.getRemovedStaments(supportRepoConn, (IRI) obj,
								sail.validationGraph)).forEach(s -> {
									removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
											VALIDATION.stagingRemoveGraph(s.getContext()));
								});
					} else {
						throw new SailException("Unrecognized operation: it should be either "
								+ NTriplesUtil.toNTriplesString(CHANGETRACKER.ACCEPT) + " or "
								+ NTriplesUtil.toNTriplesString(CHANGETRACKER.REJECT));
					}

					removeLastCommit(sail.validationGraph, (IRI) obj, null, new LinkedHashModel(), false,
							false);

					supportRepoConn.commit();
				}
			}
		} finally {
			validationEnabled = true;
		}
	}
}
