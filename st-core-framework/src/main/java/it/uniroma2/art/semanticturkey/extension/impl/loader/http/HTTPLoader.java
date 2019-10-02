package it.uniroma2.art.semanticturkey.extension.impl.loader.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.springframework.web.util.UriComponentsBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.loader.FormattedResourceTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.StreamTargetingLoader;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Implementation of the {@link Loader} extension point that uses the HTTP protocol. This implementation can
 * load data into a {@link FormattedResourceTarget}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPLoader extends AbstractHTTPLoader<FormattedResourceTarget> implements StreamTargetingLoader {

	private HTTPLoaderConfiguration conf;

	public HTTPLoader(HTTPLoaderConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void load(FormattedResourceTarget target, DataFormat acceptedFormat) throws IOException {
		loadInternal(target, acceptedFormat);
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
	protected Map<String, String> getHttpRequestHeaders() {
		if (conf.requestHeaders != null) {
			return conf.requestHeaders;
		} else {
			return null;
		}
	}

	@Override
	protected boolean isContentNegotiationEnabled() {
		return Boolean.TRUE.equals(conf.enableContentNegotiation);
	}

	@Override
	protected void processResponse(HttpGet httpRequest, HttpClientContext httpClientContext,
			HttpResponse httpResponse, FormattedResourceTarget target) throws IOException {
		HttpEntity responseEntity = httpResponse.getEntity();
		if (responseEntity != null) {
			URI loadedURI = httpRequest.getURI();
			@Nullable
			List<URI> redirectLocations = httpClientContext.getRedirectLocations();
			if (redirectLocations != null) {
				loadedURI = redirectLocations.get(redirectLocations.size() - 1);
			}

			@Nullable
			String originalFilename = FilenameUtils.getName(loadedURI.getPath());

			ContentType contentType = Optional.ofNullable(responseEntity.getContentType())
					.map(Header::getValue).map(ContentType::parse).orElse(null);
			String mimeType;
			Charset charset;

			if (contentType != null && Boolean.TRUE.equals(conf.reportContentType)) {
				mimeType = contentType.getMimeType();
				charset = contentType.getCharset();
			} else {
				mimeType = null;
				charset = null;
			}

			File backingFile = File.createTempFile("loadRDF", null);
			target.setTargetFormattedResource(
					new ClosableFormattedResource(backingFile, null, mimeType, charset, originalFilename));
			try (InputStream is = responseEntity.getContent()) {
				FileUtils.copyInputStreamToFile(is, backingFile);
			}
		}
	}

}
