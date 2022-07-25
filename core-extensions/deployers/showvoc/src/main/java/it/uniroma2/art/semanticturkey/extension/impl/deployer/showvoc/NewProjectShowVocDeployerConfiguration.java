package it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

/**
 * Configuration class for {@link ShowVocDeployer} that targets a new project.
 *
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class NewProjectShowVocDeployerConfiguration extends ShowVocDeployerConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc.NewProjectShowVocDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";

		public static final String coreRepoSailConf$description = keyBase + ".coreRepoSailConf.description";
		public static final String coreRepoSailConf$displayName = keyBase + ".coreRepoSailConf.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.coreRepoSailConf$description + "}", displayName = "{" + MessageKeys.coreRepoSailConf$displayName + "}")
	@Required
	public @HasExtensionPoint(RepositoryImplConfigurer.class) Configuration coreRepoSailConf;

}
