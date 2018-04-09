package it.uniroma2.art.semanticturkey.config.exporter;

import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * A {@link ConfigurationManager} for managing stored exporter configurations (see
 * {@link StoredExportConfiguration}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Exporter implements PUScopedConfigurableComponent<StoredExportConfiguration> {

	@Override
	public String getId() {
		return Exporter.class.getName();
	}

}
