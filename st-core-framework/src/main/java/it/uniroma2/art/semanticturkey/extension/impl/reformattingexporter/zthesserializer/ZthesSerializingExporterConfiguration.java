package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.zthesserializer;

import java.util.Set;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ZthesSerializingExporterConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Zthes Serializing Exporter";
	}
	
	@STProperty(description = "Priority list of language tags which the relation of PT terms are based on", displayName = "pivot languages")
	public Set<String> pivotLanguages;
	
}

