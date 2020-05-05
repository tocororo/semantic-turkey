package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.IRI;

/**
 * Converts the NT serialization of an (escaped) IRI to an object implementing {@link IRI}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToRDF4JIRIConverter extends AbstractStringToRDF4JValueConverter<IRI>{
	
	public StringToRDF4JIRIConverter() {
		super(IRI.class);
	}

}
