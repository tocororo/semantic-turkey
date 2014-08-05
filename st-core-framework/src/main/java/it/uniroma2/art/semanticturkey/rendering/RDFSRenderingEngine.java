package it.uniroma2.art.semanticturkey.rendering;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.DataAccessFactory;
import it.uniroma2.art.semanticturkey.data.access.PropertyPatternDataAccess;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class RDFSRenderingEngine implements RenderingEngine {

	@Override
	public Map<ARTResource, String> render(Project<?> project, ARTResource subject,
			Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
			DataAccessException {
		Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();

		for (ARTResource res : resources) {
			if (res.isURIResource()) {
				uriresourceToBeRendered.add(res.asURIResource());
			}
		}

		PropertyPatternDataAccess dataAccess = DataAccessFactory.createPropertyPatternDataAccess(project);
		Multimap<ARTURIResource, ARTNode> propertyPattern = HashMultimap.create();
		propertyPattern.put(RDFS.Res.LABEL, NodeFilters.ANY);
		Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> subj2descriptionMap = dataAccess.retrieveInformationAbout(uriresourceToBeRendered, propertyPattern);
	
		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();

		for (ARTURIResource subj : subj2descriptionMap.keySet()) {
			Multimap<ARTURIResource, ARTNode> descr = subj2descriptionMap.get(subj);
			Collection<ARTNode> labels = descr.get(RDFS.Res.LABEL);

			StringBuilder renderingBuilder = new StringBuilder();
			
			Iterator<ARTNode> it = labels.iterator();
			
			while (it.hasNext()) {
				ARTNode obj = it.next();
				
				if (obj.isLiteral()) {
					ARTLiteral objLiteral = obj.asLiteral();
					
					renderingBuilder.append(objLiteral.getLabel()).append(" ").append("(").append(objLiteral.getLanguage()).append(")");
					
					if (it.hasNext()) {
						renderingBuilder.append(", ");
					}
				}
			}
			
			String rendering = renderingBuilder.toString();
			
			if (!rendering.isEmpty()) {
				resource2rendering.put(subj, rendering);
			}
		}
		
		return resource2rendering;
	}
	
//	@Override
//	public Map<ARTResource, String> render(Project<?> project, ARTResource subject,
//			Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
//			DataAccessException {
//		Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();
//
//		for (ARTResource res : resources) {
//			if (res.isURIResource()) {
//				uriresourceToBeRendered.add(res.asURIResource());
//			}
//		}
//
//		TupleQueryDataAccess dataAccess = DataAccessFactory.createTupleQueryDataAccess(project);
//		TupleBindingsIterator bindingsIt = dataAccess.retrieveInformationAbout(uriresourceToBeRendered, "?resource <http://www.w3.org/2000/01/rdf-schema#label> ?label");
//
//		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();
//
//		while (bindingsIt.streamOpen()) {
//			TupleBindings binding = bindingsIt.getNext();
//			
//			ARTURIResource uriResource = binding.getBoundValue("resource").asURIResource();
//			ARTLiteral label = binding.getBoundValue("label").asLiteral();
//
//			String prevRendering = resource2rendering.get(uriResource);
//			
//			if (prevRendering == null) {
//				prevRendering = "";
//			} else {
//				prevRendering = prevRendering + "; ";
//			}
//			
//			prevRendering = prevRendering + label.getLabel() + " (" + label.getLanguage() + ")";
//			
//			resource2rendering.put(uriResource, prevRendering);
//		}
//		
//		bindingsIt.close();
//		
//		return resource2rendering;
//	}
	
//	@Override
//	public Map<ARTResource, String> render(Project<?> project, ARTResource subject,
//			Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
//			DataAccessException {
//
//		Set<ARTURIResource> uriresourceToBeRendered = new HashSet<ARTURIResource>();
//
//		for (ARTResource res : resources) {
//			if (res.isURIResource()) {
//				uriresourceToBeRendered.add(res.asURIResource());
//			}
//		}
//
//		DescribeDataAccess dataAccess = DataAccessFactory.createDescribeDataAccess(project);
//		Collection<ARTStatement> additionalStatements = dataAccess.retrieveInformationAbout(uriresourceToBeRendered);
//
//		Map<ARTResource, String> resource2rendering = new HashMap<ARTResource, String>();
//
//		for (ARTResource res : resources) {
//			Collection<ARTStatement> relevantCollection;
//			if (res.isBlank()) {
//				relevantCollection = statements;
//			} else {
//				relevantCollection = additionalStatements;
//			}
//			
//			Collection<ARTStatement> lexicalizationTriples = Collections2.filter(relevantCollection,
//					StatementWithAnyOfGivenComponents_Predicate.getFilter(res, RDFS.Res.LABEL, NodeFilters.ANY));
//			
//			StringBuilder sb = new StringBuilder();
//			
//			for (ARTStatement stmt : lexicalizationTriples) {
//				ARTLiteral label = stmt.getObject().asLiteral();
//				
//				sb.append(label.getLabel() + " (" + label.getLanguage() + ")");
//				sb.append(", ");
//			}
//			
//			String temp = sb.toString();
//			
//			if (!temp.equals("")) {
//				temp = temp.substring(0, temp.length() - 2);
//				resource2rendering.put(res, temp);
//			}
//		}
//
//		return resource2rendering;
//	}

}
