package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.AbstractOntoPortalDeployerConfiguration;

/**
 * A partial implementation of {@link ServerCommunicationStrategy} addressing the common aspects of the
 * interaction with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
abstract class AbstractOntoPortalCommunicationStrategy<T extends AbstractOntoPortalDeployerConfiguration>
		implements ServerCommunicationStrategy {

	protected T conf;

	public AbstractOntoPortalCommunicationStrategy(T conf) {
		this.conf = conf;
	}

	@Override
	public Map<String, String> getHttpRequestHeaders() {
		return ImmutableMap.of("Authorization", String.format("apikey token=%s", conf.apiKey));
	}

	@Override
	public void enrichRequestBodyWithConfigurationParams(MultipartEntityBuilder entityBuilder)
			throws RuntimeException {
		entityBuilder.addTextBody("acronym", conf.acronym);
		entityBuilder.addTextBody("description", StringUtils.trim(conf.description));
		if (StringUtils.isNotBlank(conf.version)) {
			entityBuilder.addTextBody("version", StringUtils.trim(conf.version));
		}
		entityBuilder.addTextBody("hasOntologyLanguage", StringUtils.trim(conf.hasOntologyLanguage));
		entityBuilder.addTextBody("status", StringUtils.trim(conf.status));
		if (StringUtils.isNotBlank(conf.released)) {
			try {
				LocalDate.parse(conf.released); // used only to validate the input
			} catch (DateTimeException e) {
				throw new RuntimeException("Invalid release date: " + conf.released);
			}
			entityBuilder.addTextBody("released", StringUtils.trim(conf.released));
		}

		if (conf.contact.size() < 1) {
			throw new RuntimeException(
					"An ontology submission to an OntoPortal repository shall have at least one contact");
		}

		for (String encodedContact : conf.contact) {
			Matcher m = Pattern.compile(AbstractOntoPortalDeployerConfiguration.CONTACT_PATTERN)
					.matcher(encodedContact);
			if (!m.find()) {
				throw new RuntimeException("Badly formatted contact: " + encodedContact);
			}
			entityBuilder.addTextBody("contact[][name]", StringUtils.trim(m.group("name")));
			entityBuilder.addTextBody("contact[][email]", StringUtils.trim(m.group("email")));
		}

		if (conf.homepage != null && StringUtils.isNoneBlank(conf.homepage)) {
			entityBuilder.addTextBody("homepage", StringUtils.trim(conf.homepage));
		}
		if (conf.documentation != null && StringUtils.isNoneBlank(conf.documentation)) {
			entityBuilder.addTextBody("documentation", StringUtils.trim(conf.documentation));
		}
		if (conf.publication != null && StringUtils.isNoneBlank(conf.publication)) {
			entityBuilder.addTextBody("publication", StringUtils.trim(conf.publication));
		}

	}

}
