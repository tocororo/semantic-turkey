package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;

/**
 * Constants for the support repository.
 * 
 */
public abstract class SUPPORT {
	/** http://semanticturkey.uniroma2.it/ns/support/ */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/support/";

	/**
	 * Recommended prefix for the SUPPORT namespace: "support"
	 */
	public static final String PREFIX = "support";

	/**
	 * An immutable {@link Namespace} constant that represents the SUPPORT namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** support:history **/
	public static final IRI HISTORY;

	/** support:validation **/
	public static final IRI VALIDATION;

	/** support:blacklist **/
	public static final IRI BLACKLIST;

	public static final String METADATA_NS;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		HISTORY = vf.createIRI(NAMESPACE, "history");
		VALIDATION = vf.createIRI(NAMESPACE, "validation");
		BLACKLIST = vf.createIRI(NAMESPACE, "blacklist");

		METADATA_NS = ModelUtilities.extendNamespace(NAMESPACE, "metadata/");
	}

	/**
	 * Computes the namespace used for the definition of metadata in the support repository based on the base
	 * URI defined in the core repository.
	 * 
	 * @param baseURI
	 * @return
	 */
	public static String computeMetadataNS(String baseURI) {
		return ModelUtilities.extendNamespace(baseURI, "metadata");
	}
}
