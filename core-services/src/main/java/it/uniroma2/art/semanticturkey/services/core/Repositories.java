package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.exceptions.ExceptionDAO;
import it.uniroma2.art.semanticturkey.project.RepositorySummary;
import it.uniroma2.art.semanticturkey.project.RepositorySummary.RemoteRepositorySummary;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.utilities.ModelBasedRepositoryManager;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides services for accessing remote repositories.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Repositories extends STServiceAdapter {

    private static Logger logger = LoggerFactory.getLogger(Repositories.class);

    @STServiceOperation(method = RequestMethod.POST)
    // TODO: establish authorization @PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'R')")
    public Collection<RepositoryInfo> getRemoteRepositories(String serverURL, @Optional String username,
                                                            @Optional String password) {
        RemoteRepositoryManager remoteRepositoryManager = new RemoteRepositoryManager(serverURL);

        if (username != null && password != null) {
            remoteRepositoryManager.setUsernameAndPassword(username, password);
        }
        remoteRepositoryManager.init();
        try {
            return remoteRepositoryManager.getAllRepositoryInfos();
        } finally {
            remoteRepositoryManager.shutDown();
        }
    }

    /**
     * Restarts a remote repository. Currently, this only works for Ontotext GraphDB.
     *
     * @param serverURL
     * @param username
     * @param password
     * @param repositoryID
     * @throws IOException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
    public void restartRemoteRepository(String serverURL, @Optional String username,
                                        @Optional String password, String repositoryID) throws IOException {

        try (ModelBasedRepositoryManager.RemoteRepositoryManagerAdapter repMgr = new ModelBasedRepositoryManager.RemoteRepositoryManagerAdapter(serverURL, username, password)) {
            ModelBasedRepositoryManager.RemoteRepositoryManagerAdapter.restartGDBRepository(repMgr, repositoryID);
        }

    }

    /**
     * Deletes a set of remote repositories. The service returns a collection of exception, so that a non
     * <code>null</code> entry indicates a issue during the deletion of the corresponding repository in the
     * input
     *
     * @param remoteRepositories
     * @return
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm', 'D')")
    public List<ExceptionDAO> deleteRemoteRepositories(
            @JsonSerialized List<RepositorySummary.RemoteRepositorySummary> remoteRepositories) {

        List<Exception> caughtExceptions = new ArrayList<>();

        for (RemoteRepositorySummary remote : remoteRepositories) {
            String repoID = remote.getRepositoryId();
            String serverURL = remote.getServerURL();
            String username = remote.getUsername();
            String password = remote.getPassword();

            RepositoryManager repoManager;
            if (username != null) {
                repoManager = RemoteRepositoryManager.getInstance(serverURL, username, password);
            } else {
                repoManager = RemoteRepositoryManager.getInstance(serverURL);
            }
            try {

                Exception ex = null;
                try {
                    repoManager.removeRepository(repoID);
                } catch (RepositoryException | RepositoryConfigException e) {
                    ex = e;
                }
                caughtExceptions.add(ex);
            } finally {
                if (repoManager != null) {
                    repoManager.shutDown();
                }
            }
        }

        return caughtExceptions.stream().map(ExceptionDAO::valueOf).collect(Collectors.toList());
    }

}