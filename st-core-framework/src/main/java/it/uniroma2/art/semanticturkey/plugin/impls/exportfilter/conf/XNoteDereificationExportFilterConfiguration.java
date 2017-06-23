package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "XNote Dereification Export Filter";
	}

	@STProperty(description = "Preserves reified notes in the output")
	public boolean preserveReifiedNotes = true;
}
