package it.uniroma2.art.semanticturkey.rendering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

public abstract class BaseRenderingEngine implements RenderingEngine {
	private static class LabelComparator implements Comparator<ARTLiteral> {

		public static final LabelComparator INSTANCE = new LabelComparator();

		@Override
		public int compare(ARTLiteral o1, ARTLiteral o2) {

			int langCompare = compare(o1.getLanguage(), o2.getLanguage());

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
	protected boolean takeAll;
	protected List<String> languages;

	public BaseRenderingEngine(AbstractLabelBasedRenderingEngineConfiguration config) {
		this.config = config;
		this.takeAll = false;
		this.languages = new ArrayList<String>();

		for (String langTag : config.languages.split(",")) {
			langTag = langTag.trim();

			if (langTag.equals("*")) {
				this.takeAll = true;
				languages.clear();
				break;
			} else {
				this.languages.add(langTag);
			}
		}
	}

	@Override
	public Map<ARTResource, String> render(Project<?> project, ResourcePosition subjectPosition,
			ARTResource subject, OWLModel statements, Collection<ARTResource> resources,
			Collection<TupleBindings> bindings, String varPrefix)
					throws ModelAccessException, DataAccessException {

		Multimap<ARTResource, ARTLiteral> labelBuilding = HashMultimap.create();

		// /////////////////////////////
		// // Process subject statements

		Set<ARTURIResource> plainURIs = getPlainURIs();

		if (!plainURIs.isEmpty()) {
			try (ARTStatementIterator it = statements.listStatements(NodeFilters.ANY, NodeFilters.ANY,
					NodeFilters.ANY, false, NodeFilters.ANY)) {
				while (it.streamOpen()) {
					ARTStatement stmt = it.getNext();
					if (resources.contains(stmt.getSubject()) && plainURIs.contains(stmt.getPredicate())) {
						ARTNode resourceNode = stmt.getSubject();
						ARTNode labelNode = stmt.getObject();

						if (labelNode.isLiteral()) {
							ARTLiteral labelLiteral = labelNode.asLiteral();

							if (takeAll || languages.contains(labelLiteral.getLanguage())) {
								labelBuilding.put(resourceNode.asResource(), labelLiteral);
							}
						}
					}
				}
			}
		}

		// /////////////////////////
		// // Process tuple bindings

		String objectLabelVar = varPrefix + "_object_label";
		String indirectObjectLabelVar = varPrefix + "_indirectObject_label";
		String subjectLabelVar = varPrefix + "_subject_label";

		String objectVar = "object";
		String indirectObjectVar = varPrefix + "_indirectObject";

		for (TupleBindings aBinding : bindings) {
			ARTResource res;
			ARTLiteral label = null;

			if (aBinding.hasBinding(objectLabelVar)) {
				res = aBinding.getBoundValue(objectVar).asResource();
				label = aBinding.getBoundValue(objectLabelVar).asLiteral();
			} else if (aBinding.hasBinding(subjectLabelVar)) {
				res = subject;
				label = aBinding.getBoundValue(subjectLabelVar).asLiteral();
			} else if (aBinding.hasBinding(indirectObjectLabelVar)) {
				res = aBinding.getBoundValue(indirectObjectVar).asResource();
				label = aBinding.getBoundValue(indirectObjectLabelVar).asLiteral();
			} else {
				continue;
			}

			if (takeAll || languages.contains(label.getLanguage())) {
				labelBuilding.put(res, label);
			}
		}

		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();

		for (ARTResource key : labelBuilding.keySet()) {
			StringBuilder sb = new StringBuilder();

			Set<ARTLiteral> sortedLabels = new TreeSet<ARTLiteral>(LabelComparator.INSTANCE);
			sortedLabels.addAll(labelBuilding.get(key));

			for (ARTLiteral label : sortedLabels) {
				if (sb.length() != 0) {
					sb.append(", ");
				}

				sb.append(label.getLabel());

				if (label.getLanguage() != null) {
					sb.append(" (").append(label.getLanguage()).append(")");
				}
			}

			resource2rendering.put(key, sb.toString());
		}

		return resource2rendering;
	}

	protected abstract Set<ARTURIResource> getPlainURIs();

	@Override
	public GraphPattern getGraphPattern() {
		return null;
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}
	
	@Override
	public String getBindingVariable() {
		return "resource";
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		Repository rep = currentProject.getRepository();

		HashMap<String, String> ns2prefix = new HashMap<>();
		RepositoryConnection repConn = RDF4JRepositoryUtils.getConnection(rep);

		try {
			Iterations.stream(repConn.getNamespaces()).forEach(ns -> ns2prefix.put(ns.getName(), ns.getPrefix()));
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
					IRI resourceIRI = (IRI)resource;
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
