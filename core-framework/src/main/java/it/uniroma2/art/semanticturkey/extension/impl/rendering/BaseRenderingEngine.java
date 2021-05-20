package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngineExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEnginePUSettings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElement;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.UsersManager;

public abstract class BaseRenderingEngine implements RenderingEngine {

	private static final String OLD_LANGUAGES_VAR = "languages";

	private boolean fallbackToTerm;
	private AbstractLabelBasedRenderingEngineConfiguration conf;
	protected String languages;

	private static final Logger logger = LoggerFactory.getLogger(BaseRenderingEngine.class);

	private static final Pattern rawLabelDestructuringPattern = Pattern
			.compile("((?:\\\\@|\\\\,|[^@,])+)(?:@((?:\\\\@|\\\\,|[^@,])+))?");

	private static final Pattern aggregationSplittingPattern = Pattern
			.compile("(?<!\\\\)(\\\\\\\\)*(?<splitter>,)");

	private static final Pattern propPattern = Pattern
			.compile("\\$\\{(" + AbstractLabelBasedRenderingEngineConfiguration.USER_LANGS_VAR + "|" + OLD_LANGUAGES_VAR + ")\\}");

	private static enum ValueStatus {
		ADDED, REMOVED, COMMITTED;

		public static ValueStatus parse(char charAt) {
			if (charAt == 'a') {
				return ADDED;
			} else if (charAt == 'r') {
				return REMOVED;
			} else if (charAt == '-') {
				return COMMITTED;
			} else {
				throw new IllegalArgumentException("Not a valid ValueStatus: " + charAt);
			}
		}
	}

	public BaseRenderingEngine(AbstractLabelBasedRenderingEngineConfiguration conf) {
		this(conf, true);
	}

	public BaseRenderingEngine(AbstractLabelBasedRenderingEngineConfiguration conf, boolean fallbackToTerm) {
		this.fallbackToTerm = fallbackToTerm;
		this.conf = conf;
		this.languages = conf.languages;
		if (this.languages == null) {
			this.languages = "*";
		}
	}

	/**
	 * Computes the list of languages by interpolating the configured languages with ST Properties. The order
	 * of languages is significative, since it may determine the order of labels displayed by concrete
	 * rendering engines.
	 * 
	 * @param context
	 * @return
	 */
	private List<String> computeLanguages(STServiceContext context) {
		StringBuffer sb = new StringBuffer();
		Matcher m = propPattern.matcher(languages);
		String languagesPropValue = context.getLanguages();
		while (m.find()) {
			if (languagesPropValue == null) {
				try {
					RenderingEnginePUSettings exptPointSettings = STPropertiesManager.getPUSettings(RenderingEnginePUSettings.class, context.getProject(), UsersManager.getLoggedUser(), RenderingEngine.class.getName());
					languagesPropValue = exptPointSettings.languages;
				} catch (STPropertyAccessException e) {
					logger.debug("Could not access property: RenderingEnginePUSettings.languages", e);
				}
				if (languagesPropValue == null) {
					languagesPropValue = "*";
				}
			}
			m.appendReplacement(sb, languagesPropValue);
		}
		m.appendTail(sb);

		String interpolatedLanguages = sb.toString();
		if (interpolatedLanguages.isEmpty() || interpolatedLanguages.equals("*")) {
			return Collections.emptyList();
		} else {
			return Arrays.stream(interpolatedLanguages.split(",")).map(String::trim).map(lang -> lang.replace("--", "")).collect(toList());
		}
	}

	@Override
	public GraphPattern getGraphPattern(STServiceContext context) {
		Project currentProject = context.getProject();

		StringBuilder gp = new StringBuilder();
		getGraphPatternInternal(gp);

		List<String> acceptedLanguges = computeLanguages(context);

		if (!acceptedLanguges.isEmpty()) {
			gp.append(String.format(" FILTER(LANG(?labelInternal) IN (%s))", acceptedLanguges.stream()
					.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"").collect(joining(", "))));
		}
		gp.append("BIND(REPLACE(str(?labelInternal), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLexicalForm)");
		gp.append("BIND(REPLACE(lang(?labelInternal), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLang)");
		gp.append(
				"BIND(IF(?labelLang != \"\", CONCAT(STR(?labelLexicalForm), \"@\", ?labelLang), ?labelLexicalForm) AS ?labelInternal2)       \n");

		List<ProjectionElement> projection = new ArrayList<>();
		projection.add(ProjectionElementBuilder.groupConcat("labelInternal2", "label"));

		boolean isValidationEnabled = !Boolean.TRUE.equals(conf.ignoreValidation)
				&& currentProject.isValidationEnabled();

		if (!CollectionUtils.sizeIsEmpty(conf.variables)) {
			StringBuilder gp2 = new StringBuilder();
			gp2.append("{\n");
			gp2.append(gp.toString());
			gp2.append("}");

			for (Entry<String, VariableDefinition> entry : conf.variables.entrySet()) {
				gp2.append(" UNION {\n");
				String variableName = entry.getKey();
				String sparqlVarName = "v_" + variableName;
				String sparqlSupportVarName = "sv_" + variableName;
				String sparqlSupportVar = "?" + sparqlSupportVarName;

				VariableDefinition variableDefinition = entry.getValue();

				String subjectOfProp = "?" + getBindingVariable();

				if (CollectionUtils.isEmpty(variableDefinition.propertyPath)) {
					throw new IllegalStateException("Empty property path not allowed");
				}

				int count = 0;

				String undefVariable = "?v_" + variableName + "_internal_undef";
				for (IRI prop : variableDefinition.propertyPath) {
					count++;

					String sparqlInternalVarName = "v_" + variableName + "_internal_" + count;
					String sparqlInternalVar = "?" + sparqlInternalVarName;

					String sparqlInternalGraphVarName = "v_" + variableName + "_internal_" + count + "_g";
					String sparqlInternalGraphVar = "?" + sparqlInternalGraphVarName;

					if (isValidationEnabled) {
						gp2.append("GRAPH ").append(sparqlInternalGraphVar).append(" {\n");
					}
					gp2.append(subjectOfProp).append(" ");
					RenderUtils.toSPARQL(prop, gp2);
					gp2.append(" ");
					gp2.append(sparqlInternalVar);
					gp2.append(" .\n");
					if (isValidationEnabled) {
						gp2.append("}\n");
						String sparqlInternalGraphStatusVar = sparqlInternalGraphVar + "_status";
						gp2.append("BIND(IF(" + VALIDATION.isAddGraphSPARQL(sparqlInternalGraphVar)
								+ ", \"a\", IF(" + VALIDATION.isRemoveGraphSPARQL(sparqlInternalGraphVar)
								+ ", \"r\", " + undefVariable + ")) as " + sparqlInternalGraphStatusVar
								+ ")\n");

					}
					subjectOfProp = sparqlInternalVar;
				}

				if (!acceptedLanguges.isEmpty()) {
					gp2.append(
							String.format(" FILTER(LANG(%1$s) = \"\" || LANG(%1$s) IN (%2$s))", subjectOfProp,
									acceptedLanguges.stream()
											.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"")
											.collect(joining(", "))));
				}

				if (isValidationEnabled) {
					String sparqlSerializedValueVar = subjectOfProp + "_sparql";

					StringBuilder sparqlInternalGraphStatusVars = new StringBuilder();
					for (int i = 1; i <= count; i++) {
						sparqlInternalGraphStatusVars
								.append("?v_" + variableName + "_internal_" + i + "_g_status").append(", ");
					}
					gp2.append("BIND(REPLACE(IF(isIRI(" + subjectOfProp + "), CONCAT(\"<\", STR("
							+ subjectOfProp + "), \">\"), IF(isBlank(" + subjectOfProp
							+ "), CONCAT(\"_:\", STR(" + subjectOfProp
							+ ")), CONCAT(\"\\\"\", REPLACE(REPLACE(STR(" + subjectOfProp
							+ "),\"(\\\"|\\\\\\\\)\",\"\\\\\\\\$1\"),\"\\\\\\\\n\", \"\\\\n\"), \"\\\"\", IF(LANG("
							+ subjectOfProp + ") != \"\", CONCAT(\"@\", LANG(" + subjectOfProp
							+ ")), CONCAT(\"^^<\", STR(datatype(" + subjectOfProp
							+ ")), \">\"))))), \"(,|\\\\\\\\)\", \"\\\\\\\\$1\") as "
							+ sparqlSerializedValueVar + ")\n");
					gp2.append("BIND(CONCAT(COALESCE(" + sparqlInternalGraphStatusVars + "\"-\"), STR("
							+ sparqlSerializedValueVar + ")) AS " + sparqlSupportVar + ")\n");
				}

				gp2.append("}");

				if (isValidationEnabled) {
					projection.add(ProjectionElementBuilder.groupConcat(sparqlSupportVarName, sparqlVarName));
				} else {
					projection.add(ProjectionElementBuilder
							.min(subjectOfProp.substring(1) /* removes the leading ? */, sparqlVarName));
				}
			}
			gp2.append("\n");

			gp = gp2;
		}

		return GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE).projection(projection)
				.pattern(gp.toString()).graphPattern();

	}

	public abstract void getGraphPatternInternal(StringBuilder gp);

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		Project currentProject = context.getProject();

		Repository rep = currentProject.getRepository();

		HashMap<String, String> ns2prefix = new HashMap<>();
		RepositoryConnection repConn = RDF4JRepositoryUtils.getConnection(rep);

		try {
			Iterations.stream(repConn.getNamespaces())
					.forEach(ns -> ns2prefix.put(ns.getName(), ns.getPrefix()));
		} finally {
			RDF4JRepositoryUtils.releaseConnection(repConn, rep);
		}

		Map<Value, Literal> renderings = new HashMap<>();
		ValueFactory vf = SimpleValueFactory.getInstance();

		// note: there might be a race condition between this invocation and the generation of the graph
		// pattern
		List<String> acceptedLanguges = computeLanguages(context);
		@Nullable
		Template template;
		boolean isValidationEnabled;
		try {
			if (conf.template != null) {
				template = parseTemplate(conf.template);
			} else {
				template = null;
			}
			isValidationEnabled = !Boolean.TRUE.equals(conf.ignoreValidation)
					&& currentProject.isValidationEnabled();
		} catch (TemplateParsingException | IOException e) {
			throw new RuntimeException(e);
		}

		resultTable.forEach(bindingSet -> {
			Resource resource = (Resource) bindingSet.getValue("resource");
			Literal rawLabelLiteral = ((Literal) bindingSet.getValue("label"));

			if (renderings.get(resource) != null)
				return;

			String labelsVar;

			if (rawLabelLiteral != null && !rawLabelLiteral.getLabel().isEmpty()) {
				Matcher matcher = rawLabelDestructuringPattern.matcher(rawLabelLiteral.getLabel());

				// If no language has been specified (or *), then all labels are displayed in alphabetic order
				// of language tag; otherwise, the order is based on the configured list of languages
				Multimap<String, String> lang2label = acceptedLanguges.isEmpty() ? TreeMultimap.create()
						: HashMultimap.create();

				while (matcher.find()) {
					String lexicalForm = matcher.group(1);
					lexicalForm = lexicalForm.replace("\\,", ",").replace("\\@", "@").replace("\\\\", "\\")
							.trim();
					String lang = matcher.group(2);
					if (lang != null) {
						lang = lang.replace("\\,", ",").replace("\\@", "@").replace("\\\\", "\\").trim();
						lang2label.put(lang, lexicalForm);
					} else {
						lang2label.put("", lexicalForm);
					}
				}

				Iterator<String> keyIt;
				if (acceptedLanguges.isEmpty()) {
					keyIt = lang2label.keySet().iterator();
				} else {
					keyIt = acceptedLanguges.iterator();
				}

				StringBuilder sb = new StringBuilder();

				while (keyIt.hasNext()) {
					String lang = keyIt.next();

					Collection<String> labels = lang2label.get(lang);

					if (labels.isEmpty())
						continue;

					if (sb.length() != 0) {
						sb.append(", ");
					}

					if (lang.isEmpty()) {
						sb.append(labels.stream().collect(joining(", ")));
					} else {
						sb.append(labels.stream().map(l -> l + " (" + lang + ")").collect(joining(", ")));
					}

				}

				labelsVar = sb.toString();
			} else {
				labelsVar = null;
			}

			String show = labelsVar;

			if (template != null) {
				Map<String, Value> variableValues = new HashMap<>();

				if (labelsVar != null && !labelsVar.isEmpty()) {
					variableValues.put("show", vf.createLiteral(labelsVar)); // maintain the old variable name for backward compatibility
					variableValues.put("labels", vf.createLiteral(labelsVar)); // forward compatible as existing user-defined variable will override it
				}

				for (String bindingName : bindingSet.getBindingNames()) {
					if (bindingName.startsWith("v_")) {
						Value value = bindingSet.getValue(bindingName);
						if (value != null) {
							if (isValidationEnabled) {
								String stringValue = value.stringValue();
								if (stringValue.isEmpty()) {
									continue;
								}
								Matcher m = aggregationSplittingPattern.matcher(stringValue);
								int index = 0;

								Map<Value, ValueStatus> components = new HashMap<>(5);
								while (m.find()) {
									int splitter = m.start("splitter");
									String componentString = stringValue.substring(index, splitter)
											.replace("\\,", ",").replace("\\\\", "\\");
									Value parsedValue = NTriplesUtil.parseValue(componentString.substring(1),
											vf);
									ValueStatus valueStatus = ValueStatus.parse(componentString.charAt(0));
									ValueStatus currentValueStatus = components.get(parsedValue);
									if (currentValueStatus == null
											|| ValueStatus.COMMITTED.equals(currentValueStatus)) {
										components.put(parsedValue, valueStatus);
									}
									index = splitter + 1;
								}

								if (index < stringValue.length()) {
									String componentString = stringValue.substring(index).replace("\\,", ",")
											.replace("\\\\", "\\");
									ValueStatus valueStatus = ValueStatus.parse(componentString.charAt(0));
									Value parsedValue = NTriplesUtil.parseValue(componentString.substring(1),
											vf);
									ValueStatus currentValueStatus = components.get(parsedValue);
									if (currentValueStatus == null
											|| ValueStatus.COMMITTED.equals(components.get(parsedValue))) {
										components.put(parsedValue, valueStatus);
									}
								}

								Value variableValue;
								if (components.size() == 1) {
									variableValue = components.keySet().iterator().next();
								} else {
									Value committedValue = null;
									Value removedValue = null;
									Value addedValue = null;

									for (Map.Entry<Value, ValueStatus> entry : components.entrySet()) {
										Value v = entry.getKey();
										ValueStatus vs = entry.getValue();

										if (vs == ValueStatus.COMMITTED) {
											committedValue = v;
										} else if (vs == ValueStatus.REMOVED) {
											removedValue = v;
										} else if (vs == ValueStatus.ADDED) {
											addedValue = v;
										}
									}

									if (committedValue != null) {
										variableValue = committedValue;
									} else if (addedValue != null) {
										variableValue = addedValue;
									} else {
										variableValue = removedValue;
									}
								}

								if (variableValue != null) {
									variableValues.put(bindingName.substring(2), variableValue);
								}
							} else {
								variableValues.put(bindingName.substring(2), value);
							}
						}
					}
				}

				String interpolatedShow = template.instantiate(variableValues);

				if (interpolatedShow != null && !interpolatedShow.isEmpty()) {
					show = interpolatedShow;
				}
			}

			if (show != null) {
				renderings.put(resource, vf.createLiteral(show));
			} else if (fallbackToTerm) {
				renderings.put(resource, null);
			}
		});

		for (Entry<Value, Literal> renderingEntry : renderings.entrySet()) {
			if (renderingEntry.getValue() == null) {
				Resource resource = (Resource) renderingEntry.getKey();
				String show = resource.toString();
				if (resource instanceof IRI) {
					IRI resourceIRI = (IRI) resource;
					String resNs = resourceIRI.getNamespace();
					String prefix = ns2prefix.get(resNs);
					if (prefix != null) {
						show = prefix + ":" + resourceIRI.getLocalName();
					}
				}
				renderingEntry.setValue(vf.createLiteral(show));
			}
		}

		return renderings;
	}

	public static Template parseTemplate(String template) throws TemplateParsingException, IOException {
		StringReader reader = new StringReader(template);

		return parseTemplate(template, reader, 0, new MutableInt(0));
	}

	public static Template parseTemplate(String originalInput, Reader reader, int nesting,
			MutableInt consumedCodePointsCounter) throws TemplateParsingException, IOException {

		List<TemplateComponent> components = new ArrayList<>();

		int c;
		boolean isEscaping = false;
		boolean expectingQuant = false;

		StringBuilder sb = new StringBuilder();

		while ((c = reader.read()) != -1) {
			consumedCodePointsCounter.increment();
			if (isEscaping) {
				isEscaping = false;
				sb.append(Character.toChars(c));
			} else {
				if (c == '\\') {
					isEscaping = true;
				} else if (c == '$') {
					if (sb.length() != 0) {
						components.add(new LiteralText(sb.toString()));
						sb.setLength(0);
					}
					VariableComponent varComp = parseVariable(originalInput, reader,
							consumedCodePointsCounter);
					components.add(varComp);
					continue; // skip later positionm increment
				} else if (c == '(') {
					if (sb.length() != 0) {
						components.add(new LiteralText(sb.toString()));
						sb.setLength(0);
					}
					Template nestedTemplate = parseTemplate(originalInput, reader, nesting + 1,
							consumedCodePointsCounter);
					components.add(new NestedTemplateComponent(nestedTemplate));
					continue; // skip later position increment
				} else if (c == ')') {
					if (nesting == 0) {
						throw new TemplateParsingException(originalInput,
								consumedCodePointsCounter.getValue() - 1, "Unexpected ')'");
					}

					if (sb.length() != 0) {
						components.add(new LiteralText(sb.toString()));
						sb.setLength(0);
					}

					expectingQuant = true;
				} else if (expectingQuant) {
					if (c != '?') {
						throw new TemplateParsingException(originalInput,
								consumedCodePointsCounter.getValue() - 1,
								"Found '" + String.valueOf(Character.toChars(c)) + "' but expected '?'");
					}
					expectingQuant = false;
					if (components.isEmpty()) {
						components.add(new LiteralText(""));
					}

					return new Template(components);
				} else {
					sb.append(Character.toChars(c));
				}
			}
		}

		if (isEscaping) {
			throw new TemplateParsingException(originalInput, consumedCodePointsCounter.getValue(),
					"Unterminated escape sequence");
		}

		if (expectingQuant) {
			throw new TemplateParsingException(originalInput, consumedCodePointsCounter.getValue(),
					"Missing '?' before the end of input");
		}

		if (nesting != 0) {
			throw new TemplateParsingException(originalInput, consumedCodePointsCounter.getValue(),
					"Missing ')' before the end of input");
		}

		if (sb.length() != 0) {
			components.add(new LiteralText(sb.toString()));
		}

		if (components.isEmpty()) {
			components.add(new LiteralText(""));
		}

		return new Template(components);
	}

	private static VariableComponent parseVariable(String originalInput, Reader reader,
			MutableInt consumedCodePointsCounter) throws TemplateParsingException, IOException {
		int c;
		boolean expectingOpen = true;
		StringBuilder varName = new StringBuilder();

		while ((c = reader.read()) != -1) {
			consumedCodePointsCounter.increment();
			if (expectingOpen) {
				if (c != '{') {
					throw new TemplateParsingException(originalInput,
							consumedCodePointsCounter.getValue() - 1,
							"Found '" + String.valueOf(Character.toChars(c)) + "' but expected '{'");
				} else {
					expectingOpen = false;
				}
			} else {
				if (Character.isLetter(c)) {
					varName.append(Character.toChars(c));
				} else if (c == '}') {
					return new VariableComponent(varName.toString());
				} else {
					throw new TemplateParsingException(originalInput,
							consumedCodePointsCounter.getValue() - 1,
							"Unexpected character '" + String.valueOf(Character.toChars(c)) + "'");
				}
			}
		}

		throw new TemplateParsingException(originalInput, consumedCodePointsCounter.getValue(),
				"Missing '}' before the end of input");
	}

	public static Optional<RenderingEngine> getRenderingEngineForLexicalizationModel(
			ExtensionPointManager exptMgr, IRI lexicalizationModel) {

		Optional<PluginSpecification> spec = getRenderingEngineSpecificationForLexicalModel(
				lexicalizationModel);

		return spec.map(s -> {
			try {
				return exptMgr.instantiateExtension(RenderingEngine.class, s);
			} catch (IllegalArgumentException | NoSuchExtensionException | WrongPropertiesException
					| STPropertyAccessException | InvalidConfigurationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static Optional<PluginSpecification> getRenderingEngineSpecificationForLexicalModel(
			IRI lexicalizationModel) {
		String extensionID;
		String configType;
		if (lexicalizationModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
			extensionID = "it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngine";
			configType = "it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngineConfiguration";
		} else if (lexicalizationModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			extensionID = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngine";
			configType = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngineConfiguration";
		} else if (lexicalizationModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			extensionID = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngine";
			configType = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngineConfiguration";
		} else if (lexicalizationModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			extensionID = "it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEngine";
			configType = "it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEngineConfiguration";
		} else {
			return Optional.empty();
		}

		ObjectNode configObj = JsonNodeFactory.instance.objectNode();
		configObj.put("@type", configType);
		PluginSpecification spec = new PluginSpecification(extensionID, null, null, configObj);
		return Optional.of(spec);
	}

}
