package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.FormCollection;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	?propertyType	.							\n" +
				"     FILTER (?propertyType = rdf:Property || " +
				"				?propertyType = owl:ObjectProperty ||" +
				"				?propertyType = owl:DatatypeProperty ||" +
				"				?propertyType = owl:AnnotationProperty ||" +
				"				?propertyType = owl:OntologyProperty  )						\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	rdf:Property	.							\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:ObjectProperty	.						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:DatatypeProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:AnnotationProperty  .					\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:OntologyProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     VALUES(?resource) {");
		for(int i=0; i<propList.length; ++i){
			sb.append("(<"+propList[i].stringValue()+">)");
		}
		sb.append("}													 				\n" +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>								\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>											\n" +
                "																						\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdfs:subPropertyOf	?superProperty .								\n" +
				" }																						\n" +
				" GROUP BY ?resource															\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
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
	public AnnotatedValue<IRI> createProperty(
			@SubClassOf(superClassIRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property") IRI propertyType,
			@NotLocallyDefined IRI newProperty, @Optional IRI superProperty,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws ProjectInconsistentException, CODAException, CustomFormException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		if (!propertyType.equals(OWL.OBJECTPROPERTY) || !propertyType.equals(OWL.DATATYPEPROPERTY) ||
			!propertyType.equals(OWL.ANNOTATIONPROPERTY) || !propertyType.equals(OWL.ONTOLOGYPROPERTY) ||
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
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newProperty);
		if (propertyType.equals(OWL.OBJECTPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.objectProperty.name());
		} else if (propertyType.equals(OWL.DATATYPEPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.datatypeProperty.name());
		} else if (propertyType.equals(OWL.ANNOTATIONPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.annotationProperty.name());
		} else if (propertyType.equals(OWL.ONTOLOGYPROPERTY)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.ontologyProperty.name());
		} else { //rdf:Property
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.property.name());
		}
		//TODO compute show
		return annotatedValue; 
	}
	
	/**
	 * TODO: move to STServiceAdapter?
	 * 
	 * Enrich the <code>modelAdditions</code> and <code>modelAdditions</code> with the triples to add and remove
	 * suggested by CODA running the PEARL rule defined in the CustomForm with the given <code>cfId</code>  
	 */
	private void enrichWithCustomForm(RepositoryConnection repoConn, Model modelAdditions, Model modelRemovals,
			String cfId, Map<String, Object> userPromptMap, StandardForm stdForm)
			throws ProjectInconsistentException, CODAException, CustomFormException {
		CODACore codaCore = getInitializedCodaCore(repoConn);
		try {

			CustomForm cForm = cfManager.getCustomForm(getProject(), cfId);
			if (cForm.isTypeGraph()) {
				CustomFormGraph cfGraph = cForm.asCustomFormGraph();
				UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm);
				shutDownCodaCore(codaCore);

				for (ARTTriple t : updates.getInsertTriples()) {
					modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
				for (ARTTriple t : updates.getDeleteTriples()) {
					modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
			} else {
				throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
			}
		} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e) {
			throw new CODAException(e);
		} finally {
			shutDownCodaCore(codaCore);
		}
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
	public GraphPattern getGraphPattern(Project<?> currentProject) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		return null;
	}
};