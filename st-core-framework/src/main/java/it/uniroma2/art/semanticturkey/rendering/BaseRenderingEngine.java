package it.uniroma2.art.semanticturkey.rendering;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	 * Computes the set of languages by interpolating the configured langues with ST Properties.
	 * 
	 * @param currentProject
	 * @return
	 */
	private Set<String> computeLanguages(Project currentProject) {
		StringBuffer sb = new StringBuffer();
		Matcher m = propPattern.matcher(languages);
		String languagesPropValue = null;
		while (m.find()) {
			if (languagesPropValue == null) {
				try {
					languagesPropValue = STPropertiesManager.getProjectPreference(
							STPropertiesManager.PREF_LANGUAGES, currentProject, UsersManager.getLoggedUser(), RenderingEngine.class.getName());
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
			return Collections.emptySet();
		} else {
			return Arrays.stream(interpolatedLanguages.split(",")).map(String::trim).collect(toSet());
		}
	}

	@Override
	public GraphPattern getGraphPattern(Project currentProject) {
		StringBuilder gp = new StringBuilder();
		getGraphPatternInternal(gp);

		Set<String> acceptedLanguges = computeLanguages(currentProject);

		if (!acceptedLanguges.isEmpty()) {
			gp.append(String.format(" FILTER(LANG(?labelInternal) IN (%s))", acceptedLanguges.stream()
					.map(lang -> "\"" + SPARQLUtil.encodeString(lang) + "\"").collect(joining(", "))));
		}
		gp.append(
				"BIND(IF(LANG(?labelInternal) != \"\", CONCAT(STR(?labelInternal), \" (\", LANG(?labelInternal), \")\"), STR(?labelInternal)) AS ?labelInternal2)       \n");
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

		resultTable.forEach(bindingSet -> {
			Resource resource = (Resource) bindingSet.getValue("resource");
			String show = ((Literal) bindingSet.getValue("label")).getLabel();
			if (show.isEmpty()) {
				show = resource.toString();
				if (resource instanceof IRI) {
					IRI resourceIRI = (IRI) resource;
					String resNs = resourceIRI.getNamespace();
					String prefix = ns2prefix.get(resNs);
					if (prefix != null) {
						show = prefix + ":" + resourceIRI.getLocalName();
					}
				}
			}

			renderings.put(resource, vf.createLiteral(show));
		});

		return renderings;
	}
}
