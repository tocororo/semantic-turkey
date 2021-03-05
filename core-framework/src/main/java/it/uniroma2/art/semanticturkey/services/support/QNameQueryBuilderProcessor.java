package it.uniroma2.art.semanticturkey.services.support;

import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link QueryBuilderProcessor} computing the QName of resources that are IRIs. It uses the prefix mappings
 * returned by the {@link OntologyManager} (see {@link OntologyManager#getNSPrefixMappings(boolean)}, which is
 * invoked with the argument <code>false</code>).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class QNameQueryBuilderProcessor implements QueryBuilderProcessor {

	@Override
	public GraphPattern getGraphPattern(STServiceContext context) {
		return GraphPatternBuilder.create()
				.projection(Collections.singletonList(ProjectionElementBuilder.variable("dummy"))).pattern("")
				.graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return false;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		Map<Value, Literal> rv = new HashMap<>();

		try {
			Project currentProject = context.getProject();
			Map<String, String> prefix2ns = currentProject.getOntologyManager().getNSPrefixMappings(false);

			Map<String, String> ns2prefix = new HashMap<>();
			prefix2ns.forEach((prefix, ns) -> ns2prefix.put(ns, prefix));

			ValueFactory vf = SimpleValueFactory.getInstance();

			resultTable.stream().map(bs -> bs.getValue("resource")).filter(IRI.class::isInstance)
					.map(IRI.class::cast).forEach(iri -> {
						String prefix = ns2prefix.get(iri.getNamespace());
						if (prefix != null) {
							rv.put(iri, vf.createLiteral(prefix + ":" + iri.getLocalName()));
						}
					});

		} catch (OntologyManagerException e) {
			// nothing to do
		}

		return rv;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

}
