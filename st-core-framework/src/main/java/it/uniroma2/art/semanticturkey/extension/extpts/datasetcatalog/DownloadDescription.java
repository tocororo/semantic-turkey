package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.Literal;

import com.google.common.base.MoreObjects;

public class DownloadDescription {
	private final URL accessURL;
	private final List<Literal> titles;
	private final List<Literal> descriptions;
	private final Optional<String> mimeType;

	public DownloadDescription(URL accessURL, List<Literal> titles, List<Literal> descriptions,
			@Nullable String mimeType) {
		this.accessURL = accessURL;
		this.titles = titles;
		this.descriptions = descriptions;
		this.mimeType = Optional.ofNullable(mimeType);
	}
	
	public URL getAccessURL() {
		return accessURL;
	}
	
	public List<Literal> getTitles() {
		return titles;
	}
	
	public List<Literal> getDescriptions() {
		return descriptions;
	}
	
	public Optional<String> getMimeType() {
		return mimeType;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("accessURL", accessURL).add("titles", titles)
				.add("descriptions", descriptions).add("mimeType", mimeType).toString();
	}

}
