package it.uniroma2.art.semanticturkey.extension.impl.loader.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.Target;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Abstract base class of HTTP loaders.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractHTTPLoader<T extends Target> implements Loader {

	public void loadInternal(T target, DataFormat acceptedFormat) throws IOException {
		URI address;
		try {
			address = getAddress();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		HttpClientContext context = HttpClientContext.create();

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

			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);
		}
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
			HttpGet request = new HttpGet(address);

			Map<String, String> requestHeaders = new LinkedHashMap<>();

			if (acceptedFormat != null && isContentNegotiationEnabled()) {
				requestHeaders.put("Accept", acceptedFormat.getDefaultMimeType());
			}

			// User headers overrides the default ones

			@Nullable
			Map<String, String> userSuppliedHeaders = getHttpRequestHeaders();
			if (userSuppliedHeaders != null) {
				requestHeaders.putAll(userSuppliedHeaders);
			}

			for (Map.Entry<String, String> aHeader : requestHeaders.entrySet()) {
				request.addHeader(aHeader.getKey(), aHeader.getValue());
			}

			processRequest(request);

			try (CloseableHttpResponse httpResponse = httpClient.execute(request, context)) {
				StatusLine statusLine = httpResponse.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				processResponse(request, context, httpResponse, target);
			}
		}

	}

	protected boolean isContentNegotiationEnabled() {
		return false;
	}

	protected abstract URI getAddress() throws URISyntaxException;

	protected abstract @Nullable Pair<String, String> getUsernameAndPassword();

	protected @Nullable Map<String, String> getHttpRequestHeaders() {
		return null;
	}

	protected void processRequest(HttpGet request) {

	}

	protected abstract void processResponse(HttpGet httpRequest, HttpClientContext context,
			HttpResponse httpResponse, T target) throws IOException;

}
