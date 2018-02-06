package it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

import it.uniroma2.art.semanticturkey.extension.Extension;

/**
 * A component providing the configuration for a {@link RepositoryImplConfig}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface RepositoryImplConfigurer extends Extension {
	/**
	 * Builds a {@link RepositoryImplConfig}, considering the provided <code>backend decorator</code> (if not
	 * <code>null</code>). A <code>backend decorator</code> wraps the backed sail with any stacable sail
	 * (useful to add history/validation).
	 * 
	 * @param backendDecorator
	 * @return
	 */
	RepositoryImplConfig buildRepositoryImplConfig(
			@Nullable Function<SailImplConfig, SailImplConfig> backendDecorator);
}
