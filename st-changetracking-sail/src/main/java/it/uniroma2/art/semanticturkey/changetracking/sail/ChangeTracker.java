package it.uniroma2.art.semanticturkey.changetracking.sail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link NotifyingSail} keeping track of changes to an underlying {@code Sail}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTracker extends NotifyingSailWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ChangeTracker.class);

	final Repository metadataRepo;
	final String metadataNS;
	final IRI metadataGraph;

	public ChangeTracker(Repository metadataRepo, String metadataNS, IRI metadataGraph) {
		this.metadataRepo = metadataRepo;
		this.metadataNS = metadataNS;
		this.metadataGraph = metadataGraph;
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
