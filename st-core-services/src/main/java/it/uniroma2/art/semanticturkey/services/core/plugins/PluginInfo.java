package it.uniroma2.art.semanticturkey.services.core.plugins;

/**
 * Information about a plugin.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PluginInfo {
	private String factoryID;

	public PluginInfo(String factoryID) {
		this.factoryID = factoryID;
	}

	public String getFactoryID() {
		return factoryID;
	}
}
