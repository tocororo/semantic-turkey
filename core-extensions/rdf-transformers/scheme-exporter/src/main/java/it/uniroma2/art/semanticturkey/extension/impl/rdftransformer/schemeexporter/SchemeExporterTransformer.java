package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.schemeexporter;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.HashMap;
import java.util.Map;


/**
 * An {@link RDFTransformer} that removes some property values. If no <code>value</code> is provided, then it
 * will remove all triples with <code>resource</code> and <code>property</code> as subject an predicate,
 * respectively.
 *
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class SchemeExporterTransformer implements RDFTransformer {

    private IRI scheme;

    public  SchemeExporterTransformer(SchemeExporterTransformerConfiguration config) {
        this.scheme = config.scheme;
    }

    @Override
    public void transform(RepositoryConnection sourceRepositoryConnection,
                          RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
        IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);

        if (expandedGraphs.length == 0)
            return;

        Map<String, Resource> variableToResourceMap = new HashMap<>();

        //@formatter:off
        String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> " +
                "\nDELETE { " +
                "\n    ?concept ?p1 ?o1 . " +
                "\n    ?res ?inProp ?concept . "+
                "\n    ?label ?pLabel ?oLabel . " +
                "\n    ?scheme ?pScheme ?oScheme . " +
                "\n} " +
                "\nWHERE{ " +
                "\n    ?concept a skos:Concept . " +
                "\n    FILTER NOT EXISTS{ " +
                "\n        ?concept skos:inScheme|skos:topConceptOf|^skos:hasTopConcept ?targetScheme . " +
                "\n    } " +
                "\n    ?concept ?p1 ?o1 . " +
                "\n    OPTIONAL { ?res ?inProp ?concept . }" +
                "\n    OPTIONAL { " +
                "\n        ?concept ?p2 ?label . " + // not in the DELETE since the generic property dealt with it
                "\n        ?label a skosxl:Label . " + // not in the DELETE since the generic property dealt with it
                "\n        ?label ?pLabel ?oLabel . " +
                "\n    } " +
                "\n}";
        //@formatter:on

        // delete all concepts not belonging to this scheme
        variableToResourceMap.clear();
        variableToResourceMap.put("targetScheme", scheme);
        executeQuery(query, workingRepositoryConnection, variableToResourceMap);


        query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> " +
                "\nDELETE { " +
                "\n    ?scheme ?pScheme ?oScheme . " +
                "\n    ?res ?inProp ?scheme . " +
                "\n    ?labelScheme ?pLabelScheme ?oLabelScheme . " +
                "\n} " +
                "\nWHERE {" +
                "\n    ?scheme a skos:ConceptScheme . " +
                "\n    FILTER (?scheme != ?targetScheme)  " +
                "\n    ?scheme ?pScheme ?oScheme . " +
                "\n    OPTIONAL { ?res ?inProp ?scheme . }" +
                "\n    OPTIONAL{ " +
                "\n        ?scheme ?p3 ?labelScheme . " + // not in the DELETE since the generic property dealt with it
                "\n        ?labelScheme a skosxl:Label . " + // not in the DELETE since the generic property dealt with it
                "\n        ?labelScheme ?pLabelScheme ?oLabelScheme . " +
                "\n    } "+
                "\n}";

        // delete all infos about the other schemes
        variableToResourceMap.clear();
        variableToResourceMap.put("targetScheme", scheme);
        executeQuery(query, workingRepositoryConnection, variableToResourceMap);

    }

    private void executeQuery(String query, RepositoryConnection conn, Map<String, Resource> variableToResourceMap) {
        Update update = conn.prepareUpdate(query);
        for (String variable : variableToResourceMap.keySet()) {
            update.setBinding(variable, variableToResourceMap.get(variable));
        }
        update.execute();
    }
}
