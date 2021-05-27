package it.uniroma2.art.semanticturkey.showvoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import org.apache.commons.lang.time.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PendingContributionStore {

    private static final String SETTING_KEY = "showvoc.pending_contribution_map";

    private Properties properties; //properties file containing the map
    private Map<String, PendingContribution> pendingContributions; //token -> pendingContribution
    private ObjectMapper mapper;

    /*
    When a ShowVoc contribution is approved by the admin, ST delete its description from the ContributionStore
    (stored previously once submitted by the contributor) and create a project (on ST-ShowVoc if stable or ST-VB if dev).
    In the meantime that the system wait the contributor to load the data into the project
    (the workflow foreseen that a link for loading data is sent to the contributor)
    ST needs to keep trace of this pending contribution (namely waiting to be completed)
    in order to verify, once contributor requests to load data, that the provided info are ok
    (e.g. token exchanged is aligned with the contributor email and the project name provided in the request)
    and to finally allow the load.
    So, in order to keep trace of those pending contribution ST stores a map <token,contribution_info>.
    Even if this is not a real "Settings" such map is stored under the system settings folder in
    a folder named after this class as every other Settings.
    I avoided to use the Settings mechanism because this map doesn't need to be shown/handled by the Settings manager
     */

    public PendingContributionStore() throws IOException {
        mapper = new ObjectMapper();

        initProperties();
        String pendingContribSetting = properties.getProperty(SETTING_KEY);
        if (pendingContribSetting != null) {
            this.pendingContributions = mapper.readValue(pendingContribSetting, new TypeReference<Map<String, PendingContribution>>() {
            });
        } else {
            this.pendingContributions = new HashMap<>();
        }
        this.clean();
    }

    public void addPendingContribution(String token, String projectName, String contributorEmail, String contributorName,
            String contributorLastName) throws IOException {
        pendingContributions.put(token, new PendingContribution(projectName, contributorEmail, contributorName, contributorLastName));
        this.store();
    }

    /**
     * Given a token returns the related pending contribution (if any)
     *
     * @param token
     * @return the project name of the pending contribution related to the token. Null if the token has no pending contribution
     */
    public PendingContribution getPendingContribution(String token) {
        return pendingContributions.get(token);
    }

    public void removePendingContribution(String token) throws IOException {
        pendingContributions.remove(token);
        this.store();
    }

    private void store() throws IOException {
        this.clean();
        String pcAsString = mapper.writeValueAsString(pendingContributions);
        properties.setProperty(SETTING_KEY, pcAsString);
        updateProperties();
    }

    /**
     * Delete from the map the pending contribution older than 30 days (2592000000ms).
     * Invoked in constructor, so each time the PendingContributionStore is initialized,
     * and in store(), so each time the PendingContributionStore is updated (contribution added/removed)
     */
    private void clean() {
        long currentTime = System.currentTimeMillis();
        pendingContributions.entrySet().removeIf(pc -> currentTime - pc.getValue().getTimestamp() > DateUtils.MILLIS_PER_DAY * 30);
    }


    private void initProperties() {
        properties = new Properties();
        File propertiesFile = STPropertiesManager.getSystemSettingsFile(this.getClass().getName());
        if (propertiesFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propertiesFile);
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void updateProperties() throws IOException {
        File propertiesFile = STPropertiesManager.getSystemSettingsFile(this.getClass().getName());
        if (!propertiesFile.getParentFile().exists()) { // if path doesn't exist, first create it
            propertiesFile.getParentFile().mkdirs();
        }
        try (FileOutputStream os = new FileOutputStream(propertiesFile)) {
            properties.store(os, null);
        }
    }

}


