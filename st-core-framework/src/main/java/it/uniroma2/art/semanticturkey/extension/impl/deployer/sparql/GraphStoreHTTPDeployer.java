package it.uniroma2.art.semanticturkey.extension.impl.deployer.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.util.UriComponentsBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySourcedDeployer;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;

/**
 * Implementation of the {@link URIGenerator} extension point based on templates.
 * 
 */
public class GraphStoreHTTPDeployer implements RepositorySourcedDeployer {

	private GraphStoreHTTPDeployerConfiguration conf;

	public GraphStoreHTTPDeployer(GraphStoreHTTPDeployerConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void deploy(RepositorySource source) throws IOException {
		URI address;
		try {
			address = getAddress();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		HttpClientContext context;

		if (conf.username != null && conf.password != null) {
			BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(conf.username, conf.password));
			HttpHost targetHost = new HttpHost(address.getHost(),
					address.getPort() != -1 ? address.getPort() : 80, address.getScheme());

			AuthCache authCache = new BasicAuthCache();
			authCache.put(targetHost, new BasicScheme());

			context = HttpClientContext.create();
			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);
		} else {
			context = null;
		}

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpEntityEnclosingRequestBase request;
			if (conf.clearFirst) {
				request = new HttpPut(address);
			} else {
				request = new HttpPost(address);
			}

			request.setEntity(new AbstractHttpEntity() {

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
			});
			try (CloseableHttpResponse httpResponse = httpClient.execute(request, context)) {
				StatusLine statusLine = httpResponse.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}
			}
		}

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

}
