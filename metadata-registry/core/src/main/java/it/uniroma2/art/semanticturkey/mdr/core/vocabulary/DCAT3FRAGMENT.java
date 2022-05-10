package it.uniroma2.art.semanticturkey.mdr.core.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Optional;

public abstract class DCAT3FRAGMENT {
	/** http://www.w3.org/ns/dcat# */
	public static final String NAMESPACE = "http://www.w3.org/ns/dcat#";

	/**
	 * Recommended prefix for the DCAT3FRAGMENT namespace: "dcat"
	 */
	public static final String PREFIX = "dcat";

	/**
	 * An immutable {@link Namespace} constant that represents the DCAT3FRAGMENT namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** http://www.w3.org/ns/dcat#hasVersion */
	public static final IRI HAS_VERSION;

	/** http://www.w3.org/ns/dcat#isVersionOf */
	public static final IRI IS_VERSION_OF;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		HAS_VERSION = vf.createIRI(NAMESPACE, "hasVersion");
		IS_VERSION_OF = vf.createIRI(NAMESPACE, "isVersionOf");
	}
}
