package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Constants for the Validation vocabulary used by the {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class VALIDATION {
	/** http://semanticturkey.uniroma2.it/ns/validation# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/validation#";

	/**
	 * Recommended prefix for the VALIDATION namespace: "val"
	 */
	public static final String PREFIX = "val";

	/**
	 * An immutable {@link Namespace} constant that represents the VALIDATION namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** val:staging-add-graph/ */
	public static final IRI STAGING_ADD_GRAPH;

	/** val:staging-remove-graph/ */
	public static final IRI STAGING_REMOVE_GRAPH;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		STAGING_ADD_GRAPH = vf.createIRI(NAMESPACE, "staging-add-graph/");
		STAGING_REMOVE_GRAPH = vf.createIRI(NAMESPACE, "staging-remove-graph/");
	}

	public static Resource stagingAddGraph(Resource context) {
		IRI contextIRI = (IRI) context;

		return SimpleValueFactory.getInstance().createIRI(STAGING_ADD_GRAPH.stringValue() + contextIRI);
	}

	public static Resource stagingRemoveGraph(Resource context) {
		IRI contextIRI = (IRI) context;

		return SimpleValueFactory.getInstance().createIRI(STAGING_REMOVE_GRAPH.stringValue() + contextIRI);
	}

	public static boolean isAddGraph(Resource ctx) {
		return ctx instanceof IRI && ((IRI) ctx).stringValue().startsWith(STAGING_ADD_GRAPH.stringValue());
	}

	public static boolean isRemoveGraph(Resource ctx) {
		return ctx instanceof IRI && ((IRI) ctx).stringValue().startsWith(STAGING_REMOVE_GRAPH.stringValue());
	}

	public static IRI unmangleAddGraph(IRI ctx) {
		return SimpleValueFactory.getInstance()
				.createIRI(ctx.stringValue().substring(STAGING_ADD_GRAPH.stringValue().length()));
	}

	public static IRI unmangleRemoveGraph(IRI ctx) {
		return SimpleValueFactory.getInstance()
				.createIRI(ctx.stringValue().substring(STAGING_REMOVE_GRAPH.stringValue().length()));
	}

}
