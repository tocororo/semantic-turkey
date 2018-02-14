package it.uniroma2.art.semanticturkey.extension;

public interface ExtensionPoint extends IdentifiableComponent, ScopedComponent {

	@Override
	default String getId() {
		return getInterface().getName();
	}
	
	Class<?> getInterface();
	
	
}
