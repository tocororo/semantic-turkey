package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * Factory for the instantiation of {@link SKOSXLRenderingEngine}.
 */
public class SKOSXLRenderingEngineFactory implements ExtensionFactory<SKOSXLRenderingEngine> {

	@Override
	public String getName() {
		return "SKOSXL Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on skosxl:prefLabel(s)";
	}
}
