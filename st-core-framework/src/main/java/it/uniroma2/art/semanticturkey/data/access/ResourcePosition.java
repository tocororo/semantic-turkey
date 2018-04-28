package it.uniroma2.art.semanticturkey.data.access;

public abstract class ResourcePosition {
	public abstract String getPosition();

	@Override
	public String toString() {
		return getPosition();
	}
}
