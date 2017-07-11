package it.uniroma2.art.semanticturkey.data.nature;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

public class NatureRecognitionOrchestrator {
	
	public static String getNatureSPARQLSelectPart(){
		String sparqlPartText = "(group_concat(DISTINCT concat(str($rt), \",\", str($go), \",\", "
				+ "str($dep));separator=\"|_|\") as ?attr_nature) \n";
		return sparqlPartText;
	}
	
	public static String getNatureSPARQLWherePart(String varName){
		String sparqlPartText;
		
		if(!varName.startsWith("?")){
			varName = "?"+varName;
		}
		sparqlPartText =
		" OPTIONAL { \n" +
		"  values($st) {(rdfs:Datatype)} \n" +
		"  graph $go { \n" +
		" "+varName+" a $st . \n" +
		"  } \n" +
		" } \n" +
				
		" OPTIONAL { \n" +
		"  values($st) {"
		+ "		(skos:Concept)(rdfs:Class)(skosxl:Label)(skos:ConceptScheme)(skos:OrderedCollection)"
		+ "		(owl:ObjectProperty)(owl:DatatypeProperty)(owl:AnnotationProperty)(owl:OntologyProperty)"
		+ "} \n" +
		"  graph $go { \n" +
		" "+varName+" a $t . \n" +
		"  } \n" +
		"  $t rdfs:subClassOf* $st \n" +
		" } \n" +
		
		" OPTIONAL { \n" +
		"  values($st) {(skos:Collection)(rdf:Property)} \n" +
		"  graph $go { \n" +
		" "+varName+" a $st . \n" +
		"  } \n" +
		" } \n" + 
		//in case of instance (that has no a type among the above) ?st is unboud, so don't constrain the values of ?st
		" OPTIONAL { " +
		"  graph $go { \n" +
		" "+varName+" a $st . \n" +
		"  } \n" +
		" }" +
		//convert type to role ?st (super type) to ?rt (role type)
		" BIND("
				+ "IF(!BOUND(?st), \"" + RDFResourceRole.individual + "\","
				+ "IF(?st = skos:Concept, \"" + RDFResourceRole.concept + "\","
				+ "IF(?st = skos:ConceptScheme, \"" + RDFResourceRole.conceptScheme + "\","
				+ "IF(?st = skos:Collection, \"" + RDFResourceRole.skosCollection + "\","
				+ "IF(?st = skos:OrderedCollection, \"" + RDFResourceRole.skosOrderedCollection + "\","
				+ "IF(?st = skosxl:Label, \"" + RDFResourceRole.xLabel + "\","
				+ "IF(?st = rdf:Property, \"" + RDFResourceRole.property + "\","
				+ "IF(?st = owl:ObjectProperty, \"" + RDFResourceRole.objectProperty + "\","
				+ "IF(?st = owl:DatatypeProperty, \"" + RDFResourceRole.datatypeProperty + "\","
				+ "IF(?st = owl:AnnotationProperty, \"" + RDFResourceRole.annotationProperty + "\","
				+ "IF(?st = owl:OntologyProperty, \"" + RDFResourceRole.ontologyProperty + "\","
				+ "IF(?st = rdfs:Class, \"" + RDFResourceRole.cls + "\","
				+ "IF(?st = rdfs:Datatype, \"" + RDFResourceRole.dataRange + "\","
				+ "\"" + RDFResourceRole.individual + "\""
				+ "))))))))))))) as ?rt) \n" +
		" OPTIONAL { \n" +
		"	   BIND( \n" +
		"	     IF(EXISTS {"+varName+" owl:deprecated true}, \"true\", \n" +
		"	      IF(EXISTS {"+varName+" a owl:DeprecatedClass}, \"true\", \n" +
		"	       IF(EXISTS {"+varName+" a owl:DeprecatedProperty}, \"true\", \n" +
		"	        \"false\"))) \n" +
		"	   as $dep ) \n" +
		"	 } \n" ;
		return sparqlPartText;
	}
	
	public static String computeNature(Resource resource, RepositoryConnection repoConn) {
		
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>		\n"                                      
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n"                                      
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n"                                      
				+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n"
				+ "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n"
				+ "SELECT ?resource " + getNatureSPARQLSelectPart() + " {				\n"
				+ "		?resource ?p ?o													\n"
				+ getNatureSPARQLWherePart("?resource")
				+ "}																	\n"
				+ "GROUP BY ?resource";
		
		TupleQuery tq = repoConn.prepareTupleQuery(query);
		tq.setBinding("resource", resource);
		tq.setIncludeInferred(false);
		try (TupleQueryResult result = tq.evaluate()) {
			String natureValue = result.next().getValue("attr_nature").stringValue();
			return natureValue;
		}
	}

}
