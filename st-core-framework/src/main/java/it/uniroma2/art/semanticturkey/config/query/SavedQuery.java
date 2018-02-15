package it.uniroma2.art.semanticturkey.config.query;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class SavedQuery implements Configuration {

	@Override
	public String getShortName() {
		return "Saved Query";
	}

	@STProperty(description="Query")
	@Required
	public String query;
	
	@STProperty(description="Query language")
	@Required
	public String queryLanguage = "SPARQL";

	@STProperty(description="Tells whehter to inlude inferred statements in the evaluation of the query")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	@Required
	public boolean includeInferred = true;
	
	@STProperty(description = "Maximum execution time")
	@Required
	public int maxExecTime = 0;
	
	@STProperty(description = "Comma-separated list of graph names to use as default graph")
	@Required
	public String defaultGraphs = "";
	
	@STProperty(description = "Comma-separates list of graph names to use as named graphs")
	@Required
	public String namedGraphs = "";
}
