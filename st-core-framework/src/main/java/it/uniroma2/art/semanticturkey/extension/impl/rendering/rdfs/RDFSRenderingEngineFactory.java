package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * Factory for the instantiation of {@link RDFSRenderingEngine}.
 */
public class RDFSRenderingEngineFactory implements ExtensionFactory<RDFSRenderingEngine> {

	@Override
	public String getName() {
		return "RDFS Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on rdfs:label(s)";
	}
}
