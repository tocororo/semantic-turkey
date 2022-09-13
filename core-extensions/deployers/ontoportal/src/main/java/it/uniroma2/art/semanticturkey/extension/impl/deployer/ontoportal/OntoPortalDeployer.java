package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

	private AbstractOntoPortalDeployerConfiguration conf;

	// can be used to register resources to be freed after the deployment
	protected ThreadLocal<Closer> requestScopedResourcesToRelease;

	private ServerCommunicationStrategy strategy;

	public OntoPortalDeployer(AbstractOntoPortalDeployerConfiguration conf) {
		this.conf = conf;
		this.strategy = ServerCommunicationStrategy.getStrategy(conf);
		this.requestScopedResourcesToRelease = ThreadLocal.withInitial(Closer::create);
	}

	@Override
	public void deploy(RepositorySource source) throws IOException {
		try {
			checkConstraints(source);
			deployInternal(source);
		} finally {
			this.requestScopedResourcesToRelease.get().close();
		}
	}

	protected void checkConstraints(RepositorySource source) throws IOException {

		SimpleDataset dataset = new SimpleDataset();
		Arrays.stream(source.getGraphs()).forEach(dataset::addDefaultGraph);

		RepositoryConnection con = source.getSourceRepositoryConnection();
		// checks whether there are reified labels for which there is not the corresponding plain version
		BooleanQuery checkReifiedLabels = con.prepareBooleanQuery(
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
						"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
						"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
						"\n" +
						"ASK {\n" +
						"    VALUES (?reifiedProp ?plainProp) {\n" +
						"        (skosxl:prefLabel skos:prefLabel)\n" +
						"        (skosxl:altbel skos:altLabel)\n" +
						"        (skosxl:hidddenLabel skos:hiddenLabel)\n" +
						"    }\n" +
						"    ?s ?reifiedProp [\n" +
						"    \tskosxl:literalForm ?lit\n" +
						"    ]\n" +
						"    FILTER NOT EXISTS {\n" +
						"        ?s ?plainProp ?lit\n" +
						"    }\n" +
						"} ");
		checkReifiedLabels.setDataset(dataset);
		checkReifiedLabels.setIncludeInferred(false);
		boolean nonPlainLabels = checkReifiedLabels.evaluate();

		List<OntoPortalConstraintsViolationException.Violation> violations = new ArrayList<>();

		if (nonPlainLabels) {
			OntoPortalConstraintsViolationException.Violation violation = new OntoPortalConstraintsViolationException.Violation();
			violation.message = "Some entities have labels expressed in SKOS-XL without a corresponding plain label";
			violation.fixes = new ArrayList<>();
			OntoPortalConstraintsViolationException.Repair nonPlainLabelsRepair = new OntoPortalConstraintsViolationException.Repair();
			nonPlainLabelsRepair.message = "Generate plain labels from reified ones";
			nonPlainLabelsRepair.transformerSpecification = new PluginSpecification(
					"it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification.XLabelDereificationRDFTransformer",
					null,
					null,
					JsonNodeFactory.instance.objectNode()
							.set("@type",
									JsonNodeFactory.instance.textNode("it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification.XLabelDereificationRDFTransformerConfiguration")

							)
			);
			violation.fixes.add(nonPlainLabelsRepair);

			violations.add(violation);
		}

		if (!violations.isEmpty()) {
			OntoPortalConstraintsViolationException e = new OntoPortalConstraintsViolationException();
			violations.forEach(e::addViolation);
			throw e;
		}
	}

	protected URI getAddress() throws URISyntaxException {
		String apiBaseURL = this.strategy.getAPIBaseURL();
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
