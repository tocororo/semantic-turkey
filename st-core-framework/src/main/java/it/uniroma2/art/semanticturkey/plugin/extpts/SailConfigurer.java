package it.uniroma2.art.semanticturkey.plugin.extpts;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

/**
 * A component providing the configuration for a {@link Sail}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface SailConfigurer {
	SailImplConfig buildSailConfig();
}
