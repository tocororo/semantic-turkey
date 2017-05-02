package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;

@JsonSerialize(using = PropertyFacetsSectionSerializer.class)
public class PropertyFacetsSection implements ResourceViewSection {
	private final boolean symmetric;
	private final boolean symmetricExplicit;

	private final boolean functional;
	private final boolean functionalExplicit;

	private final boolean inverseFunctional;
	private final boolean inverseFunctionalExplicit;

	private final boolean transitive;
	private final boolean transitiveExplicit;

	private final PredicateObjectsListSection inverseOf;
	private boolean asymmetric;
	private boolean asymmetricExplicit;
	private boolean reflexive;
	private boolean reflexiveExplicit;
	private boolean irreflexive;
	private boolean irreflexiveExplicit;

	/**
	 * @deprecated Use
	 *             {@link #PropertyFacetsSection(boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,PredicateObjectsListSection)}
	 *             instead
	 */
	public PropertyFacetsSection(boolean symmetric, boolean symmetricExplicit, boolean asymmetric,
			boolean asymmetricExplicit, boolean functional, boolean functionalExplicit,
			boolean inverseFunctional, boolean inverseFunctionalExplicit, boolean transitive,
			boolean transitiveExplicit, PredicateObjectsListSection inverseOf) {
		this(symmetric, symmetricExplicit, asymmetric, asymmetricExplicit, functional, functionalExplicit,
				inverseFunctional, inverseFunctionalExplicit, false, false, false, false, transitive,
				transitiveExplicit, inverseOf);
	}

	public PropertyFacetsSection(boolean symmetric, boolean symmetricExplicit, boolean asymmetric,
			boolean asymmetricExplicit, boolean functional, boolean functionalExplicit,
			boolean inverseFunctional, boolean inverseFunctionalExplicit, boolean reflexive,
			boolean reflexiveExplicit, boolean irreflexive, boolean irreflexiveExplicit, boolean transitive,
			boolean transitiveExplicit, PredicateObjectsListSection inverseOf) {
		this.symmetric = symmetric;
		this.symmetricExplicit = symmetricExplicit;
		this.asymmetric = asymmetric;
		this.asymmetricExplicit = asymmetricExplicit;
		this.functional = functional;
		this.functionalExplicit = functionalExplicit;
		this.inverseFunctional = inverseFunctional;
		this.inverseFunctionalExplicit = inverseFunctionalExplicit;
		this.reflexive = reflexive;
		this.reflexiveExplicit = reflexiveExplicit;
		this.irreflexive = irreflexive;
		this.irreflexiveExplicit = irreflexiveExplicit;
		this.transitive = transitive;
		this.transitiveExplicit = transitiveExplicit;
		this.inverseOf = inverseOf;
	}

	public boolean isSymmetric() {
		return symmetric;
	}

	public boolean isSymmetricExplicit() {
		return symmetricExplicit;
	}

	public boolean isAsymmetric() {
		return asymmetric;
	}

	public boolean isAsymmetricExplicit() {
		return asymmetricExplicit;
	}

	public boolean isFunctional() {
		return functional;
	}

	public boolean isFunctionalExplicit() {
		return functionalExplicit;
	}

	public boolean isInverseFunctional() {
		return inverseFunctional;
	}

	public boolean isInverseFunctionalExplicit() {
		return inverseFunctionalExplicit;
	}

	public boolean isReflexive() {
		return reflexive;
	}

	public boolean isReflexiveExplicit() {
		return reflexiveExplicit;
	}

	public boolean isIrreflexive() {
		return irreflexive;
	}

	public boolean isIrreflexiveExplicit() {
		return irreflexiveExplicit;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public boolean isTransitiveExplicit() {
		return transitiveExplicit;
	}

	public PredicateObjectsListSection getInverseOf() {
		return inverseOf;
	}
}
