package it.uniroma2.art.semanticturkey.mdr.core.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Optional;

/**
 * Constants for the vocabulary of the metadata registry binding to Semantic Turkey
 *
 */
public abstract class STMETADATAREGISTRY {
	/** http://semanticturkey.uniroma2.it/ns/stmdr# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/stmdr#";

	/**
	 * Recommended prefix for the STMETADATAREGISTRY namespace: "stmdr"
	 */
	public static final String PREFIX = "stmdr";

	/**
	 * An immutable {@link Namespace} constant that represents the STMETADATAREGISTRY namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** http://semanticturkey.uniroma2.it/ns/stmdr#Project */
	public static final IRI PROJECT;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		PROJECT = vf.createIRI(NAMESPACE, "Project");
	}

}
