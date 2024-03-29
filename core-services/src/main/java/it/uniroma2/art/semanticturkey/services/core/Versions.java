package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.DelegatingRepositoryConnection;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.uniroma2.art.semanticturkey.exceptions.AlreadyExistingRepositoryException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.CreateRemote;
import it.uniroma2.art.semanticturkey.project.ProjectUtils;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.project.VersionInfo;
import it.uniroma2.art.semanticturkey.project.VersionInfo.RepositoryLocation;
import it.uniroma2.art.semanticturkey.project.VersionInfo.RepositoryStatus;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for handling versions of a repository.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Versions extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Versions.class);

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rdf(dataset, version)', 'R')")
	public List<VersionInfo> getVersions(@Optional(defaultValue = "false") boolean setRepositoryStatus,
			@Optional(defaultValue = "false") boolean setRepositoryLocation)
			throws JsonParseException, JsonMappingException, IOException {
		List<VersionInfo> versionInfoList = getProject().getVersionManager().getVersions();

		STLocalRepositoryManager repositoryManager = getProject().getRepositoryManager();
		Set<String> initializedRepositories = repositoryManager.getInitializedRepositoryIDs();

		if (setRepositoryStatus || setRepositoryLocation) {
			return versionInfoList.stream().map(versionInfo -> {
				String repositoryId = versionInfo.getRepositoryId();
				RepositoryStatus repositoryStatus;
				if (setRepositoryStatus) {
					repositoryStatus = initializedRepositories.contains(repositoryId)
							? VersionInfo.RepositoryStatus.INITIALIZED
							: VersionInfo.RepositoryStatus.UNITIALIZED;
				} else {
					repositoryStatus = null;
				}

				RepositoryLocation repositoryLocation;
				if (setRepositoryLocation) {
					RepositoryConfig repositoryConfig = repositoryManager.getRepositoryConfig(repositoryId);
					if (repositoryConfig == null) {
						repositoryLocation = RepositoryLocation.NOT_FOUND;
					} else {
						repositoryLocation = STLocalRepositoryManager.getUnfoldedRepositoryImplConfig(
								repositoryConfig) instanceof HTTPRepositoryConfig ? RepositoryLocation.REMOTE
										: RepositoryLocation.LOCAL;
					}
				} else {
					repositoryLocation = null;
				}

				return versionInfo.newWithRepositoryInfo(repositoryStatus, repositoryLocation);
			}).collect(Collectors.toList());
		}

		return versionInfoList;
	}

	/**
	 * Dumps the current content of the core repository to a dedicated repository.
	 * 
	 * @param repositoryAccess
	 *            tells the location of the repository. If <code>null</code>, the default repository location
	 *            associated with the project is used
	 * @param repositoryId
	 *            tells the name of the version repository. If the repository is local, this parameter must be
	 *            <code>null</code>
	 * @param repoConfigurerSpecification
	 *            specifies the configuration of the repository
	 * @param backendType
	 *            type of the triple store that will host the dump
	 * @param versionId
	 *            the version identifier
	 * @return
	 * @throws AlreadyExistingRepositoryException
	 * @throws ReservedPropertyUpdateException
	 * @throws ProjectUpdateException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(dataset, version)', 'C')")
	public VersionInfo createVersionDump(@Optional RepositoryAccess repositoryAccess,
			@Optional String repositoryId,
			@Optional PluginSpecification repoConfigurerSpecification,
			@Optional String backendType, String versionId) throws AlreadyExistingRepositoryException,
			JsonProcessingException, ProjectUpdateException, ReservedPropertyUpdateException, Exception {

		String localRepostoryId = ProjectUtils.computeVersionRepository(versionId);

		try {
			Repository versionRepository = getProject().createReadOnlyRepository(repositoryAccess,
					repositoryId, repoConfigurerSpecification, localRepostoryId, backendType, true);

			try (RepositoryConnection outConn = versionRepository.getConnection()) {
				outConn.begin(IsolationLevels.READ_COMMITTED);

				// Unwraps the read-only connection wrapper to access the underlying writable connection
				RepositoryConnection delegateWritableConnection = ((DelegatingRepositoryConnection) outConn)
						.getDelegate();
				getManagedConnection().export(new RDFInserter(delegateWritableConnection));

				outConn.commit();

				outConn.begin();

				SearchStrategyUtils
						.instantiateSearchStrategy(exptManager,
								STRepositoryInfoUtils.getSearchStrategy(getProject().getRepositoryManager()
										.getSTRepositoryInfo(localRepostoryId)))
						.initialize(getProject().getName(), delegateWritableConnection, true);

				outConn.commit();
			}
		} catch (Exception e) {
			try {
				logger.debug(String.format("Version %s was not dumped correctly to repository %s", versionId,
						localRepostoryId), e);
				if (!(e instanceof AlreadyExistingRepositoryException)) { // if not an already existing repo,
																			// delete it
					getProject().deleteRepository(localRepostoryId,
							(repositoryAccess instanceof CreateRemote));
				}
			} catch (Exception e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}

		VersionInfo newVersionInfo = new VersionInfo(versionId, new Date(), localRepostoryId);

		getProject().getVersionManager().recordVersion(newVersionInfo);

		return newVersionInfo;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rdf(dataset, version)', 'R')")
	public void closeVersion(String versionId) {
		VersionInfo versionInfo = getProject().getVersionManager().getVersion(versionId).orElseThrow(
				() -> new IllegalArgumentException("Unexisting version identifier: " + versionId));

		Repository versionRepository = getProject().getRepositoryManager()
				.getRepository(versionInfo.getRepositoryId());
		versionRepository.shutDown();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('rdf(dataset, version)', 'D')")
	public void deleteVersion(String versionId)
			throws JsonProcessingException, ProjectUpdateException, ReservedPropertyUpdateException {
		VersionInfo versionInfo = getProject().getVersionManager().withdrawVersionRecord(versionId);
		getProject().getRepositoryManager().removeRepository(versionInfo.getRepositoryId(), true);
	}

};