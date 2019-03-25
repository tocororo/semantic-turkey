package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;

/**
 * Factory for the instantiation of {@link RDFSRenderingEngine}.
 */
public class RDFSRenderingEngineFactory
		implements NonConfigurableExtensionFactory<RDFSRenderingEngine>, PUSettingsManager<RDFSRenderingEnginePUSettings> {

	@Override
	public String getName() {
		return "RDFS Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on rdfs:label(s)";
	}

	@Override
	public RDFSRenderingEngine createInstance() {
		return new RDFSRenderingEngine(this);
	}
}
