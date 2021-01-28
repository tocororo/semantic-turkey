package it.uniroma2.art.semanticturkey.services.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;

import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * A {@link QueryBuilderProcessor} computing the nature of resources. It uses code in
 * {@link NatureRecognitionOrchestrator}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ResourceNatureQueryBuilderProcessor implements QueryBuilderProcessor {

	@Override
	public GraphPattern getGraphPattern(STServiceContext context) {
		return GraphPatternBuilder.create()
				.projection(Collections
						.singletonList(ProjectionElementBuilder.groupConcat("tempNature", "nature", "|_|")))
				.prefix(RDF.NS).prefix(RDFS.NS).prefix(OWL.NS).prefix(SKOS.NS).prefix(SKOSXL.NS)
				.pattern(NatureRecognitionOrchestrator.getNatureSPARQLWherePart("resource")
						+ "\nBIND(concat(str(?rt), \",\", str(?go), \",\", str(?dep)) as ?tempNature)\n")
				.graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return false;
	}

	@Override
	public boolean requiresOptionalWrapper() {
		return false;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return resultTable.stream().filter(bs -> bs.hasBinding("nature")).collect(
				Collectors.toMap(bs -> bs.getValue("resource"), bs -> (Literal) bs.getValue("nature")));
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

}
