package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * Factory for the instantiation of {@link SKOSRenderingEngine}.
 */
public class SKOSRenderingEngineFactory implements ExtensionFactory<SKOSRenderingEngine> {

	@Override
	public String getName() {
		return "SKOS Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on skos:prefLabel(s)";
	}
}
