package it.uniroma2.art.semanticturkey.mdr.core.vocabulary;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the vocabulary of the metadata registry
 *
 */
public abstract class METADATAREGISTRY {
	/** http://semanticturkey.uniroma2.it/ns/mdr# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/mdr#";

	/**
	 * Recommended prefix for the STCHANGELOG namespace: "mdr"
	 */
	public static final String PREFIX = "mdr";

	/**
	 * An immutable {@link Namespace} constant that represents the METADATAREGISTRY namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** http://semanticturkey.uniroma2.it/ns/mdr#dereferenciationSystem */
	public static final IRI DEREFERENCIATION_SYSTEM;

	/** http://semanticturkey.uniroma2.it/ns/mdr#standardDereferenciation */
	public static final IRI STANDARD_DEREFERENCIATION;

	/** http://semanticturkey.uniroma2.it/ns/mdr#noDereferenciation */
	public static final IRI NO_DEREFERENCIATION;

	/** http://semanticturkey.uniroma2.it/ns/mdr#sparqlEndpointLimitation */
	public static final IRI SPARQL_ENDPOINT_LIMITATION;

	/** http://semanticturkey.uniroma2.it/ns/mdr#noAggregation */
	public static final IRI NO_AGGREGATION;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		DEREFERENCIATION_SYSTEM = vf.createIRI(NAMESPACE, "dereferenciationSystem");
		STANDARD_DEREFERENCIATION = vf.createIRI(NAMESPACE, "standardDereferenciation");
		NO_DEREFERENCIATION = vf.createIRI(NAMESPACE, "noDereferenciation");
		SPARQL_ENDPOINT_LIMITATION = vf.createIRI(NAMESPACE, "sparqlEndpointLimitation");
		NO_AGGREGATION = vf.createIRI(NAMESPACE, "noAggregation");
	}

	public static Optional<IRI> getDereferenciationSystem(Boolean b) {
		if (b == null) {
			return Optional.empty();
		} else if (Boolean.TRUE.equals(b)) {
			return Optional.of(STANDARD_DEREFERENCIATION);
		} else {
			return Optional.of(NO_DEREFERENCIATION);
		}
	}

}
