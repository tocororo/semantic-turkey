package it.uniroma2.art.semanticturkey.extension.config;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.IdentifiableComponent;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public interface ConfigurationManager<CONFTYPE extends Configuration> extends IdentifiableComponent {

	/* @TODO implement! */
	default Collection<Reference> getConfigurationReferences(Project project, STUser user) {
		throw new RuntimeException("still not implemented!!!");
	}
	
	/* @TODO implement! */
	default CONFTYPE getConfiguration(Reference reference) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	/* @TODO implement! */
	default void storeConfiguration(Reference reference, CONFTYPE configuration) {
		throw new RuntimeException("still not implemented!!!");
	}
	
}
