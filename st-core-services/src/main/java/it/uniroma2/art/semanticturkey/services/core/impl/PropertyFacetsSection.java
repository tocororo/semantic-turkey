package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsSectionSerializer;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.List;

import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class PropertyFacetsSection implements ResourceViewSection {

	private boolean symmetric;
	private boolean symmetricExplicit;

	private boolean functional;
	private boolean functionalExplicit;

	private boolean inverseFunctional;
	private boolean inverseFunctionalExplicit;

	private boolean transitive;
	private boolean transitiveExplicit;

	private List<STRDFNode> inverseOf;

	public PropertyFacetsSection(boolean symmetric, boolean symmetricExplicit, boolean functional,
			boolean functionalExplicit, boolean inverseFunctional, boolean inverseFunctionalExplicit,
			boolean transitive, boolean transitiveExplicit, List<STRDFNode> inverseOf) {
		this.symmetric = symmetric;
		this.symmetricExplicit = symmetricExplicit;
		this.functional = functional;
		this.functionalExplicit = functionalExplicit;
		this.inverseFunctional = inverseFunctional;
		this.inverseFunctionalExplicit = inverseFunctionalExplicit;
		this.transitive = transitive;
		this.transitiveExplicit = transitiveExplicit;
		this.inverseOf = inverseOf;
	}

	@Override
	public void appendToElement(Element parent) {
		if (symmetric) {
			Element symmetricElement = XMLHelp.newElement(parent, "symmetric");
			symmetricElement.setAttribute("value", "true");
			symmetricElement.setAttribute("explicit", Boolean.toString(symmetricExplicit));
		}

		if (functional) {
			Element functionalElement = XMLHelp.newElement(parent, "functional");
			functionalElement.setAttribute("value", "true");
			functionalElement.setAttribute("explicit", Boolean.toString(functionalExplicit));
		}

		if (inverseFunctional) {
			Element inverseFunctionalElement = XMLHelp.newElement(parent, "inverseFunctional");
			inverseFunctionalElement.setAttribute("value", "true");
			inverseFunctionalElement.setAttribute("explicit", Boolean.toString(inverseFunctionalExplicit));
		}

		if (transitive) {
			Element transitiveElement = XMLHelp.newElement(parent, "transitive");
			transitiveElement.setAttribute("value", "true");
			transitiveElement.setAttribute("explicit", Boolean.toString(transitiveExplicit));
		}

		Element inverseOfElement = XMLHelp.newElement(parent, "inverseof");
		RDFXMLHelp.addRDFNodes(inverseOfElement, inverseOf);
	}

}