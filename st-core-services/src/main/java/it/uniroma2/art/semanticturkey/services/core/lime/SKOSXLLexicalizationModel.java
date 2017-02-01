package it.uniroma2.art.semanticturkey.services.core.lime;

import static java.util.stream.Collectors.toMap;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class SKOSXLLexicalizationModel implements LexicalizationModel {

	/* (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.services.core.lime.LexicalizationModel#computeStatistics(org.eclipse.rdf4j.repository.RepositoryConnection, org.eclipse.rdf4j.model.IRI[])
	 */
	@Override
	public Map<String, LanguageStatistics> computeStatistics(RepositoryConnection repConn,
			IRI[] lexicalizationSetGraphs) {
		TupleQuery query = repConn.prepareTupleQuery(
			// @formatter:off
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                                         \n"+
			"                                                                                             \n"+
			" SELECT ?lang (COUNT(DISTINCT ?reference) as ?references) (COUNT(?label) as ?lexicalizations)\n"+
			" WHERE {                                                                                     \n"+
			" 	?reference skosxl:prefLabel|skosxl:altLabel|skosxl:hiddenLabel [                          \n"+
			"                                                                    skosxl:literalForm ?label\n"+
		    "                                                                  ]                          \n"+	
			" 	BIND(LANG(?label) as ?lang)                                                               \n"+
			" }                                                                                           \n"+
			" GROUP BY ?lang                                                                              \n"+
			" HAVING BOUND(?lang)                                                                         \n"+
			" ORDER BY ?lang                                                                              \n"
			// @formatter:on
		);

		query.setIncludeInferred(false);

		SimpleDataset dataset = new SimpleDataset();
		for (IRI g : lexicalizationSetGraphs) {
			dataset.addDefaultGraph(g);
		}
		query.setDataset(dataset);

		return QueryResults.stream(query.evaluate())
				.collect(toMap(bs -> bs.getValue("lang").stringValue(), bs -> {
					LanguageStatistics stats = new LanguageStatistics();
					stats.setLexicalizations(Literals.getIntegerValue(bs.getValue("lexicalizations"), BigInteger.ZERO));
					stats.setReferences(Literals.getIntegerValue(bs.getValue("references"), BigInteger.ZERO));
					return stats;
				}));
	}

	/* (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.services.core.lime.LexicalizationModel#getName()
	 */
	@Override
	public String getName() {
		return "skos";
	}

	/* (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.services.core.lime.LexicalizationModel#getIRI()
	 */
	@Override
	public IRI getIRI() {
		return SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2008/05/skos-xl");
	}

}