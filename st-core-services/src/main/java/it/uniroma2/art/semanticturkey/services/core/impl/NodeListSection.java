package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;

import java.util.List;

import org.w3c.dom.Element;

public class NodeListSection implements ResourceViewSection {

	private List<STRDFNode> nodeList;

	public NodeListSection(List<STRDFNode> nodeList) {
		this.nodeList = nodeList;
	}

	@Override
	public void appendToElement(Element parent) {
		RDFXMLHelp.addRDFNodes(parent, nodeList);
	}
}