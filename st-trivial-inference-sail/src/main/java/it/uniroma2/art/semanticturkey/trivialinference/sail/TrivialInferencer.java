package it.uniroma2.art.semanticturkey.trivialinference.sail;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.semanticturkey.trivialinference.sail.config.TrivialInferencerConfig;

/**
 * A {@link Sail} implementation that manages trivial inferences (i.e. symmetric and inverse properties).
 * 
 * <p>
 * Upon the addition of a triple <code>s p o</code>:
 * </p>
 * <ul>
 * <li>if <code>p</code> is an <code>owl:SymmetricProperty</code>, then this sail also adds the triple
 * <code>o p s</code> (swapping the subject and object of the original triple)</li>
 * <li>if <code>p</code> has inverse <code>p'</code> or <code>p'</code> has inverse <code>p</code>, then this
 * sails also adds the triple <code>o p' s</code> (swapping the subject and object of the original triple, and
 * using the inverse property as predicate)</li>
 * </ul>
 * 
 * <p>
 * Upon the deletion of a triple <code>s p o</code>, this sail also deletes the triple that would have been
 * materialized according to the rules above.
 * </p>
 * 
 * <p>
 * Materialization of trivial inferences is done upon each triple addition/removal.
 * </p>
 * 
 * <p>
 * This sail caches schema-level definitions, such as the inverse of a property and the fact that it is
 * symmetric.
 * </p>
 * 
 * <p>
 * Within a transaction, this sail performs its reasoning activities using a snapshot of the cache describing
 * its state before the transaction happens. Consequently, every schema-level change within a transaction will
 * not affect materialization during that transaction. This limitation shouldn't be a problem in most cases,
 * in which the <code>TBox</code> and <code>ABox</code> are modified in different transactions. This
 * limitation frees the sail from the need to store a temporary copy of all triple edits within a transaction,
 * just in case some of these edits use a predicate whose definition is changed within the same transaction.
 * </p>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TrivialInferencer extends NotifyingSailWrapper {
	@SuppressWarnings("unused")
	private TrivialInferencerConfig config;

	private Repository schemaCache;
	private volatile boolean cacheValid;

	public TrivialInferencer(TrivialInferencerConfig config) {
		this.config = config;
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		cacheValid = false;
		schemaCache = new SailRepository(new MemoryStore());
		schemaCache.init();
		rebuildCache();
	}

	@Override
	public void shutDown() throws SailException {
		try {
			schemaCache.shutDown();
		} finally {
			super.shutDown();
		}
	}

	@Override
	public TrivialInferencerConnection getConnection() throws SailException {
		return new TrivialInferencerConnection(this, super.getConnection());
	}

	protected synchronized Repository getSchemaCache() {
		if (!cacheValid) {
			rebuildCache();
		}
		return schemaCache;
	}

	protected void rebuildCache() {
		SailRepository tempSail = new SailRepository(getBaseSail()) {
			@Override
			protected void shutDownInternal() throws RepositoryException {
				// do nothing
			}
		};
		try {

			cacheValid = false;
			try (RepositoryConnection tempSailCon = tempSail.getConnection();
					RepositoryConnection schemaCacheCon = schemaCache.getConnection()) {
				schemaCacheCon.clear();
				tempSailCon.exportStatements(null, OWL.INVERSEOF, null, false,
						new RDFInserter(schemaCacheCon));
				tempSailCon.exportStatements(null, RDF.TYPE, OWL.SYMMETRICPROPERTY, false,
						new RDFInserter(schemaCacheCon));
			}
			cacheValid = true;
		} finally {
			tempSail.shutDown();
		}
	}

	protected void invalidateCache() {
		cacheValid = false;
	}

}
