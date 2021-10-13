package it.uniroma2.art.semanticturkey.changetracking.sail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import it.uniroma2.art.semanticturkey.changetracking.model.HistoryRepositories;
import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.BLACKLIST;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
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
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A {@link NotifyingSailConnection} which is returned by a {@link ChangeTracker}.
 *
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerConnection extends NotifyingSailConnectionWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ChangeTrackerConnection.class);
    private static final IRI PARAMETERS = SimpleValueFactory.getInstance()
            .createIRI("http://semanticturkey.uniroma2.it/ns/st-changelog#parameters");
    private final ChangeTracker sail;
    private final LoggingUpdateHandler validatableOpertionHandler;
    private final UpdateHandler readonlyHandler;
    private Optional<SailConnectionListener> connectionListener;
    private StagingArea stagingArea;
    private Model connectionLocalGraphManagement;
    private boolean historyEnabled;
    private boolean validationEnabled;
    private Literal startTime;
    private IRI pendingBlacklisting;
    private IRI pendingValidation;
    private Literal pendingComment;
    private UndoSource pendingUndoFor;

    public ChangeTrackerConnection(NotifyingSailConnection wrappedCon, ChangeTracker sail) {
        super(wrappedCon);
        this.sail = sail;
        this.stagingArea = new StagingArea();
        this.connectionLocalGraphManagement = null;

        readonlyHandler = new FlagUpdateHandler();
        validatableOpertionHandler = new LoggingUpdateHandler();
        validationEnabled = sail.validationEnabled;
        connectionListener = Optional.empty();

        if (sail.historyEnabled || sail.undoEnabled) {
            initializeListener();
        }
    }

    // We used to remove the connection listener after the connection has been closed. In GDB 9.0, this raised
    // an
    // exception (because its Sail first checks that the sail is open). While we might have swapped the two
    // operations (close the connection and remove the listener), we decided not to explicitly remove the
    // listener
    // anymore, since the connection is being closed

    // @Override
    // public void close() throws SailException {
    // try {
    // super.close();
    // } finally {
    // connectionListener.ifPresent(this::removeConnectionListener);
    // }
    // }

    public void initializeListener() {
        this.connectionListener = Optional.of(new SailConnectionListener() {

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

        });
        addConnectionListener(connectionListener.get());
    }

    @Override
    public void begin() throws SailException {
        begin(sail.getDefaultIsolationLevel());
    }

    @Override
    public void begin(IsolationLevel level) throws SailException {
        if (level == null) {
            level = sail.getDefaultIsolationLevel();
        }

        List<IsolationLevel> supportedIsolationLevels = sail.getSupportedIsolationLevels();
        IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(level,
                supportedIsolationLevels);
        if (compatibleLevel == null) {
            throw new UnknownSailTransactionStateException("Isolation level " + level
                    + " not compatible with this Sail. Supported levels are: " + supportedIsolationLevels);
        }
        super.begin(compatibleLevel);

        stagingArea.clear();
        readonlyHandler.clearHandler();
        validatableOpertionHandler.clearHandler();
        startTime = currentTimeAsLiteral();
        pendingValidation = null;
        pendingBlacklisting = null;
        pendingUndoFor = null;
        logger.debug("Transaction Begin / Isolation Level = {}", level);
    }

    @Override
    public void rollback() throws SailException {
        try {
            readonlyHandler.clearHandler();
            validatableOpertionHandler.clearHandler();
            stagingArea.clear();
            pendingUndoFor = null;
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
            conn.add(stmtRes, CHANGELOG.SUBJECT,
                    HistoryRepositories.cloneValue(stmt.getSubject(), conn.getValueFactory(), null),
                    sail.historyGraph);
            conn.add(stmtRes, CHANGELOG.PREDICATE,
                    HistoryRepositories.cloneValue(stmt.getPredicate(), conn.getValueFactory(), null),
                    sail.historyGraph);
            conn.add(stmtRes, CHANGELOG.OBJECT,
                    HistoryRepositories.cloneValue(stmt.getObject(), conn.getValueFactory(), null),
                    sail.historyGraph);

            Resource ctx = stmt.getContext();
            if (ctx == null) {
                ctx = CHANGELOG.NULL;
            }

            conn.add(stmtRes, CHANGELOG.CONTEXT,
                    HistoryRepositories.cloneValue(ctx, conn.getValueFactory(), null), sail.historyGraph);
            conn.add(commit, predicate, stmtRes, sail.historyGraph);
        };

        Function<IRI, Function<RepositoryConnection, Function<IRI, Consumer<? super QuadPattern>>>> consumer2 = modifiedTriples -> conn -> predicate -> quad -> {
            Resource stmtRes = conn.getValueFactory().createIRI(sail.metadataNS,
                    UUID.randomUUID().toString());

            conn.add(stmtRes, RDF.TYPE, CHANGELOG.QUADRUPLE, sail.validationGraph);

            Resource subj = quad.getSubject();
            if (subj != null) {
                subj = HistoryRepositories.cloneValue(subj, conn.getValueFactory(), null);
            } else {
                subj = CHANGELOG.NULL;
            }

            IRI pred = quad.getPredicate();
            if (pred != null) {
                pred = HistoryRepositories.cloneValue(pred, conn.getValueFactory(), null);
            } else {
                pred = CHANGELOG.NULL;
            }

            Value obj = quad.getObject();
            if (obj != null) {
                obj = HistoryRepositories.cloneValue(obj, conn.getValueFactory(), null);
            } else {
                obj = CHANGELOG.NULL;
            }

            Resource ctx = quad.getContext();
            if (ctx != null) {
                ctx = HistoryRepositories.cloneValue(ctx, conn.getValueFactory(), null);
            } else {
                ctx = CHANGELOG.NULL;
            }
            conn.add(stmtRes, CHANGELOG.SUBJECT, subj, sail.validationGraph);
            conn.add(stmtRes, CHANGELOG.PREDICATE, pred, sail.validationGraph);
            conn.add(stmtRes, CHANGELOG.OBJECT, obj, sail.validationGraph);
            conn.add(stmtRes, CHANGELOG.CONTEXT, ctx, sail.validationGraph);
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

        // the connection enforces that no triple modification can be staged or recorded into the history if there is
        // a pending undo. So, if there is a pending undo we can be certain that this is the sole thing to do

        // Some triple stores (e.g. GraphDB, at least version 8.0.4) notify triple additions/deletions
        // only during the commit operation. Therefore, we can't be certain that an empty staging area means a
        // read-only transaction.

        // In a read-only connection, just execute the commit

        // Similarly, if history is disabled, just do the commit

        if (pendingUndoFor != null) {
            // implement the actual undo

            // 3 cases: i) with validation, ii) with history, iii) w/o history & validation, thus using an in-memory stack

            synchronized (sail) {
                pendingUndoFor.accept(new UndoSource.UndoSourceVisitor() {
                    @Override
                    public void visitValidationSourced(UndoSource.ValidationSourcedUndo undoStackTip) {
                        try (RepositoryConnection suppRepConn = sail.supportRepo.getConnection()) {
                            suppRepConn.begin();

                            TupleQuery latestCommitQuery = suppRepConn.prepareTupleQuery(
                                    "SELECT * WHERE { ?commit a <http://semanticturkey.uniroma2.it/ns/changelog#Commit> ; " +
                                            "  <http://www.w3.org/ns/prov#endedAtTime> ?endTime ;" +
                                            "}" +
                                            "ORDER by DESC(?endTime)" +
                                            "LIMIT 1");
                            SimpleDataset dataset = new SimpleDataset();
                            dataset.addDefaultGraph(sail.validationGraph);
                            latestCommitQuery.setDataset(dataset);

                            BindingSet candidateForUndo = QueryResults.asList(latestCommitQuery.evaluate()).stream().findAny().orElseThrow(() -> new SailException("Empty undo stack"));

                            IRI commit = (IRI) candidateForUndo.getValue("commit");

                            if (!commit.equals(undoStackTip.getCommit())) {
                                throw new SailException("Concurrent undo");
                            }

                            QueryResults.stream(HistoryRepositories.getRemovedStaments(suppRepConn,
                                    commit, sail.validationGraph)).map(NILDecoder.INSTANCE).forEach(s -> {
                                ChangeTrackerConnection.super.removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingRemoveGraph(s.getContext()));
                            });
                            QueryResults.stream(HistoryRepositories.getAddedStaments(suppRepConn, commit,
                                    sail.validationGraph)).forEach(s -> {
                                ChangeTrackerConnection.super.removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingAddGraph(s.getContext()));
                            });

                            ChangeTrackerConnection.super.commit();

                            removeLastCommit(sail.validationGraph, commit, null, false, false);

                            suppRepConn.commit();
                        }
                    }

                    @Override
                    public void visitHistorySourced(UndoSource.HistorySourcedUndo undoStackTip) {
                        try (RepositoryConnection suppRepConn = sail.supportRepo.getConnection()) {
                            suppRepConn.begin();

                            TupleQuery latestCommitQuery = suppRepConn.prepareTupleQuery(
                                    "SELECT * WHERE {" +
                                            "  <http://semanticturkey.uniroma2.it/ns/changelog#MASTER> <http://semanticturkey.uniroma2.it/ns/changelog#tip> ?commit . " +
                                            "  OPTIONAL { ?commit <http://semanticturkey.uniroma2.it/ns/changelog#parentCommit> ?newTip }" +
                                            "}" +
                                            "LIMIT 1");
                            SimpleDataset dataset = new SimpleDataset();
                            dataset.addDefaultGraph(sail.historyGraph);
                            latestCommitQuery.setDataset(dataset);

                            BindingSet candidateForUndo = QueryResults.asList(latestCommitQuery.evaluate()).stream().findAny().orElseThrow(() -> new SailException("Empty undo stack"));
                            IRI commit = (IRI) candidateForUndo.getValue("commit");

                            if (!commit.equals(undoStackTip.getCommit())) {
                                throw new SailException("Concurrent undo");
                            }

                            IRI newTip = (IRI) candidateForUndo.getValue("newTip");
                            QueryResults.stream(HistoryRepositories.getRemovedStaments(suppRepConn,
                                    commit, sail.historyGraph)).map(NILDecoder.INSTANCE).forEach(s -> {
                                ChangeTrackerConnection.super.addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
                                        s.getContext());
                            });
                            QueryResults.stream(HistoryRepositories.getAddedStaments(suppRepConn, commit,
                                    sail.historyGraph)).forEach(s -> {
                                ChangeTrackerConnection.super.removeStatements(s.getSubject(), s.getPredicate(), s.getObject(), s.getContext());
                            });

                            ChangeTrackerConnection.super.commit();

                            removeLastCommit(sail.historyGraph, commit, newTip, false, true);

                            suppRepConn.commit();
                        }

                    }

                    @Override
                    public void visitStackSourced(UndoSource.StackSourcedUndo undoStackTip) {

                        // get the tip of the undo stack
                        StagingArea latestUndoStackTip = sail.undoStack.get().peek().orElseThrow(() -> new SailException("Empty undo stack"));

                        if (!latestUndoStackTip.equals(undoStackTip.getStagingArea())) {
                            throw new SailException("Concurrent undo");
                        }

                        undoStackTip.getStagingArea().getRemovedStatements().forEach(st -> ChangeTrackerConnection.super.removeStatements(st.getSubject(), st.getPredicate(), st.getObject(), (Resource)st.getContext()));
                        undoStackTip.getStagingArea().getAddedStatements().forEach(st -> ChangeTrackerConnection.super.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), (Resource)st.getContext()));

                        ChangeTrackerConnection.super.commit();

                        sail.undoStack.get().pop();

                    }
                });
            }

        } else if (readonlyHandler.isReadOnly()) {
            super.commit();
        } else if (!sail.historyEnabled) {
            synchronized (sail) {
                try {
                    super.commit();
                } finally {
                    if (!stagingArea.isEmpty()) { // if the operation produced actual triple modifications
                        if (sail.undoStack.isPresent()) { // put the staging area on the in-memory undo stack
                            sail.undoStack.get().push(stagingArea);
                            stagingArea = new StagingArea(); // create a new (empty) staging area
                        } else { // otherwise, if undo is disabled, simply clear the staging area
                            stagingArea.clear();
                        }
                    }
                }
            }
        } else if (sail.interactiveNotifications.equals(ChangeTracker.OPTIONAL_TRUE)
                && stagingArea.isEmpty()) {
            // If the underlying triple store sends interactive notifications, and the staging area is
            // empty then we should commit and return
            super.commit();
        } else {

            // However, if some triples have been staged or the triple triple store does not send
            // interactive
            // notifications, then we need to log the operations.

            synchronized (sail) {
                // Prepares data commit (in case of success, it is unlikely that a subsequent commit()
                // fails)
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
                    List<Statement> headList = QueryResults.asList(supporRepoConn
                            .getStatements(CHANGELOG.MASTER, CHANGELOG.TIP, null, sail.historyGraph));

                    if (headList.size() > 1) {
                        throw new SailException(
                                "Could not commit the changeset metadata, since the tip of MASTER is not unique: "
                                        + headList);
                    }

                    // If the tip of MASTER is defined, check that has status committed. Otherwise, it
                    // could be possible that the registered commit has not been effectively applied to
                    // the data repository. In this case, subsequent commits are denied, until the
                    // consistency between data and metadata is restored.

                    if (headList.size() == 1) {
                        previousTip = (Resource) headList.iterator().next().getObject();

                        // boolean previousTipCommitted = supporRepoConn.hasStatement(previousTip,
                        // CHANGELOG.STATUS, vf.createLiteral("committed"), false, sail.historyGraph);
                        //
                        // if (!previousTipCommitted) {
                        // throw new SailException(
                        // "Could not commit the changeset metadata, since there is a pending commit: "
                        // + previousTip);
                        // }

                        BigInteger lastRevisionNumber = Models
                                .objectLiteral(QueryResults.asModel(supporRepoConn.getStatements(previousTip,
                                        CHANGELOG.REVISION_NUMBER, null, sail.historyGraph)))
                                .map(lit -> {
                                    try {
                                        return new BigInteger(lit.getLabel());
                                    } catch (NumberFormatException e) {
                                        throw new SailException("Current tip has an illegal revision number: "
                                                + lit.getLabel());
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
                            supporRepoConn.getValueFactory().createLiteral(revisionNumber),
                            sail.historyGraph);

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
                        supporRepoConn.add(commitIRI, CHANGELOG.PARENT_COMMIT, previousTip,
                                sail.historyGraph);
                        supporRepoConn.remove(CHANGELOG.MASTER, CHANGELOG.TIP, previousTip,
                                sail.historyGraph);
                    }
                    supporRepoConn.add(CHANGELOG.MASTER, CHANGELOG.TIP, commitIRI, sail.historyGraph);

                    supporRepoConn.commit();
                } catch (RepositoryException e) {
                    // It may be the case that metadata have been committed, but for some reason (e.g.
                    // disconnection from a remote metadata repo) the transaction status cannot be
                    // reported back
                    throw new SailException(e);
                }

                // Commits the data
                try {
                    super.commit();
                } catch (SailException e) {
                    // commit() has failed, so we should undo the history
                    removeLastCommit(sail.historyGraph, commitIRI, previousTip, triplesUnknown, true);
                    throw e;
                }

                // Data has been committed. So, mark the MASTER commit as committed

                // If triples were unknown when the commit was first logged, then add that information now
                // (note that in the meantime the triple store should have sent the necessary
                // notifications)

                if (triplesUnknown && stagingArea.isEmpty()) {
                    removeLastCommit(sail.historyGraph, commitIRI, previousTip, triplesUnknown, true);
                } else {
                    try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
                        ValueFactory vf = supportRepoConn.getValueFactory();

                        supportRepoConn.begin();

                        // If triples were unknown when the commit was first logged, then add that
                        // information now (note that in the meantime the triple store should have sent
                        // the necessary
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
                        // Since we don't known if data have been committed or not, it is safest to make
                        // fail subsequent commit attempts.
                        throw new SailException(e);
                    }
                }

                stagingArea.clear();
                readonlyHandler.clearHandler();
                validatableOpertionHandler.clearHandler();

                // Note that if the MASTER's tip is not marked as committed, we are unsure about whether or
                // not
                // the commit has been applied to the data repo
            }
        }

        if (pendingValidation != null) {
            try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
                conditionalAddToBlacklist(supportRepoConn, pendingBlacklisting, pendingComment);
                removeLastCommit(sail.validationGraph, pendingValidation, null, false, false);
            }
        }
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

    private void removeLastCommit(IRI graph, IRI commitIRI, Resource previousTip, boolean triplesUnknown,
                                  boolean updateTip) throws SailException {
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

            GraphQuery commitDescribeQuery = supportRepoConn.prepareGraphQuery(
                    "describe " + RenderUtils.toSPARQL(commitIRI) + " from " + RenderUtils.toSPARQL(graph));
            commitDescribeQuery.setIncludeInferred(false);
            supportRepoConn.remove((CloseableIteration<Statement, QueryEvaluationException>) commitDescribeQuery.evaluate(), graph);

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
        if (contextList.contains(CHANGETRACKER.VALIDATION)) {
            Model tempModel = new LinkedHashModel();
            tempModel.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ENABLED,
                    SimpleValueFactory.getInstance().createLiteral(validationEnabled));
            iterations.add(new CollectionIteration<>(tempModel.filter(subj, pred, obj)));
            contextList.remove(CHANGETRACKER.VALIDATION);
        }
        if (contextList.size() > 0 || iterations.isEmpty()) {
            Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
            iterations.add(super.getStatements(subj, pred, obj, includeInferred, newContexts));
        }
        return new UnionIteration<>(iterations);
    }

    @Override
    public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
                                                                                       Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {

        Map<IRI, Function<IRI, Model>> virtualResourceHandlers = ImmutableMap.of(CHANGETRACKER.SYSINFO, this::generateSysInfoModel, CHANGETRACKER.UNDO, this::generateUndoModel);

        if (tupleExpr instanceof DescribeOperator) {

            if (dataset != null) {
                Map<IRI, Function<IRI, Model>> enabledVirtualResources = Maps.filterKeys(virtualResourceHandlers, v -> ((BiFunction<Set<IRI>, IRI, Boolean>)Collection::contains).apply(dataset.getDefaultGraphs(), v));

                if (!enabledVirtualResources.isEmpty()) {
                    TupleExpr argTupleExpr = ((UnaryTupleOperator) tupleExpr).getArg();
                    Model generatedTriples = new LinkedHashModel();
                    QueryResults.stream(super.evaluate(argTupleExpr, dataset, bindings, includeInferred))
                            .flatMap(bs -> StreamSupport.stream(bs.spliterator(), false)).map(Binding::getValue)
                            .forEach(v -> {
                                Function<IRI, Model> fun = enabledVirtualResources.entrySet().stream().filter(entry -> v.stringValue().startsWith(entry.getKey().stringValue())).map(Map.Entry::getValue).findAny().orElse(null);

                                if (fun != null) {
                                    generatedTriples.addAll(fun.apply((IRI) v));
                                }
                            });

                    List<MapBindingSet> statementCollection = generatedTriples.stream().map(st -> {
                        MapBindingSet bs = new MapBindingSet();
                        bs.addBinding("subject", st.getSubject());
                        bs.addBinding("predicate", st.getPredicate());
                        bs.addBinding("object", st.getObject());
                        if (st.getContext() != null) {
                            bs.addBinding("context", st.getContext());
                        }
                        return bs;
                    }).collect(Collectors.toList());

                    return new CollectionIteration<>(statementCollection);
                }
            }
        }

        return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
    }

    protected Model generateSysInfoModel(IRI subj) {
        Model model = new LinkedHashModel();
        model.add(subj,
                SimpleValueFactory.getInstance().createIRI("http://schema.org/version"),
                SimpleValueFactory.getInstance()
                        .createLiteral(ChangeTracker.getVersion()));
        model.add(subj, ChangeTrackerSchema.SUPPORT_REPOSITORY_ID,
                SimpleValueFactory.getInstance().createLiteral(sail.supportRepoId));
        if (sail.serverURL != null) {
            model.add(subj, ChangeTrackerSchema.SERVER_URL,
                    SimpleValueFactory.getInstance().createLiteral(sail.serverURL));
        }

        return model;
    }

    protected Model generateUndoModel(IRI subj) throws SailException {
        Model generatedTriples = new LinkedHashModel();

        if (pendingUndoFor == null) return generatedTriples;

        pendingUndoFor.accept(new UndoSource.UndoSourceVisitor() {
            @Override
            public void visitValidationSourced(UndoSource.ValidationSourcedUndo undoStackTip) {
                try(RepositoryConnection suppRepCon = sail.supportRepo.getConnection()) {
                    Model commitMetadata = HistoryRepositories.getCommitUserMetadata(suppRepCon, undoStackTip.getCommit(), sail.validationGraph, false);
                    generatedTriples.addAll(commitMetadata);
                    generatedTriples.add(undoStackTip.getCommit(), RDF.TYPE, CHANGELOG.COMMIT);
                }
            }

            @Override
            public void visitHistorySourced(UndoSource.HistorySourcedUndo undoStackTip) {
                try(RepositoryConnection suppRepCon = sail.supportRepo.getConnection()) {
                    Model commitMetadata = HistoryRepositories.getCommitUserMetadata(suppRepCon, undoStackTip.getCommit(), sail.historyGraph, false);
                    generatedTriples.addAll(commitMetadata);
                    generatedTriples.add(undoStackTip.getCommit(), RDF.TYPE, CHANGELOG.COMMIT);
                }
            }

            @Override
            public void visitStackSourced(UndoSource.StackSourcedUndo undoStackTip) {
                Model commitMetadata = undoStackTip.getStagingArea().getCommitMetadataModel();
                generatedTriples.addAll(commitMetadata);
            }
        });

        return generatedTriples;
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
        if (contextList.contains(CHANGETRACKER.UNDO)) {
            handleUndo(subj, pred, obj);
            contextList.remove(CHANGETRACKER.UNDO);
        }

        if (contexts.length == 0 || !contextList.isEmpty()) {
            if (pendingUndoFor != null) {
                throw new SailException("Could not modify triples because of a pending undo");
            }
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
        if (contextList.contains(CHANGETRACKER.UNDO)) {
            handleUndo(subj, pred, obj);
            contextList.remove(CHANGETRACKER.UNDO);
        }


        if (contexts.length == 0 || !contextList.isEmpty()) {
            if (pendingUndoFor != null) {
                throw new SailException("Could not modify triples because of a pending undo");
            }

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

    private void handleUndo(Resource subj, IRI pred, Value obj) throws SailException {
        if (!readonlyHandler.isReadOnly()) {
            throw new SailException("Could not record the undo because of previous modifications of the repository");
        }

        if (!validatableOpertionHandler.isReadOnly()) {
            throw new SailException("Could not record the undo because of previously staged of the repository");
        }

        if (!PROV.AGENT.equals(pred)) {
            throw new SailException("Expected predicate prov:agent, but given " + NTriplesUtil.toNTriplesString(pred));
        }

        if (!obj.isIRI()) {
            throw new SailException("Expected an IRI as performer for an UNDO, but given " + NTriplesUtil.toNTriplesString(obj));
        }

        if (pendingUndoFor != null) {
            throw new SailException("Already requested an undo");
        }

        if (sail.validationEnabled) {
            try (RepositoryConnection suppRepConn = sail.supportRepo.getConnection()) {
                suppRepConn.begin();

                TupleQuery latestCommitQuery = suppRepConn.prepareTupleQuery(
                        "SELECT * WHERE { ?commit a <http://semanticturkey.uniroma2.it/ns/changelog#Commit> ; " +
                                "  <http://www.w3.org/ns/prov#endedAtTime> ?endTime ;" +
                                "  <http://www.w3.org/ns/prov#qualifiedAssociation> [" +
                                "    <http://www.w3.org/ns/prov#hadRole> <http://semanticturkey.uniroma2.it/ns/st-changelog#performer> ; " +
                                "    <http://www.w3.org/ns/prov#agent> ?performer " +
                                "   ] . " +
                                "}" +
                                "ORDER by DESC(?endTime)" +
                                "LIMIT 1");
                SimpleDataset dataset = new SimpleDataset();
                dataset.addDefaultGraph(sail.validationGraph);
                latestCommitQuery.setDataset(dataset);

                BindingSet candidateForUndo = QueryResults.asList(latestCommitQuery.evaluate()).stream().findAny().orElseThrow(() -> new SailException("Empty undo stack"));
                Optional<Value> performer = Optional.of(candidateForUndo.getValue("performer"));
                if (!performer.filter(obj::equals).isPresent()) {
                    throw new SailException("The performer of the last operation does not match the agent for whom undo has been requested");
                }

                pendingUndoFor = new UndoSource.ValidationSourcedUndo((IRI) candidateForUndo.getValue("commit"));
            }
        } else if (sail.historyEnabled) {
            try (RepositoryConnection suppRepConn = sail.supportRepo.getConnection()) {
                suppRepConn.begin();

                TupleQuery latestCommitQuery = suppRepConn.prepareTupleQuery(
                        "SELECT * WHERE {" +
                                "  <http://semanticturkey.uniroma2.it/ns/changelog#MASTER> <http://semanticturkey.uniroma2.it/ns/changelog#tip> ?commit . " +
                                "  ?commit a <http://semanticturkey.uniroma2.it/ns/changelog#Commit> ; " +
                                "    <http://www.w3.org/ns/prov#endedAtTime> ?endTime ;" +
                                "    <http://www.w3.org/ns/prov#qualifiedAssociation> [" +
                                "      <http://www.w3.org/ns/prov#hadRole> <http://semanticturkey.uniroma2.it/ns/st-changelog#performer> ; " +
                                "      <http://www.w3.org/ns/prov#agent> ?performer " +
                                "   ] . " +
                                "  OPTIONAL { ?commit <http://semanticturkey.uniroma2.it/ns/changelog#parentCommit> ?newTip }" +
                                "}" +
                                "LIMIT 1");
                SimpleDataset dataset = new SimpleDataset();
                dataset.addDefaultGraph(sail.historyGraph);
                latestCommitQuery.setDataset(dataset);

                BindingSet candidateForUndo = QueryResults.asList(latestCommitQuery.evaluate()).stream().findAny().orElseThrow(() -> new SailException("Empty undo stack"));
                Optional<Value> performer = Optional.of(candidateForUndo.getValue("performer"));
                if (!performer.filter(obj::equals).isPresent()) {
                    throw new SailException("The performer of the last operation does not match the agent for whom undo has been requested");
                }

                pendingUndoFor = new UndoSource.HistorySourcedUndo((IRI) candidateForUndo.getValue("commit"));
            }
        } else if (sail.undoEnabled) {

            StagingArea undoStackTip = sail.undoStack.get().peek().orElseThrow(() -> new SailException("Empty undo stack"));

            // check the performer of the operation at the tip
            Optional<IRI> performer = UndoStack.getPerformer(undoStackTip.getCommitMetadataModel());
            if (!performer.filter(obj::equals).isPresent()) {
                throw new SailException("The performer of the last operation does not match the agent for whom undo has been requested");
            }

            pendingUndoFor = new UndoSource.StackSourcedUndo(undoStackTip);
        } else {
            throw new SailException("Undo not supported");
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
            if (pendingUndoFor != null) {
                throw new SailException("Could not modify triples because of a pending undo");
            }

            Resource[] newContexts = contextList.toArray(new Resource[contextList.size()]);
            try {
                if (validationEnabled) {
                    validatableOpertionHandler.removeStatements(subj, pred, obj, newContexts);
                    if (subj == null || pred == null || obj == null) {
                        // in case of non-ground delete, try to read to statements to be deleted
                        try (CloseableIteration<? extends Statement, SailException> it = super.getStatements(
                                subj, pred, obj, false, newContexts)) {
                            while (it.hasNext()) {
                                Statement stmt = it.next();
                                super.addStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
                                        VALIDATION.stagingRemoveGraph(stmt.getContext()));
                            }
                        }
                    } else {
                        mangleRemoveContextsForValidation(newContexts);
                        super.addStatement(subj, pred, obj, newContexts);
                    }
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
            if (pendingUndoFor != null) {
                throw new SailException("Could not modify triples because of a pending undo");
            }

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

        List<Resource> contextsToCopy = new ArrayList<>();
        List<Resource> contextsToClear = new ArrayList<>();

        try {
            if (sail.validationEnabled) {
                for (int i = 0; i < contexts.length; i++) {
                    Resource ctx = contexts[i];

                    // a clear-through only cancels staged additions if validation is currently enabled;
                    // otherwise, it clears the actual graph
                    if (VALIDATION.isClearThroughGraph(ctx)) {
                        Resource unmangled = VALIDATION.unmangleClearThroughGraph(ctx);
                        if (validationEnabled) {
                            ctx = VALIDATION.stagingAddGraph(unmangled);
                        } else {
                            contextsToClear.add(unmangled);
                        }
                    }

                    if (validationEnabled) {
                        if (VALIDATION.isAddGraph(ctx)) {
                            IRI unmangleGraph = VALIDATION.unmangleAddGraph((IRI) ctx);
                            validatableOpertionHandler.clearHandler(unmangleGraph);
                            contextsToClear.add(ctx);
                        } else if (!VALIDATION.isRemoveGraph(ctx)) {
                            validatableOpertionHandler.removeStatements(null, null, null,
                                    new Resource[]{ctx});
                            contextsToCopy.add(ctx);
                        }
                    }
                }

                if (!contextsToClear.isEmpty() || !validationEnabled) {
                    super.clear(contextsToClear.toArray(new Resource[contextsToClear.size()]));
                }

                if (validationEnabled) {

                    if (contexts.length == 0) {
                        contextsToCopy = QueryResults.stream(getContextIDs())
                                .filter(r -> !VALIDATION.isAddGraph(r) && !VALIDATION.isRemoveGraph(r))
                                .collect(Collectors.toList());
                    }

                    if (!contextsToCopy.isEmpty()) {
                        removeStatements(null, null, null,
                                contextsToCopy.toArray(new Resource[contextsToCopy.size()]));
                    }
                }
            } else {
                super.clear(contexts);
            }
        } catch (Exception e) {
            readonlyHandler.recordCorruption();
            throw e;
        }
    }

    @Override
    public void setNamespace(String prefix, String name) throws SailException {
        readonlyHandler.setNamespace(prefix, name);
        try {
            super.setNamespace(prefix, name);
        } catch (Exception e) {
            readonlyHandler.recordCorruption();
            throw e;
        }
    }

    @Override
    public void clearNamespaces() throws SailException {
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
            context = CHANGELOG.NULL;
        }

        String contextString = context.stringValue();

        if (contextString.startsWith(VALIDATION.STAGING_ADD_GRAPH.stringValue())
                || contextString.startsWith(VALIDATION.STAGING_REMOVE_GRAPH.stringValue())) {
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
        if (CHANGETRACKER.ENABLED.equals(pred)) {
            if (BooleanLiteral.FALSE.equals(obj)) {
                validationEnabled = false;
            } else if (BooleanLiteral.TRUE.equals(obj)) {
                if (sail.validationEnabled) {
                    validationEnabled = true;
                } else {
                    throw new SailException(
                            "Could not enable validation on a connection to a Sail without validation");
                }
            } else {
                throw new SailException("A boolean value expected. Given: " + obj);
            }
            return;
        }
        try {
            validationEnabled = false;
            synchronized (sail) {
                if (RDFS.COMMENT.equals(pred)) {
                    if (!(obj instanceof Literal)) {
                        throw new SailException(
                                "The comment on a commit should be a literal. Instead it was: "
                                        + NTriplesUtil.toNTriplesString(obj));
                    }
                    pendingComment = (Literal) obj;
                } else {
                    try (RepositoryConnection supportRepoConn = sail.supportRepo.getConnection()) {
                        supportRepoConn.begin();

                        IRI commit = obj instanceof IRI ? (IRI) obj
                                : SimpleValueFactory.getInstance().createIRI(obj.stringValue());

                        if (CHANGETRACKER.ACCEPT.equals(pred)) {
                            QueryResults.stream(HistoryRepositories.getRemovedStaments(supportRepoConn,
                                    commit, sail.validationGraph)).map(NILDecoder.INSTANCE).forEach(s -> {
                                removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        s.getContext());
                                removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingRemoveGraph(s.getContext()));
                            });
                            QueryResults.stream(HistoryRepositories.getAddedStaments(supportRepoConn, commit,
                                    sail.validationGraph)).forEach(s -> {
                                addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
                                        s.getContext());
                                removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingAddGraph(s.getContext()));
                            });

                            Model validatedUserMetadataModel = HistoryRepositories.getCommitUserMetadata(
                                    supportRepoConn, commit, sail.validationGraph, true);

                            stagingArea.getCommitMetadataModel().addAll(validatedUserMetadataModel);

                        } else if (CHANGETRACKER.REJECT.equals(pred)) {
                            QueryResults.stream(HistoryRepositories.getRemovedStaments(supportRepoConn,
                                    commit, sail.validationGraph)).map(NILDecoder.INSTANCE).forEach(s -> {
                                removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingRemoveGraph(s.getContext()));
                            });
                            QueryResults.stream(HistoryRepositories.getAddedStaments(supportRepoConn, commit,
                                    sail.validationGraph)).forEach(s -> {
                                removeStatements(s.getSubject(), s.getPredicate(), s.getObject(),
                                        VALIDATION.stagingAddGraph(s.getContext()));
                            });

                            pendingBlacklisting = commit;
                        } else {
                            throw new SailException("Unrecognized operation: it should be either "
                                    + NTriplesUtil.toNTriplesString(CHANGETRACKER.ACCEPT) + " or "
                                    + NTriplesUtil.toNTriplesString(CHANGETRACKER.REJECT));
                        }

                        pendingValidation = commit;

                        supportRepoConn.commit();
                    }
                }
            }
        } finally {
            validationEnabled = true;
        }
    }

    private void conditionalAddToBlacklist(RepositoryConnection supportRepoConn, /* @Nullable */ IRI commit,
            /* @Nullable */ Literal pendingComment) {
        if (!sail.blacklistEnabled || commit == null) {
            return;
        }

        IRI blacklistGraph = sail.blacklistGraph;

        GraphQuery blacklistTemplateQuery = supportRepoConn.prepareGraphQuery(
                //@formatter:off
                "PREFIX blacklist: <http://semanticturkey.uniroma2.it/ns/blacklist#>                         \n" +
                        "PREFIX stcl: <http://semanticturkey.uniroma2.it/ns/st-changelog#>                           \n" +
                        "DESCRIBE ?x { ?commit stcl:parameters|blacklist:template ?x }                               \n"
                //@formatter:on
        );
        blacklistTemplateQuery.setIncludeInferred(false);
        blacklistTemplateQuery.setBinding("commit", commit);
        Model blacklistTemplate = QueryResults.asModel(blacklistTemplateQuery.evaluate());

        Optional<Resource> templateResOpt = Models.getPropertyResource(blacklistTemplate, commit,
                BLACKLIST.TEMPLATE);
        Optional<Resource> parametersResOpt = Models.getPropertyResource(blacklistTemplate, commit,
                PARAMETERS);

        if (!templateResOpt.isPresent() || !parametersResOpt.isPresent()) {
            return;
        }

        Resource templateRes = templateResOpt.get();
        Resource parametersRes = parametersResOpt.get();
        ValueFactory vf = supportRepoConn.getValueFactory();

        Resource blacklistItem = vf.createBNode();
        Model blacklistItemDescription = new LinkedHashModel();

        // processes templateType

        blacklistTemplate.filter(templateRes, BLACKLIST.TEMPLATE_TYPE, null).forEach(stmt -> {
            blacklistItemDescription.add(vf.createStatement(blacklistItem, RDF.TYPE, stmt.getObject()));
        });

        blacklistTemplate.filter(templateRes, BLACKLIST.CONSTANT_BINDING, null).stream()
                .map(Statement::getObject).filter(Resource.class::isInstance).map(Resource.class::cast)
                .forEach(head -> {
                    List<Value> values = RDFCollections.asValues(blacklistTemplate, head, new ArrayList<>(2));

                    if (values.size() == 2) {

                        Value predicate = values.get(0);
                        Value constant = values.get(1);

                        if (predicate instanceof IRI) {
                            blacklistItemDescription
                                    .add(vf.createStatement(blacklistItem, (IRI) predicate, constant));
                        }
                    }
                });

        blacklistTemplate.filter(templateRes, BLACKLIST.PARAMETER_BINDING, null).stream()
                .map(Statement::getObject).filter(Resource.class::isInstance).map(Resource.class::cast)
                .forEach(head -> {
                    List<Value> values = RDFCollections.asValues(blacklistTemplate, head, new ArrayList<>(2));

                    if (values.size() == 2) {

                        Value predicate = values.get(0);
                        Value parameter = values.get(1);

                        if (predicate instanceof IRI && parameter instanceof IRI) {
                            Models.getProperty(blacklistTemplate, parametersRes, (IRI) parameter)
                                    .ifPresent(rawValue -> {

                                        if (rawValue instanceof Literal) {
                                            Literal rawValueLiteral = (Literal) rawValue;

                                            if (XSD.STRING.equals(rawValueLiteral.getDatatype())) {

                                                try {
                                                    Value processedValue = NTriplesUtil
                                                            .parseValue(rawValueLiteral.getLabel(), vf);
                                                    blacklistItemDescription.add(vf.createStatement(
                                                            blacklistItem, (IRI) predicate, processedValue));
                                                } catch (IllegalArgumentException e) {
                                                    logger.error(
                                                            "Skip parameter during the instantiation of a blacklist template",
                                                            e);
                                                }
                                            }

                                        }

                                    });
                        }
                    }
                });

        Set<Literal> labels = Models.getPropertyLiterals(blacklistItemDescription, blacklistItem,
                BLACKLIST.LABEL);

        for (Literal l : labels) {
            if (RDF.LANGSTRING.equals(l.getDatatype())) {
                Literal lowercased;
                if (l.getLanguage().isPresent()) {
                    Locale locale = Locale.forLanguageTag(l.getLanguage().get());
                    lowercased = vf.createLiteral(l.getLabel().toLowerCase(locale), locale.toLanguageTag());
                } else {
                    lowercased = vf.createLiteral(l.getLabel().toLowerCase(), "");
                }
                blacklistItemDescription.add(blacklistItem, BLACKLIST.LOWERCASED_LABEL, lowercased);
            }
        }

        if (!blacklistItemDescription.isEmpty()
                && blacklistItemDescription.contains(blacklistItem, BLACKLIST.LOWERCASED_LABEL, null)) {

            if (pendingComment != null) {
                blacklistItemDescription.add(blacklistItem, RDFS.COMMENT, pendingComment);
            }

            supportRepoConn.add(blacklistItemDescription, blacklistGraph);
        } else {
            logger.error("Skip wrong instantiation of a blacklist template: {}", blacklistItemDescription);
        }
    }
}
