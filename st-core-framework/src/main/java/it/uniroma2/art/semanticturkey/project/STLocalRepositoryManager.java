package it.uniroma2.art.semanticturkey.project;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.DelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.DelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreFactory;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;

/**
 * A subclass of {@link LocalRepositoryManager} adding ST-related capabilities. Currently, an important
 * capability is the configuration of username/passwords, which are not stored inside SYSTEM.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STLocalRepositoryManager extends LocalRepositoryManager {

	private ObjectMapper mapper;
	private Map<String, STRepositoryInfo> repo2Info;
	private File repositoriesInfoFile;

	private static final String REPOSITORIES_INFO_JSON = "repositories-info.json";

	public STLocalRepositoryManager(File baseDir) {
		super(baseDir);
		mapper = new ObjectMapper();
		repo2Info = new ConcurrentHashMap<>();
		repositoriesInfoFile = new File(baseDir, REPOSITORIES_INFO_JSON);
	}

	@Override
	public synchronized void initialize() throws RepositoryException {
		if (isInitialized()) {
			return;
		}
		if (repositoriesInfoFile.exists()) {
			try {
				Map<String, STRepositoryInfo> tempRepo2Credentials = mapper.readValue(repositoriesInfoFile,
						new TypeReference<Map<String, STRepositoryInfo>>() {
						});
				repo2Info.clear();
				repo2Info.putAll(tempRepo2Credentials);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
		super.initialize();
	}

	@Override
	public synchronized void addRepositoryConfig(RepositoryConfig config)
			throws RepositoryException, RepositoryConfigException {
		updateRepositoryInfo(config, null, false);
		super.addRepositoryConfig(config);
	}

	public synchronized void addRepositoryConfig(RepositoryConfig config, String backendType,
			boolean customizeSearch) throws RepositoryException, RepositoryConfigException {
		updateRepositoryInfo(config, backendType, customizeSearch);
		super.addRepositoryConfig(config);
	}

	@Override
	protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
		Repository repository = super.createRepository(id);
		injectPwdIfAvailable(id, repository);
		return repository;
	}

	protected void injectPwdIfAvailable(String id, Repository repository) {
		// unwraps delegates, such as the one making a repository read-only
		while (repository instanceof DelegatingRepository) {
			repository = ((DelegatingRepository) repository).getDelegate();
		}

		if (repository instanceof HTTPRepository) {
			STRepositoryInfo credentials = repo2Info.get(id);

			if (credentials != null) {
				((HTTPRepository) repository).setUsernameAndPassword(credentials.getUsername(),
						credentials.getPassword());
			}
		}
	}

	public Optional<STRepositoryInfo> getSTRepositoryInfo(String id) {
		return Optional.ofNullable(repo2Info.get(id));
	}

	private synchronized void updateRepositoryInfo(RepositoryConfig config, String backendType,
			boolean customizeSearch) throws RepositoryException {
		String repositoryId = config.getID();

		RepositoryImplConfig repoImplConfig = config.getRepositoryImplConfig();

		while (repoImplConfig instanceof DelegatingRepositoryImplConfig) {
			repoImplConfig = ((DelegatingRepositoryImplConfig) repoImplConfig).getDelegate();
		}

		if (backendType == null) {
			backendType = detectBackendType(repoImplConfig);
		}
		String username;
		String password;

		if (repoImplConfig instanceof HTTPRepositoryConfig) {
			username = ((HTTPRepositoryConfig) repoImplConfig).getUsername();
			password = ((HTTPRepositoryConfig) repoImplConfig).getPassword();
		} else {
			username = null;
			password = null;
		}

		IsolationLevel defaultReadIsolationLevel = null;
		IsolationLevel defaultWriteIsolationLevel = null;

		if (MemoryStoreFactory.SAIL_TYPE.equals(backendType)
				|| NativeStoreFactory.SAIL_TYPE.equals(backendType)) {
			defaultWriteIsolationLevel = IsolationLevels.SERIALIZABLE;
		}

		// customizeSearch = false; // ignores search customization until we decide it is safe

		SearchStrategies searchStrategy = customizeSearch && isGraphDBBackEnd(backendType)
				? SearchStrategies.GRAPH_DB
				: SearchStrategies.REGEX;

		repo2Info.put(repositoryId, new STRepositoryInfo(backendType, username, password,
				defaultReadIsolationLevel, defaultWriteIsolationLevel, searchStrategy));
		writeAdditionalRepositoryInfo();
	}

	@Override
	protected void cleanUpRepository(String repositoryID) throws IOException {
		super.cleanUpRepository(repositoryID);
		repo2Info.remove(repositoryID);
		writeAdditionalRepositoryInfo();
	}

	public void removeRepository(String repositoryId, boolean propagateDelete)
			throws RepositoryException, RepositoryConfigException {
		logger.debug("Remove repository {} with propagateDelete={}", repositoryId, propagateDelete);
		synchronized (initializedRepositories) {
			@Nullable
			RepositoryConfig repoConfig;
			try { // if the configuration is not supported, it is impossible to parse it
				repoConfig = getRepositoryConfig(repositoryId);
			} catch (Exception e) {
				repoConfig = null;
			}
			@Nullable
			STRepositoryInfo repoInfo = repo2Info.get(repositoryId);

			boolean removed = forceRemoveRepository(this, repositoryId);

			logger.debug("Repository {} removed={}", removed);

			// Skip the rest of the method, if the repository was not actually removed or deletion should not
			// be propagated
			if (!removed || !propagateDelete)
				return;

			RepositoryImplConfig repoImplConfig = repoConfig != null ? repoConfig.getRepositoryImplConfig()
					: null;
			while (repoImplConfig instanceof DelegatingRepositoryImplConfig) {
				repoImplConfig = ((DelegatingRepositoryImplConfig) repoImplConfig).getDelegate();
			}

			if (repoImplConfig instanceof HTTPRepositoryConfig) {

				logger.debug("Delete remote counterpart");

				HTTPRepositoryConfig httpRepoImpl = (HTTPRepositoryConfig) repoImplConfig;
				String username;
				String password;

				if (repoInfo != null) {
					username = repoInfo.getUsername();
					password = repoInfo.getPassword();
				} else {
					username = null;
					password = null;
				}
				RemoteRepositoryManager remoteRepoMgr = RemoteRepositoryManager
						.getInstance(Protocol.getServerLocation(httpRepoImpl.getURL()), username, password);
				String remoteRepositoryId = Protocol.getRepositoryID(httpRepoImpl.getURL());

				try {
					try {
						forceRemoveRepository(remoteRepoMgr, remoteRepositoryId);
					} catch (Exception e) {
						throw new RepositoryException(
								"Unable to remove the remote counterpart of repository " + repositoryId, e);
					}
				} finally {
					remoteRepoMgr.shutDown();
				}
			}
		}
	}

	/**
	 * Removes a repository. When the configuration of a repository is wrong, the method
	 * {@link RepositoryManager#removeRepository(String)} throws {@link RepositoryConfigException}.
	 * Conversely, this method would try to delete the repository, by manipulating the <em>system
	 * repository</em> (see {@link #getSystemRepository()}).
	 * 
	 * @param repoMgr
	 * @param repostoryId
	 * @return
	 * @throws RepositoryException
	 * @throws RepositoryConfigException
	 */
	protected boolean forceRemoveRepository(RepositoryManager repoMgr, String repostoryId)
			throws RepositoryException, RepositoryConfigException {
		try {
			return repoMgr.removeRepository(repostoryId);
		} catch (RepositoryConfigException | RepositoryException e) {
			/*
			 * In case of incorrect repository configuration, the methdod above will throw, so an alternate
			 * lower-layer mechanism is used
			 */
			return RepositoryConfigUtil.removeRepositoryConfigs(repoMgr.getSystemRepository(), repostoryId);
		}
	}

	protected void writeAdditionalRepositoryInfo() throws RepositoryException {
		try {
			mapper.writeValue(repositoriesInfoFile, repo2Info);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public static boolean isGraphDBBackEnd(@Nullable String backendType) {
		return backendType != null && backendType.startsWith("graphdb:");
	}

	public static @Nullable String detectBackendType(RepositoryImplConfig repoImplConfig) {
		if (repoImplConfig instanceof SailRepositoryConfig) {
			SailImplConfig sailImplConfig = ((SailRepositoryConfig) repoImplConfig).getSailImplConfig();

			while (sailImplConfig instanceof DelegatingSailImplConfig) {
				sailImplConfig = ((DelegatingSailImplConfig) sailImplConfig).getDelegate();
			}

			return sailImplConfig.getType();
		}
		return null;
	}

	public synchronized void modifyAccessCredentials(String repositoryID, @Nullable String newUsername,
			@Nullable String newPassword) {
		RepositoryConfig repConfig = Objects.requireNonNull(this.getRepositoryConfig(repositoryID),
				() -> "Not configured repository: " + repositoryID);

		RepositoryImplConfig repImplConfig = getUnfoldedRepositoryImplConfig(repConfig);

		if (!(repImplConfig instanceof HTTPRepositoryConfig)) {
			throw new IllegalArgumentException("Not a remote repository: " + repositoryID);
		}

		STRepositoryInfo stRepInfo = repo2Info.getOrDefault(repositoryID, STRepositoryInfo.createDefault());

		STRepositoryInfo newStRepInfo = stRepInfo.withNewAccessCredentials(newUsername, newPassword);

		repo2Info.put(repositoryID, newStRepInfo);
		writeAdditionalRepositoryInfo();
	}

	public synchronized void batchModifyAccessCredentials(String serverURL, boolean matchUsername,
			@Nullable String currentUserName, @Nullable String newUsername, @Nullable String newPassword) {
		for (RepositoryInfo repositoryInfo : getAllRepositoryInfos(true)) {
			RepositoryConfig repConfig = this.getRepositoryConfig(repositoryInfo.getId());
			RepositoryImplConfig repImplConfig = getUnfoldedRepositoryImplConfig(repConfig);

			if (!(repImplConfig instanceof HTTPRepositoryConfig)) {
				continue; // skip non-remote repositories
			}

			HTTPRepositoryConfig httpRepImplConfig = (HTTPRepositoryConfig) repImplConfig;

			// Skip repositories on irrelevant servers
			if (!Protocol.getServerLocation(httpRepImplConfig.getURL()).equals(serverURL))
				continue;

			STRepositoryInfo stRepInfo = repo2Info.getOrDefault(repositoryInfo.getId(),
					STRepositoryInfo.createDefault());

			if (matchUsername && !Objects.equals(currentUserName, stRepInfo.getUsername())) {
				continue; // skip repositories for irrelevant usernames
			}

			STRepositoryInfo newStRepInfo = stRepInfo.withNewAccessCredentials(newUsername, newPassword);

			repo2Info.put(repositoryInfo.getId(), newStRepInfo);
		}

		writeAdditionalRepositoryInfo();
	}

	public static RepositoryImplConfig getUnfoldedRepositoryImplConfig(RepositoryConfig config) {
		RepositoryImplConfig repImplConfig = config.getRepositoryImplConfig();
		if (repImplConfig instanceof DelegatingRepositoryImplConfig) {
			repImplConfig = ((DelegatingRepositoryImplConfig) repImplConfig).getDelegate();
		}
		return repImplConfig;
	}

}
