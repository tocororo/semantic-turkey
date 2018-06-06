package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import java.io.IOException;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * A {@link Loader} which can load data into a {@link ClosableFormattedResource}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface StreamTargetingLoader extends Loader {

	void load(FormattedResourceTarget target, @Nullable DataFormat acceptedFormat) throws IOException;
}
