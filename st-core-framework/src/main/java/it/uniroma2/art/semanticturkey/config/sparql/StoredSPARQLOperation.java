package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredSPARQLOperation implements Configuration {

	@Override
	public String getShortName() {
		return "Stored SPARQL Operation";
	}

	@STProperty(description="SPARQL")
	@Required
	public String sparql;

	@STProperty(description="Tells whether the provided SPARQL string represents a query or an update")
	@Required
	@Enumeration({"query", "update"})
	public String type;
	
	@STProperty(description="Tells whether to inlude inferred statements in the evaluation of the query")
	@Required
	public Boolean includeInferred;
}
