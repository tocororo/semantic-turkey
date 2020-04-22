package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;

import com.google.common.collect.ImmutableSet;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.Type;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * The {@link ExtensionFactory} for the the {@link SPARQLCustomServiceBackend}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLCustomServiceBackendFactory implements ExtensionFactory<SPARQLCustomServiceBackend>,
		ConfigurableExtensionFactory<SPARQLCustomServiceBackend, SPARQLOperation> {

	@Override
	public String getName() {
		return "SPARQL Custom Service Backend";
	}

	@Override
	public String getDescription() {
		return "A backend for implementing custom services using SPARQL";
	}

	@Override
	public SPARQLCustomServiceBackend createInstance(SPARQLOperation conf)
			throws InvalidConfigurationException {
		StringBuilder validationExceptionMessageBuilder = new StringBuilder();

		if (conf.parameters != null) {
			Map<String, Long> parameterNameHistogram = conf.parameters.stream().map(p -> p.name)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			List<String> ambiguousParameterNames = parameterNameHistogram.entrySet().stream()
					.filter(p -> p.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());

			// checks parameter uniqueness
			if (!ambiguousParameterNames.isEmpty()) {
				validationExceptionMessageBuilder.append("Ambiguous parameters: ")
						.append(ambiguousParameterNames.stream().collect(Collectors.joining(", ")))
						.append(".\n");
			}

			// checks reserved parameter names
			if (parameterNameHistogram.containsKey("workingGraph")) {
				validationExceptionMessageBuilder.append("'workingGraph' is a reserved parameter name.\n");
			}

			List<String> parametersWithUnsupportedTypes = conf.parameters.stream()
					.filter(p -> !SPARQLCustomServiceBackend.SUPPORTED_TYPES.contains(p.type.getName()))
					.map(p -> p.name).collect(Collectors.toList());

			if (!parametersWithUnsupportedTypes.isEmpty()) {
				validationExceptionMessageBuilder.append("Parameters with unsupported types: ")
						.append(parametersWithUnsupportedTypes).append(".\n");
			}
		}

		ParsedOperation parsedQuery = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, conf.sparql, null);

		// checks operation type and compliance of the return type
		if (parsedQuery instanceof ParsedBooleanQuery) { // boolean queries must return a boolean
			if (!Objects.equals("boolean", conf.returns.getName())) {
				validationExceptionMessageBuilder.append("return type not compatible with boolean queries: ")
						.append(conf.returns.toString()).append(".\n");
			}
		} else if (parsedQuery instanceof ParsedTupleQuery) { // tuple queries must return a List of items
			if (!Objects.equals("List", conf.returns.getName())) {
				validationExceptionMessageBuilder
						.append("return type not compatible with select queries, which require a List: ")
						.append(conf.returns.toString()).append(".\n");
			}

			if (conf.returns.getTypeArguments() == null || conf.returns.getTypeArguments().size() != 1) {
				validationExceptionMessageBuilder.append("List may only have one type argument")
						.append(conf.returns.toString()).append(".\n");
			}

			Type listItem = conf.returns.getTypeArguments().iterator().next();

			Set<String> bindingNames = ((ParsedTupleQuery) parsedQuery).getTupleExpr().getBindingNames();

			Type unwrappedType; // the list item, or the type of annotated value
			if (Objects.equals(listItem.getName(), "AnnotatedValue")) { // if returning an AnnotatedValue, the
																		// query can return additional attr_
																		// variables
				List<String> variablesOtherThanAttributes = bindingNames.stream()
						.filter(n -> !n.startsWith("attr_")).collect(Collectors.toList());

				if (variablesOtherThanAttributes.size() != 1) {
					validationExceptionMessageBuilder
							.append("The SELECT query returns more than one varible or attr_ variables:")
							.append(bindingNames).append(".\n");
				}

				if (listItem.getTypeArguments() != null && listItem.getTypeArguments().iterator().hasNext()) {
					unwrappedType = listItem.getTypeArguments().iterator().next();

					if (!ImmutableSet.of("RDFValue", "Resource", "BNode", "IRI", "Literal")
							.contains(unwrappedType.getName())) {
						validationExceptionMessageBuilder
								.append("Illegal argument type of AnnotatedValue. It should be an RDF type: "
										+ unwrappedType + ".\n");
					}
				} else {
					validationExceptionMessageBuilder.append("AnnotatedValue requires a type argument.\n");
					unwrappedType = null;
				}
			} else { // otherwise, the query may only return one variable
				if (bindingNames.size() != 1) {
					validationExceptionMessageBuilder.append("The SELECT query returns more than one varible")
							.append(bindingNames).append(".\n");
				}

				unwrappedType = listItem;
			}

			if (unwrappedType == null
					|| !SPARQLCustomServiceBackend.SUPPORTED_TYPES.contains(unwrappedType.getName())) {
				validationExceptionMessageBuilder.append("Unsupported type for returned items: ")
						.append(unwrappedType).append(".\n");
			}
		} else if (parsedQuery instanceof ParsedUpdate) { // updates must return void
			if (!Objects.equals("void", conf.returns.getName())) {
				validationExceptionMessageBuilder.append("return type not compatible with updates: ")
						.append(conf.returns.toString()).append(".\n");
			}
		} else {
			validationExceptionMessageBuilder.append(
					"unsupported SPARQL operation. It should be either a boolean query, a graph query or an update.")
					.append(conf.returns.toString()).append(".\n");
		}

		if (validationExceptionMessageBuilder.length() > 1) {
			throw new InvalidConfigurationException(validationExceptionMessageBuilder.toString());
		}

		return new SPARQLCustomServiceBackend(conf);
	}

	@Override
	public Collection<SPARQLOperation> getConfigurations() {
		return Arrays.asList(new SPARQLOperation());
	}

}
