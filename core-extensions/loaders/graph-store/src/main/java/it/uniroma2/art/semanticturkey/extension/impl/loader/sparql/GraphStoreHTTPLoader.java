package it.uniroma2.art.semanticturkey.extension.impl.loader.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.Null;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.RepositoryTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.RepositoryTargetingLoader;
import it.uniroma2.art.semanticturkey.extension.impl.loader.http.AbstractHTTPLoader;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Implementation of the {@link Loader} that uses the SPARQL 1.1 HTTP Graph Store API.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPLoader extends AbstractHTTPLoader<RepositoryTarget>
		implements RepositoryTargetingLoader {

	private GraphStoreHTTPLoaderConfiguration conf;

	public GraphStoreHTTPLoader(GraphStoreHTTPLoaderConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void load(RepositoryTarget source, @Null DataFormat acceptedFormat) throws IOException {
		if (acceptedFormat != null) {
			throw new IllegalArgumentException("A " + GraphStoreHTTPLoader.class.getSimpleName()
					+ " should not receive a non-null data format");
		}
		loadInternal(source, acceptedFormat);
	}

	protected URI getAddress() throws URISyntaxException {
		if (conf.graphStoreHTTPEndpoint != null) { // indirect identification
			UriComponentsBuilder uriBuilder = UriComponentsBuilder
					.fromHttpUrl(conf.graphStoreHTTPEndpoint.toExternalForm());
			if (conf.sourceGraph == null) {
				uriBuilder.queryParam("default");
			} else {
				uriBuilder.queryParam("graph", conf.sourceGraph);
			}
			return uriBuilder.build().toUri();
		} else { // direct identification
			if (conf.sourceGraph == null) {
				throw new IllegalStateException(
						"Either <graphStoreHTTPEndpoint> or <destinationGraph> should be defined");
			}

			return new URI(conf.sourceGraph.stringValue());
		}
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
	protected Map<String, String> getHttpRequestHeaders() {
		return ImmutableMap.of("Accept", RDFFormat.NTRIPLES.getDefaultMIMEType());
	}

	@Override
	protected void processResponse(HttpGet request, HttpClientContext httpClientContext,
			HttpResponse httpResponse, RepositoryTarget target) throws IOException {
		HttpEntity responseEntity = httpResponse.getEntity();
		if (responseEntity != null) {
			ContentType contentType = Optional.ofNullable(responseEntity.getContentType())
					.map(Header::getValue).map(ContentType::parse).orElse(ContentType.create(
							RDFFormat.NTRIPLES.getDefaultMIMEType(), RDFFormat.NTRIPLES.getCharset()));

			RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(contentType.getMimeType()).orElseThrow(
					() -> new IOException("Unsupported MIME type: " + contentType.getMimeType()));
			RDFParser rdfParser = Rio.createParser(rdfFormat);
			rdfParser.setRDFHandler(target.getTargetRepositoryConnection());
			try (InputStream is = responseEntity.getContent()) {
				rdfParser.parse(is, "");
			}
		}
	}
}
