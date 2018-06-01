package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;

/**
 * A {@link Target} for a {@link Loader} that wraps a {@link ClosableFormattedResource}. This kind of target
 * is used, for example, when data are not loaded from a triple store (broadly interpreted as any source
 * natively producing RDF data).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class FormattedResourceTarget extends Target {
	private @Nullable ClosableFormattedResource targetClosableFormattedResource;

	public FormattedResourceTarget() {
		this.targetClosableFormattedResource = null;
	}
	
	public FormattedResourceTarget(ClosableFormattedResource targetFormattedResource) {
		this.targetClosableFormattedResource = targetFormattedResource;
	}

	public void setTargetFormattedResource(
			ClosableFormattedResource targetClosableFormattedResource) {
		this.targetClosableFormattedResource = targetClosableFormattedResource;
	}
	
	public ClosableFormattedResource getTargetFormattedResource() {
		return targetClosableFormattedResource;
	}
}
