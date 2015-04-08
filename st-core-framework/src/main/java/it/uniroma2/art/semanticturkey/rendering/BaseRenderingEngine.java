package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.TupleBindings;
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
	@Override
	public Map<ARTResource, String> render(Project<?> project,
			ResourcePosition subjectPosition, ARTResource subject,
			Collection<ARTStatement> statements,
			Collection<ARTResource> resources,
			Collection<TupleBindings> bindings, String varPrefix)
			throws ModelAccessException, DataAccessException {

		Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();

		for (ARTResource r : resources) {
			if (r.isURIResource()) {
				uriresourceToBeRendered.add(r.asURIResource());
			}
		}


		Multimap<ARTResource, ARTLiteral> labelBuilding = HashMultimap.create();

		///////////////////////////////
		//// Process subject statements
		
		Set<ARTURIResource> plainURIs = getPlainURIs();
		
		if (!plainURIs.isEmpty()) {
			for (ARTStatement stmt : statements) {
				if (resources.contains(stmt.getSubject()) && plainURIs.contains(stmt.getPredicate())) {
					ARTNode resourceNode = stmt.getSubject();
					ARTNode labelNode = stmt.getObject();
	
					if (labelNode.isLiteral()) {
						ARTLiteral labelLiteral = labelNode.asLiteral();
	
						labelBuilding.put(resourceNode.asResource(), labelLiteral);
					}
				}
			}
		}
		
		///////////////////////////
		//// Process tuple bindings
		
		String objectLabelVar = varPrefix + "_object_label";
		String subjectLabelVar = varPrefix + "_subject_label";

		String objectVar = "object";

		for (TupleBindings aBinding : bindings) {
			ARTResource res;
			ARTLiteral label = null;

			if (aBinding.hasBinding(objectLabelVar)) {
				res = aBinding.getBoundValue(objectVar).asResource();
				label = aBinding.getBoundValue(objectLabelVar).asLiteral();
			} else if (aBinding.hasBinding(subjectLabelVar)) {
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
	
	protected abstract Set<ARTURIResource> getPlainURIs();
}
