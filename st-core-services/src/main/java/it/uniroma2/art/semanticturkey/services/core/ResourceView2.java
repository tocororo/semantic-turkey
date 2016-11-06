/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2014.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */
package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.StatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.StatementConsumerProvider;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * This service produces a view showing the details of a resource. This service operates uniformly (as much as
 * possible) both on local resources and remote ones.
 * 
 */
@STService
public class ResourceView2 extends STServiceAdapter {

	public static final IRI INFERENCE_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://semanticturkey/inference-graph");

	private static final Logger logger = LoggerFactory.getLogger(ResourceView2.class);

	@Autowired
	private ResourceLocator resourceLocator;

	@Autowired
	private StatementConsumerProvider statementConsumerProvider;

	@STServiceOperation
	@Read
	public Map<String, ResourceViewSection> getResourceView(Resource resource,
			@Optional ResourcePosition resourcePosition) throws Exception {
		try {
			Project<? extends RDFModel> project = getProject();
			Resource workingGraph = getWorkingGraph();

			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			if (resourcePosition == null) {
				resourcePosition = resourceLocator.locateResource(getProject(), resource);
			}

			Model retrievedStatements = retrieveStatements(resourcePosition, resource);

			Map<Resource, Map<String, Value>> resource2attributes = retrieveSubjectAndObjectsAddtionalInformation(
					resourcePosition, resource);

			Set<Statement> processedStatements = new HashSet<>();

			Map<String, ResourceViewSection> description = new LinkedHashMap<>();

			AnnotatedValue<Resource> annotatedResource = new AnnotatedValue<Resource>(resource);
			annotatedResource.setAttribute("resourcePosition", resourcePosition.toString());
			AbstractStatementConsumer.addRole(annotatedResource, resource2attributes);

			RDFResourceRolesEnum resourceRole = RDFResourceRolesEnum
					.valueOf(annotatedResource.getAttributes().get("role").stringValue());

			AbstractStatementConsumer.addShowOrRenderXLabel(annotatedResource, resource2attributes,
					retrievedStatements);

			description.put("resource", new ResourceSection(annotatedResource));

			List<StatementConsumer> viewTemplate = statementConsumerProvider
					.getTemplateForResourceRole(resourceRole);

			Set<IRI> resourcePredicates = retrievedStatements.filter(resource, null, null).predicates();
			List<IRI> specialProperties = viewTemplate.stream()
					.flatMap(c -> c.getMatchedProperties().stream()).collect(toList());

			Model propertyModel = retrievePredicateInformation(resourcePosition, resourcePredicates,
					specialProperties);

			for (StatementConsumer aConsumer : viewTemplate) {
				Map<String, ResourceViewSection> producedSections = aConsumer.consumeStatements(project,
						resourcePosition, resource, retrievedStatements, processedStatements, workingGraph,
						resource2attributes, propertyModel);
				description.putAll(producedSections);
			}

			return description;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Model retrievePredicateInformation(ResourcePosition resourcePosition, Set<IRI> resourcePredicates,
			List<IRI> specialProperties) {
		String predicatesValuesFrag = resourcePredicates.stream()
				.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));
		String specialPredicatesValuesFrag = specialProperties.stream()
				.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));

		if (resourcePosition instanceof LocalResourcePosition) {
			RepositoryConnection acquireManagedConnection = acquireManagedConnectionToProject(
					((LocalResourcePosition) resourcePosition).getProject());

			TupleQuery query = acquireManagedConnection.prepareTupleQuery(String.format(
				// @formatter:off  
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                             \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                            \n" +
					" 	SELECT ?resource (GROUP_CONCAT(DISTINCT ?parentProp; separator = \"><\") AS ?parents) WHERE {     \n" +
					" 	  VALUES(?resource){                                                          \n" +
					" 	    %s						                                                  \n" +
					" 	  }                                                                           \n" +
					" 	  VALUES(?specialProp){                                                       \n" +
					" 	    %s						                                                  \n" +
					" 	  }                                                                           \n" +
					" 	  OPTIONAL {                                                                  \n" +
					" 	    ?resource rdfs:subPropertyOf* ?specialProp                                \n" +
					" 	    BIND(STR(?specialProp) as ?parentProp)                                    \n" +
					" 	  }	                                                                          \n" +
					" 	}                                                                             \n" +
					" 	GROUP BY ?resource                                                            \n"
				// @formatter:on
							,
					predicatesValuesFrag, specialPredicatesValuesFrag));

			Model propertyModel = new LinkedHashModel();
			QueryResults.stream(query.evaluate()).forEach(bs -> {
				List<IRI> parents = Arrays.stream(bs.getValue("parents").stringValue().split("><"))
						.filter(s -> !s.isEmpty()).map(s -> SimpleValueFactory.getInstance().createIRI(s))
						.collect(toList());
				IRI predicate = (IRI) bs.getValue("resource");

				parents.forEach(parent -> {
					propertyModel.add(predicate, RDFS.SUBPROPERTYOF, parent);
				});
			});
			
			return propertyModel;
		} else {
			throw new IllegalArgumentException("Resource position not supported yet: " + resourcePosition);
		}
	}

	private Map<Resource, Map<String, Value>> retrieveSubjectAndObjectsAddtionalInformation(
			ResourcePosition resourcePosition, Resource resource) {
		if (resourcePosition instanceof LocalResourcePosition) {
			LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
			QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +
				" SELECT ?resource WHERE {                                                    \n" +
				"   {                                                                         \n" +
				"     ?subjectResource ?predicate ?tempResource .                             \n" +
				"     ?tempResource (rdf:rest*/rdf:first)* ?resource                          \n" +
				"   } UNION {                                                                 \n" +
				"     bind(?subjectResource as ?resource)                                     \n" +
				"   }                                                                         \n" +
				"   FILTER(!isLITERAL(?resource))                                             \n" +
				" }                                                                           \n" +
				" GROUP BY ?resource                                                          \n"
				// @formatter:on
			);
			qb.processRendering();
			qb.processRole();
			qb.process(XLabelLiteralFormQueryProcessor.INSTANCE, "resource", "attr_literalForm");
			qb.setBinding("subjectResource", resource);
			Collection<AnnotatedValue<Resource>> annotatedResources = qb
					.runQuery(acquireManagedConnectionToProject(localResourcePosition.getProject()));
			Map<Resource, Map<String, Value>> resource2attributes = new HashMap<>();
			for (AnnotatedValue<Resource> annotatedValue : annotatedResources) {
				Resource res = annotatedValue.getValue();
				Map<String, Value> resAttributes = annotatedValue.getAttributes();
				resource2attributes.put(res, resAttributes);
			}

			return resource2attributes;
		} else {
			throw new IllegalArgumentException("Resource position not supported yet: " + resourcePosition);
		}
	}

	private Model retrieveStatements(ResourcePosition resourcePosition, Resource resource) {
		Model retrievedStatements = new LinkedHashModel();
		if (resourcePosition instanceof LocalResourcePosition) {
			LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;

			Project<?> resourceHoldingProject = localResourcePosition.getProject();

			RepositoryConnection managedConnection = acquireManagedConnectionToProject(
					resourceHoldingProject);
			QueryResults.addAll(managedConnection.getStatements(resource, null, null, false),
					retrievedStatements);

			GraphQuery describeQuery = managedConnection
					.prepareGraphQuery("DESCRIBE ?x WHERE {BIND(?y as ?x)}");
			describeQuery.setBinding("y", resource);
			describeQuery.setIncludeInferred(true);
			QueryResults.stream(describeQuery.evaluate()).forEach(stmt -> {
				Resource subject = stmt.getSubject();
				IRI predicate = stmt.getPredicate();
				Value object = stmt.getObject();
				if (retrievedStatements.contains(subject, predicate, object))
					return;

				retrievedStatements.add(subject, predicate, object, INFERENCE_GRAPH);
			});

		} else {
			throw new IllegalArgumentException("Unknown resource position: " + resourcePosition);
		}

		return retrievedStatements;
	}

	private RepositoryConnection acquireManagedConnectionToProject(Project<?> resourceHoldingProject) {
		if (!resourceHoldingProject.getName().equals(getProject().getName())) {
			throw new IllegalArgumentException("Cross-project RV not supported yet");
		}

		return getManagedConnection();
	}
}

class XLabelLiteralFormQueryProcessor implements QueryBuilderProcessor {

	public static final QueryBuilderProcessor INSTANCE = new XLabelLiteralFormQueryProcessor();

	@Override
	public GraphPattern getGraphPattern() {
		return GraphPatternBuilder.create().prefix("skosxl", SKOSXL.NAMESPACE)
				.pattern("?resource skosxl:literalForm ?literalForm . FILTER(isLITERAL(?literalForm))")
				.projection(ProjectionElementBuilder.variable("literalForm")).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		return resultTable.stream().filter(bs -> bs.getValue("literalForm") != null)
				.collect(groupingBy(bs -> bs.getValue("resource"),
						mapping(bs -> (Literal) bs.getValue("literalForm"),
								reducing(null, (v1, v2) -> v1 != null ? v1 : v2))));
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

}