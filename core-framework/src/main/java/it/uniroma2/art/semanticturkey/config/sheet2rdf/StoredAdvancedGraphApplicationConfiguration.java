package it.uniroma2.art.semanticturkey.config.sheet2rdf;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.sheet2rdf.header.NodeConversion;

public class StoredAdvancedGraphApplicationConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Stored Advanced Graph Application";
	}
	
	@STProperty(description = "", displayName = "")
	@Required
	public String pattern;
	
	@STProperty(description = "", displayName = "")
	@Required
	public List<NodeConversion> nodes;
	
	@STProperty(description = "", displayName = "")
	public Map<String, String> prefixMapping;
	
	@STProperty(description = "", displayName = "")
	public IRI defaultPredicate;

}
