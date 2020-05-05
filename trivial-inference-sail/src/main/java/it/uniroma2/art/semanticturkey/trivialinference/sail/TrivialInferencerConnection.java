package it.uniroma2.art.semanticturkey.trivialinference.sail;

import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

/**
 * A {@link SailConnection} for {@link TrivialInferencer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TrivialInferencerConnection extends NotifyingSailConnectionWrapper {

	private TrivialInferencer sail;
	private RepositoryConnection referenceSchemaConnection;
	private RepositoryConnection editableSchemaConnection;

	private SailConnectionListener listener = new SailConnectionListener() {

		@Override
		public void statementRemoved(Statement st) {
			if (Objects.equals(st.getPredicate(), OWL.INVERSEOF)
					|| Objects.equals(st.getPredicate(), RDF.TYPE)
							&& Objects.equals(st.getObject(), OWL.SYMMETRICPROPERTY)) {
				editableSchemaConnection.remove(st);
			}
		}

		@Override
		public void statementAdded(Statement st) {
			if (Objects.equals(st.getPredicate(), OWL.INVERSEOF)
					|| Objects.equals(st.getPredicate(), RDF.TYPE)
							&& Objects.equals(st.getObject(), OWL.SYMMETRICPROPERTY)) {
				editableSchemaConnection.add(st);
			}
		}
	};

	public TrivialInferencerConnection(TrivialInferencer sail, NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
		this.sail = sail;
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

		super.begin(level);

		Repository schemaCache = sail.getSchemaCache();
		referenceSchemaConnection = schemaCache.getConnection();
		referenceSchemaConnection.begin(IsolationLevels.SNAPSHOT);
		referenceSchemaConnection.hasStatement(null, null, null, false);

		editableSchemaConnection = schemaCache.getConnection();
		editableSchemaConnection.begin(level);
		editableSchemaConnection.hasStatement(null, null, null, false);

		this.addConnectionListener(listener);
	}

	public boolean isTripleInteresting(Resource subj, IRI pred, Value obj) {
		if (subj != null && pred != null && obj != null && obj instanceof Resource) {
			return referenceSchemaConnection.hasStatement(pred, OWL.INVERSEOF, null, true)
					|| referenceSchemaConnection.hasStatement(pred, RDF.TYPE, OWL.SYMMETRICPROPERTY, true);
		}

		return false;
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		super.addStatement(subj, pred, obj, contexts);
		if (isTripleInteresting(subj, pred, obj)) {
			doTrivialInference(super::addStatement, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		super.addStatement(modify, subj, pred, obj, contexts);
		if (isTripleInteresting(subj, pred, obj)) {
			doTrivialInference(super::addStatement, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		super.removeStatement(modify, subj, pred, obj, contexts);
		if (isTripleInteresting(subj, pred, obj)) {
			doTrivialInference(super::removeStatements, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		super.removeStatements(subj, pred, obj, contexts);
		if (isTripleInteresting(subj, pred, obj)) {
			doTrivialInference(super::removeStatements, subj, pred, obj, contexts);
		}
	}

	@Override
	public void commit() throws SailException {
		referenceSchemaConnection.commit();
		referenceSchemaConnection.close();
		super.commit();
		try {
			editableSchemaConnection.commit();
			editableSchemaConnection.close();
		} catch (Exception e) {
			sail.invalidateCache();
		}
	}

	@Override
	public void close() throws SailException {
		super.close();
		if (referenceSchemaConnection != null && referenceSchemaConnection.isOpen()) {
			referenceSchemaConnection.close();
		}

		if (editableSchemaConnection != null && editableSchemaConnection.isOpen()) {
			editableSchemaConnection.close();
		}
	}

	@FunctionalInterface
	private static interface EditOperation {
		void execute(Resource subj, IRI pred, Value obj, Resource... contexts);
	}

	private void doTrivialInference(EditOperation op, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		Set<IRI> inverseProps = computePredicatesForInverseTriple(pred);

		inverseProps.forEach(invProp -> {
			op.execute((Resource) obj, invProp, subj, contexts);
		});
	}

	private Set<IRI> computePredicatesForInverseTriple(IRI pred)
			throws QueryEvaluationException, RepositoryException {
		Set<IRI> inverseProps = Models.objectIRIs(QueryResults
				.asModel(referenceSchemaConnection.getStatements(pred, OWL.INVERSEOF, null, false)));
		inverseProps.addAll(Models.subjectIRIs(QueryResults
				.asModel(referenceSchemaConnection.getStatements(null, OWL.INVERSEOF, pred, false))));
		if (editableSchemaConnection.hasStatement(pred, RDF.TYPE, OWL.SYMMETRICPROPERTY, false)) {
			inverseProps.add(pred);
		}
		return inverseProps;
	}

}
