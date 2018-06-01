package it.uniroma2.art.semanticturkey.extension.extpts.rdflifter;

import java.io.IOException;

import org.eclipse.rdf4j.rio.RDFHandler;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.StreamTargetingLoader;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;

/**
 * Extension point for RDF lifters. These components can be placed inside an export pipeline, just after a
 * {@link StreamTargetingLoader} for the RDF conversion of data in other formats.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface RDFLifter extends Extension {
	public void lift(ClosableFormattedResource sourceFormattedResource, String format,
			RDFHandler targetRDFHandle) throws LiftingException, IOException;
}
