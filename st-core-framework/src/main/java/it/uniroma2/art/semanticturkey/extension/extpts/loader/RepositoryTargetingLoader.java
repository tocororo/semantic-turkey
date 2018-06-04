package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A {@link Loader} which can load data into {@link RepositoryConnection}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface RepositoryTargetingLoader extends Loader {

	void load(RepositoryTarget target) throws IOException;
}
