package it.uniroma2.art.semanticturkey.filter;

import java.util.Iterator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.filter.NoLanguageResourcePredicate;
import it.uniroma2.art.owlart.filter.URIResourcePredicate;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFSModel;

/**
 * This class is a copy of it.uniroma2.art.owlart.filter.RootClassesResourcePredicate except for 
 * the replace of 
 * it = Iterators.filter(((DirectReasoning) repo).listDirectSuperClasses(res), filter);
 * with
 * it = Iterators.filter(repo.listSuperClasses(res, false, NodeFilters.ANY), filter);
 * to avoid that local Classes may disappear from the class tree when are equivalent 
 * to "external" Classes
 * @author Tiziano
 *
 */
public class STRootClassesResourcePredicate implements Predicate<ARTResource> {

	RDFSModel repo;

	public STRootClassesResourcePredicate(RDFSModel model) {
		this.repo = model;
	}

	public boolean apply(ARTResource res) {
		Predicate<ARTResource> noLanguageResourcePredicate = new NoLanguageResourcePredicate();

		Predicate<ARTResource> filter = Predicates.and(noLanguageResourcePredicate,
				URIResourcePredicate.uriFilter);

		// this works on the fact that either a class has no superclasses, or it has owl:Thing as its direct
		// superclass; in this second case, owl:Thing is filtered out by the noLanguageResourcePredicate
		// so, globally, it suffices to check that the filtered iterator of superclasses is empty
		Iterator<ARTResource> it;
		try {
			it = Iterators.filter(repo.listSuperClasses(res, false, NodeFilters.ANY), filter);
			if (!it.hasNext())
				return true;
		} catch (ModelAccessException e) {
			return false;
		}

		return false;
		/*
		 * while ( it.hasNext() && (noSuperConcepts=(it.next().equals(OWL.Res.THING))) ) {} return
		 * noSuperConcepts;
		 */
	}
}
