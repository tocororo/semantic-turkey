package it.uniroma2.art.semanticturkey.project;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.DelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.DelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Override
	public synchronized void addRepositoryConfig(RepositoryConfig config)
			throws RepositoryException, RepositoryConfigException {
		updateRepositoryInfo(config, null);
		super.addRepositoryConfig(config);
	}

	public synchronized void addRepositoryConfig(RepositoryConfig config, String backendType)
			throws RepositoryException, RepositoryConfigException {
		updateRepositoryInfo(config, backendType);
		super.addRepositoryConfig(config);
	}

	private synchronized void updateRepositoryInfo(RepositoryConfig config, String backendType)
			throws RepositoryException {
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

		repo2Info.put(repositoryId, new STRepositoryInfo(backendType, username, password));
		try {
			mapper.writeValue(repositoriesInfoFile, repo2Info);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
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
}
