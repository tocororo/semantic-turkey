package it.uniroma2.art.semanticturkey.services.aspects;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.BLACKLIST;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.exceptions.BlacklistForbiddendException;
import it.uniroma2.art.semanticturkey.history.HistoryMetadataSupport;
import it.uniroma2.art.semanticturkey.history.OperationMetadata;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.logging.TermCreation;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that takes care of term rejection logging
 * metadata.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RejectedTermsBlacklistingInterceptor implements MethodInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(RejectedTermsBlacklistingInterceptor.class);

	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Project project = stServiceContext.getProject();

		// if rejected terms is enabled (and validation, as well)
		if (project.isBlacklistingEnabled()) {
			// invocation stuff
			Method method = invocation.getMethod();
			LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
			String[] methodParameterNames = parameterNameDiscoverer.getParameterNames(method);
			Object[] methodArguments = invocation.getArguments();

			// retrieves the annotation and the specified bindings
			TermCreation annot = AnnotationUtils.getAnnotation(method, TermCreation.class);
			String labelBinding = annot.label();
			String conceptBinding = annot.concept();
			TermCreation.Facets facet = annot.facet();

			// resolve the binding for 'label'

			Literal label = resolveBinding(method, methodParameterNames, methodArguments, "label",
					Literal.class, labelBinding)
							.orElseThrow(() -> new IllegalStateException("Missing binding for 'label'"));

			@SuppressWarnings("unused")
			Optional<IRI> concept = resolveBinding(method, methodParameterNames, methodArguments, "concept",
					IRI.class, conceptBinding);

			Locale locale = label.getLanguage().map(lang -> new Locale.Builder().setLanguageTag(lang).build())
					.orElseGet(Locale::getDefault);
			String lowercaseLiteralForm = label.getLabel().toLowerCase(locale);
			Literal lowercasedLabel = label.getLanguage().isPresent()
					? SimpleValueFactory.getInstance().createLiteral(lowercaseLiteralForm,
							label.getLanguage().get())
					: SimpleValueFactory.getInstance().createLiteral(lowercaseLiteralForm,
							label.getDatatype());

			logger.debug("blacklist interceptor executed for {}", annot);
			RepositoryConnection coreRepoConnection = RDF4JRepositoryUtils
					.getConnection(STServiceContextUtils.getRepostory(stServiceContext), false);

			IRI blacklistGraph = Models
					.objectIRI(QueryResults
							.asModel(coreRepoConnection.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT,
									CHANGETRACKER.BLACKLIST_GRAPH, null, CHANGETRACKER.GRAPH_MANAGEMENT)))
					.orElseThrow(() -> new IllegalStateException(
							"Could not obtain the blacklist graph. Perhaps this project is without blacklist management"));

			logger.debug("blacklist graph: {}", blacklistGraph);

			// 1. checks that the provided label is not blacklisted

			boolean isForced = StringUtils.equalsIgnoreCase("true",
					stServiceContext.getContextParameter("force"));

			if (!isForced) {
				try (RepositoryConnection supportRepoConnection = project.getRepositoryManager()
						.getRepository(Project.SUPPORT_REPOSITORY).getConnection()) {

					logger.debug("Lookup the blacklist for " + lowercasedLabel);
					TupleQuery blacklistSearchQuery = supportRepoConnection.prepareTupleQuery(
					//@formatter:off
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>               \n" +
					"SELECT ?blacklistedTerm ?comment {                                 \n" +
					"  GRAPH " + RenderUtils.toSPARQL(blacklistGraph) + " {             \n" +
					"    ?blacklistedTerm a <" + BLACKLIST.BLACKLISTED_TERM + "> ;      \n" +
					"      <" + BLACKLIST.LOWERCASED_LABEL + "> " + RenderUtils.toSPARQL(lowercasedLabel) + " . \n" +
					"    OPTIONAL {                                                     \n" +
					"      ?blacklistedTerm rdfs:comment ?comment .                     \n" +
					"    }                                                              \n" +
					"  }                                                                \n" +
					"}"
					//@formatter:on
					);
					blacklistSearchQuery.setIncludeInferred(false);
					List<BindingSet> blacklistItems = QueryResults.asList(blacklistSearchQuery.evaluate());

					boolean isBlacklisted = !blacklistItems.isEmpty();
					logger.debug("Is blacklisted? {}", isBlacklisted);

					if (isBlacklisted) {
						String comment = blacklistItems.stream().map(bs -> bs.getValue("comment"))
								.filter(c -> c != null).map(Value::stringValue).collect(joining("; "));
						throw new BlacklistForbiddendException(
								"The term " + NTriplesUtil.toNTriplesString(label) + " is blacklisted"
										+ (comment.isEmpty() ? "" : ": " + comment));
					}
				}
			}

			// 2. adds the necessary blacklist template inside the commit metadata

			OperationMetadata currentOperationMetadata = HistoryMetadataSupport.currentOperationMetadata();

			ValueFactory vf = coreRepoConnection.getValueFactory();
			Model blacklistTemplate = new LinkedHashModel();

			BNode templateNode = vf.createBNode();
			BNode labelBindingNode = vf.createBNode();

			blacklistTemplate.add(CHANGETRACKER.COMMIT_METADATA, BLACKLIST.TEMPLATE, templateNode);
			blacklistTemplate.add(templateNode, BLACKLIST.TEMPLATE_TYPE, BLACKLIST.BLACKLISTED_TERM);
			blacklistTemplate.add(templateNode, BLACKLIST.PARAMETER_BINDING, labelBindingNode);
			RDFCollections.asRDF(
					Arrays.asList(BLACKLIST.LABEL, currentOperationMetadata.getParameterIRI(labelBinding)),
					labelBindingNode, blacklistTemplate);
			if (!conceptBinding.isEmpty()) {
				BNode conceptBindingNode = vf.createBNode();
				blacklistTemplate.add(templateNode, BLACKLIST.PARAMETER_BINDING, conceptBindingNode);
				RDFCollections.asRDF(
						Arrays.asList(BLACKLIST.CONCEPT,
								currentOperationMetadata.getParameterIRI(conceptBinding)),
						conceptBindingNode, blacklistTemplate);
			}

			if (facet != TermCreation.Facets.UNSPECIFIED) {
				BNode facetBindingNode = vf.createBNode();
				blacklistTemplate.add(templateNode, BLACKLIST.CONSTANT_BINDING, facetBindingNode);
				RDFCollections.asRDF(Arrays.asList(BLACKLIST.FACET, vf.createLiteral(facet.toString())),
						facetBindingNode, blacklistTemplate);
			}

			coreRepoConnection.add(blacklistTemplate, CHANGETRACKER.COMMIT_METADATA);

		}

		return invocation.proceed();
	}

	public <T> Optional<T> resolveBinding(Method method, String[] methodParameterNames,
			Object[] methodArguments, String variable, Class<T> expectedType, String variableBinding)
			throws IllegalStateException {
		if (variableBinding.isEmpty()) {
			return Optional.empty();
		}
		int labelBindingIndex = ArrayUtils.indexOf(methodParameterNames, variableBinding);

		if (labelBindingIndex == -1) {
			throw new IllegalStateException(
					"The " + TermCreation.class.getSimpleName() + " annotation binds '" + variable
							+ "' to the unexisting parameter '" + variableBinding + "'");
		}

		Class<?> labelBoundType = method.getParameterTypes()[labelBindingIndex];
		if (!expectedType.isAssignableFrom(labelBoundType)) {
			throw new IllegalStateException("The " + TermCreation.class.getSimpleName()
					+ " annotation contains binds '" + variable + "' to the parameter '" + variableBinding
					+ "' with the unacceptable type '" + labelBoundType + "'");
		}

		return Optional.of(expectedType.cast(methodArguments[labelBindingIndex]));
	}
}