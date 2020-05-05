package it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.util.UriComponentsBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySourcedDeployer;
import it.uniroma2.art.semanticturkey.extension.impl.deployer.http.AbstractHTTPDeployer;

/**
 * Implementation of the {@link Deployer} that uses the SPARQL 1.1 HTTP Graph Store API.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPDeployer extends AbstractHTTPDeployer<RepositorySource>
		implements RepositorySourcedDeployer {

	private GraphStoreHTTPDeployerConfiguration conf;

	public GraphStoreHTTPDeployer(GraphStoreHTTPDeployerConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void deploy(RepositorySource source) throws IOException {
		deployInternal(source);
	}

	protected URI getAddress() throws URISyntaxException {
		if (conf.graphStoreHTTPEndpoint != null) { // indirect identification
			UriComponentsBuilder uriBuilder = UriComponentsBuilder
					.fromHttpUrl(conf.graphStoreHTTPEndpoint.toExternalForm());
			if (conf.destinationGraph == null) {
				uriBuilder.queryParam("default");
			} else {
				uriBuilder.queryParam("graph", conf.destinationGraph);
			}
			return uriBuilder.build().toUri();
		} else { // direct identification
			if (conf.destinationGraph == null) {
				throw new IllegalStateException(
						"Either <graphStoreHTTPEndpoint> or <destinationGraph> should be defined");
			}

			return new URI(conf.destinationGraph.stringValue());
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
	protected HttpVerbs getHttpVerb() {
		if (conf.clearFirst) {
			return HttpVerbs.PUT;
		} else {
			return HttpVerbs.POST;
		}
	}

	protected HttpEntity createHttpEntity(RepositorySource source) {
		return new AbstractHttpEntity() {

			@Override
			public void writeTo(OutputStream arg0) throws IOException {
				RepositoryConnection sourceConn = source.getSourceRepositoryConnection();
				sourceConn.export(Rio.createWriter(RDFFormat.NTRIPLES, arg0), source.getGraphs());
			}

			@Override
			public boolean isStreaming() {
				return false;
			}

			@Override
			public boolean isRepeatable() {
				return true;
			}

			@Override
			public long getContentLength() {
				return -1;
			}

			@Override
			public InputStream getContent() throws IOException, UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Header getContentType() {
				return new BasicHeader("Content-Type", RDFFormat.NTRIPLES.getDefaultMIMEType());
			}
		};
	}

}
