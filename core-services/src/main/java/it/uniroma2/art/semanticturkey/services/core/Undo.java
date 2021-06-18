package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.history.OperationMetadata;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class related to the management of undo
 */
@STService
public class Undo extends STServiceAdapter {
    private static final Logger logger = LoggerFactory.getLogger(Undo.class);

    /**
     * Undoes the latest operation operation of the currently logged-in user
     * @return
     */
    @STServiceOperation(method = RequestMethod.POST)
    @Write
    public OperationMetadata undo() {
        RepositoryConnection con = getManagedConnection();
        ValueFactory vf = getManagedConnection().getValueFactory();
        con.add(vf.createBNode(), PROV.AGENT, UsersManager.getLoggedUser().getIRI(), CHANGETRACKER.UNDO);
        return null;
    }

}
