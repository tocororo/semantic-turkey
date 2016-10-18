package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter2;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SKOS constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class SKOS extends STServiceAdapter2 {

	private static Logger logger = LoggerFactory.getLogger(SKOS.class);

	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getNarrowerConcepts(@LocallyDefined Resource concept,
			@Optional @LocallyDefined Resource conceptScheme,
			@Optional(defaultValue = "") String[] languages) {
		try (RepositoryConnection conn = getManagedConnection()) {
			String queryString = ""
			// @formatter:off
				+ "        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                            \n"
				+ "        PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                            \n"
				+ "        SELECT DISTINCT ?narrower ?label where {                                       \n"
				+ "           ?narrower skos:broader|^skos:narrower ?concept .                            \n";
			// @formatter:on

			if (conceptScheme != null) {
				queryString += ""
					// @formatter:off
						+ "   ?narrower skos:inScheme ?conceptScheme .                                   \n";
					// @formatter:on
			}

			if (languages != null) {
				queryString += ""
					// @formatter:off
						+ "   OPTIONAL {                                                                  \n"
						+ "      ?narrower skosxl:prefLabel [                                             \n"
						+ "          skosxl:literalForm ?label                                            \n"
						+ "      ] .                                                                      \n"
						+ "   }                                                                           \n";
					// @formatter:on
			}

			queryString += "}\n";

			logger.debug("query = " + queryString);

			TupleQuery query = conn.prepareTupleQuery(queryString);
			query.setBinding("concept", concept);

			if (conceptScheme != null) {
				query.setBinding("conceptScheme", conceptScheme);
			}

			query.setIncludeInferred(false);

			// SimpleDataset simpleDataset = new SimpleDataset();
			// simpleDataset.addDefaultGraph(null);
			// query.setDataset(simpleDataset);

			return QueryResults.stream(query.evaluate()).map(bindingSet -> {
				return new AnnotatedValue<Resource>((Resource) bindingSet.getValue("narrower"));
			}).collect(Collectors.toList());
		}
	}

	@STServiceOperation
	@Read
	public void failingReadServiceContainingUpdate() {
		try (RepositoryConnection conn = getManagedConnection()) {
			Update update = conn
					.prepareUpdate("insert data {<http://test.it/> <http://test.it/> <http://test.it/> . }");
			update.execute();
		}
	}

	@STServiceOperation
	@Write
	public void createConcept(@Optional @LanguageTaggedString Literal newConcept,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefined IRI conceptScheme)
					throws URIGenerationException {
		Model quadAdditions = new LinkedHashModel();
		Model quadRemovals = new LinkedHashModel();

		IRI newConceptIRI = generateConceptURI(newConcept, conceptScheme);

		quadAdditions.add(newConceptIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT);

		if (newConcept != null) {
			quadAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, newConcept);
		}

		quadAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);

		if (broaderConcept != null) {
			quadAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else {
			quadAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF,
					conceptScheme);
		}

		applyPatch(quadAdditions, quadRemovals);
	}

	/**
	 * Generates a new URI for a SKOS concept, optionally given its accompanying preferred label and concept
	 * scheme. The actual generation of the URI is delegated to {@link #generateURI(String, Map)}, which in
	 * turn invokes the current binding for the extension point {@link URIGenerator}. In the end, the <i>URI
	 * generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>concept</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>label</code> and <code>scheme</code> (each, if
	 * not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param label
	 *            the preferred label accompanying the concept (can be <code>null</code>)
	 * @param scheme
	 *            the scheme to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptURI(Literal label, IRI scheme) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (scheme != null) {
			args.put(URIGenerator.Parameters.scheme, scheme);
		}

		return generateURI(URIGenerator.Roles.concept, args);
	}

	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
		Method m = SKOS.class.getMethod("getNarrowerConcepts", Resource.class, Resource.class,
				String[].class);
		System.out.println(m.getGenericReturnType());
	}
}
