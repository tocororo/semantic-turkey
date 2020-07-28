package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;

import com.google.common.collect.ImmutableSet;

import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Implementation of the {@link CustomServiceBackend} that uses SPARQL.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class SPARQLCustomServiceBackend implements CustomServiceBackend {

	public static final Set<String> SUPPORTED_TYPES = ImmutableSet
			.copyOf(new String[] { "boolean", "integer", "short", "long", "float", "double",
					"java.lang.String", "IRI", "BNode", "Resource", "Literal", "RDFValue" });
	private SPARQLOperation conf;

	public SPARQLCustomServiceBackend(SPARQLOperation conf) {
		this.conf = conf;
	}

	@Override
	public InvocationHandler createInvocationHandler() {
		String queryString = conf.sparql;
		ParsedOperation parsedQuery = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, queryString, null);

		BiFunction<STServiceContext, BindingSet, Object> handler;

		it.uniroma2.art.semanticturkey.config.customservice.Type returnedTypeDescription = conf.returns;

		if (parsedQuery instanceof ParsedBooleanQuery) {
			if (!"boolean".equals(returnedTypeDescription.getName())) {
				throw new IllegalStateException("ASK queries only allowed in boolean operations");
			}

			handler = (stServiceContext, bindingSet) -> {
				Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
				RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

				BooleanQuery query = conn.prepareBooleanQuery(queryString);
				bindingSet.forEach(b -> query.setBinding(b.getName(), b.getValue()));
				return query.evaluate();
			};
		} else if (parsedQuery instanceof ParsedTupleQuery) {
			Set<String> queryBindingNames = ((ParsedTupleQuery) parsedQuery).getTupleExpr().getBindingNames();

			if ("List".equals(returnedTypeDescription.getName())) {
				List<it.uniroma2.art.semanticturkey.config.customservice.Type> typeArgs = returnedTypeDescription
						.getTypeArguments();

				if (typeArgs == null || typeArgs.size() != 1) {
					throw new IllegalArgumentException(
							"A List return type requires exactly one type argument");
				}

				it.uniroma2.art.semanticturkey.config.customservice.Type elementType = typeArgs.iterator()
						.next();

				if ("AnnotatedValue".equals(elementType.getName())) {
					List<String> variablesOtherThanAttributes = queryBindingNames.stream()
							.filter(n -> !n.startsWith("attr_")).collect(Collectors.toList());
					List<String> attributeVariables = queryBindingNames.stream()
							.filter(n -> n.startsWith("attr_")).collect(Collectors.toList());

					if (variablesOtherThanAttributes.size() != 1) {
						throw new IllegalArgumentException(
								"Requires exactly one return variable other than attributes");
					}

					String resourceVariableName = variablesOtherThanAttributes.iterator().next();

					handler = (stServiceContext, bindingSet) -> {
						Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
						RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

						String queryStringWithoutProlog = QueryParserUtil
								.removeSPARQLQueryProlog(queryString);
						String queryProlog = queryString.substring(0,
								queryString.indexOf(queryStringWithoutProlog));

						// skos, owl, skosxl, rdfs, rdf

						StringBuilder newQueryPrologBuilder = new StringBuilder(queryProlog);

						// add prefixes required by the nature computation pattern
						for (Namespace ns : Arrays.asList(SKOS.NS,
								org.eclipse.rdf4j.model.vocabulary.SKOSXL.NS, RDF.NS, RDFS.NS, OWL.NS)) {
							if (queryProlog.indexOf(ns.getPrefix() + ":") == -1) {
								newQueryPrologBuilder.append("prefix " + ns.getPrefix() + ":");
								RenderUtils.toSPARQL(SimpleValueFactory.getInstance().createIRI(ns.getName()),
										newQueryPrologBuilder);
								newQueryPrologBuilder.append("\n");
							}
						}

						String groundQueryStringWithoutProlog = QueryStringUtil
								.getTupleQueryString(queryStringWithoutProlog, bindingSet);

						QueryBuilder qb = new QueryBuilder(stServiceContext,
								newQueryPrologBuilder.toString() + "\nSELECT DISTINCT ?"
										+ resourceVariableName + " "
										+ (!attributeVariables.isEmpty() ? " " + attributeVariables.stream()
												.map(v -> "?" + v).collect(Collectors.joining(" ")) : "")
										+ " " + NatureRecognitionOrchestrator.getNatureSPARQLSelectPart()
										+ " WHERE {{" + groundQueryStringWithoutProlog + "}\n"
										+ NatureRecognitionOrchestrator
												.getNatureSPARQLWherePart(resourceVariableName)
										+ "} GROUP BY ?" + resourceVariableName
										+ (!attributeVariables.isEmpty() ? " " + attributeVariables.stream()
												.map(v -> "?" + v).collect(Collectors.joining(" ")) : "")
										+ " ");
						qb.setResourceVariable(resourceVariableName);
						qb.processRendering();
						qb.processQName();

						return qb.runQuery();

					};

				} else {
					if (queryBindingNames.size() != 1) {
						throw new IllegalArgumentException("Requires exactly one retured variable");
					}

					String bindingName = queryBindingNames.iterator().next();

					handler = (stServiceContext, bindingSet) -> {
						Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
						RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

						TupleQuery query = conn.prepareTupleQuery(queryString);
						bindingSet.forEach(b -> query.setBinding(b.getName(), b.getValue()));
						return QueryResults.stream(query.evaluate()).map(
								bs -> this.convertRDFValueToJavaValue(bs.getValue(bindingName), elementType))
								.collect(Collectors.toList());
					};
				}
			} else if ("TupleQueryResult".equals(returnedTypeDescription.getName())) {
				handler = (stServiceContext, bindingSet) -> {
					Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
					RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

					TupleQuery query = conn.prepareTupleQuery(queryString);
					bindingSet.forEach(b -> query.setBinding(b.getName(), b.getValue()));

					List<String> bindingNames;
					List<BindingSet> bindingSets;
					try (TupleQueryResult queryresult = query.evaluate()) {
						bindingNames = queryresult.getBindingNames();
						bindingSets = QueryResults.asList(queryresult);
					}

					return new IteratingTupleQueryResult(bindingNames, bindingSets);
				};
			} else { // assumes single scalar
				if (queryBindingNames.size() != 1) {
					throw new IllegalArgumentException("Requires exactly one retured variable");
				}

				handler = (stServiceContext, bindingSet) -> {
					Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
					RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

					TupleQuery query = conn.prepareTupleQuery(queryString);
					bindingSet.forEach(b -> query.setBinding(b.getName(), b.getValue()));

					try (TupleQueryResult queryresult = query.evaluate()) {
						Value rdfValue = QueryResults.singleResult(queryresult)
								.getValue(queryBindingNames.iterator().next());
						return convertRDFValueToJavaValue(rdfValue, returnedTypeDescription);
					}
				};

			}
		} else if (parsedQuery instanceof ParsedUpdate) {
			handler = (stServiceContext, bindingSet) -> {
				Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
				RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);

				Update query = conn.prepareUpdate(queryString);
				bindingSet.forEach(b -> query.setBinding(b.getName(), b.getValue()));
				query.execute();
				return null;
			};
		} else {
			throw new IllegalStateException("Unsupported SPARQL operation type");
		}

		return new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				STServiceContext stServiceContext = (STServiceContext) proxy.getClass()
						.getDeclaredField("stServiceContext").get(proxy);

				Parameter[] params = method.getParameters();
				Type[] genericParameterTypes = method.getGenericParameterTypes();

				MapBindingSet bindingSet = new MapBindingSet();

				SimpleValueFactory vf = SimpleValueFactory.getInstance();

				for (int i = 0; i < params.length; i++) {
					if (args[i] != null) {
						bindingSet.addBinding(params[i].getName(),
								convertJavaValueToRDFValue(args[i], genericParameterTypes[i], vf));
					}
				}

				// automatically binds the working graph
				bindingSet.addBinding("workingGraph", stServiceContext.getWGraph());

				return handler.apply(stServiceContext, bindingSet);
			}
		};
	}

	@Override
	public boolean isWrite() {
		return QueryParserUtil.parseOperation(QueryLanguage.SPARQL, conf.sparql,
				null) instanceof ParsedUpdate;
	}

	protected Value convertJavaValueToRDFValue(Object input, Type inputType, ValueFactory vf) {
		if (inputType.equals(String.class)) {
			return vf.createLiteral(input.toString());
		} else if (TypeUtils.isAssignable(inputType, Value.class)) {
			return (Value) input;
		} else if (TypeUtils.isAssignable(inputType, Integer.class)) {
			return vf.createLiteral(input.toString(), XMLSchema.INT);
		} else if (TypeUtils.isAssignable(inputType, Long.class)) {
			return vf.createLiteral(input.toString(), XMLSchema.LONG);
		} else if (TypeUtils.isAssignable(inputType, Float.class)) {
			return vf.createLiteral(input.toString(), XMLSchema.FLOAT);
		} else if (TypeUtils.isAssignable(inputType, Double.class)) {
			return vf.createLiteral(input.toString(), XMLSchema.DOUBLE);
		} else if (TypeUtils.isAssignable(inputType, Boolean.class)) {
			return vf.createLiteral(input.toString(), XMLSchema.BOOLEAN);
		} else {
			throw new IllegalStateException("Unsupported Java type " + inputType);
		}
	}

	protected Object convertRDFValueToJavaValue(Value input,
			it.uniroma2.art.semanticturkey.config.customservice.Type outputType) {
		String outputTypeName = outputType.getName();

		if (outputTypeName.equals("java.lang.String")) {
			return new String(input.stringValue());
		} else if (outputTypeName.equals("RDFValue")) {
			return (Value) input;
		} else if (outputTypeName.equals("Literal")) {
			return (Literal) input;
		} else if (outputTypeName.equals("BNode")) {
			return (BNode) input;
		} else if (outputTypeName.equals("IRI")) {
			return (IRI) input;
		} else if (outputTypeName.equals("Resource")) {
			return (Resource) input;
		} else if (outputTypeName.equals("boolean")) {
			return Literals.getBooleanValue(input, false);
		} else if (outputTypeName.equals("float")) {
			return Literals.getFloatValue(input, 0);
		} else if (outputTypeName.equals("double")) {
			return Literals.getDoubleValue(input, 0);
		} else if (outputTypeName.equals("integer")) {
			return Literals.getIntValue(input, 0);
		} else if (outputTypeName.equals("long")) {
			return Literals.getLongValue(input, 0);
		} else {
			throw new IllegalStateException("Unsupported Java type " + outputType);
		}
	}

	protected Object adaptTupleQueryResults(TupleQueryResult result, Type returnType) {
		if (TypeUtils.isAssignable(returnType, List.class) && TypeUtils.isAssignable(
				((ParameterizedType) returnType).getActualTypeArguments()[0], AnnotatedValue.class)) {
			return QueryResults.stream((TupleQueryResult) result).map(bs -> {
				return new AnnotatedValue<>(bs.iterator().next().getValue());
			}).collect(Collectors.toList());
		}

		throw new IllegalStateException("Unsupported return type for SPARQL operation: " + returnType);
	}

}
