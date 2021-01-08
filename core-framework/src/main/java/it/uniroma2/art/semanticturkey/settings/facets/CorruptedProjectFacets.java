package it.uniroma2.art.semanticturkey.settings.facets;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Helper for corrupted project facets.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CorruptedProjectFacets extends ProjectFacets {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.facets.CorruptedProjectFacets";

		public static final String shortName = keyBase + ".shortName";
		public static final String customFacets$description = keyBase + ".customFacets.description";
		public static final String customFacets$displayName = keyBase + ".customFacets.displayName";
	}

	public CorruptedProjectFacets(Throwable e) {
		StringWriter out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		htmlWarning = StringEscapeUtils.escapeHtml4(out.toString());
	}

	@Override
	public String getHTMLWarning() {
		return htmlWarning;
	}

	@JsonIgnore
	public String htmlWarning;
}
