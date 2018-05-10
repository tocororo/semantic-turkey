package it.uniroma2.art.semanticturkey.extension.impl.deployer.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.util.UriComponentsBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.FormattedResourceSource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySourcedDeployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.StreamSourcedDeployer;

/**
 * Implementation of the {@link Deployer} extension point based uses the HTTP protocol. This implementation
 * can deploy data provided by a {@link RepositorySource} or by a {@link FormattedResourceSources}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPDeployer extends AbstractHTTPDeployer
		implements RepositorySourcedDeployer, StreamSourcedDeployer {

	private HTTPDeployerConfiguration conf;

	public HTTPDeployer(HTTPDeployerConfiguration conf) {
		this.conf = conf;
	}

	@Override
	protected URI getAddress() throws URISyntaxException {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.endpoint.toExternalForm());
		if (conf.queryParameters != null) {
			for (Map.Entry<String, String> entry : conf.queryParameters.entrySet()) {
				uriBuilder.queryParam(entry.getKey(), entry.getValue());
			}
		}

		return uriBuilder.build().toUri();
	}

	@Override
	protected Pair<String, String> getUsernameAndPassword() {
		if (conf.username != null && conf.password != null) {
			return ImmutablePair.of(conf.username, conf.password);
		} else {
			return null;
		}
	}

	@Override
	protected HttpVerbs getHttpVerb() {
		return HttpVerbs.valueOf(conf.httpVerb);
	}

	@Override
	protected Map<String, String> getHttpRequestHeaders() {
		if (conf.requestHeaders != null) {
			return conf.requestHeaders;
		} else {
			return null;
		}
	}

	@Override
	public void deploy(FormattedResourceSource source) throws IOException {
		deployInternal(source);
	}

	@Override
	public void deploy(RepositorySource source) throws IOException {
		deployInternal(source);
	}

}
