package it.uniroma2.art.semanticturkey.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;

/**
 * Utilities for working with RDF4J.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class RDF4JUtilities {

	/**
	 * Returns an {@link RDFFormat} whose name matches the provided string.
	 * 
	 * @param format
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static RDFFormat getRDFFormat(String format) throws IllegalArgumentException {
		Objects.requireNonNull(format);
		
		for (Field field : RDFFormat.class.getDeclaredFields()) {
			if ((field.getModifiers() & Modifier.STATIC) == 0)
				continue; // skip non static fields
			if ((field.getModifiers() & Modifier.PUBLIC) == 0)
				continue; // skip non public fields

			if (!RDFFormat.class.isAssignableFrom(field.getType()))
				continue; // skip non RDFormat fields

			RDFFormat rdfFormat;
			try {
				rdfFormat = (RDFFormat) field.get(null);
			} catch (IllegalAccessException e) {
				continue;
			}

			if (rdfFormat.getName().equals(format)) {
				return rdfFormat;
			}
		}

		throw new IllegalArgumentException("Unsupported format: " + format);
	}

	/**
	 * Returns {@link RDFFormat}s for which an {@link RDFWriter} is registered.
	 * 
	 * @return
	 */
	public static Set<RDFFormat> getOutputFormats() {
		return RDFWriterRegistry.getInstance().getKeys();
	}

}
