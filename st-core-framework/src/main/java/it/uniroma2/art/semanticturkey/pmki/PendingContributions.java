package it.uniroma2.art.semanticturkey.pmki;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import org.apache.commons.lang.time.DateUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PendingContributions {

	private static final String SETTING_KEY = "pmki.pending_contribution_map";

	private Map<String, PendingContribution> pendingContributions; //token -> pendingContribution
	private ObjectMapper mapper;

	public PendingContributions() throws STPropertyAccessException, IOException {
		mapper = new ObjectMapper();
		String pendingContribSetting = STPropertiesManager.getSystemSetting(SETTING_KEY, this.getClass().getName());
		if (pendingContribSetting != null) {
			this.pendingContributions = mapper.readValue(pendingContribSetting, new TypeReference<Map<String, PendingContribution>>(){});
		} else {
			this.pendingContributions = new HashMap<>();
		}
	}

	public void addPendingContribution(String token, String projectName) throws STPropertyUpdateException, JsonProcessingException {
		pendingContributions.put(token, new PendingContribution(projectName));
		this.store();
	}

	/**
	 * Given a token returns the related project name (if any)
	 * @param token
	 * @return the project name of the pending contribution related to the token. Null if the token has no pending contribution
	 */
	public String getPendingContributionProject(String token) {
		PendingContribution pc = pendingContributions.get(token);
		if (pc != null) {
			return pc.getProjectName();
		} else { //no pending contribution (wrong token or contribution expired
			return null;
		}
	}

	public void removePendingContribution(String token) throws STPropertyUpdateException, JsonProcessingException {
		pendingContributions.remove(token);
		this.store();
	}

	private void store() throws JsonProcessingException, STPropertyUpdateException {
		this.clean();
		String pcAsString = mapper.writeValueAsString(pendingContributions);
		STPropertiesManager.setSystemSetting(SETTING_KEY, pcAsString, this.getClass().getName());
	}

	/**
	 * Delete from the map the pending contribution older than 30 days (2592000000ms)
	 */
	private void clean() {
		long currentTime = System.currentTimeMillis();
		pendingContributions.entrySet().removeIf(pc -> currentTime - pc.getValue().getTimestamp() > DateUtils.MILLIS_PER_DAY * 30);
	}


	private static class PendingContribution {
		private String projectName;
		private long timestamp;

		@JsonCreator
		public PendingContribution(@JsonProperty("projectName") String projectName, @JsonProperty("timestamp") long timestamp) {
			this.projectName = projectName;
			this.timestamp = timestamp;
		}

		public PendingContribution(String projectName) {
			this.projectName = projectName;
			timestamp = System.currentTimeMillis();
		}

		public String getProjectName() {
			return projectName;
		}

		public long getTimestamp() {
			return timestamp;
		}

	}

}


