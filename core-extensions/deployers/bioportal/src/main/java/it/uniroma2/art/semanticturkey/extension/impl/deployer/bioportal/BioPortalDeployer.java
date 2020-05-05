package it.uniroma2.art.semanticturkey.extension.impl.deployer.bioportal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

/**
 * Implementation of the {@link Deployer} that uses the BioPortal REST API.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see <a href="http://data.bioontology.org/documentation#OntologySubmission">Ontology submission</a>
 * 
 */
public class BioPortalDeployer extends AbstractHTTPDeployer<RepositorySource>
		implements RepositorySourcedDeployer {

	public static String BIOPORTAL_BASE = "https://data.bioontology.org/";
	public static String SUBMISSIONS_ENDPOINT = "ontologies/{acronym}/submissions";

	private BioPortalDeployerConfiguration conf;

	// can be used to register resources to be freed after the deployment
	protected ThreadLocal<Closer> requestScopedResourcesToRelease;

	public BioPortalDeployer(BioPortalDeployerConfiguration conf) {
		this.conf = conf;
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
		return UriComponentsBuilder
				.fromHttpUrl(
						StringUtils.isNoneBlank(conf.apiBaseURL) ? conf.apiBaseURL.trim() : BIOPORTAL_BASE)
				.path(SUBMISSIONS_ENDPOINT).buildAndExpand(ImmutableMap.of("acronym", conf.acronym)).toUri();
	}

	@Override
	protected HttpVerbs getHttpVerb() {
		return HttpVerbs.POST;
	}

	@Override
	protected Map<String, String> getHttpRequestHeaders() {
		return ImmutableMap.of("Authorization", String.format("apikey token=%s", conf.apiKey));
	}

	public static Pattern CONTACT_PATTERN = Pattern.compile("^\\s*(?<name>.+)\\s*\\((?<email>.+)\\s*\\)$");

	protected HttpEntity createHttpEntity(RepositorySource source) throws IOException {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.addTextBody("acronym", conf.acronym);
		entityBuilder.addTextBody("description", conf.description);
		if (StringUtils.isNotBlank(conf.version)) {
			entityBuilder.addTextBody("version", conf.version);
		}
		entityBuilder.addTextBody("hasOntologyLanguage", conf.hasOntologyLanguage);
		entityBuilder.addTextBody("status", conf.status);
		if (StringUtils.isNotBlank(conf.released)) {
			try {
				LocalDate.parse(conf.released); // used only to validate the input
			} catch (DateTimeException e) {
				throw new RuntimeException("Invalid release date: " + conf.released);
			}
			entityBuilder.addTextBody("released", conf.released);
		}

		if (conf.contact.size() < 1) {
			throw new RuntimeException("An ontology submission to BioPortal shall have at least one contact");
		}

		for (String encodedContact : conf.contact) {
			Matcher m = CONTACT_PATTERN.matcher(encodedContact);
			if (!m.find()) {
				throw new RuntimeException("Badly formatted contact: " + encodedContact);
			}
			entityBuilder.addTextBody("contact[]name", m.group("name"));
			entityBuilder.addTextBody("contact[]email", m.group("email"));
		}

		if (conf.homepage != null && StringUtils.isNoneBlank(conf.homepage)) {
			entityBuilder.addTextBody("homepage", conf.homepage);
		}
		if (conf.documentation != null && StringUtils.isNoneBlank(conf.documentation)) {
			entityBuilder.addTextBody("documentation", conf.documentation);
		}
		if (conf.publication != null && StringUtils.isNoneBlank(conf.publication)) {
			entityBuilder.addTextBody("publication", conf.publication);
		}

		Path tempFilePath = Files.createTempFile("bioportal_deployer", "rdf");
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
