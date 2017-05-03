package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.exceptions.RepositoryCreationException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.ProjectUtils;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.versions.VersionInfo;

/**
 * This class provides services for handling versions of a repository.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class Versions extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Versions.class);

	// TODO: establish authorizations for each operation

	@STServiceOperation
	public List<VersionInfo> getVersions() throws JsonParseException, JsonMappingException, IOException {
		RepositoryManager repositoryManager = getProject().getRepositoryManager();
		return repositoryManager.getAllRepositoryInfos(true).stream()
				.filter(ProjectUtils::isVersionRepostory).map(info -> {
					String versionId = ProjectUtils.extractVersionFromRepositoryId(info.getId());
					return new VersionInfo(versionId);
				}).collect(Collectors.toList());
	}

	/**
	 * Dumps the current content of the core repository to a dedicated repository.
	 * 
	 * @param repositoryAccess
	 *            tells the location of the repository. If <code>null</code>, then the same location of the
	 *            core repository is used
	 * @param repositoryId
	 *            tells the name of the version repository. If the repository is local, this paramater must be
	 *            <code>null</code>
	 * @param versionId
	 *            the version identifier
	 * @return
	 * @throws RepositoryCreationException
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@Read
	public VersionInfo createVersionDump(@Optional RepositoryAccess repositoryAccess,
			@Optional String repositoryId,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.PredefinedRepositoryImplConfigurerFactory\"}") PluginSpecification repoConfigurerSpecification,
			String versionId) throws RepositoryCreationException {

		try {
			repoConfigurerSpecification.expandDefaults();
		} catch (ClassNotFoundException | BadConfigurationException | UnsupportedPluginConfigurationException
				| UnloadablePluginConfigurationException e) {
			throw new RepositoryCreationException(e);
		}

		if (repositoryAccess == null) {
			throw new UnsupportedOperationException(
					"Implement inherintance of reposuitory access from the prjoect");
		}
		if (repositoryId != null) {
			if (repositoryAccess.isLocal()) {
				throw new IllegalArgumentException("Cannot specify the identifier of a local repository");
			}
		} else {
			repositoryId = getProject().getName() + "-" + ProjectUtils.computeVersionRepository(versionId);
		}

		String localRepostoryId = ProjectUtils.computeVersionRepository(versionId);

		Repository versionRepository = getProject().createRepository(repositoryAccess, repositoryId,
				repoConfigurerSpecification, localRepostoryId);

		try (RepositoryConnection outConn = versionRepository.getConnection()) {
			outConn.begin(IsolationLevels.READ_COMMITTED);
			getManagedConnection().export(new RDFInserter(outConn));
			outConn.commit();
		}

		return new VersionInfo(localRepostoryId);
	}

	protected List<VersionInfo> getVersionsInternal()
			throws IOException, JsonParseException, JsonMappingException {
		String versionsProperty = getProject().getProperty("versions");
		if (versionsProperty == null)
			return Collections.emptyList();

		ObjectMapper objectMapper = new ObjectMapper();
		List<VersionInfo> versions = objectMapper.readValue(versionsProperty,
				new TypeReference<List<VersionInfo>>() {
				});
		return versions;
	}

};