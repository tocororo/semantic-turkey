package it.uniroma2.art.semanticturkey.extension.impl.deployer.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.FormattedResourceSource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Source;

/**
 * Abstract base class of HTTP deployers.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractHTTPDeployer implements Deployer {

	public enum HttpVerbs {
		GET, POST, PUT, PATCH, DELETE, UPDATE
	}

	protected void deployInternal(Source source) throws IOException {
		URI address;
		try {
			address = getAddress();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		HttpClientContext context;

		@Nullable
		Pair<String, String> usernameAndPassword = getUsernameAndPassword();

		if (usernameAndPassword != null) {
			BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
					usernameAndPassword.getLeft(), usernameAndPassword.getRight()));
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
			HttpVerbs httpVerb = getHttpVerb();

			if (httpVerb == HttpVerbs.PUT) {
				request = new HttpPut(address);
			} else if (httpVerb == HttpVerbs.POST) {
				request = new HttpPost(address);
			} else {
				throw new IllegalStateException("Unsupported HTTP verb: " + httpVerb);
			}

			@Nullable
			Map<String, String> requestHeaders = getHttpRequestHeaders();
			if (requestHeaders != null) {
				for (Map.Entry<String, String> aHeader : requestHeaders.entrySet()) {
					request.addHeader(aHeader.getKey(), aHeader.getValue());
				}
			}

			request.setEntity(createHttpEntity(source));
			try (CloseableHttpResponse httpResponse = httpClient.execute(request, context)) {
				StatusLine statusLine = httpResponse.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}
			}
		}

	}

	protected abstract URI getAddress() throws URISyntaxException;

	protected abstract @Nullable Pair<String, String> getUsernameAndPassword();

	protected abstract HttpVerbs getHttpVerb();

	protected @Nullable Map<String, String> getHttpRequestHeaders() {
		return null;
	}

	protected HttpEntity createHttpEntity(Source source) {
		if (source instanceof RepositorySource) {
			return createHttpEntity((RepositorySource) source);
		} else if (source instanceof FormattedResourceSource) {
			return createHttpEntity((FormattedResourceSource) source);
		} else {
			throw new IllegalArgumentException("Unsupported source type: " + source.getClass());
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

	protected HttpEntity createHttpEntity(FormattedResourceSource source) {
		return new AbstractHttpEntity() {

			@Override
			public void writeTo(OutputStream arg0) throws IOException {
				source.getSourceFormattedResource().writeTo(arg0);
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
				return new BasicHeader("Content-Type", source.getSourceFormattedResource().getMIMEType());
			}
		};
	}

}
