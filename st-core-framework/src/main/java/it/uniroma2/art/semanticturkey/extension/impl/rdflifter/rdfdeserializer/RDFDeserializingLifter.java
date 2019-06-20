package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer;

import java.io.IOException;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;

import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LifterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.RDFLifter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * An {@link RDFLifter} that deserializes RDF data according to the provided serialization format
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFDeserializingLifter implements RDFLifter {

	@Override
	public void lift(ClosableFormattedResource sourceFormattedResource, String format,
			RDFHandler targetRDFHandler, LifterContext lifterContext) throws LiftingException, IOException {

		RDFFormat rdfFormat = RDF4JUtilities.getRDFFormat(format);

		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), SimpleValueFactory.getInstance());
		rdfLoader.load(sourceFormattedResource.getInputStream(), "", rdfFormat, targetRDFHandler);
	}

}
