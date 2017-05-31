package it.uniroma2.art.semanticturkey.project;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;

/**
 * Manages version dumps associated with a project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VersionManager {

	private Project project;
	private List<VersionInfo> versionInfoList;
	private List<VersionInfo> immutableVersionInfoList;

	public VersionManager(Project project) throws JsonParseException, JsonMappingException, IOException {
		this.project = project;
		String versionsProperty = project.getProperty(Project.VERSIONS_PROP);
		if (versionsProperty == null) {
			versionInfoList = new CopyOnWriteArrayList<>();
		} else {
			ObjectMapper objectMapper = new ObjectMapper();
			List<VersionInfo> tempVersions = objectMapper.readValue(versionsProperty,
					new TypeReference<List<VersionInfo>>() {
					});
			versionInfoList = new CopyOnWriteArrayList<>(tempVersions);
		}
		immutableVersionInfoList = Collections.unmodifiableList(versionInfoList);
	}

	public List<VersionInfo> getVersions() {
		return immutableVersionInfoList;
	}

	public synchronized void recordVersion(VersionInfo newVersionInfo)
			throws JsonProcessingException, ProjectUpdateException, ReservedPropertyUpdateException {
		if (versionInfoList.stream().anyMatch(v -> v.getVersionId().equals(newVersionInfo.getVersionId()))) {
			throw new IllegalArgumentException("A version with the same identifier was already dumped: "
					+ newVersionInfo.getVersionId());
		}
		versionInfoList.add(newVersionInfo);
		saveVersions();
	}

	private void saveVersions()
			throws JsonProcessingException, ProjectUpdateException, ReservedPropertyUpdateException {
		ObjectMapper objectMapper = new ObjectMapper();
		project.setProperty(Project.VERSIONS_PROP, objectMapper.writeValueAsString(versionInfoList));

	}

	public Optional<VersionInfo> getVersion(String versionId) {
		return versionInfoList.stream().filter(v -> versionId.equals(v.getVersionId())).findAny();
	}

}
