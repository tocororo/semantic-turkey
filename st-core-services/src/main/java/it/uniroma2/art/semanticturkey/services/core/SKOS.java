package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.SpecialValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.AlreadyExistingLiteralFormForResourceException;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.PrefAltLabelClashException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Deleted;
import it.uniroma2.art.semanticturkey.services.annotations.DisplayName;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.SchemeAssignment;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;
import it.uniroma2.art.semanticturkey.versioning.VersioningMetadataSupport;

/**
 * This class provides services for manipulating SKOS constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class SKOS extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOS.class);
	
	@Autowired
	private CustomFormManager cfManager;

	/**
	 * Returns the list of top concepts
	 * @param schemes an optional list of schemes. When passed, only concept being topConcept of one of the 
	 * passed schemes will be returned. When not passed, all concepts not having a broader concept will be 
	 * returned
	 * @param broaderProp an optional property used as broader. When not passed, skos:broader will be used 
	 * @param narrowerProp an optional property used as narrower. if skos:broader is passed as broaderProp 
	 * and narrowerProp is null, then narrowerProp will have the value skos:narrower
	 * @param includeSubProperties if null or true, then all subProperty of broaderProp and narrowerProp will be
	 * used in the concept hierarchy
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopConcepts(@Optional @LocallyDefinedResources List<IRI> schemes,
			@Optional @LocallyDefinedResources List<IRI> broaderProps, 
			@Optional @LocallyDefinedResources List<IRI> narrowerProps,
			@Optional(defaultValue="true") boolean includeSubProperties) {
		QueryBuilder qb;
		
		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		List<IRI>broaderPropsToUse  = getHierachicalProps(broaderProps, narrowerProps);
		//narrowerProp could be null if the broaderProp  has no inverse
		List<IRI>narrowerPropsToUse = getInverseOfHierachicalProp(broaderProps, narrowerProps);
		
		String broaderNarrowerPath = preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse, 
				getManagedConnection(), includeSubProperties);
		
		if (schemes != null && !schemes.isEmpty()) {
			String query = 
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					//" SELECT ?resource ?attr_more {                                                \n" +
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					" SELECT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+" 					 \n" + 
					" WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?resource skos:topConceptOf|^skos:hasTopConcept ?scheme .                      \n" +
					"FILTER(";
			boolean first=true;
			//filter according to the scheme
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
			}		
			query += ") 																				 \n" +
					//prepareHierarchicalPartForQueryPart1(broaderProp , narrowerProp, "?aNarrowerConcept", 
					//		"?resource", false, includeSubProperties) +
					"     \nOPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {															 \n" +
					//"					?aNarrowerConcept skos:broader|^skos:narrower ?resource .    	 \n" + OLD
					//prepareHierarchicalPartForQueryPart2(broaderProp , narrowerProp, "?aNarrowerConcept", 
					//		"?resource", false, includeSubProperties) +										"\n" +
					combinePathWithVarOrIri("?aNarrowerConcept", "?resource", broaderNarrowerPath, false)+"\n" +
					"         			?subPropInScheme rdfs:subPropertyOf* skos:inScheme .         	 \n" +
					//"                   ?aNarrowerConcept ?subPropInScheme ?scheme . } as ?attr_more )   \n" +
					//"     }                                                                              \n" +
					"                   ?aNarrowerConcept ?subPropInScheme2 ?scheme2 . 					 \n" +
					"FILTER (";
					first=true;
					for(IRI scheme : schemes){
						if(!first){
							query+= " || ";
						}
						first=false;
						query+="?scheme2="+NTriplesUtil.toNTriplesString(scheme);
					}	
					query += ") 																		\n" +		 
							"} as ?attr_more )   \n" +
							"      }                                                                      \n" +
					
					
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n";
			// @formatter:on
			qb = createQueryBuilder(query);
		} else {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					//" SELECT ?resource ?attr_more                                                 \n" +
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					" SELECT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+"					 \n" + 
					" WHERE {																			 \n" + 
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					//TODO, this should be done now with MINUS
					//"     FILTER NOT EXISTS {?resource skos:broader|^skos:narrower []}                   \n" +
					//"\nMINUS {																			 \n" +
					
					" FILTER NOT EXISTS { 																 \n"+
					//preparePropPathForHierarchicalForQuery(broaderProp, narrowerProp, "?resource", 
					//		"?aNarrowerConcept", getManagedConnection(), false, includeSubProperties) +		 "\n" +	
					combinePathWithVarOrIri("?resource", "?aNarrowerConcept", broaderNarrowerPath, false)+"\n" +
					"}																					  \n" +
					//prepareHierarchicalPartForQueryPart1(broaderProp , narrowerProp, "?resource", 
					//		"?aNarrowerConcept", false, includeSubProperties) +
					/*"\nFILTER NOT EXISTS {																 \n" +
					prepareHierarchicalPartForQueryPart2(broaderProp , narrowerProp, "?resource", 
							"?aNarrowerConcept", false, includeSubProperties) +
					"\n}																				 \n" +*/
					
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {															 \n" +
						//"?aNarrowerConcept skos:broader|^skos:narrower ?resource . 					 \n" + OLD
					//prepareHierarchicalPartForQueryPart2(broaderProp , narrowerProp, "?aNarrowerConcept2", 
					//			"?resource", false, includeSubProperties) +
					combinePathWithVarOrIri("?aNarrowerConcept2", "?resource", broaderNarrowerPath, false)+"\n" +
						"} as ?attr_more ) \n" +
					"     }                                                                              \n" +
					
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					// @formatter:on
			);
		}
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	
	/**
	 * Returns the list of narrower concepts
	 * @param concept the concept to which the returned concepts are the narrower of
	 * @param schemes an optional list of schemes. When passed, only narrower concepts belonging to of one of the 
	 * passed schemes will be returned. When not passed, all narrower concepts will be returned
	 * @param broaderProp an optional property used as broader. When not passed, skos:broader will be used 
	 * @param narrowerProp an optional property used as narrower. If skos:broader is passed as broaderProp 
	 * and narrowerProp is null, then narrowerProp will have the value skos:narrower
	 * @param includeSubProperties if null or true, then all subProperty of broaderProp and narrowerProp will be
	 * used in the concept hierarchy
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getNarrowerConcepts(@LocallyDefined Resource concept,
			@Optional @LocallyDefinedResources List<IRI> schemes, 
			@Optional @LocallyDefinedResources List<IRI> broaderProps,
			@Optional @LocallyDefinedResources List<IRI> narrowerProps, 
			@Optional(defaultValue="true") boolean includeSubProperties) {
		QueryBuilder qb;

		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		List<IRI>broaderPropsToUse = getHierachicalProps(broaderProps, narrowerProps);
		//narrowerProp could be null if the broaderProp  has no inverse
		List<IRI>narrowerPropsToUse = getInverseOfHierachicalProp(broaderProps, narrowerProps);
		
		String broaderNarrowerPath = preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse, 
				getManagedConnection(), includeSubProperties);
		
		
		// @formatter:off
		if (schemes != null && !schemes.isEmpty()) {
			String query = 
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					//" SELECT ?resource ?attr_more WHERE {                                                \n" +
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					" SELECT DISTINCT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+"					 \n" + 
					" WHERE {																			 \n" +
						combinePathWithVarOrIri("?resource", (IRI)concept, broaderNarrowerPath, false)+"\n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					//"     ?resource skos:broader|^skos:narrower ?concept .                               \n" + OLD
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?resource", 
					//		(IRI)concept, false, includeSubProperties) +
					"\n     ?subPropInScheme rdfs:subPropertyOf* skos:inScheme .                         \n" +
					"     ?resource ?subPropInScheme ?scheme .                                           \n" +
					"FILTER (";
			boolean first=true;
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
			}	
			query += ") 																				 \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {															 \n" +
					//"?aNarrowerConcept skos:broader|^skos:narrower ?resource .    \n" + OLD
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?aNarrowerConcept", 
					//		"?resource", true, includeSubProperties) + 										 "\n"+
					combinePathWithVarOrIri("?aNarrowerConcept", "?resource", broaderNarrowerPath, false)+"\n" +
					"         			?subPropInScheme2 rdfs:subPropertyOf* skos:inScheme .         	 \n" +
					"                   ?aNarrowerConcept ?subPropInScheme2 ?scheme2 . 					 \n" +
					"FILTER (";
			first=true;
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme2="+NTriplesUtil.toNTriplesString(scheme);
			}	
			query += ") 																				\n" +		 
					"} as ?attr_more )   \n" +
					"     }                                                                              \n" +
					
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n";
			qb = createQueryBuilder(query);
			//qb.setBinding("scheme", scheme);
		} else {
			qb = createQueryBuilder(
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					//" SELECT ?resource ?attr_more WHERE {                                                \n" +
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					" SELECT DISTINCT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+"					 \n" + 
					" WHERE {																			 \n" +
						combinePathWithVarOrIri("?resource", (IRI)concept, broaderNarrowerPath, false)+		"\n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					//"     ?resource skos:broader|^skos:narrower ?concept .                               \n" + OLD
					
					
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?resource", 
					//		"?concept", false, includeSubProperties) +
					//preparePropPathForHierarchicalForQuery(broaderProp, narrowerProp, "?resource", 
					//		(IRI)concept, getManagedConnection(), false, includeSubProperties) +	 	    "\n" +
					
					"     OPTIONAL {                                                                     \n" +
					//"         BIND( EXISTS {?aNarrowerConcept skos:broader|^skos:narrower ?resource . } as ?attr_more ) \n" +
					"         BIND( EXISTS {															 \n" +
					//"?aNarrowerConcept skos:broader|^skos:narrower ?resource .    \n" + OLD
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?aNarrowerConcept", 
					//		"?resource", false, includeSubProperties) + " } as ?attr_more )					 \n" +
					combinePathWithVarOrIri("?aNarrowerConcept", "?resource", broaderNarrowerPath, false)+"\n" +
					" } as ?attr_more ) 																 \n" +
					"     }                                                                              \n" +
					
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
			);
		}
		// @formatter:on
		qb.processRendering();
		qb.processQName();
		//qb.setBinding("concept", concept);
		return qb.runQuery();
	}
	
	/**
	 * Returns the list of broader concepts
	 * @param concept the concept to which the returned concepts are the broader of 
	 * @param schemes an optional list of schemes. When passed, only broader concepts belonging to of one of the 
	 * passed schemes will be returned. When not passed, all broader concepts will be returned
	 * @param broaderProp an optional property used as broader. When not passed, skos:broader will be used 
	 * @param narrowerProp an optional property used as narrower. If skos:broader is passed as broaderProp 
	 * and narrowerProp is null, then narrowerProp will have the value skos:narrower
	 * @param includeSubProperties if null or true, then all subProperty of broaderProp and narrowerProp will be
	 * used in the concept hierarchy
	 * @return
	 */
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getBroaderConcepts(@LocallyDefined Resource concept,
			@Optional @LocallyDefinedResources List<IRI> schemes, 
			@Optional @LocallyDefinedResources List<IRI> broaderProps,
			@Optional @LocallyDefinedResources List<IRI> narrowerProps, 
			@Optional(defaultValue="true") boolean includeSubProperties) {
		QueryBuilder qb;

		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		List<IRI>broaderPropsToUse = getHierachicalProps(broaderProps, narrowerProps);
		//narrowerProp could be null if the broaderProp  has no inverse
		List<IRI>narrowerPropsToUse = getInverseOfHierachicalProp(broaderProps, narrowerProps);
		
		String broaderNarrowerPath = preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse, 
				getManagedConnection(), includeSubProperties);
		
		// @formatter:off
		if (schemes != null && !schemes.isEmpty()) {
			String query = 
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+"					 \n" + 
					" WHERE {																			 \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					//"     ?resource skos:narrower|^skos:broader ?concept .                               \n" + OLD
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?concept", 
					//		"?resource", false, includeSubProperties) +
					combinePathWithVarOrIri("?concept", "?resource", broaderNarrowerPath, false)+		"\n" +
					"     ?subPropInScheme rdfs:subPropertyOf* skos:inScheme .                           \n" +
					"     ?resource ?subPropInScheme ?scheme .                                           \n" +
					"FILTER (";
			boolean first=true;
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
			}	
			query += ") 																				 \n" +
					generateNatureSPARQLWherePart("?resource") +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n";
			qb = createQueryBuilder(query);
			//qb.setBinding("scheme", scheme);
		} else {
			qb = createQueryBuilder(
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          		 \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>			                             \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more "+generateNatureSPARQLSelectPart()+"					 \n" + 
					" WHERE {																			 \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					//"     ?resource skos:narrower|^skos:broader ?concept .                               \n" + OLD
					//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?", 
					//		"?resource", false, includeSubProperties) +
					combinePathWithVarOrIri("?concept", "?resource", broaderNarrowerPath, false)+		"\n" +
					generateNatureSPARQLWherePart("?resource") +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					
			);
		}
		// @formatter:on
		qb.processRendering();
		qb.processQName();
		qb.setBinding("concept", concept);
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Collection<AnnotatedValue<Resource>> getAllSchemes() {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "                                                                           \n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?conceptSchemeSubClass rdfs:subClassOf* skos:ConceptScheme .          \n" +
				"     ?resource rdf:type ?conceptSchemeSubClass .                           \n" +
				generateNatureSPARQLWherePart("?resource") +
				" }                                                                         \n" +
				" GROUP BY ?resource                                                        \n"
				// @formatter:on
		);
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSchemesMatrixPerConcept(@LocallyDefined Resource concept) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                    \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                   \n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                              \n" +
                "                                                                                        \n" +
				" SELECT ?resource ?attr_inScheme WHERE {                                                \n" +
				"    ?conceptSchemeSubClass rdfs:subClassOf* skos:ConceptScheme .                        \n" +
				"    ?resource rdf:type ?conceptSchemeSubClass .                                         \n" +
				"    BIND(EXISTS{                                                                        \n" +
				"             ?resource skos:hasTopConcept|^skos:topConceptOf|^skos:inScheme ?concept .\n" +
				"         } AS ?attr_inScheme )                                                          \n" +
				" }                                                                                      \n" +
				" GROUP BY ?resource ?attr_inScheme                                                      \n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("concept", concept);
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getRootCollections() {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "                                                                            \n" +
				" SELECT DISTINCT ?resource " + generateNatureSPARQLSelectPart() + " WHERE { \n" +
                "   ?collectionSubClass rdfs:subClassOf* skos:Collection .                   \n" +
				"   ?resource rdf:type ?collectionSubClass.                                  \n" +
				"   FILTER NOT EXISTS {                                                      \n" +
				" 	  [] skos:member ?resource .                                             \n" +
				" 	}                                                                        \n" +
				" 	FILTER NOT EXISTS {                                                      \n" +
				" 	  [] skos:memberList/rdf:rest*/rdf:first ?resource .                     \n" +
				" 	}                                                                        \n" +
				generateNatureSPARQLWherePart("?resource") +
				" }                                                                          \n" +
				" GROUP BY ?resource                                                         \n"
				// @formatter:on
		);
		qb.process(CollectionsMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getNestedCollections(@LocallyDefined Resource container) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "                                                                            \n" +
                " SELECT ?resource (COUNT(DISTINCT ?mid) AS ?index) " + generateNatureSPARQLSelectPart() + " WHERE { \n" +
				"   {                                                                        \n" +
                //for the skos:Collection
				" 	  FILTER NOT EXISTS {?container skos:memberList [] }                     \n" +
				" 	  ?container skos:member ?resource .                                     \n" +
				"   } UNION {                                                                \n" +
				//for the skos:OrderedCollection 
				" 	  ?container skos:memberList ?memberList .                               \n" +
				" 	  ?memberList rdf:rest* ?mid .                                           \n" +
				" 	  ?mid rdf:rest* ?node .                                                 \n" +
				" 	  ?node rdf:first ?resource .                                            \n" +
				"   }                                                                        \n" +
				" 	FILTER EXISTS { ?resource rdf:type/rdfs:subClassOf* skos:Collection . }  \n" +
				generateNatureSPARQLWherePart("?resource") +
				" }                                                                          \n" +
				" GROUP BY ?resource                                                         \n" +
				" ORDER BY ?index                                                            \n"
				// @formatter:on
		);
		qb.process(CollectionsMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		qb.setBinding("container", container);
		return qb.runQuery();
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

	/**
	 * @param newConcept
	 * 		IRI of the new created concept. If not provided a random IRI is generated.
	 * @param label
	 * 		preferred label of the concept
	 * @param broaderConcept
	 * 		broader of the new created concept. If not provided the new concept will be a top concept
	 * @param conceptScheme
	 * 		concept scheme where the concept belongs
	 * @param conceptCls
	 * 		class type of the new created concept. It must be a subClassOf skos:Concept. If not provided the new concept
	 * 		will be simply a skos:Concept
	 * @param customFormId
	 * 		id of the custom form to use to add additional info to the concept
	 * @param userPromptMap
	 * 		(json) map of userPrompt field to use with the custom form
	 * @return
	 * @throws URIGenerationException
	 * @throws ProjectInconsistentException
	 * @throws CustomFormException
	 * @throws CODAException
	 * @throws UnsupportedLexicalizationModelException 
	 * @throws AlreadyExistingLiteralFormForResourceException 
	 * @throws PrefAltLabelClashException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', '{lang: ''' +@auth.langof(#label)+ '''}', 'C')")
	public AnnotatedValue<IRI> createConcept(
			@Optional @NotLocallyDefined IRI newConcept, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefinedResources @SchemeAssignment List<IRI> conceptSchemes,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#Concept") IRI conceptCls,
			@Optional CustomFormValue customFormValue,
			@Optional(defaultValue="true") boolean checkExistingAltLabel, @Optional @LocallyDefined IRI broaderProp )
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, 
					CODAException, UnsupportedLexicalizationModelException, 
					AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException {
		
		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		broaderProp  = getHierachicalProp(broaderProp);
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newConceptIRI;
		if (newConcept == null) {
			newConceptIRI = generateConceptIRI(label, conceptSchemes);
		} else {
			newConceptIRI = newConcept;
		}
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newConceptIRI,
				RDFResourceRole.concept); // set created for versioning
		
		IRI conceptClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT;
		if (conceptCls != null) {
			conceptClass = conceptCls;
		}
		
		modelAdditions.add(newConceptIRI, RDF.TYPE, conceptClass);
		
		IRI xLabelIRI = null;
		if (label != null) { //?conc skos:prefLabel ?label
			xLabelIRI = createLabelUsingLexicalizationModel(newConceptIRI, label, modelAdditions, checkExistingAltLabel,
					true, conceptSchemes);
			VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(xLabelIRI,
					RDFResourceRole.xLabel); // set created for versioning
		}
		for(IRI conceptScheme : conceptSchemes){
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);//?conc skos:inScheme ?sc
		}
		if (broaderConcept != null) {//?conc skos:broader ?broad
			//modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept); OLD
			modelAdditions.add(newConceptIRI, broaderProp , broaderConcept);
			
		} else { //?conc skos:topConceptOf ?sc
			for(IRI conceptScheme : conceptSchemes){
				modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
			}
		}
		
		//CustomForm further info
		if (customFormValue != null) {
			enrichWithCustomFormUsingLexicalizationModel(newConceptIRI, label, repoConnection, modelAdditions, modelRemovals,
					customFormValue, xLabelIRI);
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newConceptIRI);
		annotatedValue.setAttribute("role", RDFResourceRole.concept.name());
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue; 
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', '{lang: ''' +@auth.langof(#label)+ '''}', 'C')")
	public AnnotatedValue<IRI> createConceptScheme(
			@Optional @NotLocallyDefined IRI newScheme, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#ConceptScheme") IRI schemeCls,
			@Optional CustomFormValue customFormValue,
			@Optional(defaultValue="true") boolean checkExistingAltLabel)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException,
					CODAException, UnsupportedLexicalizationModelException, 
					AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		IRI newSchemeIRI;
		if (newScheme == null) {
			newSchemeIRI = generateConceptSchemeURI(label);
		} else {
			newSchemeIRI = newScheme;
		}
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newSchemeIRI,
				RDFResourceRole.conceptScheme); // set created for versioning
		
		IRI schemeClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT_SCHEME;
		if (schemeCls != null) {
			schemeClass = schemeCls;
		}
		
		modelAdditions.add(newSchemeIRI, RDF.TYPE, schemeClass);
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = createLabelUsingLexicalizationModel(newSchemeIRI, label, modelAdditions, 
					checkExistingAltLabel, true, null);
		}

		RepositoryConnection repoConnection = getManagedConnection();
		
		//CustomForm further info
		if (customFormValue != null) {
			enrichWithCustomFormUsingLexicalizationModel(newSchemeIRI, label, repoConnection, modelAdditions, modelRemovals,
					customFormValue, xLabelIRI);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newSchemeIRI);
		annotatedValue.setAttribute("role", RDFResourceRole.conceptScheme.name());
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue; 
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("set preferred label")
	public void setPrefLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal,
			@Optional(defaultValue="true") boolean checkExistingAltLabel) 
			throws AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException{
		RepositoryConnection repoConnection = getManagedConnection();
		List<IRI> conceptSchemeList = getAllSchemesForConcept(concept, repoConnection);
		checkIfAddPrefLabelIsPossible(repoConnection, literal, concept, false, conceptSchemeList);
		if(checkExistingAltLabel) {
			checkIfPrefAltLabelClash(repoConnection, literal, concept);
		}
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		//check if there is always an existing prefLabel for the given language, in this case, delete it and
		// set it as altLabel
		String language = literal.getLanguage().get();
		Resource graphs = getWorkingGraph();
		try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, null, graphs)) {
			while(repositoryResult.hasNext()){
				Value value = repositoryResult.next().getObject();
				if(value instanceof Literal){
					Literal oldLiteral = (Literal) value;
					String oldLanguage = oldLiteral.getLanguage().get();
					if(oldLanguage.equals(language)){
						modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
								org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, oldLiteral));
						modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
								org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL, oldLiteral));
					}
				}
			}
		}
		
		//add the desired label as prefLabel
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, literal));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	public static List<IRI> getAllSchemesForConcept(IRI concept, RepositoryConnection repoConnection){
		// @formatter:off
		String query="SELECT DISTINCT ?scheme" +
				"\nWHERE{"+
				"\n?subPropInScheme "+
				NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF)+"* "+
				NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME)+" ." +
				"\n"+NTriplesUtil.toNTriplesString(concept)+" ?subPropInScheme ?scheme" +
				"\nFILTER isIRI(?scheme)" +
				"\n}";
		// @formatter:on
		logger.debug("query: " + query);
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		
		List<IRI> schemeList = new ArrayList<>();
		while(tupleQueryResult.hasNext()) {
			schemeList.add((IRI) tupleQueryResult.next().getValue("scheme"));
		}
		tupleQueryResult.close();
		
		return schemeList;
		
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("add alternative label")
	public void addAltLabel(@LocallyDefined @Modified IRI concept,
			@LanguageTaggedString Literal literal) throws AlreadyExistingLiteralFormForResourceException {
		RepositoryConnection repoConnection = getManagedConnection();
		checkIfAddAltLabelIsPossible(repoConnection, literal, concept);
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL, literal));
		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("add hidden label")
	public void addHiddenLabel(@LocallyDefined @Modified IRI concept,
			@LanguageTaggedString Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.HIDDEN_LABEL, literal));
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'C')")
	public void addBroaderConcept(
			@LocallyDefined @Modified(role = RDFResourceRole.concept) IRI concept,
			@LocallyDefined IRI broaderConcept, @Optional @LocallyDefined IRI broaderProp) {

		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		if (broaderProp == null) {
			broaderProp = org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER;
		}
		
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept,
				broaderProp , broaderConcept));
		

		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'C')")
	public void addConceptToScheme(
			@LocallyDefined @Modified(role = RDFResourceRole.concept) IRI concept,
			@LocallyDefined @SchemeAssignment IRI scheme) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, scheme));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'C')")
	public void addMultipleConceptsToScheme(
			@Optional @LocallyDefined IRI rootConcept,
			@LocallyDefined @SchemeAssignment IRI scheme,
			@Optional IRI inSchemeProp,
			@Optional(defaultValue="true") boolean includeSubProperties,
			@Optional @LocallyDefinedResources List<IRI> broaderProps,
			@Optional @LocallyDefinedResources List<IRI> narrowerProps,
			@Optional @LocallyDefinedResources List<IRI> filterSchemes) {
		RepositoryConnection repoConnection = getManagedConnection();
		
		List<IRI>broaderPropsToUse = null;
		List<IRI>narrowerPropsToUse = null;
		String broaderNarrowerPath = null;
		
		if(rootConcept!=null) {
			//if the client passed a rootConcept, then 
			//check if the client passed a broaderProp , otherwise, set it as skos:broader
			broaderPropsToUse = getHierachicalProps(broaderProps, narrowerProps);
			//narrowerProp could be null if the broaderProp  has no inverse
			narrowerPropsToUse = getInverseOfHierachicalProp(broaderProps, narrowerProps);
			
			broaderNarrowerPath = preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse, 
					getManagedConnection(), includeSubProperties);
		}
		
		String inSchemePropString = "skos:inScheme";
		if(inSchemeProp!=null ) {
			inSchemePropString = NTriplesUtil.toNTriplesString(inSchemeProp);
		}

		//this part is related to the modified date to be added (in case) to each selected concepts
		boolean addModifiedDate = false;
		RDFResourceRole role = RDFResourceRole.concept;
		//part copied from VersionMetadataInterceptor to check whether the modification date should be added
		for (RDFResourceRole updatableRole : stServiceContext.getProject().getUpdateForRoles()) {
			if (RDFResourceRole.subsumes(updatableRole, role, true)) {
				addModifiedDate = true;
			}
		}
		Project project = stServiceContext.getProject();
		String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);
		String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);
		if (creationDateProp == null && modificationDateProp == null) {
			addModifiedDate = false;
		}
		ValueFactory vf = repoConnection.getValueFactory();
		Literal currentTime = vf.createLiteral(new Date());
		IRI modificationDatePropIRI = null;
		if(addModifiedDate) {
			modificationDatePropIRI = vf.createIRI(modificationDateProp);
		}
		
		//prepare the SPARQL UPDATE
		// @formatter:off
		String updateQuery =
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
					"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
					"\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
					"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>  " +
					"\nPREFIX owl: <http://www.w3.org/2002/07/owl#> " ;
		if(addModifiedDate) {
			updateQuery +="\nDELETE {GRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph())+ "" +
					"\n{?concept "+NTriplesUtil.toNTriplesString(modificationDatePropIRI)+" ?oldModDate .}}";
		}
		updateQuery +="\nINSERT {GRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph())+
                    " \n{ ?concept  "+inSchemePropString+ " "+NTriplesUtil.toNTriplesString(scheme)+" . " ;
		if(addModifiedDate) {
			updateQuery+= "\n?concept "+NTriplesUtil.toNTriplesString(modificationDatePropIRI)+" "+NTriplesUtil.toNTriplesString(currentTime)+" .";
		}
		updateQuery+= "\n} }" +
					"\nWHERE{" +
                    "\nGRAPH "+NTriplesUtil.toNTriplesString(getWorkingGraph())+" {" +
                    "\n?concept rdf:type ?conceptSubClass ." +
					"\nconceptSubClass rdfs:subClassOf* skos:Concept";
		if(rootConcept!=null) {
			updateQuery += combinePathWithVarOrIri("?concept", rootConcept, broaderNarrowerPath, true);
		}
		if(filterSchemes!=null && filterSchemes.size()>0) {
			updateQuery+="\n?subPropInScheme rdfs:subPropertyOf* skos:inScheme ." +
					"\n?concept ?subPropInScheme ?scheme ." +
					"FILTER (";
			boolean first=true;
			for(IRI filterScheme : filterSchemes){
				if(!first){
					updateQuery+= " || ";
				}
				first=false;
				updateQuery+="?scheme="+NTriplesUtil.toNTriplesString(filterScheme);
			}	
			updateQuery += ")" ;
		}
		if(addModifiedDate) {
			updateQuery+="\nOPTIONAL{ ?concept "+NTriplesUtil.toNTriplesString(modificationDatePropIRI)+" ?oldModDate .}";
		}
		updateQuery +="\n}"+
				"\n}";
		// @formatter:on
		
		Update update = repoConnection.prepareUpdate(updateQuery);
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'C')")
	public void addTopConcept(@Modified(role = RDFResourceRole.concept) IRI concept,
			@LocallyDefined @SchemeAssignment IRI scheme) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, scheme));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	/**
	 * this service adds an element to a (unordered) collection.
	 * @throws DeniedOperationException 
	 * 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void addToCollection(
			@LocallyDefined @Modified(role = RDFResourceRole.skosCollection) Resource collection,
			@LocallyDefined Resource element) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		Resource[] graphs = getUserNamedGraphs();

		boolean hasElement = repoConnection.hasStatement(collection,
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, element, false, graphs);
		if (hasElement) {
			throw new DeniedOperationException("Element: " + element.stringValue()
					+ " already contained in collection: " + collection.stringValue());
		}

		modelAdditions.add(repoConnection.getValueFactory().createStatement(collection,
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, element));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void addFirstToOrderedCollection(
			@LocallyDefined @Modified(role = RDFResourceRole.skosCollection) Resource collection,
			@LocallyDefined Resource element) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		// first check if the orderedCollection as any element (check SKOS.MEMBERLIST)
		Resource list = getFirstElemOfOrderedCollection(collection, repoConnection);
		if (!list.equals(RDF.NIL)) {
			boolean found = hasElementInList(list, element, repoConnection);
			if (found) {
				throw new DeniedOperationException("Element: " + element.stringValue()
						+ " already contained in collection: " + collection.stringValue());
			}
		}
		// if the list was empty, then list=RDF.NIL otherwise list is the first element, it does not matter
		// for the rest of the code
		addFirstToOrderedCollection(element, collection, repoConnection, modelAdditions, modelRemovals);

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void addInPositionToOrderedCollection(
			@LocallyDefined @Modified(role = RDFResourceRole.skosCollection) Resource collection,
			@LocallyDefined Resource element, int index) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (index < 1) {
			throw new IllegalArgumentException("The postion should be a positive value bigger than 0");
		}

		Resource list = getFirstElemOfOrderedCollection(collection, repoConnection);
		if (list.equals(RDF.NIL)) {
			// the collection is empty, so if the index is not 1, then the operation cannot be completed
			if (index != 1) {
				throw new IllegalArgumentException(
						"The collection is empty, so the element can be added just" + " in position 1");
			}
			addFirstToOrderedCollection(element, collection, repoConnection, modelAdditions, modelRemovals);
		}
		boolean found = hasElementInList(list, element, repoConnection);
		if (found) {
			throw new DeniedOperationException("Element: " + element.stringValue()
					+ " already contained in collection: " + collection.stringValue());
		}
		if (index == 1) {
			addFirstToOrderedCollection(element, collection, repoConnection, modelAdditions, modelRemovals);
		} else {
			addInPositionToSKOSOrderedCollection(list, element, repoConnection, index, modelAdditions,
					modelRemovals);
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void addLastToOrderedCollection(
			@LocallyDefined @Modified(role = RDFResourceRole.skosCollection) Resource collection,
			@LocallyDefined Resource element) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		Resource list = getFirstElemOfOrderedCollection(collection, repoConnection);
		if (list.equals(RDF.NIL)) {
			// the ordered collection is empty, so create a list, having just one element, the input one
			BNode newBnode = createElementInList(element, RDF.NIL, repoConnection, modelAdditions);
			modelRemovals.add(repoConnection.getValueFactory().createStatement(collection,
					org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, RDF.NIL));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(collection,
					org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newBnode));
		} else {
			boolean found = hasElementInList(list, element, repoConnection);
			if (found) {
				throw new DeniedOperationException("Element: " + element.stringValue()
						+ " already contained in collection: " + collection.stringValue());
			}

			// create the last element of the list with the desired value (as RDF.FIRST)
			BNode newBNode = createElementInList(element, RDF.NIL, repoConnection, modelAdditions);

			// now walk the original list to obtain the last element of such list
			Resource lastElem = getLastElemInList(list, repoConnection);
			modelRemovals.add(repoConnection.getValueFactory().createStatement(lastElem, RDF.REST, RDF.NIL));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(lastElem, RDF.REST, newBNode));
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	protected Resource getFirstElemOfOrderedCollection(Resource collection, RepositoryConnection repoConnection){
		Resource[] graphs = getUserNamedGraphs();
		try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(collection, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, null, false, graphs)){
			return (Resource) repositoryResult.next().getObject();
		}
	}
	
	protected boolean hasElementInList(Resource list, Resource element, RepositoryConnection repoConnection){
		Resource[] graphs = getUserNamedGraphs();
		if(repoConnection.hasStatement(list, RDF.FIRST, element, false, graphs)){
			return true;
		} else if(repoConnection.hasStatement(list, RDF.REST, RDF.NIL, false, graphs)){
			return false;
		} else{
			try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(list, RDF.REST, null, graphs)){
				return hasElementInList((Resource) repositoryResult.next().getObject(), element, repoConnection);
			}
		}
	}
	
	protected void addFirstToOrderedCollection(Resource element, Resource collection, 
			RepositoryConnection repoConnection, Model modelAdditions, Model modelRemovals){
		Resource list = getFirstElemOfOrderedCollection(collection, repoConnection);
		BNode newBnode = createElementInList(element, list, repoConnection, modelAdditions);
		modelRemovals.add(repoConnection.getValueFactory().createStatement(collection, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, list));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(collection, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newBnode));
	}
	
	protected void addInPositionToSKOSOrderedCollection(Resource list, Resource element, 
			RepositoryConnection repoConnection, int pos, Model modelAdditions, Model modelRemovals) {
		Resource []graphs = getUserNamedGraphs();
		
		Resource next;
		try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(list, RDF.REST, null, false, graphs)){
			next = (Resource)repositoryResult.next().getObject();
		}
		--pos;
		if(pos == 1){
			// add the element in this position
			modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.REST, next));
			
			BNode newBNode = createElementInList(element, next, repoConnection, modelAdditions);
			modelAdditions.add(repoConnection.getValueFactory().createStatement(list, RDF.REST, newBNode));
		} else{
			if(next.equals(RDF.NIL)){
				throw new IllegalArgumentException("the collection does not have enough elements");
			}
			addInPositionToSKOSOrderedCollection(next, element, repoConnection, pos, modelAdditions, modelRemovals);
		}
	}
	
	protected BNode createElementInList(Value first, Resource next, RepositoryConnection repoConnection, 
			Model modelAdditions){
		BNode newBNode = repoConnection.getValueFactory().createBNode();
		modelAdditions.add(repoConnection.getValueFactory().createStatement(newBNode, RDF.TYPE, RDF.LIST));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(newBNode, RDF.FIRST, first));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(newBNode, RDF.REST, next));
		return newBNode;
	}
	
	protected Resource getLastElemInList(Resource list, RepositoryConnection repoConnection){
		Resource[] graphs = getUserNamedGraphs();
		if(repoConnection.hasStatement(list, RDF.REST, RDF.NIL, false, graphs)){
			return list;
		}
		try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(list, RDF.REST, null, graphs)){
			return 	getLastElemInList((Resource)repositoryResult.next().getObject(), 
					repoConnection);
		}
	}
	
	protected Resource getPrevElem(Resource list, Resource element, RepositoryConnection repoConnection){
		Resource[] graphs = getUserNamedGraphs();
		try(RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(list, RDF.REST, null, graphs)){
			Resource next = (Resource)repositoryResult.next().getObject();
			if(next.equals(RDF.NIL)){
				//the element was not found, so return null
				return null;
			}
			try(RepositoryResult<Statement> repositoryResultNext = repoConnection.getStatements(next, 
					RDF.FIRST, null, graphs)){
				if(repositoryResultNext.next().getObject().equals(element)){
					//element found, so return the list (which is the previous element to the one we were 
					//looking for)
					return list;
				} else{
					return getPrevElem(next, element, repoConnection);
				}
				
			}
		}
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'D')")
	public void removeConceptFromScheme(@LocallyDefined @Modified(role=RDFResourceRole.concept) IRI concept, IRI scheme){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, scheme));
		modelRemovals.add(repoConnection.getValueFactory().createStatement(scheme, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.HAS_TOP_CONCEPT, concept));
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, scheme));
				
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, schemes)', 'D')")
	public void removeTopConcept(
			@LocallyDefined @Modified(role = RDFResourceRole.concept) IRI concept,
			IRI scheme) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, scheme));
		modelRemovals.add(repoConnection.getValueFactory().createStatement(scheme, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.HAS_TOP_CONCEPT, concept));
		
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'D')")
	public void removePrefLabel(@LocallyDefined @Modified IRI concept, Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, literal));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'D')")
	public void removeAltLabel(@LocallyDefined @Modified IRI concept, Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL, literal));
				
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', 'R')")
	public Collection<AnnotatedValue<Literal>> getAltLabels(@LocallyDefined IRI concept, String language){
		Collection<AnnotatedValue<Literal>> literalList = new ArrayList<>();
		RepositoryConnection repoConnection = getManagedConnection();
		
		Resource[] graphs = getUserNamedGraphs();
		
		try (RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL, null, graphs)) {
			while (repositoryResult.hasNext()) {
				Value value = repositoryResult.next().getObject();
				if (value instanceof Literal) {
					AnnotatedValue<Literal> annotatedValue = new AnnotatedValue<Literal>((Literal) value);
					literalList.add(annotatedValue);
				}
			}
		}
		return literalList;
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'D')")
	public void removeBroaderConcept(
			@LocallyDefined @Modified(role = RDFResourceRole.concept) IRI concept,
			IRI broaderConcept, @Optional @LocallyDefinedResources List<IRI> broaderProps , 
			@Optional @LocallyDefinedResources List<IRI> narrowerProps) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		List<IRI>broaderPropsToUse  = getHierachicalProps(broaderProps, narrowerProps);
		//narrowerProp could be null if the broaderProp  has no inverse
		List<IRI>narrowerPropsToUse = getInverseOfHierachicalProp(broaderProps, narrowerProps);
		
		/* OLD
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept));
		modelRemovals.add(repoConnection.getValueFactory().createStatement(broaderConcept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.NARROWER, concept));
		 */
		for(IRI broaderProp : broaderPropsToUse) {
			modelRemovals.add(repoConnection.getValueFactory().createStatement(concept,
				broaderProp, broaderConcept));
		}
		for(IRI narrowerProp : narrowerPropsToUse) {
			modelRemovals.add(repoConnection.getValueFactory().createStatement(broaderConcept,
				narrowerProp, concept));
		}
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', 'D')")
	public void removeHiddenLabel(@LocallyDefined @Modified IRI concept, Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept,
				org.eclipse.rdf4j.model.vocabulary.SKOS.HIDDEN_LABEL, literal));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	/**
	 * Checks if a ConceptScheme is empty. Useful before deleting a ConceptScheme.
	 * @param scheme
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Boolean isSchemeEmpty(@LocallyDefined IRI scheme) {
		String query = 
				"ASK {																								\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " { 								\n"
				+ "		{																							\n"
				+ "			?prop " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* 						\n" 
					+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME) + " . 		\n"
				+ 			"?res ?prop " + NTriplesUtil.toNTriplesString(scheme) + " .								\n"
				+ "		} UNION {																					\n"
				+ "			?prop " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* 						\n" 
					+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.HAS_TOP_CONCEPT) + " .	\n"
				+ 			NTriplesUtil.toNTriplesString(scheme) + " ?prop ?res .									\n"
				+ "		}																							\n"
				+ "	}																								\n"
				+ "}";
		RepositoryConnection repoConnection = getManagedConnection();
		return !repoConnection.prepareBooleanQuery(query).evaluate();
	}
	
	/**
	 * Deletes a ConceptScheme
	 * @param scheme
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'D')")
	public void deleteConceptScheme(@LocallyDefined @SchemeAssignment IRI scheme) {
		String query = 
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n"
				+ "DELETE {																\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		?s1 ?p1 ?scheme .												\n"
				+ "		?scheme ?p2 ?o2 .												\n"
				+ "		?o2 ?p3 ?o3	.													\n"	
				+ "		?s4 ?p4 ?o2	.													\n"	
				+ "	}																	\n"
				+ "} WHERE {															\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		{ ?s1 ?p1 ?scheme . }											\n"
				+ "		UNION															\n"
				+ "		{ ?scheme ?p2 ?o2 . }											\n"
				+ "		UNION															\n"
				
				+ "		{ "
				+ "			?scheme ?p2 ?o2 . 											\n"
				+ "			FILTER(?p2 = 												\n"
				+ NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) + " || 				  " 
				+ NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL) + " || 				  "
				+ NTriplesUtil.toNTriplesString(SKOSXL.HIDDEN_LABEL) + ")				  "
				+ "			?o2 ?p3 ?o3 .												\n"
				+ "			?s4 ?p4 ?o2 .												\n"
				+ "		}																\n"
				
				+ "	}																	\n"
				+ "}";
		RepositoryConnection repoConnection = getManagedConnection();
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("scheme", scheme);
		update.execute();
	}
	
	/**
	 * Deletes a Concept
	 * @param scheme
	 * @throws DeniedOperationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'D')")
	public void deleteConcept(@LocallyDefined @Deleted IRI concept) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();

		//check if the client passed a broaderProp , otherwise, set it as skos:broader
		List<IRI> broaderPropsToUse  = getHierachicalProps(null, null);
		//narrowerProp could be null if the broaderProp  has no inverse
		List<IRI> narrowerPropsToUse = getInverseOfHierachicalProp(null, null);
		
		String broaderNarrowerPath = preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse, 
				getManagedConnection(), true);
		
		//first check if the concept has any narrower (or is it broader to any other concept)
		String query =
			// @formatter:off
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
			"ASK {                                                                              \n" +
			//"	[] skos:broader|^skos:narrower ?concept                                         \n" + OLD
			//prepareHierarchicalPartForQuery(broaderProp , narrowerProp, "?aNarrowerConcept", 
			//		"?concept", false, true) +
			combinePathWithVarOrIri("?aNarrowerConcept", "?concept", broaderNarrowerPath, false)+"\n" +
			"}                                                                                  \n";
			// @formatter:on
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setBinding("concept", concept);
		if(booleanQuery.evaluate()){
			throw new DeniedOperationException(
					"concept: " + concept.stringValue() + " has narrower concepts; delete them before");
		}
		
		query = 
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n"
				+ "DELETE {																\n"
				+ "	GRAPH ?g {															\n"
				+ "		?s1 ?p1 ?concept .												\n"
				+ "		?concept ?p2 ?o2 .												\n"
				+ "		?o2 ?p3 ?o3	.													\n"	
				+ "		?s4 ?p4 ?o2	.													\n"	
				+ "		?xlabel ?prop ?value .											\n"
				+ "	}																	\n"
				+ "} WHERE {															\n"
				//+ "	BIND(URI('" + concept.stringValue() + "') AS ?concept)			\n"
				+ "	GRAPH ?g {															\n"
				+ "		{ ?s1 ?p1 ?concept . }											\n"
				+ "		UNION															\n"
				+ "		{ ?concept ?p2 ?o2 . }											\n"
				+ "		UNION															\n"
				
				+ "		{ "
				+ "			?concept ?p2 ?o2 . 											\n"
				+ "			FILTER(?p2 = 												\n"
				+ NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) + " || 				  " 
				+ NTriplesUtil.toNTriplesString(SKOSXL.ALT_LABEL) + " || 				  "
				+ NTriplesUtil.toNTriplesString(SKOSXL.HIDDEN_LABEL) + ")				  "
				+ "			?o2 ?p3 ?o3 .												\n"
				+ "			?s4 ?p4 ?o2 .												\n"
				+ "		}																\n"
				
				+ "	}																	\n"
				+ "}";
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("g", getWorkingGraph());
		update.setBinding("concept", concept);
		
		update.execute();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', '{lang: ''' +@auth.langof(#label)+ '''}', 'C')")
	public AnnotatedValue<Resource> createCollection(
			IRI collectionType, @Optional @NotLocallyDefined IRI newCollection, 
			@Optional @LanguageTaggedString Literal label, @Optional @LocallyDefined IRI containingCollection,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#Collection") IRI collectionCls,
			@Optional(defaultValue = "false") boolean bnodeCreationMode,
			@Optional CustomFormValue customFormValue,
			@Optional(defaultValue="true") boolean checkExistingAltLabel)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException, 
					IllegalAccessException, UnsupportedLexicalizationModelException, 
					AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException {
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		
		Resource newCollectionRes;
		if (newCollection == null) {
			if (bnodeCreationMode) {
				newCollectionRes = vf.createBNode();
			} else { //uri
				newCollectionRes = generateCollectionURI(label);
			}
		} else {
			if (newCollection.equals(RDF.NIL)) { throw new IllegalArgumentException("Cannot create collection rdf:nil"); }
			newCollectionRes = newCollection;
		}
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newCollectionRes,
				RDFResourceRole.skosCollection); // set created for versioning
		
		IRI collectionClass = collectionType;
		if (collectionCls != null) {
			/* check consistency between collection type an class: @SubClassOf just check that collectionCls is 
			 * subClassOf Collection, but if collectionCls is subClassOf OrderedCollection and collectionType
			 * is collection throw exception */
			if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
				String query = "ASK { "
						+ NTriplesUtil.toNTriplesString(collectionCls) + " " 
						+ NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION) + " }";
				boolean inconsistent = repoConnection.prepareBooleanQuery(query).evaluate();
				if (inconsistent) {
					throw new IllegalArgumentException("Inconsistent collection type: cannot create a collection (not-ordered)"
							+ " of type " + collectionCls.stringValue() + " that is an ordered collection instead");
				}
			}
			collectionClass = collectionCls;
		}
		
		if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
			modelAdditions.add(newCollectionRes, RDF.TYPE, collectionClass);
		} else if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION)){
			modelAdditions.add(newCollectionRes, RDF.TYPE, collectionClass);
			modelAdditions.add(newCollectionRes, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, RDF.NIL);
		} else {
			throw new IllegalAccessException(collectionType.stringValue() + " is not a valid collection type");
		}
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = createLabelUsingLexicalizationModel(newCollectionRes, label, modelAdditions,
					checkExistingAltLabel, true, null);
		}
		
		
		Resource[] graphs = getUserNamedGraphs();
		if (containingCollection != null) {
			if (repoConnection.hasStatement(containingCollection, RDF.TYPE, 
					org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION, false, graphs)) {
				//add newCollection as last of containingCollection (inspired from SKOSModelImpl.addLastToSKOSOrderedCollection())
				Resource memberList = null;
				RepositoryResult<Statement> res = repoConnection.getStatements(
						containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, null, false, graphs);
				if (res.hasNext()) {
					memberList = (Resource) res.next().getObject();//it's a resource for sure since the predicate is skos:memberList
				}
				if (memberList == null) {
					BNode newNode = vf.createBNode();
					modelAdditions.add(newNode, RDF.TYPE, RDF.LIST);
					modelAdditions.add(newNode, RDF.FIRST, newCollectionRes);
					modelAdditions.add(newNode, RDF.REST, RDF.NIL);
					modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newNode);
				} else {
					BNode newNode = vf.createBNode();
					modelAdditions.add(newNode, RDF.TYPE, RDF.LIST);
					modelAdditions.add(newNode, RDF.FIRST, newCollectionRes);
					modelAdditions.add(newNode, RDF.REST, RDF.NIL);
					
					if (memberList.equals(RDF.NIL)) {
						modelRemovals.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, memberList);
						modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newNode);
					} else {
						//get last node of the list
						Resource lastNode = walkMemberList(repoConnection, memberList);
						modelRemovals.add(lastNode, RDF.REST, RDF.NIL);
						modelAdditions.add(lastNode, RDF.REST, newNode);
					}
				}
				
			} else if (repoConnection.hasStatement(containingCollection, RDF.TYPE,
					org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION, false, graphs)) {
				modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, newCollectionRes);
			}
		}
		
		//CustomForm further info
		if (customFormValue != null) {
			enrichWithCustomFormUsingLexicalizationModel(newCollectionRes, label, repoConnection, modelAdditions, 
					modelRemovals, customFormValue, xLabelIRI);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(newCollectionRes);
		if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRole.skosCollection.name());
		} else { //ORDERED
			annotatedValue.setAttribute("role", RDFResourceRole.skosOrderedCollection.name());
		}
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue; 
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'D')")
	public void deleteCollection(@LocallyDefined Resource collection) throws DeniedOperationException{
		
		RepositoryConnection repConn = getManagedConnection();
		String query = "ASK {?resource <http://www.w3.org/2004/02/skos/core#member> ?member ."
				+ "\n{?member a <http://www.w3.org/2004/02/skos/core#Collection>} "
				+ "\nUNION "
				+ "\n{?member a <http://www.w3.org/2004/02/skos/core#OrderedCollection>}}"
				;
		BooleanQuery booleanQuery = repConn.prepareBooleanQuery(query);
		booleanQuery.setBinding("resource", collection);
		booleanQuery.setIncludeInferred(false);
		boolean result = booleanQuery.evaluate();
		if(result){
			throw new DeniedOperationException("collection: " + collection.stringValue() + 
					" has nested collections; delete them before");
		}		
		// @formatter:off
		String updateString =
		"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
		"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"DELETE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"      ?parentUnorderedCollection skos:member ?deletedCollection .\n" +
		"      ?parentOrderedCollection skos:memberList ?memberListFirstNode .\n" +
		"      ?prevNode rdf:rest ?itemNode .\n" +
		"      ?itemNode ?p2 ?o2 .\n" +
		"      ?memberListFirstNode ?p1 ?o1 .\n" +
		"   }\n" +
		"}\n" +
		"INSERT {\n" +
		"   GRAPH ?workingGraph {\n" +
		"	   ?parentOrderedCollection skos:memberList ?memberListNewFirstNode .\n" +
		"   	?prevNode rdf:rest ?memberListRest .\n" +
		"   }\n" +
		"}\n" +
		"WHERE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"		optional {\n" +
		"       	?parentUnorderedCollection skos:member ?deletedCollection .\n" +
		"		}\n" +
		"		optional {\n" +
		"			?parentOrderedCollection skos:memberList ?memberList .\n" +
		"			optional {\n" +
		"				?memberList rdf:first ?deletedCollection .\n" +
		"				?memberList rdf:rest ?memberListNewFirstNode .\n" +
		"				BIND(?memberList as ?memberListFirstNode)\n" +
		"				?memberListFirstNode ?p1 ?o1 .\n" +
		"			}\n" +
		"			optional {\n" +
		"				?memberList rdf:rest* ?prevNode .\n" +
		"				?prevNode rdf:rest ?itemNode .\n" +
		"				?itemNode rdf:first ?deletedCollection .\n" +
		"				?itemNode rdf:rest ?memberListRest .\n" +
		" 		        ?itemNode ?p2 ?o2 .\n" +
		"			}\n" +
		"		}\n" +
		"	}\n" +
		"};\n" +
		"DELETE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"      ?s ?p ?o.\n" +
		"   }\n" +
		"}\n" +
		"WHERE {\n" +
		"\n" +
		"   {\n" +
		"      BIND(?deletedCollection as ?s)\n" +
		"      GRAPH ?workingGraph {\n" +
		"	      ?s ?p ?o .\n" +
		"      }\n" +
		"   }\n" +
		"   UNION {\n" +
		"      GRAPH ?workingGraph {\n" +
		"     	 ?deletedCollection skosxl:prefLabel|skosxl:altLabel|skosxl:hiddenLabel ?xLabel .\n" +
		"         BIND(?xLabel as ?s)\n" +
		"	     ?s ?p ?o .\n" +
		"      }\n" +
		"   }\n" +
		"}\n";
		// @formatter:on
		logger.debug(updateString);
		
		Update update = repConn.prepareUpdate(updateString);
		update.setBinding("workingGraph", getWorkingGraph());
		update.setBinding("deletedCollection", collection);
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'D')")
	public void deleteOrderedCollection(@LocallyDefined Resource collection) throws DeniedOperationException{
		
		RepositoryConnection repConn = getManagedConnection();
		String query = "ASK {?resource <http://www.w3.org/2004/02/skos/core#memberList> ?memberList . "
				+ "\n?memberList <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?member . "
				+ "\n{?member a <http://www.w3.org/2004/02/skos/core#Collection>} "
				+ "\nUNION "
				+ "\n{?member a <http://www.w3.org/2004/02/skos/core#OrderedCollection>}}";
		BooleanQuery booleanQuery = repConn.prepareBooleanQuery(query);
		booleanQuery.setBinding("resource", collection);
		booleanQuery.setIncludeInferred(false);
		boolean result = booleanQuery.evaluate();
		if(result){
			throw new DeniedOperationException("collection: " + collection.stringValue() + 
					" has nested collections; delete them before");
		}		
		
		
		// @formatter:off
		String updateString =
		"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
		"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"DELETE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"      ?parentUnorderedCollection skos:member ?deletedCollection .\n" +
		"      ?parentOrderedCollection skos:memberList ?memberListFirstNode .\n" +
		"      ?prevNode rdf:rest ?itemNode .\n" +
		"      ?itemNode ?p2 ?o2 .\n" +
		"      ?memberListFirstNode ?p1 ?o1 .\n" +
		"   }\n" +
		"}\n" +
		"INSERT {\n" +
		"   GRAPH ?workingGraph {\n" +
		"	   ?parentOrderedCollection skos:memberList ?memberListNewFirstNode .\n" +
		"   	?prevNode rdf:rest ?memberListRest .\n" +
		"   }\n" +
		"}\n" +
		"WHERE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"		optional {\n" +
		"       	?parentUnorderedCollection skos:member ?deletedCollection .\n" +
		"		}\n" +
		"		optional {\n" +
		"			?parentOrderedCollection skos:memberList ?memberList .\n" +
		"			optional {\n" +
		"				?memberList rdf:first ?deletedCollection .\n" +
		"				?memberList rdf:rest ?memberListNewFirstNode .\n" +
		"				BIND(?memberList as ?memberListFirstNode)\n" +
		"				?memberListFirstNode ?p1 ?o1 .\n" +
		"			}\n" +
		"			optional {\n" +
		"				?memberList rdf:rest* ?prevNode .\n" +
		"				?prevNode rdf:rest ?itemNode .\n" +
		"				?itemNode rdf:first ?deletedCollection .\n" +
		"				?itemNode rdf:rest ?memberListRest .\n" +
		" 		        ?itemNode ?p2 ?o2 .\n" +
		"			}\n" +
		"		}\n" +
		"	}\n" +
		"};\n" +
		"DELETE {\n" +
		"   GRAPH ?workingGraph {\n" +
		"      ?s ?p ?o.\n" +
		"   }\n" +
		"}\n" +
		"WHERE {\n" +
		"\n" +
		"   {\n" +
		"      BIND(?deletedCollection as ?s)\n" +
		"      GRAPH ?workingGraph {\n" +
		"	      ?s ?p ?o .\n" +
		"      }\n" +
		"   }\n" +
		"   UNION {\n" +
		"      GRAPH ?workingGraph {\n" +
		"     	 ?deletedCollection skosxl:prefLabel|skosxl:altLabel|skosxl:hiddenLabel ?xLabel .\n" +
		"         BIND(?xLabel as ?s)\n" +
		"	     ?s ?p ?o .\n" +
		"      }\n" +
		"   }\n" +
		"   UNION {\n" +
		"      GRAPH ?workingGraph {\n" +
		"     	 ?deletedCollection skos:memberList/rdf:rest* ?list .\n" +
		"		 FILTER(!sameTerm(?list, rdf:nil))\n"+
		"        BIND(?list as ?s)\n" +
		"	     ?s ?p ?o .\n" +
		"      }\n" +
		"   }\n" +
		"}\n";
		// @formatter:on
		
		logger.debug(updateString);
		
		Update update = repConn.prepareUpdate(updateString);
		update.setBinding("workingGraph", getWorkingGraph());
		update.setBinding("deletedCollection", collection);
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void removeFromCollection(@LocallyDefined Resource element,
			@Modified(role = RDFResourceRole.skosCollection) @LocallyDefined Resource collection)
			throws DeniedOperationException {
		Model modelRemovals = new LinkedHashModel();
		RepositoryConnection repoConnection = getManagedConnection();
		
		
		Resource [] grahs = getUserNamedGraphs();
		
		if(!repoConnection.hasStatement(collection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, 
				element, false, grahs)){
			throw new DeniedOperationException("collection: " + collection.stringValue() + 
					" does not have elment "+element.stringValue());
		} 
		modelRemovals.add(repoConnection.getValueFactory().createStatement(collection, 
				org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, element));
		
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'U')")
	public void removeFromOrderedCollection(
			@LocallyDefined Resource element,
			@Modified(role = RDFResourceRole.skosCollection) @LocallyDefined Resource collection)
			throws DeniedOperationException {
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		RepositoryConnection repoConnection = getManagedConnection();
		Resource[] graphs = getUserNamedGraphs();
		
		Resource list = getFirstElemOfOrderedCollection(collection, repoConnection);
		if(list.equals(RDF.NIL)){
			throw new DeniedOperationException("collection: " + collection.stringValue() + " is empty");
		}
		//check if the desired element is the first one of the list
		if(repoConnection.hasStatement(list, RDF.FIRST, element, false, graphs)){
			//the element is the first one of the list
			modelRemovals.add(repoConnection.getValueFactory().createStatement(collection, 
					org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, list));
			modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.TYPE, RDF.LIST));
			modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.FIRST, element));
			
			//get the next element
			try(RepositoryResult<Statement>repositoryResult = repoConnection.getStatements(list, RDF.REST, null, graphs)){
				Resource next = (Resource) repositoryResult.next().getObject();
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.REST, next));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(collection, 
						org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, next));
				
			}
		} else{
			// the desired element is not the first one, so search for it
			Resource prevElem = getPrevElem(list, element, repoConnection);
			//prev element is the element before the one we were looking for
			try(RepositoryResult<Statement>repositoryResult = repoConnection.getStatements(prevElem, RDF.REST, null, false, graphs)){
				Resource desiredElement = (Resource) repositoryResult.next().getObject();
				modelRemovals.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST, desiredElement));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElement, RDF.TYPE, RDF.LIST));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElement, RDF.FIRST, element));
				//get the next element to the one we have just found
				try(RepositoryResult<Statement>repositoryResult2 = repoConnection.getStatements(desiredElement, RDF.REST, null, false, graphs)){
					Resource next = (Resource) repositoryResult2.next().getObject();
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElement, RDF.REST, next));
					modelAdditions.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST, next));
				}
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', notes)', '{lang: ''' +@auth.langof(#value)+ '''}','C')")
	@DisplayName("add alternative label")
	public void addNote(@LocallyDefined @Modified IRI resource, 
			@Optional @LocallyDefined @SubPropertyOf(superPropertyIRI = "http://www.w3.org/2004/02/skos/core#note") IRI predicate,
			SpecialValue value) throws ProjectInconsistentException, CODAException {
		addValue(getManagedConnection(), resource, predicate, value);
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
	 * @param schemes
	 *            the schemes to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptIRI(Literal label, List<IRI> schemes) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (schemes != null) {
			args.put(URIGenerator.Parameters.schemes,
					SimpleValueFactory.getInstance().createLiteral(TurtleHelp.serializeCollection(schemes)));
		}

		return generateIRI(URIGenerator.Roles.concept, args);
	}

	/**
	 * Generates a new URI for a SKOS concept scheme, optionally given its accompanying preferred label.
	 * 
	 * @param label
	 *            the preferred label accompanying the concept scheme (can be <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptSchemeURI(Literal label) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}
		return generateIRI(URIGenerator.Roles.conceptScheme, args);
	}

	/**
	 * Generates a new URI for a SKOS collection, optionally given its accompanying preferred label.
	 * 
	 * @param label
	 *            the preferred label accompanying the collection (can be <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateCollectionURI(Literal label) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}
		return generateIRI(URIGenerator.Roles.skosCollection, args);
	}
	
	/**
	 * Returns the last member of a member list (the one that has no rest or has rdf:nil as rest)
	 */
	private Resource walkMemberList(RepositoryConnection repoConnection, Resource list) {
		RepositoryResult<Statement> stmts = repoConnection.getStatements(list, RDF.REST, null, getWorkingGraph());
		if (stmts.hasNext()) {
			Resource obj = (Resource) stmts.next().getObject();
			if (obj.equals(RDF.NIL)) {
				return list;
			} else {
				return walkMemberList(repoConnection, obj);
			}
		} else {
			//if list has no object for rdf:rest property, assume that rest was rdf:nil and list was the last element
			return list;
		}
	}
	
	/**
	 * In case of SKOSXL_LEXICALIZATION_MODEL it return the xLabelIRI, null in other cases
	 * @throws URIGenerationException 
	 * @throws UnsupportedLexicalizationModelException 
	 * @throws AlreadyExistingLiteralFormForResourceException 
	 * @throws PrefAltLabelClashException 
	 */
	private IRI createLabelUsingLexicalizationModel(Resource resource, Literal label, Model modelAdditions,
			boolean checkPrefAltLabelClash, boolean newResource, List<IRI> conceptSchemes) 
			throws URIGenerationException, UnsupportedLexicalizationModelException, 
			AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException {
		IRI lexModel = getProject().getLexicalizationModel();
		IRI xLabelIRI = null;
		if (lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)
				|| lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			modelAdditions.add(resource, RDFS.LABEL, label);
		} else if (lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			checkIfAddPrefLabelIsPossible(getManagedConnection(), label, resource, newResource, conceptSchemes);
			if(checkPrefAltLabelClash) {
				checkIfPrefAltLabelClash(getManagedConnection(), label, resource);
			}
			modelAdditions.add(resource, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
		} else if (lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			it.uniroma2.art.semanticturkey.services.core.SKOSXL.checkIfAddPrefLabelIsPossible(
					getManagedConnection(), label, resource, newResource, conceptSchemes);
			if(checkPrefAltLabelClash) {
				it.uniroma2.art.semanticturkey.services.core.SKOSXL.checkIfPrefAltLabelClash(
						getManagedConnection(), label, resource);
			}
			xLabelIRI = generateXLabelIRI(resource, label, SKOSXL.PREF_LABEL);
			modelAdditions.add(resource, SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, SKOSXL.LITERAL_FORM, label);
		} else {
			throw new UnsupportedLexicalizationModelException(lexModel.stringValue() +
					" is not a valid lexicalization model");
		}
		return xLabelIRI;
	}
	
	
	private void enrichWithCustomFormUsingLexicalizationModel(Resource resource, Literal label, 
			RepositoryConnection repoConnection, Model modelAdditions, Model modelRemovals, 
			CustomFormValue customFormValue, IRI xLabelIRI) 
					throws ProjectInconsistentException, CODAException, CustomFormException{
		IRI lexModel = getProject().getLexicalizationModel();
		
		StandardForm stdForm = new StandardForm();
		stdForm.addFormEntry(StandardForm.Prompt.resource, resource.stringValue());
		
		CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
		
		if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) || lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
			if (label != null) {
				stdForm.addFormEntry(StandardForm.Prompt.label, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
		} else{ //Project.SKOSXL_LEXICALIZATION_MODEL
			if (xLabelIRI != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabelIRI.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
		}
		enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, customFormValue.getUserPromptMap(), stdForm);
	}
	
	
	/**
	 * Generates a new URI for a SKOSXL Label, based on the provided mandatory parameters. The actual
	 * generation of the URI is delegated to {@link #generateURI(String, Map)}, which in turn invokes the
	 * current binding for the extension point {@link URIGenerator}. In the end, the <i>URI generator</i> will
	 * be provided with the following:
	 * <ul>
	 * <li><code>xLabel</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>lexicalForm</code>,
	 * <code>lexicalizedResource</code> and <code>type</code> (each, if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * All arguments should be not <code>null</code>, but in the end is the specific implementation of the
	 * extension point that would complain about the absence of one of these theoretically mandatory
	 * parameters.
	 * 
	 * @param lexicalForm
	 *            the textual content of the label
	 * @param lexicalizedResource
	 *            the resource to which the label will be attached to
	 * @param lexicalizationProperty
	 *            the property used for attaching the label
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateXLabelIRI(Resource lexicalizedResource, Literal lexicalForm,
			IRI lexicalizationProperty) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (lexicalizedResource != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, lexicalizedResource);
		}

		if (lexicalForm != null) {
			args.put(URIGenerator.Parameters.lexicalForm, lexicalForm);
		}

		if (lexicalizationProperty != null) {
			args.put(URIGenerator.Parameters.lexicalizationProperty, lexicalizationProperty);
		}

		return generateIRI(URIGenerator.Roles.xLabel, args);
	}
	
	
	public static void checkIfAddPrefLabelIsPossible(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource, boolean newResource, List<IRI> conceptSchemes) 
					throws AlreadyExistingLiteralFormForResourceException{
		
		
		//see if there is no other resource that has a prefLabel with the same Literal or that the resource 
		// to which the Literal will be added has not already an alternative label with the input
		String query = "ASK {"+
				"\n{?resource "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . ";
		//if at least one concept scheme is passed, filter the ?resource to that scheme(s)
		if(conceptSchemes!=null && conceptSchemes.size()>0) {
			query+="\n?subPropInScheme "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF)+"* "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME)+" .";
			if(conceptSchemes.size()==1) {
				//since it is a single scheme, there is no need to use the FILTER, check check the triple
				query+="\n?resource ?subPropInScheme "+NTriplesUtil.toNTriplesString(conceptSchemes.get(0))+" .";
			}else {
				//since there are at least two schemes, use the filter
				boolean first=true;
				query+="\n?resource ?subPropInScheme ?scheme ."+
						"\nFILTER(";
				for(IRI scheme : conceptSchemes) {
					if(!first) {
						query+= " || ";
					}
					first = false;
					query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
				}
				query+=")";
			}
		}
		query+= "\n}"+
				"\nUNION"+
				"\n{"+NTriplesUtil.toNTriplesString(resource)+" "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . }";	
				//see the type to check
		query+="\n}";
		
		logger.debug("query: " + query);
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		if(booleanQuery.evaluate()){
			String text;
			if(!newResource) {
				text = "prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since either "
					+ "there is already a resource with the same prefLabel or this resource has already an altLabel "
					+ "with the same value";
			} else {
				text = "prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since "
						+ "there is already a resource with the same prefLabel";
			}
			throw new AlreadyExistingLiteralFormForResourceException(text);
		}
	}
	
	public static void checkIfPrefAltLabelClash(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource) throws PrefAltLabelClashException{
		//see if there is no other resource that has a altLabel with the same Literal 
		String query = "ASK {"+
				"\n?resource "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . "+
				"\n}";
		
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		if(booleanQuery.evaluate()){
			String text = "prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since "
					+ "there is already a resource with the same altLabel.";
			throw new PrefAltLabelClashException(text);
		}
	}
	
	public static void checkIfAddAltLabelIsPossible(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource) throws AlreadyExistingLiteralFormForResourceException{
		//see if the resource to which the Literal will be added has not already a pref label or an  
		// alternative label with the input
		String query = "ASK {"+
				"\n"+NTriplesUtil.toNTriplesString(resource)+" "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . "+
				"\n}";
		
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		if(booleanQuery.evaluate()){
			String text = "altLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since this "
					+ "resource has already a prefLabel with the same value";
			throw new AlreadyExistingLiteralFormForResourceException(text);
		}
	}
	
	public static List<IRI> getHierachicalProps(List<IRI> broaderProps, List<IRI> narrowerProps ) {
		if((broaderProps==null || broaderProps.isEmpty()) && 
				(narrowerProps==null || narrowerProps.isEmpty()) ) {
			List<IRI> broaderPropsToUse = new ArrayList<>();
			broaderPropsToUse.add(org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER);
			return broaderPropsToUse;
		} 
		return broaderProps;
	}
	
	public static IRI getHierachicalProp(IRI broaderProps) {
		if(broaderProps  == null ) {
			 return  org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER;
		}
		return broaderProps;
	}
	
	/**
	 * return the inverse of the input broaderProp  or null if it ha no inverse
	 */
	public static List<IRI> getInverseOfHierachicalProp(List<IRI> broaderProps , List<IRI> narrowerProps) {
		if (narrowerProps != null && !narrowerProps.isEmpty()) {
			return narrowerProps;
		}
		//if the hierachical property is null or empty, then return NARROWER
		else if(broaderProps  == null || broaderProps.size()==0){
			narrowerProps = new ArrayList<>();
			narrowerProps.add(org.eclipse.rdf4j.model.vocabulary.SKOS.NARROWER);
			return narrowerProps;
		}
		//in all other case, return null
		else {
			return null;
		}
		/*
		// @formatter:off
		String query = "SELECT ?inverseProp"
				+ "\nWHERE{"
				+ "\n{<"+broaderProp .stringValue()+"> <"+org.eclipse.rdf4j.model.vocabulary.OWL.INVERSEOF+"> "
						+ "?inverseProp}"
				+ "\nUNION"
				+ "\n{?inverseProp <"+org.eclipse.rdf4j.model.vocabulary.OWL.INVERSEOF+"> "
						+ "<"+broaderProp .stringValue()+">}"		
				+ "\n}\n";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		IRI inverseHierarchicalProp = null;
		if(tupleQueryResult.hasNext()) {
			inverseHierarchicalProp = (IRI) tupleQueryResult.next().getValue("inverseProp");
		}
		return inverseHierarchicalProp;
		*/
	}
	
	
	public static String combinePathWithVarOrIri(IRI iri, IRI superIri, String propertyPath, 
			boolean transitiveClosure ) {
		return combinePathWithVarOrIri("<"+iri.stringValue()+">", "<"+superIri.stringValue()+">", propertyPath, 
				transitiveClosure);
	}
	
	public static String combinePathWithVarOrIri(IRI iri, String superVar, String propertyPath, 
			boolean transitiveClosure ) {
		return combinePathWithVarOrIri("<"+iri.stringValue()+">", superVar, propertyPath, 
				transitiveClosure);
	}
	
	public static String combinePathWithVarOrIri(String var, IRI superIri, String propertyPath, 
			boolean transitiveClosure ) {
		return combinePathWithVarOrIri(var, "<"+superIri.stringValue()+">", propertyPath, 
				transitiveClosure);
	}
	
	public static String combinePathWithVarOrIri(String var, String superVar, String propertyPath, 
			boolean transitiveClosure ) {
		if(!var.startsWith("?") && !var.startsWith("<")) {
			var = "?"+var;
		}
		if(!superVar.startsWith("?") && !superVar.startsWith("<")) {
			superVar = "?"+superVar;
		}
		
		String starOrNot = "";
		if(transitiveClosure) {
			starOrNot = "*";
		}
		
		String query ="\n" + var + " (" + propertyPath  + " ) "+starOrNot+" "+superVar+" .";
		
		return query;
	}
	
	
	public static String preparePropPathForHierarchicalForQuery(List<IRI> broaderProps, 
			List<IRI> inverseHierachicalProps, RepositoryConnection repoConnection, 
			boolean includeSubProperties) {
		
		List<IRI> subPropList = new ArrayList<>();
		List<IRI> inverseSubPropList = new ArrayList<>();
		
		if(includeSubProperties) {
			boolean first = true;
			// @formatter:off
			String query = "SELECT ?subProp ?subInverseProp"
					+ "\nWHERE{";
			
			for(IRI broaderProp : broaderProps) {
				if(!first) {
					query+="\nUNION";
				}
				first = false;
				query+="\n{ ?subProp <"+org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF.stringValue()+">* "
						+ "<"+broaderProp.stringValue()+"> . }";
			}
			if(inverseHierachicalProps!=null) {
				for(IRI inverseHierachicalProp : inverseHierachicalProps) {
					if(!first) {
						query+="\nUNION";
					}
					first = false;
					query+="\n{ ?subInverseProp <"+org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF.stringValue()+">* "
								+ "<"+inverseHierachicalProp.stringValue()+"> . }";
				}
			}
			
			query +="\n}";
			// @formatter:on
			logger.debug("query: " + query);
			TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
			//get the subProperties from the results of the SPARQL query
			while(tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				if(bindingSet.hasBinding("subProp")) {
					Value value = bindingSet.getValue("subProp");
					if(value instanceof IRI && !subPropList.contains((IRI)value)) {
						subPropList.add((IRI)value);
					}
				}
				if(bindingSet.hasBinding("subInverseProp")) {
					Value value = bindingSet.getValue("subInverseProp");
					if(value instanceof IRI && !inverseSubPropList.contains((IRI)value)) {
						inverseSubPropList.add((IRI)value);
					}
				}
			}
			tupleQueryResult.close();
		} else {
			//do not use the sub properties, so there was no need to execute the SPARQL query, just set in
			// subPropList and inverseSubPropList the input properties
			subPropList.addAll(broaderProps);
			if(inverseHierachicalProps!=null) {
				inverseSubPropList.addAll(inverseHierachicalProps);
			}
		}
			
		//now construct the propertyPath
		String propertyPath = "";
		boolean first = true;
		for(IRI subProp : subPropList) {
			if(!first) {
				propertyPath += " | ";
			}
			else {
				first = false;
			}
			propertyPath += "<"+subProp.stringValue()+">";
		}
		for(IRI inverseSubProp : inverseSubPropList) {
			if(!first) {
				propertyPath += " | ";
			}
			else {
				first = false;
			}
			propertyPath += "^<"+inverseSubProp.stringValue()+">";
		}
		return propertyPath;
	}
	
	/*public String prepareInnerQueryForHierarchicalProp(IRI broaderProp , String varName) {
		if(!varName.startsWith("?")) {
			varName ="?"+varName;
		}
		String subQuery = "SELECT "+varName+""
				+ "\nWEHERE{"
				+ "\n"+varName+" <"+org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF.stringValue()+"> "
						+ "<"+broaderProp .stringValue()+">"
				+ "\n}";
		
		return subQuery;
	}*/

	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
		Method m = SKOS.class.getMethod("getNarrowerConcepts", Resource.class, Resource.class,
				String[].class);
		System.out.println(m.getGenericReturnType());
	}
}

class CollectionsMoreProcessor implements QueryBuilderProcessor {

	public static final CollectionsMoreProcessor INSTANCE = new CollectionsMoreProcessor();
	private GraphPattern graphPattern;

	private CollectionsMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE)
				.prefix("rdfs", RDFS.NAMESPACE)
				.prefix("skos", org.eclipse.rdf4j.model.vocabulary.SKOS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern(
					// @formatter:off
					"BIND(EXISTS { {                                                                   \n" +
					"				FILTER EXISTS {                                                    \n" +
					"					?collClassSubj rdfs:subClassOf* skos:Collection .              \n" +
					"					MINUS {                                                        \n" +
					"						?collClassSubj rdfs:subClassOf* skos:OrderedCollection .   \n" +
					"					}                                                              \n" +
					"					?resource a ?collClassSubj .                                   \n" +
					"				}															       \n" +
					"                ?resource skos:member ?member .                                   \n" +
					"              } union {                                                           \n" +
					"				FILTER EXISTS {                                                    \n" +
					"					?ordCollClassSubj rdfs:subClassOf* skos:OrderedCollection .    \n" +
					"					?resource a ?ordCollClassSubj .                                \n" +
					"				}															       \n" +
					"                ?resource skos:memberList/rdf:rest*/rdf:first ?member .           \n" +
					"              }                                                                   \n" +
					"			?collClassMember rdfs:subClassOf* skos:Collection .                    \n" +
					"			?member a ?collClassMember .                                           \n" +
					"			} AS ?attr_more)                                                       \n"
					// @formatter:on
		).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return false;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

	@Override
	public GraphPattern getGraphPattern(Project currentProject) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(Project currentProject, List<BindingSet> resultTable) {
		return null;
	}
};