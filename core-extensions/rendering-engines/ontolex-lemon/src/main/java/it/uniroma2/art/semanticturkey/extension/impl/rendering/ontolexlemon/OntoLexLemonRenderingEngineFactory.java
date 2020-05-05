package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;

/**
 * Factory for the instantiation of {@link OntoLexLemonRenderingEngine}.
 */
public class OntoLexLemonRenderingEngineFactory implements NonConfigurableExtensionFactory<OntoLexLemonRenderingEngine>,
		PUSettingsManager<OntoLexLemonRenderingEnginePUSettings> {

	@Override
	public String getName() {
		return "W3C OntoLex-Lemon Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on the W3C OntoLex-Lemon Model)";
	}

	@Override
	public OntoLexLemonRenderingEngine createInstance() {
		return new OntoLexLemonRenderingEngine(this);
	}
}
