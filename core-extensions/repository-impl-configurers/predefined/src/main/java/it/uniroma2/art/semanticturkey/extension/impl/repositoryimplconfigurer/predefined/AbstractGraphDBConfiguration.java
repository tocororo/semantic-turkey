package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Abstract base class of GraphDB configurations. For information about the configuration properties, see:
 * http://graphdb.ontotext.com/documentation/free/configuring-a-repository.html#configuration-parameters
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class AbstractGraphDBConfiguration
		implements PredefinedConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.AbstractGraphDBConfiguration";

		public static final String baseURL$description = keyBase + ".baseURL.description";
		public static final String baseURL$displayName = keyBase + ".baseURL.displayName";
		public static final String defaultNS$description = keyBase + ".defaultNS.description";
		public static final String defaultNS$displayName = keyBase + ".defaultNS.displayName";
		public static final String entityIndexSize$description = keyBase + ".entityIndexSize.description";
		public static final String entityIndexSize$displayName = keyBase + ".entityIndexSize.displayName";
		public static final String entityIdSize$description = keyBase + ".entityIdSize.description";
		public static final String entityIdSize$displayName = keyBase + ".entityIdSize.displayName";
		public static final String imports$description = keyBase + ".imports.description";
		public static final String imports$displayName = keyBase + ".imports.displayName";
		public static final String repositoryType$description = keyBase + ".repositoryType.description";
		public static final String repositoryType$displayName = keyBase + ".repositoryType.displayName";
		public static final String ruleset$description = keyBase + ".ruleset.description";
		public static final String ruleset$displayName = keyBase + ".ruleset.displayName";
		public static final String storageFolder$description = keyBase + ".storageFolder.description";
		public static final String storageFolder$displayName = keyBase + ".storageFolder.displayName";
		public static final String enableContextIndex$description = keyBase + ".enableContextIndex.description";
		public static final String enableContextIndex$displayName = keyBase + ".enableContextIndex.displayName";
		public static final String enablePredicateList$description = keyBase + ".enablePredicateList.description";
		public static final String enablePredicateList$displayName = keyBase + ".enablePredicateList.displayName";
		public static final String inMemoryLiteralProperties$description = keyBase + ".inMemoryLiteralProperties.description";
		public static final String inMemoryLiteralProperties$displayName = keyBase + ".inMemoryLiteralProperties.displayName";
		public static final String enableLiteralIndex$description = keyBase + ".enableLiteralIndex.description";
		public static final String enableLiteralIndex$displayName = keyBase + ".enableLiteralIndex.displayName";
		public static final String checkForInconsistencies$description = keyBase + ".checkForInconsistencies.description";
		public static final String checkForInconsistencies$displayName = keyBase + ".checkForInconsistencies.displayName";
		public static final String disableSameAs$description = keyBase + ".disableSameAs.description";
		public static final String disableSameAs$displayName = keyBase + ".disableSameAs.displayName";
		public static final String queryTimeout$description = keyBase + ".queryTimeout.description";
		public static final String queryTimeout$displayName = keyBase + ".queryTimeout.displayName";
		public static final String queryLimitResults$description = keyBase + ".queryLimitResults.description";
		public static final String queryLimitResults$displayName = keyBase + ".queryLimitResults.displayName";
		public static final String throwQueryEvaluationExceptionOnTimeout$description = keyBase + ".throwQueryEvaluationExceptionOnTimeout.description";
		public static final String throwQueryEvaluationExceptionOnTimeout$displayName = keyBase + ".throwQueryEvaluationExceptionOnTimeout.displayName";
		public static final String readOnly$description = keyBase + ".readOnly.description";
		public static final String readOnly$displayName = keyBase + ".readOnly.displayName";
	}

	@STProperty(description = "{" + MessageKeys.baseURL$description + "}", displayName = "{" + MessageKeys.baseURL$displayName + "}")
	public String baseURL = "";

	@STProperty(description = "{" + MessageKeys.defaultNS$description + "}", displayName = "{" + MessageKeys.defaultNS$displayName + "}")
	public String defaultNS = "";

	@STProperty(description = "{" + MessageKeys.entityIndexSize$description + "}", displayName = "{" + MessageKeys.entityIndexSize$displayName + "}")
	public int entityIndexSize = 10000000;

	@STProperty(description = "{" + MessageKeys.entityIdSize$description + "}", displayName = "{" + MessageKeys.entityIdSize$displayName + "}")
	@Enumeration({ "32", "40" })
	public int entityIdSize = 32;

	@STProperty(description = "{" + MessageKeys.imports$description + "}", displayName = "{" + MessageKeys.imports$displayName + "}")
	public String imports = "";

	@STProperty(description = "{" + MessageKeys.repositoryType$description + "}", displayName = "{" + MessageKeys.repositoryType$displayName + "}")
	@Enumeration({ "file-repository", "weighted-file-repository" })
	public String repositoryType = "file-repository";

	@STProperty(description = "{" + MessageKeys.ruleset$description + "}", displayName = "{" + MessageKeys.ruleset$displayName + "}")
	@Enumeration({ "empty", "rdfs", "rdfsplus", "owl-horst", "owl-max", "owl2-rl", "rdfs-optimized",
			"rdfsplus-optimized", "owl-horst-optimized", "owl-max-optimized", "owl2-rl-optimized" })
	public String ruleset = "empty";

	@STProperty(description = "{" + MessageKeys.storageFolder$description + "}", displayName = "{" + MessageKeys.storageFolder$displayName + "}")
	public String storageFolder = "storage";

	@STProperty(description = "{" + MessageKeys.enableContextIndex$description + "}", displayName = "{" + MessageKeys.enableContextIndex$displayName + "}")
	public boolean enableContextIndex = false;

	@STProperty(description = "{" + MessageKeys.enablePredicateList$description + "}", displayName = "{" + MessageKeys.enablePredicateList$displayName + "}")
	public boolean enablePredicateList = true;

	@STProperty(description = "{" + MessageKeys.inMemoryLiteralProperties$description + "}", displayName = "{" + MessageKeys.inMemoryLiteralProperties$displayName + "}")
	public boolean inMemoryLiteralProperties = true;

	@STProperty(description = "{" + MessageKeys.enableLiteralIndex$description + "}", displayName = "{" + MessageKeys.enableLiteralIndex$displayName + "}")
	public boolean enableLiteralIndex = true;

	@STProperty(description = "{" + MessageKeys.checkForInconsistencies$description + "}", displayName = "{" + MessageKeys.checkForInconsistencies$displayName + "}")
	public boolean checkForInconsistencies = false;

	@STProperty(description = "{" + MessageKeys.disableSameAs$description + "}", displayName = "{" + MessageKeys.disableSameAs$displayName + "}")
	public boolean disableSameAs = true;

	@STProperty(description = "{" + MessageKeys.queryTimeout$description + "}", displayName = "{" + MessageKeys.queryTimeout$displayName + "}")
	public int queryTimeout = 0;

	@STProperty(description = "{" + MessageKeys.queryLimitResults$description + "}", displayName = "{" + MessageKeys.queryLimitResults$displayName + "}")
	public int queryLimitResults = 0;

	@STProperty(description = "{" + MessageKeys.throwQueryEvaluationExceptionOnTimeout$description + "}", displayName = "{" + MessageKeys.throwQueryEvaluationExceptionOnTimeout$displayName + "}")
	public boolean throwQueryEvaluationExceptionOnTimeout = false;

	@STProperty(description = "{" + MessageKeys.readOnly$description + "}", displayName = "{" + MessageKeys.readOnly$displayName + "}")
	public boolean readOnly = false;
}
