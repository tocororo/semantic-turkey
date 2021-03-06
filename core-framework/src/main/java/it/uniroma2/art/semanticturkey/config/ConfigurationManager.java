package it.uniroma2.art.semanticturkey.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import it.uniroma2.art.semanticturkey.extension.IdentifiableComponent;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public interface ConfigurationManager<CONFTYPE extends Configuration> extends IdentifiableComponent {

	default Collection<Scope> getConfigurationScopes() {
		List<Scope> scopes = new ArrayList<>(Scope.values().length);
		if (this instanceof SystemConfigurationManager<?>) {
			scopes.add(Scope.SYSTEM);
		}
		if (this instanceof ProjectConfigurationManager<?>) {
			scopes.add(Scope.PROJECT);
		}
		if (this instanceof UserConfigurationManager<?>) {
			scopes.add(Scope.USER);
		}
		if (this instanceof PUConfigurationManager<?>) {
			scopes.add(Scope.PROJECT_USER);
		}
		return scopes;
	}

	default Collection<Reference> getConfigurationReferences(Project project, STUser user) {
		Collection<Reference> rv = new ArrayList<>();
		if (this instanceof SystemConfigurationManager) {
			rv.addAll(Reference.liftIdentifiers(null, null,
					((SystemConfigurationManager<CONFTYPE>) this).getSystemConfigurationIdentifiers()));
		}
		if (this instanceof UserConfigurationManager) {
			rv.addAll(Reference.liftIdentifiers(null, user,
					((UserConfigurationManager<CONFTYPE>) this).getUserConfigurationIdentifiers(user)));
		}
		if (project != null) {
			if (this instanceof ProjectConfigurationManager) {
				rv.addAll(Reference.liftIdentifiers(project, null, ((ProjectConfigurationManager<CONFTYPE>) this)
						.getProjectConfigurationIdentifiers(project)));
			}
			if (this instanceof PUConfigurationManager) {
				rv.addAll(Reference.liftIdentifiers(project, user, ((PUConfigurationManager<CONFTYPE>) this)
						.getProjectConfigurationIdentifiers(project, user)));
			}
		}
		return rv;
	}

	default CONFTYPE getConfiguration(Reference reference) throws STPropertyAccessException {
		Optional<Project> project = reference.getProject();
		Optional<STUser> user = reference.getUser();
		String identifier = reference.getIdentifier();

		if (project.isPresent()) {
			if (user.isPresent()) { // project-user
				return ((PUConfigurationManager<CONFTYPE>) this).getProjectConfiguration(project.get(),
						user.get(), identifier);
			} else { // project
				return ((ProjectConfigurationManager<CONFTYPE>) this).getProjectConfiguration(project.get(),
						identifier);
			}
		} else {
			if (user.isPresent()) { // user
				return ((UserConfigurationManager<CONFTYPE>) this).getUserConfiguration(user.get(),
						identifier);
			} else { // system
				return ((SystemConfigurationManager<CONFTYPE>) this).getSystemConfiguration(identifier);
			}
		}
	}

	default void storeConfiguration(Reference reference, CONFTYPE configuration)
			throws IOException, WrongPropertiesException, STPropertyUpdateException {
		Optional<Project> project = reference.getProject();
		Optional<STUser> user = reference.getUser();
		String identifier = reference.getIdentifier();

		if (project.isPresent()) {
			if (user.isPresent()) { // project-user
				((PUConfigurationManager<CONFTYPE>) this).storeProjectConfiguration(project.get(), user.get(),
						identifier, configuration);
			} else { // project
				((ProjectConfigurationManager<CONFTYPE>) this).storeProjectConfiguration(project.get(),
						identifier, configuration);
			}
		} else {
			if (user.isPresent()) { // user
				((UserConfigurationManager<CONFTYPE>) this).storeUserConfiguration(user.get(), identifier,
						configuration);
			} else { // system
				((SystemConfigurationManager<CONFTYPE>) this).storeSystemConfiguration(identifier,
						configuration);
			}
		}
	}

	default void deleteConfiguration(Reference reference) throws ConfigurationNotFoundException {
		Optional<Project> project = reference.getProject();
		Optional<STUser> user = reference.getUser();
		String identifier = reference.getIdentifier();

		if (project.isPresent()) {
			if (user.isPresent()) { // project-user
				((PUConfigurationManager<CONFTYPE>) this).deleteProjectConfiguration(project.get(),
						user.get(), identifier);
			} else { // project
				((ProjectConfigurationManager<CONFTYPE>) this).deleteProjectConfiguration(project.get(),
						identifier);
			}
		} else {
			if (user.isPresent()) { // user
				((UserConfigurationManager<CONFTYPE>) this).deleteUserConfiguration(user.get(), identifier);
			} else { // system
				((SystemConfigurationManager<CONFTYPE>) this).deleteSystemConfiguration(identifier);
			}
		}
	}

}
