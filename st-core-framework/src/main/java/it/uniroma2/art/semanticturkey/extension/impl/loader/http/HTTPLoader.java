package it.uniroma2.art.semanticturkey.extension.impl.loader.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.springframework.web.util.UriComponentsBuilder;

import it.uniroma2.art.semanticturkey.extension.extpts.loader.FormattedResourceTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.StreamTargetingLoader;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;

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
	public void load(FormattedResourceTarget target) throws IOException {
		loadInternal(target);
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
	protected void processResponse(HttpResponse httpResponse, FormattedResourceTarget target) throws IOException {
		HttpEntity responseEntity = httpResponse.getEntity();
		if (responseEntity != null) {
			ContentType contentType = Optional.ofNullable(responseEntity.getContentType())
					.map(Header::getValue).map(ContentType::parse).orElse(null);
			String mimeType;
			Charset charset;
			
			if (contentType != null) {
				mimeType = contentType.getMimeType();
				charset = contentType.getCharset();
			} else {
				mimeType = null;
				charset = null;
			}
			
			File backingFile = File.createTempFile("loadRDF", null);
			target.setTargetFormattedResource(new ClosableFormattedResource(backingFile, null, mimeType, charset));
			try (InputStream is = responseEntity.getContent()) {
				FileUtils.copyInputStreamToFile(is, backingFile);
			}
		}
	}

}
