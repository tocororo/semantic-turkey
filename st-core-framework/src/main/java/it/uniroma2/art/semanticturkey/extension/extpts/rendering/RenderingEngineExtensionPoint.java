package it.uniroma2.art.semanticturkey.extension.extpts.rendering;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class RenderingEngineExtensionPoint
		implements ExtensionPoint, PUSettingsManager<RenderingEnginePUSettings> {
	@Override
	public Class<?> getInterface() {
		return RenderingEngine.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT;
	}

}
