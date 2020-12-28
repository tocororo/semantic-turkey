package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

public class GraphDBSEConfiguration extends AbstractGraphDBConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.GraphDBSEConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
