package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * An implementation of {@link RenderingEngine} dealing with <code>rdfs:label</a>s.
 * 
 */
public class RDFSRenderingEngine implements RenderingEngine {

	@Override
	public Map<ARTResource, String> render(Project<?> project, ResourcePosition subjectPosition,
			ARTResource subject, Collection<ARTStatement> statements, Collection<ARTResource> resources,
			Collection<TupleBindings> bindings, String varPrefix) throws ModelAccessException,
			DataAccessException {

		Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();

		for (ARTResource r : resources) {
			if (r.isURIResource()) {
				uriresourceToBeRendered.add(r.asURIResource());
			}
		}

		///////////////////////////////
		//// Process subject statements
		
		Multimap<ARTResource, ARTLiteral> labelBuilding = HashMultimap.create();

		for (ARTStatement stmt : statements) {
			if (resources.contains(stmt.getSubject()) && stmt.getPredicate().equals(RDFS.Res.LABEL)) {
				ARTURIResource resourceUri = stmt.getSubject().asURIResource();

				ARTNode resourceNode = stmt.getSubject();
				ARTNode labelNode = stmt.getObject();

				if (labelNode.isLiteral()) {
					ARTLiteral labelLiteral = labelNode.asLiteral();

					labelBuilding.put(resourceNode.asResource(), labelLiteral);
				}
			}
		}

		///////////////////////////
		//// Process tuple bindings
		
		String resourceLabelVar = varPrefix + "label";
		String subjectLabelVar = varPrefix + "subject_label";

		String resourceVar = "resource";

		for (TupleBindings aBinding : bindings) {
			ARTResource res;
			ARTLiteral label = null;

			if (aBinding.hasBinding(resourceLabelVar)) {
				res = aBinding.getBoundValue(resourceVar).asResource();
				label = aBinding.getBoundValue(resourceLabelVar).asLiteral();
			}
			if (aBinding.hasBinding(subjectLabelVar)) {
				res = subject;
				label = aBinding.getBoundValue(subjectLabelVar).asLiteral();
			} else {
				continue;
			}

			labelBuilding.put(res, label);
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

	@Override
	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRendered, String varPrefix) {
		return String
				.format("{?resource <http://www.w3.org/2000/01/rdf-schema#label> ?%1$slabel .} union {%2$s <http://www.w3.org/2000/01/rdf-schema#label> ?%1$ssubject_label .}",
						varPrefix, RDFNodeSerializer.toNT(resourceToBeRendered));
	}

	// @Override
	// public Map<ARTResource, String> render(Project<?> project, ARTResource subject,
	// Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
	// DataAccessException {
	// Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();
	//
	// for (ARTResource res : resources) {
	// if (res.isURIResource()) {
	// uriresourceToBeRendered.add(res.asURIResource());
	// }
	// }
	//
	// TupleQueryDataAccess dataAccess = DataAccessFactory.createTupleQueryDataAccess(project);
	// TupleBindingsIterator bindingsIt = dataAccess.retrieveInformationAbout(uriresourceToBeRendered,
	// "?resource <http://www.w3.org/2000/01/rdf-schema#label> ?label");
	//
	// Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();
	//
	// while (bindingsIt.streamOpen()) {
	// TupleBindings binding = bindingsIt.getNext();
	//
	// ARTURIResource uriResource = binding.getBoundValue("resource").asURIResource();
	// ARTLiteral label = binding.getBoundValue("label").asLiteral();
	//
	// String prevRendering = resource2rendering.get(uriResource);
	//
	// if (prevRendering == null) {
	// prevRendering = "";
	// } else {
	// prevRendering = prevRendering + "; ";
	// }
	//
	// prevRendering = prevRendering + label.getLabel() + " (" + label.getLanguage() + ")";
	//
	// resource2rendering.put(uriResource, prevRendering);
	// }
	//
	// bindingsIt.close();
	//
	// return resource2rendering;
	// }

	// @Override
	// public Map<ARTResource, String> render(Project<?> project, ARTResource subject,
	// Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
	// DataAccessException {
	//
	// Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();
	//
	// for (ARTResource res : resources) {
	// if (res.isURIResource()) {
	// uriresourceToBeRendered.add(res.asURIResource());
	// }
	// }
	//
	// DescribeDataAccess dataAccess = DataAccessFactory.createDescribeDataAccess(project);
	// Collection<ARTStatement> additionalStatements =
	// dataAccess.retrieveInformationAbout(uriresourceToBeRendered);
	//
	// Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();
	//
	// for (ARTResource res : resources) {
	// Collection<ARTStatement> relevantCollection;
	// if (res.isBlank()) {
	// relevantCollection = statements;
	// } else {
	// relevantCollection = additionalStatements;
	// }
	//
	// Collection<ARTStatement> lexicalizationTriples = Collections2.filter(relevantCollection,
	// StatementWithAnyOfGivenComponents_Predicate.getFilter(res, RDFS.Res.LABEL, NodeFilters.ANY));
	//
	// StringBuilder sb = new StringBuilder();
	//
	// for (ARTStatement stmt : lexicalizationTriples) {
	// ARTLiteral label = stmt.getObject().asLiteral();
	//
	// sb.append(label.getLabel() + " (" + label.getLanguage() + ")");
	// sb.append(", ");
	// }
	//
	// String temp = sb.toString();
	//
	// if (!temp.equals("")) {
	// temp = temp.substring(0, temp.length() - 2);
	// resource2rendering.put(res, temp);
	// }
	// }
	//
	// return resource2rendering;
	// }

}
