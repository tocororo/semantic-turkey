package it.uniroma2.art.semanticturkey.converters.impl;

import java.net.URI;

import it.uniroma2.art.owlart.model.ARTNode;

public abstract class STSpecificNodeChecks {
	public static void checkURIResourceConstraints(ARTNode node) {
		if (node.isURIResource()) {
			String uriSpec = node.getNominalValue();
			URI.create(uriSpec); // will throw IllegalArgumentException if the URI is not valid (e.g.
									// contains a space)
		}
	}
}
