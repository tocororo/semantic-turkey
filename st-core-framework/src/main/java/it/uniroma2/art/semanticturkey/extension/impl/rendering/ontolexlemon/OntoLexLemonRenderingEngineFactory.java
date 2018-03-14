package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;

/**
 * Factory for the instantiation of {@link OntoLexLemonRenderingEngine}.
 */
public class OntoLexLemonRenderingEngineFactory implements ExtensionFactory<OntoLexLemonRenderingEngine> {

	@Override
	public String getName() {
		return "W3C OntoLex-Lemon Rendering Engine";
	}

	@Override
	public String getDescription() {
		return "A RenderingEngine based on the W3C OntoLex-Lemon Model)";
	}
}
