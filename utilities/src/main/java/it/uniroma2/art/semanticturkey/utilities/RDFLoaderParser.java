package it.uniroma2.art.semanticturkey.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

/**
 * An {@link RDFParser} implemented using an {@link RDFLoader} (which also includes the support for dealing
 * with compressed archives).
 * 
 * @author Manuel
 *
 */
public class RDFLoaderParser extends AbstractRDFParser {

	private RDFFormat format;

	public RDFLoaderParser(RDFFormat format) {
		this.format = format;
	}

	@Override
	public void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), SimpleValueFactory.getInstance());
		rdfLoader.load(in, baseURI, format, rdfHandler);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return format;
	}

}
