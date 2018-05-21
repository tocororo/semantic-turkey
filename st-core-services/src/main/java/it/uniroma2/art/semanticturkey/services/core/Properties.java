package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
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
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
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
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.versioning.VersioningMetadataSupport;

/**
 * This class provides services for reading the Properties.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Properties extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Properties.class);

	@Autowired
	private CustomFormManager cfManager;

	/**
	 * returns all root properties
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopProperties() {
		logger.debug("request to get all the top properties");

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
				" 	{ ?resource 	rdf:type rdf:Property .}								\n" +
				"	UNION																	\n" +
				" 	{ ?resource 	rdf:type owl:ObjectProperty .}							\n" +
				"	UNION																	\n" +
				" 	{ ?resource 	rdf:type owl:DatatypeProperty .}						\n" +
				"	UNION																	\n" +
				" 	{ ?resource 	rdf:type owl:AnnotationProperty .}						\n" +
				"	UNION																	\n" +
				" 	{ ?resource 	rdf:type owl:OntologyProperty .}						\n" +
				"	FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);

		// OLD VERSION
		/*qb = createQueryBuilder(
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
		);*/
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	/**
	 * returns all root RDF properties
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopRDFProperties() {
		logger.debug("request to get the top RDF properties");

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
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(objectProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopObjectProperties() {
		logger.debug("request to get the top Object properties");

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
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatypeProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopDatatypeProperties() {
		logger.debug("request to get the top Datatype properties");

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
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(annotationProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopAnnotationProperties() {
		logger.debug("request to get the top Annotation properties");

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
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontologyProperty, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopOntologyProperties() {
		logger.debug("request to get the top Ontology properties");

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
	 * takes a list of Properties and return their description as if they were roots for a tree (so more,
	 * role, explicit etc...)
	 * 
	 * @param propList
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getPropertiesInfo(IRI[] propList) {
		logger.debug("request to get the Propery info, given a property List");

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
		logger.debug("request to get the top sub properties for " + superProperty.stringValue());

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
		logger.debug("request to get the top super properties for " + subProperty.stringValue());

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
	 * Retrieves all types of res, then all properties having their domain on any of the types for res. Note
	 * that it provides only root properties (e.g. if both rdfs:label and skos:prefLabel, which is a
	 * subProperty of rdfs:label, have domain = one of the types of res, then only rdfs:label is returned)
	 * 
	 * @param res
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForResource(
			@LocallyDefined Resource res) {
		logger.debug("request to get the top all properties having their domain on any of the types for "
				+ res.stringValue());

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
	 * Retrieves all properties having their domain on cls. Note that it has to provide only root properties
	 * (e.g. if both rdfs:label and skos:prefLabel, which is a subProperty of rdfs:label, have domain = cls,
	 * then only rdfs:label is returned)
	 * 
	 * @param classUri
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForClass(
			@LocallyDefined Resource classUri) {
		logger.debug("request to get all properties having their domain on " + classUri.stringValue());

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
	 * it takes any named class which is relevant in the domain of prop. Relevant means that if the domain of
	 * prop is (A or B) or (A and B) in any case the relevant domain classes are provided by a list with A and
	 * B.
	 * 
	 * @param property
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantDomainClasses(@LocallyDefined Resource property) {
		logger.debug("request to get any named class which is relevant in the domain of "
				+ property.stringValue());

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
	 * it takes any named class which is relevant in the range of prop. Relevant means that if the range of
	 * prop is (A or B) or (A and B) in any case the relevant domain classes are provided by a list with A and
	 * B.
	 * 
	 * @param property
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, range)', 'R')")
	public Collection<AnnotatedValue<Resource>> getRelevantRangeClasses(@LocallyDefined Resource property) {
		logger.debug(
				"request to get any named class which is relevant in the range of " + property.stringValue());
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
	public JsonNode getRange(IRI property) {
		logger.debug(
				"request to get any named class which is relevant in the range of " + property.stringValue());

		ObjectNode response = JsonNodeFactory.instance.objectNode();
		// first of all, check if there is a form collection that replace the standard range(s)
		boolean replace = cfManager.getReplace(getProject(), property, true);
		if (!replace) {
			TypesAndRanges typesAndRanges = getRangeOnlyClasses(property);

			ObjectNode rangesObjectNode = JsonNodeFactory.instance.objectNode();
			rangesObjectNode.set("type",
					JsonNodeFactory.instance.textNode(typesAndRanges.getTypeNormalized()));

			ArrayNode rangeArrayNode = JsonNodeFactory.instance.arrayNode();
			for (Range range : typesAndRanges.getRanges()) {
				rangeArrayNode.add(range.toJsonNode());
			}
			if (!typesAndRanges.getRanges().isEmpty()) {
				rangesObjectNode.set("rangeCollection", rangeArrayNode);
			}
			response.set("ranges", rangesObjectNode);
		}

		// now add the part relative to the custom ranges
		FormCollection formColl = cfManager.getFormCollection(getProject(), property);
		if (formColl != null) {
			ObjectNode formCollNode = JsonNodeFactory.instance.objectNode();
			formCollNode.set("id", JsonNodeFactory.instance.textNode(formColl.getId()));
			formCollNode.set("property", JsonNodeFactory.instance.textNode(property.stringValue()));

			ArrayNode formsArrayNode = JsonNodeFactory.instance.arrayNode();
			Collection<CustomForm> cForms = formColl.getForms();
			for (CustomForm customForm : cForms) {
				ObjectNode formObjectNode = JsonNodeFactory.instance.objectNode();
				formObjectNode.set("id", JsonNodeFactory.instance.textNode(customForm.getId()));
				formObjectNode.set("name", JsonNodeFactory.instance.textNode(customForm.getName()));
				formObjectNode.set("type", JsonNodeFactory.instance.textNode(customForm.getType()));
				formObjectNode.set("description",
						JsonNodeFactory.instance.textNode(customForm.getDescription()));
				formsArrayNode.add(formObjectNode);

			}
			formCollNode.set("forms", formsArrayNode);
			response.set("formCollection", formCollNode);
		}

		return response;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property, range)', 'R')")
	public Boolean areSubPropertiesUsed(@LocallyDefined IRI property) {
		logger.debug("request to check whether at least one of the subproperties of the property "
				+ property.stringValue() + " is used");

		ObjectNode response = JsonNodeFactory.instance.objectNode();

		String query = "ASK" + "\nWHERE{" + "\n ?subProp <"
				+ org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF.stringValue() + ">+ <"
				+ property.stringValue() + "> ." + "\n ?subj ?subProp ?obj ." + "\n}";

		BooleanQuery booleanQuery = getManagedConnection().prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		boolean result = booleanQuery.evaluate();

		return result;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public AnnotatedValue<IRI> createProperty(IRI propertyType,
			@NotLocallyDefined @Created(role = RDFResourceRole.property) IRI newProperty,
			@Optional IRI superProperty, @Optional CustomFormValue customFormValue)
			throws ProjectInconsistentException, CODAException, CustomFormException {

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (!propertyType.equals(OWL.OBJECTPROPERTY) && !propertyType.equals(OWL.DATATYPEPROPERTY)
				&& !propertyType.equals(OWL.ANNOTATIONPROPERTY) && !propertyType.equals(OWL.ONTOLOGYPROPERTY)
				&& !propertyType.equals(RDF.PROPERTY)) {
			throw new IllegalArgumentException(propertyType.stringValue() + " is not a valid property type");
		}

		modelAdditions.add(newProperty, RDF.TYPE, propertyType);

		if (superProperty != null) {
			modelAdditions.add(newProperty, RDFS.SUBPROPERTYOF, superProperty);
		}

		RepositoryConnection repoConnection = getManagedConnection();

		// CustomForm further info
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newProperty.stringValue());
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm,
					customFormValue.getUserPromptMap(), stdForm);
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
		} else { // rdf:Property
			annotatedValue.setAttribute("role", RDFResourceRole.property.name());
		}
		annotatedValue.setAttribute("explicit", true);

		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newProperty,
				RDFResourceRole.valueOf(annotatedValue.getAttributes().get("role").stringValue()));

		return annotatedValue;
	}

	/**
	 * Deletes a properties
	 * 
	 * @param property
	 * @throws DeniedOperationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void deleteProperty(@LocallyDefined IRI property) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();

		// first check if the property has any sub property
		String query =
		// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>	\n" +
			"ASK {													\n" +
			"	[] rdfs:subPropertyOf ?property						\n" +
			"}														\n";
			// @formatter:on
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setBinding("property", property);
		if (booleanQuery.evaluate()) {
			throw new DeniedOperationException(
					"Property: " + property.stringValue() + " has sub property(ies); delete them before");
		}

		query =
		// @formatter:off
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
				// @formatter:on
		repoConnection.prepareUpdate(query).execute();
		;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'C')")
	public void addEquivalentProperty(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			@LocallyDefined IRI equivalentProperty,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/2002/07/owl#equivalentProperty") @Optional(defaultValue = "<http://www.w3.org/2002/07/owl#equivalentProperty>") IRI linkingPredicate) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, linkingPredicate,
				equivalentProperty));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'D')")
	public void removeEquivalentProperty(
			@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property, IRI equivalentProperty,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/2002/07/owl#equivalentProperty") @Optional(defaultValue = "<http://www.w3.org/2002/07/owl#equivalentProperty>") IRI linkingPredicate) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, linkingPredicate,
				equivalentProperty));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'C')")
	public void addSuperProperty(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			@LocallyDefined IRI superProperty) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.SUBPROPERTYOF,
				superProperty));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property, taxonomy)', 'D')")
	public void removeSuperProperty(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			IRI superProperty) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.SUBPROPERTYOF,
				superProperty));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addPropertyDomain(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			@LocallyDefined Resource domain) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.DOMAIN, domain));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removePropertyDomain(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			Resource domain) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.DOMAIN, domain));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addPropertyRange(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			Resource range) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, RDFS.RANGE, range));

		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removePropertyRange(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			Resource range) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();

		modelRemovals.add(repoConnection.getValueFactory().createStatement(property, RDFS.RANGE, range));

		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	// DATARANGE SERVICES

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void setDataRange(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			@Optional @LocallyDefined @SubPropertyOf(superPropertyIRI = "http://www.w3.org/2000/01/rdf-schema#range") IRI predicate,
			List<Literal> literals) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();

		BNode datarange = repoConnection.getValueFactory().createBNode();

		if (predicate == null) {
			predicate = RDFS.RANGE;
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(property, predicate, datarange));
		modelAdditions
				.add(repoConnection.getValueFactory().createStatement(datarange, RDF.TYPE, RDFS.DATATYPE));

		// if values is null or empty, create an empty RDF List
		if (literals == null || literals.isEmpty()) {
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
		} else {
			BNode tempList = repoConnection.getValueFactory().createBNode();
			// add the first element to the list
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(
					repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			// iteration on the other elements of the list
			for (int i = 1; i < literals.size(); ++i) {
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(newTempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST,
						literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	/**
	 * It deletes all the triple representing the old datarange. It removes also the triple linking the
	 * property and the datarange
	 * 
	 * @param property
	 * @param datarange
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removeDataranges(@LocallyDefined @Modified(role = RDFResourceRole.property) IRI property,
			@LocallyDefined BNode datarange) {
		RepositoryConnection repoConnection = getManagedConnection();

		removeDatarangesTriples(repoConnection, property, datarange);
	}

	private void removeDatarangesTriples(RepositoryConnection repoConnection, IRI property, BNode datarange) {
		// prepare a SPARQL update to remove
		// @formatter:off 
		String query="DELETE {"+
				"\nGRAPH ?workingGraph {";
		if(property!=null){
			query+= "\n?property ?predicate ?datarange ." +
			"\n?datarange "+NTriplesUtil.toNTriplesString(RDF.TYPE)+" "+NTriplesUtil.toNTriplesString(RDFS.DATATYPE) +" .";
			
		}
		query+="\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
				"\n?elemInList ?p ?o ."+
				"\n}" +
				"\n}" +
				"\nWHERE{"+
				"\nGRAPH ?workingGraph {";
		if(property!=null) {
			query+="\n?predicate "+NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF)+"* "+NTriplesUtil.toNTriplesString(RDFS.RANGE)+" ."+ 
				"\n?property ?predicate ?datarange .";
		}
		query+="\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ." +
				//get all the element of the list (including the list itself since it is an element as well)
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+"* ?elemInList ." +
				// which is not the subject of any triple)
				"\nOPTIONAL{" +
				"\n?elemInList ?p ?o ."+
				"\n}"+
				"\n}" +
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("workingGraph", getWorkingGraph());
		if (property != null) {
			update.setBinding("property", property);
		}
		update.setBinding("datarange", datarange);
		update.execute();
	}

	/**
	 * Update the current RDF list associated to the RDFS.DATATYPE by deleting the old one and create a new
	 * RDF:List using the input one. The BNode representing the old one is maintain and used in the the one
	 * 
	 * @param datarange
	 * @param literals
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'U')")
	public void updateDataranges(@Modified(role = RDFResourceRole.property) @LocallyDefined BNode datarange,
			List<Literal> literals) {
		RepositoryConnection repoConnection = getManagedConnection();

		// first remove the old datarange
		removeDatarangesTriples(repoConnection, null, datarange);

		// now add the new list
		Model modelAdditions = new LinkedHashModel();
		// if values is null or empty, create an empty RDF List
		if (literals == null || literals.isEmpty()) {
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
		} else {
			BNode tempList = repoConnection.getValueFactory().createBNode();
			// add the first element to the list
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(
					repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			// iteration on the other elements of the list
			for (int i = 1; i < literals.size(); ++i) {
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(newTempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST,
						literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
	}

	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addValueToDatarange(
			@LocallyDefined @Modified(role = RDFResourceRole.property) Resource datarange, Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		// check if the datarange has no associated values
		if (repoConnection.hasStatement(datarange, OWL.ONEOF, RDF.NIL, false, getUserNamedGraphs())) {
			// the datarange has no associated value
			modelRemovals
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			// create a list, with just one element (the input literal)
			BNode tempList = repoConnection.getValueFactory().createBNode();
			// add the first element to the list
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literal));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		} else {
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
			try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
				Resource lastElemOfList = (Resource) tupleQueryResult.next().getValue("lastElemOfList");
				// remove the old rest (RDF.NIL)
				modelRemovals.add(
						repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, RDF.NIL));
				// add the neew value after the old last one
				BNode tempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, tempList));
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literal));
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'C')")
	public void addValuesToDatarange(
			@LocallyDefined @Modified(role = RDFResourceRole.property) Resource datarange,
			@LocallyDefinedResources List<Literal> literals) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (literals.isEmpty()) {
			throw new IllegalArgumentException("the literals list cannot be null");
		}

		// check if the datarange has no associated values
		if (repoConnection.hasStatement(datarange, OWL.ONEOF, RDF.NIL, false, getUserNamedGraphs())) {
			// the datarange has no associated value
			modelRemovals
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			// create a list, with just one element (the input literal)
			BNode tempList = repoConnection.getValueFactory().createBNode();
			// add the first element to the list
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions
					.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(
					repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));

			// iteration on the other elements of the list
			for (int i = 1; i < literals.size(); ++i) {
				BNode newTempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList, RDF.FIRST,
						literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		} else {
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
			try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
				Resource lastElemOfList = (Resource) tupleQueryResult.next().getValue("lastElemOfList");
				// remove the old rest (RDF.NIL)
				modelRemovals.add(
						repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, RDF.NIL));
				// add the neew value after the old last one
				BNode tempList = repoConnection.getValueFactory().createBNode();
				modelAdditions.add(
						repoConnection.getValueFactory().createStatement(lastElemOfList, RDF.REST, tempList));
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.FIRST,
						literals.get(0)));

				// iteration on the other elements of the list
				for (int i = 1; i < literals.size(); ++i) {
					BNode newTempList = repoConnection.getValueFactory().createBNode();
					modelAdditions.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST,
							newTempList));
					modelAdditions.add(repoConnection.getValueFactory().createStatement(newTempList,
							RDF.FIRST, literals.get(i)));
					tempList = newTempList;
				}
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Boolean hasValueInDatarange(@LocallyDefined Resource datarange, Literal literal) {
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
		try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			if (tupleQueryResult.hasNext()) {
				return true;
			} else {
				return false;
			}
		}

	}

	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'D')")
	public void removeValueFromDatarange(
			@LocallyDefined @Modified(role = RDFResourceRole.property) Resource datarange, Literal literal) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		// check if the desired element is the first one in the list
		// @formatter:off
		String query = "SELECT ?list ?next"+
				"\nWHERE{"+
				"\nGRAPH ?g{"+
				"\n?datarange "+NTriplesUtil.toNTriplesString(OWL.ONEOF)+" ?list ."+
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.FIRST)+" ?literal ." +
				"\n?list "+NTriplesUtil.toNTriplesString(RDF.REST)+" ?next ." +
				"\n}"+
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("g", getWorkingGraph());
		tupleQuery.setBinding("datange", datarange);
		tupleQuery.setBinding("literal", literal);
		boolean found = false;
		try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			if (tupleQueryResult.hasNext()) {
				found = true;
				BindingSet bindingSet = tupleQueryResult.next();
				Resource list = (Resource) bindingSet.getValue("list");
				Resource next = (Resource) bindingSet.getValue("next");
				modelRemovals
						.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, list));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.TYPE, RDF.LIST));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.FIRST, literal));
				modelRemovals.add(repoConnection.getValueFactory().createStatement(list, RDF.REST, next));
				modelAdditions
						.add(repoConnection.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
			}
		}

		if (!found) {
			// the desired element is not the one in the list, so search in all the list
			// @formatter:off
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
			// @formatter:on
			tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("g", getWorkingGraph());
			tupleQuery.setBinding("datange", datarange);
			tupleQuery.setBinding("literal", literal);
			try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
				if (tupleQueryResult.hasNext()) {
					found = true;
					BindingSet bindingSet = tupleQueryResult.next();
					Resource desiredElem = (Resource) bindingSet.getValue("desiredElem");
					Resource next = (Resource) bindingSet.getValue("next");
					Resource prevElem = (Resource) bindingSet.getValue("prevElem");

					modelRemovals.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST,
							desiredElem));
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElem, RDF.TYPE,
							RDF.LIST));
					modelRemovals.add(repoConnection.getValueFactory().createStatement(desiredElem, RDF.FIRST,
							literal));
					modelRemovals.add(
							repoConnection.getValueFactory().createStatement(desiredElem, RDF.REST, next));
					modelAdditions
							.add(repoConnection.getValueFactory().createStatement(prevElem, RDF.REST, next));
				} else {
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
	public Collection<AnnotatedValue<Literal>> getDatarangeLiterals(BNode datarange) {
		logger.debug("getLiteralEnumeration");
		Collection<AnnotatedValue<Literal>> literalList = new ArrayList<AnnotatedValue<Literal>>();

		DataRangeDataOneOf dataOneOf = null;
		DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(datarange,
				getManagedConnection());
		if (dataRangeAbstract instanceof DataRangeDataOneOf) {
			dataOneOf = (DataRangeDataOneOf) dataRangeAbstract;
		} else {
			// There was an error, since the bnode is not the expected datarange (ONEOF)
			// TODO decide what to do, at the moment, return an empty list
			return literalList;
		}

		List<Literal> literalTempList = dataOneOf.getLiteralList();

		for (Literal literal : literalTempList) {
			literalList.add(new AnnotatedValue<Literal>(literal));
		}

		return literalList;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getInverseProperties(List<IRI> properties) {
		//@formatter:off
		String q = " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ " \nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ " \nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ " \nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> "
				+ " \nPREFIX owl: <http://www.w3.org/2002/07/owl#>	"
				+ "\nSELECT DISTINCT ?resource ?attr_inverseOf " + generateNatureSPARQLSelectPart()
				+ "\nWHERE {\n"  
				+ "\n?resource <" + OWL.INVERSEOF + ">|^<"+ OWL.INVERSEOF +"> ?attr_inverseOf ."
				+ "\nFILTER(";
		boolean first = true;
		for (IRI prop : properties) {
			if (!first) {
				q += " || ";
			} else {
				first = false;
			}
			q += "?attr_inverseOf = <" + prop.stringValue() + ">";
		}
		q += ")"
				// adding the nature in the query (will be replaced by the appropriate processor),
				// remember to change the SELECT as well
				+ generateNatureSPARQLWherePart("?resource") + "\n}" + 
				"\nGROUP BY ?resource ?attr_inverseOf";
		//@formatter:on
		logger.debug("query [getInverseProperties]:\n" + q);
		QueryBuilder qb = createQueryBuilder(q);
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	protected TypesAndRanges getRangeOnlyClasses(IRI property) {

		String selectQuery =
		// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
			"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
			"SELECT ?type ?range \n " +
			"WHERE {\n" +
				"<" + property.stringValue() + "> rdf:type ?type . \n" +
				"<" + property.stringValue() + "> rdfs:subPropertyOf* ?superProperty  \n" +
				"OPTIONAL { \n " +
					"{\n" +
					"?superProperty  rdfs:range ?range . \n" +
					"FILTER isIRI(?range) \n" +
					"}\n" +
					"UNION\n" +
					"{\n" +
					"?superProperty  rdfs:range ?range . \n" +
					"FILTER isBlank(?range)\n"+
					"?range a <" + RDFS.DATATYPE +"> . \n" +
					"?range <" + OWL.ONEOF +"> ?oneOf . \n" +
					"}\n" +
				"} \n " +
			"}";
			// @formatter:on
		TupleQuery select = getManagedConnection().prepareTupleQuery(selectQuery);
		select.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = select.evaluate();
		TypesAndRanges typesAndRanges = new TypesAndRanges();

		Set<BNode> enumeratedDatatypes = new HashSet<>();

		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			Value valueType = bindingSet.getBinding("type").getValue();
			if (valueType instanceof IRI) {
				typesAndRanges.addType(valueType.stringValue());
			}
			if (bindingSet.hasBinding("range")) {
				Value valueRange = bindingSet.getBinding("range").getValue();
				if (valueRange instanceof IRI) {
					typesAndRanges.addRange(new IRIRange((IRI) valueRange));
				} else if (valueRange instanceof BNode) {
					enumeratedDatatypes.add((BNode) valueRange);
				}
			}
		}

		for (BNode anEnum : enumeratedDatatypes) {
			DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(anEnum,
					getManagedConnection());
			if (dataRangeAbstract instanceof DataRangeDataOneOf) {
				typesAndRanges.addRange(
						new EnumeratedDatatype(((DataRangeDataOneOf) dataRangeAbstract).getLiteralList()));
			} else {
				// An error occurred
			}
		}
		tupleQueryResult.close();

		// If the desired property has only a range, then do a specific check, since its normalizedType
		// could be a 'literal', in particular set it as 'isLiteral' in one of the following conditions:
		// range with namespace == XMLSchema namespace
		// range being a rdfs:subClassOf rdfs:Literal
		// range being an instance of rdfs:Datatype
		if (typesAndRanges.getRanges() != null && typesAndRanges.getRanges().size() == 1) {
			Range range = typesAndRanges.getRanges().iterator().next();
			if (range instanceof IRIRange) {
				IRIRange iriRange = (IRIRange) range;
				IRI rangeIRI = iriRange.getIRI();
				if (rangeIRI.getNamespace().equals(XMLSchema.NAMESPACE)) {
					// range with namespace == XMLSchema namespace
					typesAndRanges.setIsLiteral(true);
				} else {
					// do an ASK SPARQL to see if the iriRange is a subClass of rdfs:Literal
					String query = "ASK WHERE{" + "\n{" +
					// range being a rdfs:subClassOf rdfs:Literal
							"\n" + NTriplesUtil.toNTriplesString(rangeIRI) + " "
							+ NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
							+ NTriplesUtil.toNTriplesString(RDFS.LITERAL) + " " + "\n}" + "\nUNION" + "\n{" +
							// range being an instance of rdfs:Datatype
							"\n" + NTriplesUtil.toNTriplesString(rangeIRI) + " "
							+ NTriplesUtil.toNTriplesString(RDF.TYPE) + "* "
							+ NTriplesUtil.toNTriplesString(RDFS.DATATYPE) + " " + "\n}" +

							"\n}";
					BooleanQuery booleanQuery = getManagedConnection().prepareBooleanQuery(query);
					booleanQuery.setIncludeInferred(false);
					typesAndRanges.setIsLiteral(booleanQuery.evaluate());
				}
			}
		}

		return typesAndRanges;

	}

}

abstract class Range {
	abstract public JsonNode toJsonNode();
}

class IRIRange extends Range {
	private IRI iri;

	public IRIRange(IRI iri) {
		this.iri = iri;
	}

	public IRI getIRI() {
		return iri;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;

		if (!this.getClass().equals(obj.getClass()))
			return false;

		return iri.equals(((IRIRange) obj).iri);
	}

	@Override
	public int hashCode() {
		return iri.hashCode();
	}

	@Override
	public JsonNode toJsonNode() {
		return JsonNodeFactory.instance.textNode(iri.stringValue());
	}
}

class EnumeratedDatatype extends Range {
	private List<Literal> literals;

	public EnumeratedDatatype(List<Literal> literals) {
		this.literals = literals;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;

		if (!this.getClass().equals(obj.getClass()))
			return false;

		return literals.equals(((EnumeratedDatatype) obj).literals);
	}

	@Override
	public int hashCode() {
		return literals.hashCode();
	}

	@Override
	public JsonNode toJsonNode() {

		ArrayNode literalsArray = JsonNodeFactory.instance.arrayNode();
		for (Literal aLit : literals) {
			literalsArray.addPOJO(new AnnotatedValue<Literal>(aLit));
		}
		ObjectNode enumObj = JsonNodeFactory.instance.objectNode();
		enumObj.set("oneOf", literalsArray);
		return enumObj;
	}

}

class TypesAndRanges {
	private Set<String> types;
	private Set<Range> ranges;

	private boolean isLiteral = false;

	public TypesAndRanges() {
		types = new HashSet<>();
		ranges = new HashSet<>();
	}

	public void addType(String type) {
		types.add(type); // deduplication is performed by the set class
	}

	public void setIsLiteral(boolean isLiteral) {
		this.isLiteral = isLiteral;
	}

	public void addRange(Range range) {
		ranges.add(range);
	}

	public Set<String> getTypes() {
		return types;
	}

	public String getTypeNormalized() {

		// check if it was said that it type is literal, in this case no other checks are required,
		// return immediately 'literal'
		if (isLiteral) {
			return "literal";
		}

		// since its type was not set to be literal, do the other checks
		if (types.contains(OWL.OBJECTPROPERTY.stringValue())) {
			return "resource";
		} else if (types.contains(OWL.DATATYPEPROPERTY.stringValue())) {
			if (ranges.isEmpty()) {
				return "literal";
			} else {
				return "typedLiteral";
			}
		} else {
			if (ranges.isEmpty()) {
				return "undetermined";
			} else {
				return "resource";
			}
		}
	}

	public Set<Range> getRanges() {
		return ranges;
	}

}

class PropertiesMoreProcessor implements QueryBuilderProcessor {

	public static final PropertiesMoreProcessor INSTANCE = new PropertiesMoreProcessor();
	private GraphPattern graphPattern;

	private PropertiesMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE)
				.prefix("rdfs", RDFS.NAMESPACE).projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern("BIND( EXISTS {?aSubProperty rdfs:subPropertyOf ?resource . } AS ?attr_more )  \n")
				.graphPattern();
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