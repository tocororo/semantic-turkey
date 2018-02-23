package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredSPARQLOperation implements Configuration {

	@Override
	public String getShortName() {
		return "Stored SPARQL Operation";
	}

	@STProperty(description="Query")
	@Required
	public String query;
	
	@STProperty(description="Tells whehter to inlude inferred statements in the evaluation of the query")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	@Required
	public Boolean includeInferred;	
}
