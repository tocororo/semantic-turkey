package it.uniroma2.art.semanticturkey.services.core.metadata;

import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Information about a {@link DatasetMetadataExporter} settings.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ExporterSettingsInfo {

	private final STProperties extensionPointSettings;
	private final STProperties pluginSettings;

	public ExporterSettingsInfo(STProperties extensionPointSettings, STProperties pluginSettings) {
		this.extensionPointSettings = extensionPointSettings;
		this.pluginSettings = pluginSettings;
	}

	public STProperties getExtensionPointSettings() {
		return extensionPointSettings;
	}

	public STProperties getPluginSettings() {
		return pluginSettings;
	}

}
