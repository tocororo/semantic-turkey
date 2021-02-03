package it.uniroma2.art.semanticturkey.data.nature;

import java.util.Iterator;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

public class NatureRecognitionOrchestrator {

	public static String getNatureSPARQLSelectPart() {
		String sparqlPartText = "(group_concat(DISTINCT concat(str($rt), \",\", str($go), \",\", " +
				"str($dep));separator=\"|_|\") as ?attr_nature) \n";
		return sparqlPartText;
	}

	public static String getNatureSPARQLWherePart(String varName) {
		String sparqlPartText;

		if (!varName.startsWith("?")) {
			varName = "?" + varName;
		}
		// @formatter:off
		sparqlPartText = " \nOPTIONAL { \n" +
				"  values(?st) {(rdfs:Datatype)} \n" +
				"  graph ?go { \n" +
				" " + varName + " a ?st . \n" +
				"  } \n" +
				" } \n" +

				" OPTIONAL { \n" +
				"  values(?st) {" +
				"		(<http://www.w3.org/ns/lemon/lime#Lexicon>)(<http://www.w3.org/ns/lemon/ontolex#LexicalEntry>)" +
				"     (<http://www.w3.org/ns/lemon/ontolex#Form>)(skos:Concept)(rdfs:Class)(skosxl:Label)(skos:ConceptScheme)(skos:OrderedCollection)(owl:Ontology)" +
				"		(owl:ObjectProperty)(owl:DatatypeProperty)(owl:AnnotationProperty)(owl:OntologyProperty)" + "} \n" +
				"  ?t rdfs:subClassOf* ?st \n" +
				"  graph ?go { \n" +
				" " + varName + " a ?t . \n" +
				"  } \n" +
				" } \n" +

				" OPTIONAL { \n" +
				"  values(?st) {(skos:Collection)(rdf:Property)} \n" +
				"  graph ?go { \n" +
				" " + varName + " a ?st . \n" +
				"  } \n" +
				" } \n" +
				// in case of instance (that has no a type among the above) ?st is unboud, so don't constrain
				// the values of ?st
				" OPTIONAL { " + "  graph ?go { \n" +
				" " + varName + " a ?st . \n" +
				"  } \n" +
				" }" +
				// convert type to role ?st (super type) to ?rt (role type)
				complexIfPartForNature("?st", "?rt") +

				" OPTIONAL { \n" +
				"	   BIND( \n" +
				"	     IF(EXISTS {" + varName + " owl:deprecated true}, \"true\", \n" +
				"	      IF(EXISTS {" + varName + " a owl:DeprecatedClass}, \"true\", \n" +
				"	       IF(EXISTS {" + varName + " a owl:DeprecatedProperty}, \"true\", \n" +
				"	        \"false\"))) \n" +
				"	   as ?dep ) \n" + "	 } \n";
		// @formatter:on
		return sparqlPartText;
	}

	public static String complexIfPartForNature(String var1, String var2) {
		String queryPart;

		// @formatter:off
		//DO NOT FORMAT THE FOLLOWING CODE, OTHERWISE IT IS DIFFICULT TO MAINTAIN IT
		queryPart = " BIND(" +
				"IF(!BOUND(" + var1 + "), \"" + RDFResourceRole.individual + "\"," +
				"IF(" + var1 + " = <http://www.w3.org/ns/lemon/lime#Lexicon>, \"" + RDFResourceRole.limeLexicon + "\"," +
				"IF(" + var1 + " = <http://www.w3.org/ns/lemon/ontolex#LexicalEntry>, \"" + RDFResourceRole.ontolexLexicalEntry + "\"," +
				"IF(" + var1 + " = <http://www.w3.org/ns/lemon/ontolex#Form>, \"" + RDFResourceRole.ontolexForm + "\"," +
				"IF(" + var1 + " = <http://www.w3.org/ns/lemon/ontolex#LexicalSense>, \"" + RDFResourceRole.ontolexLexicalSense + "\"," +
				"IF(" + var1 + " = skos:Concept, \"" + RDFResourceRole.concept + "\"," +
				"IF(" + var1 + " = skos:ConceptScheme, \"" + RDFResourceRole.conceptScheme + "\"," +
				"IF(" + var1 + " = skos:Collection, \"" + RDFResourceRole.skosCollection + "\"," +
				"IF(" + var1 + " = skos:OrderedCollection, \"" + RDFResourceRole.skosOrderedCollection + "\"," +
				"IF(" + var1 + " = skosxl:Label, \"" + RDFResourceRole.xLabel + "\"," +
				"IF(" + var1 + " = rdf:Property, \"" + RDFResourceRole.property + "\"," +
				"IF(" + var1 + " = owl:ObjectProperty, \"" + RDFResourceRole.objectProperty + "\"," +
				"IF(" + var1 + " = owl:DatatypeProperty, \"" + RDFResourceRole.datatypeProperty + "\"," +
				"IF(" + var1 + " = owl:AnnotationProperty, \"" + RDFResourceRole.annotationProperty + "\"," +
				"IF(" + var1 + " = owl:OntologyProperty, \"" + RDFResourceRole.ontologyProperty + "\"," +
				"IF(" + var1 + " = owl:Ontology, \"" + RDFResourceRole.ontology + "\"," +
				"IF(" + var1 + " = rdfs:Class, \"" + RDFResourceRole.cls + "\"," +
				"IF(" + var1 + " = owl:Class, \"" + RDFResourceRole.cls + "\"," +
				"IF(" + var1 + " = rdfs:Datatype, \"" + RDFResourceRole.dataRange + "\"," +
				"\"" + RDFResourceRole.individual + "\"" +
				"))))))))))))))))))) " +// number of IF
				"as " + var2 + ") \n"; // closing the BIND
		// @formatter:on

		return queryPart;
	}

	public static String computeNature(Resource resource, RepositoryConnection repoConn) {

		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>		\n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n" +
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n" +
				"SELECT ?resource " + getNatureSPARQLSelectPart() + " {				\n" +
				"		?resource ?p ?o													\n" +
				getNatureSPARQLWherePart("?resource") +
				"}																	\n" +
				"GROUP BY ?resource";

		TupleQuery tq = repoConn.prepareTupleQuery(query);
		tq.setBinding("resource", resource);
		tq.setIncludeInferred(false);
		try (TupleQueryResult result = tq.evaluate()) {
			if (result.hasNext()) {
				return result.next().getValue("attr_nature").stringValue();
			} else {
				return null;
			}
		}
	}

	public static TripleScopes computeTripleScopeFromGraphs(Iterable<? extends Resource> graphs,
			Resource workingGraph) {
		Iterator<? extends Resource> it = graphs.iterator();

		TripleScopes scope = TripleScopes.inferred;

		boolean atLeastOne = false;
		boolean delStagedInWg = false;

		while (it.hasNext()) {
			atLeastOne = true;
			Resource g = it.next();

			if (Objects.equals(g, workingGraph)) {
				if (scope != TripleScopes.del_staged || delStagedInWg == false) {
					scope = TripleScopes.local;
				}
			} else if (VALIDATION.isAddGraph(g)) {
				if (scope != TripleScopes.local && scope != TripleScopes.del_staged) {
					scope = TripleScopes.staged;
				}
			} else if (VALIDATION.isRemoveGraph(g)) {
				boolean isRemoveWorkingGraph = VALIDATION.isRemoveGraphFor(g, workingGraph);
				if (scope != TripleScopes.local || isRemoveWorkingGraph) {
					scope = TripleScopes.del_staged;
					if (isRemoveWorkingGraph) {
						delStagedInWg = true;
					}
				}
			} else if (!Objects.equals(g, INFERENCE_GRAPH)) {
				if (scope == TripleScopes.inferred) {
					scope = TripleScopes.imported;
				}
			}

		}

		if (!atLeastOne) {
			throw new IllegalArgumentException(
					"Unable to compute the scope of a triple from an empty collection of graphs");
		}

		return scope;
	}

	public static final IRI INFERENCE_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://semanticturkey/inference-graph");

}
