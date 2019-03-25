package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;

/**
 * Factory for the instantiation of {@link SKOSXLRenderingEngine}.
 */
public class SKOSXLRenderingEngineFactory implements NonConfigurableExtensionFactory<SKOSXLRenderingEngine>,
		PUSettingsManager<SKOSXLRenderingEnginePUSettings> {

	@Override
	public String getName() {
		return "SKOSXL Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on skosxl:prefLabel(s)";
	}

	@Override
	public SKOSXLRenderingEngine createInstance() {
		return new SKOSXLRenderingEngine(this);
	}
}
