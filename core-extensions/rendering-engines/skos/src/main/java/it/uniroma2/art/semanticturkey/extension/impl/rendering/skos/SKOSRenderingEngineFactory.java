package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;

/**
 * Factory for the instantiation of {@link SKOSRenderingEngine}.
 */
public class SKOSRenderingEngineFactory
		implements NonConfigurableExtensionFactory<SKOSRenderingEngine>, PUSettingsManager<SKOSRenderingEnginePUSettings> {

	@Override
	public String getName() {
		return "SKOS Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on skos:prefLabel(s)";
	}

	@Override
	public SKOSRenderingEngine createInstance() {
		return new SKOSRenderingEngine(this);
	}
	
	
}
