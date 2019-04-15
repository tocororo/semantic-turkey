package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;

@JsonSerialize(using = PropertyFacetsSectionSerializer.class)
public class PropertyFacetsSection implements ResourceViewSection {
	public static class FacetStructure {
		private final boolean value;
		private final boolean explicit;
		private final TripleScopes tripleScope;

		public FacetStructure(boolean value, boolean explicit, TripleScopes tripleScope) {
			this.value = value;
			this.explicit = explicit;
			this.tripleScope = tripleScope;
		}

		public boolean hold() {
			return value;
		}

		public boolean isExplicit() {
			return explicit;
		}

		public TripleScopes getTripleScope() {
			return tripleScope;
		}
	}

	private final Map<String, FacetStructure> facets;
	private final PredicateObjectsListSection inverseOf;

	public PropertyFacetsSection(Map<String, FacetStructure> facets, PredicateObjectsListSection inverseOf) {
		this.facets = facets;
		this.inverseOf = inverseOf;
	}

	public Map<String, FacetStructure> getFacets() {
		return facets;
	}

	public PredicateObjectsListSection getInverseOf() {
		return inverseOf;
	}

}
