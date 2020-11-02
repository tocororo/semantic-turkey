package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closer;

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySourcedDeployer;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.http.AbstractHTTPDeployer;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy.ServerCommunicationStrategy;

/**
 * Implementation of the {@link Deployer} extension point that uses the OntoPortal REST API.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see <a href="https://ontoportal.org/">OntoPortal Alliance</a>
 * @see <a href="http://data.bioontology.org/documentation#OntologySubmission">Ontology submission</a>
 * 
 */
public class OntoPortalDeployer extends AbstractHTTPDeployer<RepositorySource>
		implements RepositorySourcedDeployer {

	public static String SUBMISSIONS_ENDPOINT = "ontologies/{acronym}/submissions";

	private OntoPortalDeployerConfiguration conf;

	// can be used to register resources to be freed after the deployment
	protected ThreadLocal<Closer> requestScopedResourcesToRelease;

	private ServerCommunicationStrategy strategy;

	public OntoPortalDeployer(OntoPortalDeployerConfiguration conf) {
		this.conf = conf;
		this.strategy = ServerCommunicationStrategy.getStrategy(conf);
		this.requestScopedResourcesToRelease = ThreadLocal.withInitial(Closer::create);
	}

	@Override
	public void deploy(RepositorySource source) throws IOException {
		try {
			deployInternal(source);
		} finally {
			this.requestScopedResourcesToRelease.get().close();
		}
	}

	protected URI getAddress() throws URISyntaxException {
		String apiBaseURL = conf.apiBaseURL.trim();
		if (!apiBaseURL.endsWith("/")) {
			apiBaseURL = apiBaseURL + "/";
		}
		return UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(SUBMISSIONS_ENDPOINT)
				.buildAndExpand(ImmutableMap.of("acronym", conf.acronym)).toUri();
	}

	@Override
	protected HttpVerbs getHttpVerb() {
		return HttpVerbs.POST;
	}

	@Override
	protected Map<String, String> getHttpRequestHeaders() {
		return this.strategy.getHttpRequestHeaders();
	}

	protected HttpEntity createHttpEntity(RepositorySource source) throws IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		this.strategy.enrichRequestBodyWithConfigurationParams(entityBuilder);

		Path tempFilePath = Files.createTempFile("ontoportal_deployer", "rdf");
		this.requestScopedResourcesToRelease.get().register(() -> Files.deleteIfExists(tempFilePath));
		try (OutputStream os = Files.newOutputStream(tempFilePath)) {
			source.getSourceRepositoryConnection().export(Rio.createWriter(RDFFormat.RDFXML, os),
					source.getGraphs());
		}
		entityBuilder.addBinaryBody("tempFile", tempFilePath.toFile(),
				ContentType.create("application/rdf+xml", StandardCharsets.UTF_8), "submission.rdf");

		return entityBuilder.build();
	}

	@Override
	protected String buildExceptionFromResponse(CloseableHttpResponse httpResponse) throws IOException {
		String exceptionMsg = super.buildExceptionFromResponse(httpResponse);
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			return exceptionMsg + "\n" + IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
		}
		return exceptionMsg;
	}
}
