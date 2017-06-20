package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.FormCollection;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.datarange.DataRangeAbstract;
import it.uniroma2.art.semanticturkey.datarange.DataRangeDataOneOf;
import it.uniroma2.art.semanticturkey.datarange.ParseDataRange;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * This class provides services for reading the Properties.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class Properties extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Properties.class);

	@Autowired
	private CustomFormManager cfManager;
	
	/**
	 * returns all root properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopProperties() {
		logger.info("request to get all the top properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	?propertyType	.							\n" +
				"     FILTER (?propertyType = rdf:Property || " +
				"				?propertyType = owl:ObjectProperty ||" +
				"				?propertyType = owl:DatatypeProperty ||" +
				"				?propertyType = owl:AnnotationProperty ||" +
				"				?propertyType = owl:OntologyProperty  )						\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root RDF properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopRDFProperties() {
		logger.info("request to get the top RDF properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	rdf:Property	.							\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Object properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(objectProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopObjectProperties() {
		logger.info("request to get the top Object properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	owl:ObjectProperty	.						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Datatype properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatypeProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopDatatypeProperties() {
		logger.info("request to get the top Datatype properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	owl:DatatypeProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Annotation properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(annotationProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopAnnotationProperties() {
		logger.info("request to get the top Annotation properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	owl:AnnotationProperty  .					\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Ontology properties
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontologyProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopOntologyProperties() {
		logger.info("request to get the top Ontology properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																			\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource 	rdf:type	owl:OntologyProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * takes a list of Properties and return their description as if they were roots for a tree
	 * (so more, role, explicit etc...)
	 * @param propList
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getPropertiesInfo(IRI[] propList) {
		logger.info("request to get the Propery info, given a property List");
		
		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																		\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {		\n" +
				"     VALUES(?resource) {");
		for(int i=0; i<propList.length; ++i){
			sb.append("(<"+propList[i].stringValue()+">)");
		}
		sb.append("}													 				\n" +
				generateNatureSPARQLWherePart("?resource") +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSubProperties(@LocallyDefined Resource superProperty) {
		logger.info("request to get the top sub properties for "+superProperty.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
                "																						\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {						\n" +
				"     ?resource 	rdfs:subPropertyOf	?superProperty .								\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																						\n" +
				" GROUP BY ?resource															\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		qb.setBinding("superProperty", superProperty);
		return qb.runQuery();
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSuperProperties(@LocallyDefined Resource subProperty) {
		logger.info("request to get the top super properties for "+subProperty.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?subProperty 	rdfs:subPropertyOf	?resource		 .				\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("subProperty", subProperty);
		return qb.runQuery();
	}
	
	/**
	 * Retrieves all types of res, then all properties having their domain on any of the types for res.
	 * Note that it provides only root properties (e.g. if both rdfs:label and skos:prefLabel,
	 * which is a subProperty of rdfs:label, have domain = one of the types of res, then only rdfs:label is returned)
	 * @param res
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForResource(@LocallyDefined Resource res) {
		logger.info("request to get the top all properties having their domain on any of the types for "+res.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?res 	rdf:type	?type		 .									\n" +
				"     ?resource 	rdfs:domain	?type		 .							\n" +
				"     {?resource 	rdf:type	?typeOfProp  .							\n" + 
				"		FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp})}	\n" + 
				"		UNION															\n" + 
				"     {?resource rdfs:subPropertyOf ?superProp  .						\n" + 
				"		FILTER (NOT EXISTS {?superProp 	rdf:domain	?type})}			\n" + 
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("res", res);
		return qb.runQuery();
	}
	
	/**
	 * Retrieves all properties having their domain on cls.
	 * Note that it has to provide only root properties (e.g. if both rdfs:label and skos:prefLabel,
	 * which is a subProperty of rdfs:label, have domain = cls, then only rdfs:label is returned)
	 * @param classUri
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForClass(@LocallyDefined Resource classUri) {
		logger.info("request to get all properties having their domain on "+classUri.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?resource 	rdf:domain	?classUri		 .						\n" +
				"     {?resource 	rdf:type	?typeOfProp  .							\n" + 
				"		FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp})}	\n" + 
				"		UNION															\n" + 
				"     {?resource rdfs:subPropertyOf ?superProp  .						\n" + 
				"		FILTER (NOT EXISTS {?superProp 	rdf:domain	?type})}			\n" + 
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("classUri", classUri);
		return qb.runQuery();
	}
	
	/**
	 * it takes any named class which is relevant in the domain of prop.
	 * Relevant means that if the domain of prop is (A or B) or (A and B) in any case the relevant domain classes
	 * are provided by a list with A and B.
	 * @param property
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantDomainClasses(@LocallyDefined Resource property) {
		logger.info("request to get any named class which is relevant in the domain of "+property.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?property 	rdfs:domain		?resource	.						\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("property", property);
		return qb.runQuery();
	}
	
	/**
	 * it takes any named class which is relevant in the range of prop.
	 * Relevant means that if the range of prop is (A or B) or (A and B) in any case the relevant domain classes
	 * are provided by a list with A and B.
	 * @param property
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, range)', 'R')")
	public Collection<AnnotatedValue<Resource>> getRelevantRangeClasses(@LocallyDefined Resource property) {
		logger.info("request to get any named class which is relevant in the range of "+property.stringValue());
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?property 	rdfs:range	?resource	.							\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("property", property);
		return qb.runQuery();
	}

	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, range)', 'R')")
	public JsonNode getRange(@LocallyDefined IRI property) {
		logger.info("request to get any named class which is relevant in the range of "+property.stringValue());
		
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		
		//first of all, check if there is a form collection that replace the standard range(s)
		boolean replace = cfManager.getReplace(getProject(), property, true);
		if(!replace) {
			TypesAndRanges typesAndRanges = getRangeOnlyClasses(property);
			
			ObjectNode rangesObjectNode = JsonNodeFactory.instance.objectNode();
			rangesObjectNode.set("type", JsonNodeFactory.instance.textNode(typesAndRanges.getTypeNormalized()));
		
			ArrayNode rangeArrayNode = JsonNodeFactory.instance.arrayNode();
			for(String range : typesAndRanges.getRangesList()){
				rangeArrayNode.add(JsonNodeFactory.instance.textNode(range));
			}
			if(!typesAndRanges.getRangesList().isEmpty()){
				rangesObjectNode.set("rangeCollection", rangeArrayNode);
			}
			response.set("ranges", rangesObjectNode);
		}
		
		//now add the part relative to the custom ranges
		FormCollection formColl = cfManager.getFormCollection(getProject(), property);
		if( formColl != null) {
			ObjectNode formCollNode = JsonNodeFactory.instance.objectNode();
			formCollNode.set("id", JsonNodeFactory.instance.textNode(formColl.getId()));
			formCollNode.set("property", JsonNodeFactory.instance.textNode(property.stringValue()));
			
			ArrayNode formsArrayNode = JsonNodeFactory.instance.arrayNode();
			Collection<CustomForm> cForms = formColl.getForms();
			for(CustomForm customForm : cForms){
				ObjectNode formObjectNode = JsonNodeFactory.instance.objectNode();
				formObjectNode.set("id", JsonNodeFactory.instance.textNode(customForm.getId()));
				formObjectNode.set("name", JsonNodeFactory.instance.textNode(customForm.getName()));
				formObjectNode.set("type", JsonNodeFactory.instance.textNode(customForm.getType()));
				formObjectNode.set("description", JsonNodeFactory.instance.textNode(customForm.getDescription()));
				formsArrayNode.add(formObjectNode);
				
			}
			formCollNode.set("forms", formsArrayNode);
			response.set("formCollection", formCollNode);
		}
			
		return response;
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public AnnotatedValue<IRI> createProperty(
			IRI propertyType, @Subject @NotLocallyDefined @Created(role=RDFResourceRole.property) IRI newProperty, @Optional IRI superProperty,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws ProjectInconsistentException, CODAException, CustomFormException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		if (!propertyType.equals(OWL.OBJECTPROPERTY) && !propertyType.equals(OWL.DATATYPEPROPERTY) &&
			!propertyType.equals(OWL.ANNOTATIONPROPERTY) && !propertyType.equals(OWL.ONTOLOGYPROPERTY) &&
			!propertyType.equals(RDF.PROPERTY)) {
			throw new IllegalArgumentException(propertyType.stringValue() + " is not a valid property type");
		}
		
		modelAdditions.add(newProperty, RDF.TYPE, propertyType);
		
		if (superProperty != null) {
			modelAdditions.add(newProperty, RDFS.SUBPROPERTYOF, superProperty);
		}
		
		RepositoryConnection repoConnection = getManagedConnection();

		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newProperty.stringValue());
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, userPromptMap, stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newProperty);
		if (propertyType.equals(OWL.OBJECTPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRole.objectProperty.name());
		} else if (propertyType.equals(OWL.DATATYPEPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRole.datatypeProperty.name());
		} else if (propertyType.equals(OWL.ANNOTATIONPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRole.annotationProperty.name());
		} else if (propertyType.equals(OWL.ONTOLOGYPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRole.ontologyProperty.name());
		} else { //rdf:Property
			annotatedValue.setAttribute("role", RDFResourceRole.property.name());
		}
		//TODO compute show
		return annotatedValue; 
	}
	
	/**
	 * Deletes a properties
	 * @param property
	 * @throws DeniedOperationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void deleteProperty(@Subject @LocallyDefined IRI property) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		
		//first check if the property has any sub property
		String query =
			// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>	\n" +
			"ASK {													\n" +
			"	[] rdfs:subPropertyOf ?property						\n" +
			"}														\n";
			// @formatter:on
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setBinding("property", property);
		if(booleanQuery.evaluate()){
			throw new DeniedOperationException(
					"Property: " + property.stringValue() + " has sub property(ies); delete them before");
		}
		
		query = 
				"DELETE {																\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		?s1 ?p1 ?property .												\n"
				+ "		?property ?p2 ?o2 .												\n"
				+ "	}																	\n"
				+ "} WHERE {															\n"
				+ "	BIND(URI('" + property.stringValue() + "') AS ?property)			\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		{ ?s1 ?p1 ?property . }											\n"
				+ "		UNION															\n"
				+ "		{ ?property ?p2 ?o2 . }											\n"
				+ "	}																	\n"
				+ "}";
		repoConnection.prepareUpdate(query).execute();;
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'C')")
	public void addSuperProperty(@LocallyDefined @Subject IRI property, @LocallyDefined IRI superProperty){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.SUBPROPERTYOF, superProperty));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'D')")
	public void removeSuperProperty(@LocallyDefined @Subject IRI property, @LocallyDefined IRI superProperty){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.SUBPROPERTYOF, superProperty));
		
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addPropertyDomain(@LocallyDefined @Subject IRI property, @LocallyDefined Resource domain){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.DOMAIN, domain));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removePropertyDomain(@LocallyDefined @Subject IRI property, @LocallyDefined Resource domain){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.DOMAIN, domain));
		
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addPropertyRange(@LocallyDefined @Subject IRI property, Resource range){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.RANGE, range));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removePropertyRange(@LocallyDefined @Subject IRI property, Resource range){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.RANGE, range));
		
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	
	// DATARANGE SERVICES
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void setDataRange(@LocallyDefined @Subject IRI property,
			@Optional @LocallyDefined @SubPropertyOf(superPropertyIRI = "http://www.w3.org/2000/01/rdf-schema#range") IRI predicate,
			List <Literal> literals) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		BNode datarange = repoConnection.getValueFactory().createBNode();
		
		if (predicate == null) {
			predicate = RDFS.RANGE;
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, predicate, datarange));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, RDF.TYPE, RDFS.DATATYPE));
		
		//if values is null or empty, create an empty RDF List
		if(literals == null || literals.isEmpty()){
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
		} else{
			BNode tempList = repoConnection.getValueFactory().createBNode();
			//add the first element to the list
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			//iteration on the other elements of the list
			for(int i=1; i<literals.size(); ++i){
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST, literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	/**
	 * It deletes all the triple representing the old datarange. It removes also the triple linking the 
	 * property and the datarange
	 * @param property
	 * @param datarange
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removeDataranges(@LocallyDefined @Subject IRI property, @LocallyDefined BNode datarange){
		RepositoryConnection repoConnection = getManagedConnection();
		//prepare a SPARQL update to remove 
		// @formatter:off
		String query="DELETE {"+
				"GRAPH ?workingGraph {\n" +
				"\n?property ?predicate ?datarange ." +
				"\n?datarange "+NTriplesUtil.toNTriplesString(RDF.TYPE)+" "+NTriplesUtil.toNTriplesString(RDFS.DATATYPE) +" ."+
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ." +
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?firstElemInList ."+
				"\n?elemInList ?p ?o ."+
				"\n}" +
				"\n}" +
				"\nWHERE{"+
				"\nGRAPH ?workingGraph {\n" +
				"\n?property ?predicate ?datarange ." +
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ." +
				//get the first element of the list
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?firstElemInList ."+
				//get all the element of the list
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"+ ?elemInList ."+
				//get all the info for regarding each element of the list (OPTIONL because the last is RDF.REST
				// which is not the subject of any triple)
				"\nOPTIONAL{" +
				"\n?elemInList ?p ?o ."+
				"\n}"+
				"\n}" +
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("?workingGraph", getWorkingGraph());
		update.setBinding("?property", property);
		update.setBinding("?datarange", datarange);
		update.execute();
	}
	
	
	/**
	 * Update the current RDF list associated to the RDFS.DATATYPE by deleting the old one and create 
	 * a new RDF:List using the input one. The BNode representing the old one is maintain and used in the 
	 * the one 
	 * @param datarange
	 * @param literals
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'U')")
	public void updateDataranges(@Subject @LocallyDefined BNode datarange, List <Literal> literals){
		RepositoryConnection repoConnection = getManagedConnection();
		//prepare a SPARQL update to remove the old list
		// @formatter:off
		String query="DELETE {"+
				"GRAPH ?workingGraph {\n" +
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ." +
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?firstElemInList ."+
				"\n?elemInList ?p ?o ."+
				"\n}" +
				"\n}" +
				"\nWHERE{"+
				"\nGRAPH ?workingGraph {\n" +
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ." +
				//get the first element of the list
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?firstElemInList ."+
				//get all the element of the list
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"+ ?elemInList ."+
				//get all the info for regarding each element of the list (OPTIONL because the last is RDF.REST
				// which is not the subject of any triple)
				"\nOPTIONAL{" +
				"\n?elemInList ?p ?o ."+
				"\n}"+
				"\n}" +
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("?workingGraph", getWorkingGraph());
		update.setBinding("?datarange", datarange);
		update.execute();
		
		//now add the new list
		Model modelAdditions = new LinkedHashModel();
		//if values is null or empty, create an empty RDF List
		if(literals == null || literals.isEmpty()){
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
		} else{
			BNode tempList = repoConnection.getValueFactory().createBNode();
			//add the first element to the list
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			//iteration on the other elements of the list
			for(int i=1; i<literals.size(); ++i){
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST, literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		}
 		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addValueToDatarange(@LocallyDefined @Subject Resource datarange, Literal literal){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		// check if the datarange has no associated values
		if(repoConnection.hasStatement(datarange, OWL.ONEOF, RDF.NIL, false, getUserNamedGraphs())){
			// the datarange has no associated value
			modelRemovals.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			//create a list, with just one element (the input literal)
			BNode tempList = repoConnection.getValueFactory().createBNode();
			//add the first element to the list
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literal));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		} else{
			// the datarange has at least one associated value, so obtain from list the last element
			// @formatter:off
			String query = "SELECT ?lastElemOfList" +
					"\nWHERE{"+
					"\nGRAPH ?g{"+
					"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
					"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"* ?lastElemOfList ."+
					"\n?lastElemOfList "+NTriplesUtil.toNTriplesString(RDF.REST)+" " +NTriplesUtil.toNTriplesString(RDF.NIL)+" ."+ 
					"\n}"+
					"\n}";
			// @formatter:on
			TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("g", getWorkingGraph());
			tupleQuery.setBinding("datarange", datarange);
			try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
				Resource lastElemOfList = (Resource) tupleQueryResult.next().getValue("lastElemOfList");
				//remove the old rest (RDF.NIL)
				modelRemovals.add(repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, RDF.NIL));
				//add the neew value after the old last one
				BNode tempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, tempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literal));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addValuesToDatarange(@LocallyDefined @Subject Resource datarange, 
			@LocallyDefinedResources List<Literal> literals){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		if(literals.isEmpty()){
			throw new IllegalArgumentException("the literals list cannot be null");
		}
		
		// check if the datarange has no associated values
		if(repoConnection.hasStatement(datarange, OWL.ONEOF, RDF.NIL, false, getUserNamedGraphs())){
			// the datarange has no associated value
			modelRemovals.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			//create a list, with just one element (the input literal)
			BNode tempList = repoConnection.getValueFactory().createBNode();
			//add the first element to the list
			modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			
			//iteration on the other elements of the list
			for(int i=1; i<literals.size(); ++i){
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST, literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		} else{
			// the datarange has at least one associated value, so obtain from list the last element
			// @formatter:off
			String query = "SELECT ?lastElemOfList" +
					"\nWHERE{"+
					"\nGRAPH ?g{"+
					"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
					"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"* ?lastElemOfList ."+
					"\n?lastElemOfList "+NTriplesUtil.toNTriplesString(RDF.REST)+" " +NTriplesUtil.toNTriplesString(RDF.NIL)+" ."+ 
					"\n}"+
					"\n}";
			// @formatter:on
			TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("g", getWorkingGraph());
			tupleQuery.setBinding("datarange", datarange);
			try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
				Resource lastElemOfList = (Resource) tupleQueryResult.next().getValue("lastElemOfList");
				//remove the old rest (RDF.NIL)
				modelRemovals.add(repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, RDF.NIL));
				//add the neew value after the old last one
				BNode tempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, tempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
				
				//iteration on the other elements of the list
				for(int i=1; i<literals.size(); ++i){
					BNode newTempList = repoConnection.getValueFactory().createBNode();
					modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
					modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST, literals.get(i)));
					tempList = newTempList;
				}
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Boolean hasValueInDatarange(@LocallyDefined @Subject Resource datarange, Literal literal){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?desiredElem" +
					"\nWHERE{"+
					"\nGRAPH ?g{"+
					"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
					"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+ "* ?desiredElem ."+
					"\n?desiredElem "+NTriplesUtil.toNTriplesString(RDF.FIRST)+" "+NTriplesUtil.toNTriplesString(literal)+" ."+
					"\n}"+
					"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("g", getWorkingGraph());
		tupleQuery.setBinding("datarange", datarange);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
			if(tupleQueryResult.hasNext()){
				return true;
			} else{
				return false;
			}
		}
		
		
	}
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removeValueFromDatarange(@LocallyDefined @Subject Resource datarange, Literal literal){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		//check if the desired element is the first one in the list
		String query = "SELECT ?list ?next"+
				"\nWHERE{"+
				"\nGRAPH ?g{"+
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.FIRST)+" ?literal ." +
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?next ." +
				"\n}"+
				"\n}";
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("g", getWorkingGraph());
		tupleQuery.setBinding("datange", datarange);
		tupleQuery.setBinding("literal", literal);
		boolean found = false;
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
			if(tupleQueryResult.hasNext()){
				found = true;
				BindingSet bindingSet = tupleQueryResult.next();
				Resource list = (Resource) bindingSet.getValue("list");
				Resource next = (Resource) bindingSet.getValue("next");
				modelRemovals.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, list));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.TYPE, RDF.LIST));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.FIRST, literal));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.REST, next));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			} 
		}
		
		if(!found){
			//the desired element is not the one in the list, so search in all the list
			query = "SELECT ?list ?next"+
					"\nWHERE{"+
					"\nGRAPH ?g{"+
					"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
					"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"* ?prevElem ." +
					"\n?prevElem "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?desiredElem ." +
					"\n?desiredElem "+NTriplesUtil.toNTriplesString(RDF.FIRST)+" ?literal ." +
					"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?next ." +
					"\n}"+
					"\n}";
			tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("g", getWorkingGraph());
			tupleQuery.setBinding("datange", datarange);
			tupleQuery.setBinding("literal", literal);
			try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
				if(tupleQueryResult.hasNext()){
					found = true;
					BindingSet bindingSet = tupleQueryResult.next();
					Resource desiredElem = (Resource) bindingSet.getValue("desiredElem");
					Resource next = (Resource) bindingSet.getValue("next");
					Resource prevElem = (Resource) bindingSet.getValue("prevElem");
					
					modelRemovals.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST, desiredElem));
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElem, RDF.TYPE, RDF.LIST));
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElem, RDF.FIRST, literal));
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElem, RDF.REST, next));
					modelAdditions.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST, next));
				}
				else{
					throw new IllegalArgumentException("the literals cannot be found in the datarange");
				}
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Literal>> getDatarangeLiterals(BNode datarange){
		logger.info("getLiteralEnumeration");
		Collection<AnnotatedValue<Literal>> literalList = new ArrayList<AnnotatedValue<Literal>>();
		
		DataRangeDataOneOf dataOneOf = null;
		DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(datarange, getManagedConnection());
		if(dataRangeAbstract instanceof DataRangeDataOneOf){
			dataOneOf = (DataRangeDataOneOf) dataRangeAbstract;
		} else{
			//There was an error, since the bnode is not the expected datarange (ONEOF)
			//TODO decide what to do, at the moment, return an empty list
			return literalList;
		}
		
		List<Literal> literalTempList = dataOneOf.getLiteralList();
		
		for(Literal literal : literalTempList){
			literalList.add(new AnnotatedValue<Literal>(literal));
		}
		
		return literalList;
	}
	
	
	protected TypesAndRanges getRangeOnlyClasses(IRI property){
		
		String selectQuery =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
	            "SELECT ?type ?range \n " +
				"WHERE {\n" +
	            "<"+property.stringValue()+"> rdf:type ?type . \n" +
				"<"+property.stringValue()+"> rdfs:subPropertyOf* ?superProperty  \n" +
	            "OPTIONAL { \n " +
	            "?superProperty  rdfs:range ?range \n" +
	            "} \n " +
				"}";
		TupleQuery select = getManagedConnection().prepareTupleQuery(selectQuery);
		select.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = select.evaluate();
		TypesAndRanges typesAndRanges = new TypesAndRanges();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value valueType = bindingSet.getBinding("type").getValue();
			if(valueType instanceof IRI){
				typesAndRanges.addType(valueType.stringValue());
			}
			if(bindingSet.hasBinding("range")){
				Value valueRange = bindingSet.getBinding("range").getValue();
				if(valueRange instanceof IRI){
					typesAndRanges.addRange(valueRange.stringValue());
				}
			}
		}
		tupleQueryResult.close();
		return typesAndRanges;
		
	}
	
}

class TypesAndRanges {
	private List <String> typesList;
	private List <String> rangesList;

	public TypesAndRanges() {
		typesList = new ArrayList<String >();
		rangesList = new ArrayList<String>();
	}
	
	public void addType(String type){
		if(!typesList.contains(type)){
			typesList.add(type);
		}
	}
	
	public void addRange(String range){
		if(!rangesList.contains(range)){
			rangesList.add(range);
		}
	}
	
	public List<String> getTypesList(){
		return typesList;
	}
	
	public String getTypeNormalized(){
		if(typesList.contains(OWL.OBJECTPROPERTY.stringValue())){
			return "resource";
		} else if(typesList.contains(OWL.DATATYPEPROPERTY.stringValue())){
			if(rangesList.isEmpty()){
				return "literal";
			} else{
				return "typedLiteral";
			}
		} else {
			if(rangesList.isEmpty()){
				return "undetermined";
			} else{
				return "resource";
			}
		}
	}
	
	public List<String> getRangesList() {
		return rangesList;
	}
	
	 
}

class PropertiesMoreProcessor implements QueryBuilderProcessor {

	public static final PropertiesMoreProcessor INSTANCE = new PropertiesMoreProcessor();
	private GraphPattern graphPattern;

	private PropertiesMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE)
				.prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern("BIND( EXISTS {?aSubProperty rdfs:subPropertyOf ?resource . } AS ?attr_more )  \n").graphPattern();
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