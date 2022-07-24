package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration.Title;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with EcoPortal.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class EcoPortalCommunicationStrategy
		extends AbstractOntoPortalCommunicationStrategy<EcoPortalDeployerConfiguration>
		implements ServerCommunicationStrategy {

	public static final String DEFAULT_API_BASE_URL = "http://ecoportal.lifewatch.eu:8080/";

	public EcoPortalCommunicationStrategy(EcoPortalDeployerConfiguration conf) {
		super(conf);
	}

	@Override
	public String getAPIBaseURL() {
		String confSuppliedURL = StringUtils.trim(conf.apiBaseURL);
		if (confSuppliedURL != null) {
			return confSuppliedURL;
		} else {
			return DEFAULT_API_BASE_URL;
		}
	}

	@Override
	public void enrichRequestBodyWithConfigurationParams(MultipartEntityBuilder entityBuilder)
			throws RuntimeException {
		super.enrichRequestBodyWithConfigurationParams(entityBuilder);

		if (conf.creators.size() < 1) {
			throw new RuntimeException("An ontology submission to EcoPortal shall have at least one creator");
		}
		

		for (String creator : conf.creators) {			
			entityBuilder.addTextBody("creators[][creatorName]", StringUtils.trim(creator));
		}

		if (conf.titles.size() < 1) {
			throw new RuntimeException("An ontology submission to EcoPortal shall have at least one title");
		}

		for (Title title : conf.titles) {			
			entityBuilder.addTextBody("titles[][title]", StringUtils.trim(title.title.getLabel()));
			entityBuilder.addTextBody("titles[][lang]", StringUtils.trim(title.title.getLanguage().get()));
			entityBuilder.addTextBody("titles[][titleType]", StringUtils.trim(title.titleType));
		}

		entityBuilder.addTextBody("publisher", StringUtils.trim(conf.publisher));

		entityBuilder.addTextBody("publicationYear", conf.publicationYear.toString());

		entityBuilder.addTextBody("resourceType", StringUtils.trim(conf.resourceType));

		entityBuilder.addTextBody("resourceTypeGeneral", StringUtils.trim(conf.resourceTypeGeneral));

	}

}
