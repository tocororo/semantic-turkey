package it.uniroma2.art.semanticturkey.data.nature;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;

public class RoleRecognitionOrchestrator {
	

	public static String computeNature(Resource resource, RepositoryConnection repoConn) {
		
		String query = " SELECT ?resource (group_concat(DISTINCT concat(str($rt), \",\", str($go), \",\", "
						+ "str($dep));separator=\"|_|\") as ?attr_nature) \n" +
				
						"WHERE{\n" +
						//just to have a triple that return something
						"?resource a ?type .\n" +
						
						" OPTIONAL { \n" +
						"  values($st) {"
						+ "		(skos:Concept)(rdfs:Class)(skosxl:Label)(skos:ConceptScheme)(skos:OrderedCollection)"
						+ "		(owl:ObjectProperty)(owl:DatatypeProperty)(owl:AnnotationProperty)(owl:OntologyProperty)"
						+ "} \n" +
						"  graph $go { \n" +
						" ?resource a $t . \n" +
						"  } \n" +
						"  $t rdfs:subClassOf* $st \n" +
						" } \n" +
						
						" OPTIONAL { \n" +
						"  values($st) {(skos:Collection)(rdf:Property)} \n" +
						"  graph $go { \n" +
						" ?resource a $st . \n" +
						"  } \n" +
						" } \n" + 
						//in case of instance (that has no a type among the above) ?st is unboud, so don't constrain the values of ?st
						" OPTIONAL { " +
						"  graph $go { \n" +
						" ?resource a $st . \n" +
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
								+ "\"" + RDFResourceRole.individual + "\""
								+ ")))))))))))) as ?rt) \n" +
						
						" OPTIONAL { \n" +
						"	   BIND( \n" +
						"	     IF(EXISTS {?resource owl:deprecated true}, \"true\", \n" +
						"	      IF(EXISTS {?resource a owl:DeprecatedClass}, \"true\", \n" +
						"	       IF(EXISTS {?resource a owl:DeprecatedProperty}, \"true\", \n" +
						"	        \"false\"))) \n" +
						"	   as $dep ) \n" +
						"	 } \n" +
						
						"}";
		
		TupleQuery tq = repoConn.prepareTupleQuery(query);
		tq.setBinding("resource", resource);
		tq.setIncludeInferred(false);
		try (TupleQueryResult result = tq.evaluate()) {
			String natureValue = result.next().getValue("nature").stringValue();
			return natureValue;
		}
	}

}
