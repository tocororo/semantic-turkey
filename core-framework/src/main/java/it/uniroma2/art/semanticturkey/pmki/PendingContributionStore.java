package it.uniroma2.art.semanticturkey.pmki;

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

public class PendingContributionStore {

	private static final String SETTING_KEY = "pmki.pending_contribution_map";

	private Map<String, PendingContribution> pendingContributions; //token -> pendingContribution
	private ObjectMapper mapper;

	public PendingContributionStore() throws STPropertyAccessException, IOException {
		mapper = new ObjectMapper();
		String pendingContribSetting = STPropertiesManager.getSystemSetting(SETTING_KEY, this.getClass().getName());
		if (pendingContribSetting != null) {
			this.pendingContributions = mapper.readValue(pendingContribSetting, new TypeReference<Map<String, PendingContribution>>(){});
		} else {
			this.pendingContributions = new HashMap<>();
		}
	}

	public void addPendingContribution(String token, String projectName, String contributorEmail, String contributorName,
			String contributorLastName) throws STPropertyUpdateException, JsonProcessingException {
		pendingContributions.put(token, new PendingContribution(projectName, contributorEmail, contributorName, contributorLastName));
		this.store();
	}

	/**
	 * Given a token returns the related pending contribution (if any)
	 * @param token
	 * @return the project name of the pending contribution related to the token. Null if the token has no pending contribution
	 */
	public PendingContribution getPendingContribution(String token) {
		return pendingContributions.get(token);
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

}


