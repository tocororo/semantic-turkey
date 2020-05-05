package it.uniroma2.art.semanticturkey.config.importer;

import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * A {@link ConfigurationManager} for managing stored importer configurations (see
 * {@link StoredImportConfiguration}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Importer implements PUScopedConfigurableComponent<StoredImportConfiguration> {

	@Override
	public String getId() {
		return Importer.class.getName();
	}

}
