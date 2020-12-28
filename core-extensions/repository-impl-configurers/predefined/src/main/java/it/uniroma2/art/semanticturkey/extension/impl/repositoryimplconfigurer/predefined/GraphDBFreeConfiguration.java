package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

public class GraphDBFreeConfiguration extends AbstractGraphDBConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.GraphDBFreeConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
