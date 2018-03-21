package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the vocabulary of the metadata registry
 *
 */
public abstract class METADATAREGISTRY {
	/** http://semanticturkey.uniroma2.it/ns/mdreg# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/mdreg#";

	/**
	 * Recommended prefix for the STCHANGELOG namespace: "mdreg"
	 */
	public static final String PREFIX = "mdreg";

	/**
	 * An immutable {@link Namespace} constant that represents the METADATAREGISTRY namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	public static final IRI DEREFERENCIATION_SYSTEM;
	public static final IRI STANDARD_DEREFERENCIATION;
	public static final IRI NO_DEREFERENCIATION;
	
	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		DEREFERENCIATION_SYSTEM = vf.createIRI(NAMESPACE, "dereferenciationSystem");
		STANDARD_DEREFERENCIATION = vf.createIRI(NAMESPACE, "standardDereferenciation");
		NO_DEREFERENCIATION = vf.createIRI(NAMESPACE, "noDereferenciation");
	}

}
