package it.uniroma2.art.semanticturkey.project;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

	private static class Credentials {
		private String username;
		private String password;

		@JsonCreator
		public Credentials(@JsonProperty("username") String username,
				@JsonProperty("password") String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

	private ObjectMapper mapper;
	private Map<String, Credentials> repo2Credentials;
	private File pwdStore;

	private static final String PWD_STORE_PATH = "pwd.json";

	public STLocalRepositoryManager(File baseDir) {
		super(baseDir);
		mapper = new ObjectMapper();
		repo2Credentials = new ConcurrentHashMap<>();
		pwdStore = new File(baseDir, PWD_STORE_PATH);
	}

	@Override
	public synchronized void initialize() throws RepositoryException {
		if (isInitialized()) {
			return;
		}
		if (pwdStore.exists()) {
			try {
				Map<String, Credentials> tempRepo2Credentials = mapper.readValue(pwdStore,
						new TypeReference<Map<String, Credentials>>() {
						});
				repo2Credentials.clear();
				repo2Credentials.putAll(tempRepo2Credentials);
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
			Credentials credentials = repo2Credentials.get(id);

			if (credentials != null) {
				((HTTPRepository) repository).setUsernameAndPassword(credentials.getUsername(),
						credentials.getPassword());
			}
		}
	}

	@Override
	public synchronized void addRepositoryConfig(RepositoryConfig config)
			throws RepositoryException, RepositoryConfigException {
		storePwdIfAvailable(config);
		super.addRepositoryConfig(config);
	}

	private void storePwdIfAvailable(RepositoryConfig config) throws RepositoryException {
		String repositoryId = config.getID();

		RepositoryImplConfig repoImplConfig = config.getRepositoryImplConfig();

		while (repoImplConfig instanceof DelegatingRepositoryImplConfig) {
			repoImplConfig = ((DelegatingRepositoryImplConfig) repoImplConfig).getDelegate();
		}

		if (repoImplConfig instanceof HTTPRepositoryConfig) {
			String username = ((HTTPRepositoryConfig) repoImplConfig).getUsername();
			String password = ((HTTPRepositoryConfig) repoImplConfig).getPassword();

			if (username != null || password != null) {
				repo2Credentials.put(repositoryId, new Credentials(username, password));
				try {
					mapper.writeValue(pwdStore, repo2Credentials);
				} catch (IOException e) {
					throw new RepositoryException(e);
				}
			}
		}

	}
}
