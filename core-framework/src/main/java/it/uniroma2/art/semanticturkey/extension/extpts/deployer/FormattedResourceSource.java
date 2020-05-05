package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;

/**
 * A {@link Source} for a {@link Deployer} that wraps a {@link ClosableFormattedResource}. This kind of
 * source is used, for example, when data are deployed as is without apply any transformation to a non-RDF
 * format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class FormattedResourceSource extends Source {
	private final ClosableFormattedResource sourceFormattedResource;

	public FormattedResourceSource(ClosableFormattedResource sourceFormattedResource) {
		this.sourceFormattedResource = sourceFormattedResource;
	}

	public ClosableFormattedResource getSourceFormattedResource() {
		return sourceFormattedResource;
	}
}
