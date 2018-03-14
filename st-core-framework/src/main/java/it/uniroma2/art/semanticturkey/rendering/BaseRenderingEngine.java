package it.uniroma2.art.semanticturkey.rendering;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.UsersManager;

public abstract class BaseRenderingEngine implements RenderingEngine {
	private static final Logger logger = LoggerFactory.getLogger(BaseRenderingEngine.class);

	private static class LabelComparator implements Comparator<Literal> {

		public static final LabelComparator INSTANCE = new LabelComparator();

		@Override
		public int compare(Literal o1, Literal o2) {

			int langCompare = compare(o1.getLanguage().orElse(null), o2.getLanguage().orElse(null));

			if (langCompare == 0) {
				return compare(o1.getLabel(), o2.getLabel());
			} else {
				return langCompare;
			}
		}

		private static int compare(String s1, String s2) {
			if (Objects.equal(s1, s2)) {
				return 0;
			} else {
				if (s1 == null) {
					return -1;
				} else if (s2 == null) {
					return 1;
				} else {
					return s1.compareTo(s2);
				}
			}
		}

	}

	private static final Pattern rawLabelDestructuringPattern = Pattern
			.compile("((?:\\\\@|\\\\,|[^@,])+)(?:@((?:\\\\@|\\\\,|[^@,])+))?");

	private AbstractLabelBasedRenderingEngineConfiguration config;
	protected String languages;

	public BaseRenderingEngine(AbstractLabelBasedRenderingEngineConfiguration config) {
		this.config = config;
		this.languages = config.languages;

		if (this.languages == null) {
			this.languages = "*";
		}
	}

	private static final Pattern propPattern = Pattern
			.compile("\\$\\{" + Pattern.quote(STPropertiesManager.PREF_LANGUAGES) + "\\}");

	/**
	 * Computes the list of languages by interpolating the configured languages with ST Properties. The order
	 * of languages is significative, since it may determine the order of labels displayed by concrete
	 * rendering engines.
	 * 
	 * @param currentProject
	 * @return
	 */
	private List<String> computeLanguages(Project currentProject) {
		StringBuffer sb = new StringBuffer();
		Matcher m = propPattern.matcher(languages);
		String languagesPropValue = null;
		while (m.find()) {
			if (languagesPropValue == null) {
				try {
					languagesPropValue = STPropertiesManager.getPUSetting(
							STPropertiesManager.PREF_LANGUAGES, currentProject, UsersManager.getLoggedUser(),
							RenderingEngine.class.getName());
				} catch (STPropertyAccessException e) {
					logger.debug("Could not access property: " + STPropertiesManager.PREF_LANGUAGES, e);
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
			return Arrays.stream(interpolatedLanguages.split(",")).map(String::trim).collect(toList());
		}
	}

	@Override
	public GraphPattern getGraphPattern(Project currentProject) {
		StringBuilder gp = new StringBuilder();
		getGraphPatternInternal(gp);

		List<String> acceptedLanguges = computeLanguages(currentProject);

		if (!acceptedLanguges.isEmpty()) {
			gp.append(String.format(" FILTER(LANG(?labelInternal) IN (%s))", acceptedLanguges.stream()
					.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"").collect(joining(", "))));
		}
		gp.append("BIND(REPLACE(str(?labelInternal), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLexicalForm)");
		gp.append("BIND(REPLACE(lang(?labelInternal), \"(,)|(@)\", \"\\\\\\\\$0\") as ?labelLang)");
		gp.append(
				"BIND(IF(?labelLang != \"\", CONCAT(STR(?labelLexicalForm), \"@\", ?labelLang), ?labelLexicalForm) AS ?labelInternal2)       \n");
		return GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.groupConcat("labelInternal2", "label"))
				.pattern(gp.toString()).graphPattern();
	}

	protected abstract void getGraphPatternInternal(StringBuilder gp);

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

	@Override
	public Map<Value, Literal> processBindings(Project currentProject, List<BindingSet> resultTable) {
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

		// note: there amy be a race condition between this invocation and the generation of the graph pattern
		List<String> acceptedLanguges = computeLanguages(currentProject);

		resultTable.forEach(bindingSet -> {
			Resource resource = (Resource) bindingSet.getValue("resource");
			Literal rawLabelLiteral = ((Literal) bindingSet.getValue("label"));
			String show;
			if (rawLabelLiteral == null || rawLabelLiteral.getLabel().isEmpty()) {
				show = resource.toString();
				if (resource instanceof IRI) {
					IRI resourceIRI = (IRI) resource;
					String resNs = resourceIRI.getNamespace();
					String prefix = ns2prefix.get(resNs);
					if (prefix != null) {
						show = prefix + ":" + resourceIRI.getLocalName();
					}
				}
			} else {
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
					
					if (labels.isEmpty()) continue;
					
					if (sb.length() != 0) {
						sb.append(", ");
					}

					if (lang.isEmpty()) {
						sb.append(labels.stream().collect(joining(", ")));
					} else {
						sb.append(labels.stream().map(l -> l + " (" + lang + ")")
								.collect(joining(", ")));
					}

				}
				
				show = sb.toString();
			}

			renderings.put(resource, vf.createLiteral(show));
		});

		return renderings;
	}
}
