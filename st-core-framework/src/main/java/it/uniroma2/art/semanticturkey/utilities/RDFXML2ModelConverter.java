package it.uniroma2.art.semanticturkey.utilities;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import com.fasterxml.jackson.databind.util.StdConverter;

public class RDFXML2ModelConverter extends StdConverter<String, Model> {

	@Override
	public Model convert(String value) {
		try {
			return Rio.parse(new StringReader(value), "http://semanticturkey.uniroma2.it/missing-base-uri", RDFFormat.RDFXML);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
