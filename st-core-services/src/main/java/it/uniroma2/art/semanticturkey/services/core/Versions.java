package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.RepositoryCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.ProjectUtils;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.project.VersionInfo;
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

	// TODO: establish authorizations for each operation

	@STServiceOperation
	public List<VersionInfo> getVersions() throws JsonParseException, JsonMappingException, IOException {
		return getProject().getVersionManager().getVersions();
	}

	/**
	 * Dumps the current content of the core repository to a dedicated repository.
	 * 
	 * @param repositoryAccess
	 *            tells the location of the repository
	 * @param repositoryId
	 *            tells the name of the version repository. If the repository is local, this parameter must be
	 *            <code>null</code>
	 * @param versionId
	 *            the version identifier
	 * @return
	 * @throws RepositoryCreationException
	 * @throws ReservedPropertyUpdateException
	 * @throws ProjectUpdateException
	 * @throws JsonProcessingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public VersionInfo createVersionDump(RepositoryAccess repositoryAccess, @Optional String repositoryId,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.PredefinedRepositoryImplConfigurerFactory\"}") PluginSpecification repoConfigurerSpecification,
			String versionId) throws RepositoryCreationException, JsonProcessingException,
			ProjectUpdateException, ReservedPropertyUpdateException {

		try {
			repoConfigurerSpecification.expandDefaults();
		} catch (ClassNotFoundException | BadConfigurationException | UnsupportedPluginConfigurationException
				| UnloadablePluginConfigurationException e) {
			throw new RepositoryCreationException(e);
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

		VersionInfo newVersionInfo = new VersionInfo(versionId, new Date(), localRepostoryId);

		getProject().getVersionManager().recordVersion(newVersionInfo);

		return newVersionInfo;
	}

};