package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class OWL2Fragment {

	/** The OWL2 namespace: http://www.w3.org/2002/07/owl# */
	public static final String NAMESPACE = "http://www.w3.org/2002/07/owl#";

	/**
	 * Recommended prefix for the OWL2 namespace: "owl"
	 */
	public static final String PREFIX = "owl";

	/**
	 * An immutable {@link Namespace} constant that represents the OWL2 namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	public static final IRI ASYMMETRICPROPERTY;
	public static final IRI REFLEXIVEPROPERTY;
	public static final IRI IRREFLEXIVEPROPERTY;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		ASYMMETRICPROPERTY = vf.createIRI(NAMESPACE, "AsymmetricProperty");
		REFLEXIVEPROPERTY = vf.createIRI(NAMESPACE, "ReflexiveProperty");
		IRREFLEXIVEPROPERTY = vf.createIRI(NAMESPACE, "IrreflexiveProperty");
	}
}
