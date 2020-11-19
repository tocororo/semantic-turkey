package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy;

import java.util.Map;
import java.util.Objects;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.aop.support.AopUtils;

import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.AbstractOntoPortalDeployerConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.OntoPortalDeployerConfiguration;

/**
 * A strategy interface declaring the required operations to interact with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public interface ServerCommunicationStrategy {
	/**
	 * The headers (key,value) that will be included in the request
	 * 
	 * @return
	 */
	Map<String, String> getHttpRequestHeaders();

	/**
	 * Enrich the multipart request body with additional parts corresponding to the configuration parameters
	 * 
	 * @param entityBuilder
	 * @throws RuntimeException
	 */
	void enrichRequestBodyWithConfigurationParams(MultipartEntityBuilder entityBuilder)
			throws RuntimeException;

	/**
	 * Returns the concrete strategy suitable for the provided configuration
	 * 
	 * @param conf
	 * @return
	 */
	static ServerCommunicationStrategy getStrategy(AbstractOntoPortalDeployerConfiguration conf) {
		Class<?> clazz = AopUtils.getTargetClass(conf);

		if (Objects.equals(clazz, OntoPortalDeployerConfiguration.class)) {
			return new OntoPortalCommunicationStrategy((OntoPortalDeployerConfiguration) conf);
		} else if (Objects.equals(clazz, EcoPortalDeployerConfiguration.class)) {
			return new EcoPortalCommunicationStrategy((EcoPortalDeployerConfiguration) conf);
		} else {
			throw new IllegalArgumentException("Unrecognized configuration type");
		}
	}

	String getAPIBaseURL();
}
