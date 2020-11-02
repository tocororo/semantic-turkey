package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy;

import java.util.Map;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration.Title;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with EcoPortal.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class EcoPortalCommunicationStrategy extends OntoPortalCommunicationStrategy
		implements ServerCommunicationStrategy {

	private EcoPortalDeployerConfiguration conf;

	public EcoPortalCommunicationStrategy(EcoPortalDeployerConfiguration conf) {
		super(conf);
		this.conf = conf;
	}

	@Override
	public Map<String, String> getHttpRequestHeaders() {
		return super.getHttpRequestHeaders();
	}

	@Override
	public void enrichRequestBodyWithConfigurationParams(MultipartEntityBuilder entityBuilder)
			throws RuntimeException {
		super.enrichRequestBodyWithConfigurationParams(entityBuilder);

		if (conf.creators.size() < 1) {
			throw new RuntimeException("An ontology submission to EcoPortal shall have at least one creator");
		}

		for (String creator : conf.creators) {
			entityBuilder.addTextBody("creators[]creatorName", creator);
		}

		if (conf.titles.size() < 1) {
			throw new RuntimeException("An ontology submission to EcoPortal shall have at least one title");
		}

		for (Title title : conf.titles) {
			entityBuilder.addTextBody("titles[]title", title.title.getLabel());
			entityBuilder.addTextBody("titles[]lang", title.title.getLanguage().get());
			entityBuilder.addTextBody("titles[]titleType", title.titleType);
		}

		entityBuilder.addTextBody("publisher", conf.publisher);

		entityBuilder.addTextBody("publicationYear", conf.publicationYear.toString());

		entityBuilder.addTextBody("resourceType", conf.resourceType);

		entityBuilder.addTextBody("resourceTypeGeneral", conf.resourceTypeGeneral);

	}

}
