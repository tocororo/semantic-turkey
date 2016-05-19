package it.uniroma2.art.semanticturkey.services.core;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryResults;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.util.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter2;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

/**
 * This class provides services for manipulating SKOS constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@GenerateSTServiceController
@Validated
@Component
public class SKOS extends STServiceAdapter2 {

	private static Logger logger = LoggerFactory.getLogger(SKOS.class);

	@GenerateSTServiceController
	@Read
	public Response getNarrowerConcepts(@LocallyDefined Resource resource) {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("getNarrowerConcept",
				RepliesStatus.ok);

		try (RepositoryConnection conn = getRepositoryConnection()) {
			TupleQuery query = conn.prepareTupleQuery("prefix skos: <http://www.w3.org/2004/02/skos/core#> "
					+ "select distinct ?narrower where {?narrower skos:broader|^skos:narrower ?broader}");
			query.setBinding("broader", resource);

			query.setIncludeInferred(false);

			// SimpleDataset simpleDataset = new SimpleDataset();
			// simpleDataset.addDefaultGraph(null);
			// query.setDataset(simpleDataset);
			
			System.out.println("- Begin Results -");
			QueryResults.stream(query.evaluate()).forEach(System.out::println);
			System.out.println("- End Results - ");
		}

		return response;
	}
	
	@GenerateSTServiceController
	@Read
	public void failingReadServiceContainingUpdate() {
		try (RepositoryConnection conn = getRepositoryConnection()) {
			Update update = conn.prepareUpdate("insert data {<http://test.it/> <http://test.it/> <http://test.it/> . }");
			update.execute();
		}
	}


	@GenerateSTServiceController
	@Write
	public void createConcept(@Optional @LanguageTaggedString Literal newConcept,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefined IRI conceptScheme)
					throws URIGenerationException {
		Model quadAdditions = new LinkedHashModel();
		Model quadRemovals = new LinkedHashModel();

		IRI newConceptIRI = generateConceptURI(newConcept, conceptScheme);

		quadAdditions.add(newConceptIRI, RDF.TYPE, org.openrdf.model.vocabulary.SKOS.CONCEPT);

		if (newConcept != null) {
			quadAdditions.add(newConceptIRI, org.openrdf.model.vocabulary.SKOS.PREF_LABEL, newConcept);
		}

		quadAdditions.add(newConceptIRI, org.openrdf.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);

		if (broaderConcept != null) {
			quadAdditions.add(newConceptIRI, org.openrdf.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else {
			quadAdditions.add(newConceptIRI, org.openrdf.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
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
}
