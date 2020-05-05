package it.uniroma2.art.semanticturkey.config.template;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;
import java.util.Map;

public class StoredTemplateConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Stored ResourceView Template Configuration";
	}

	@STProperty(description = "Template that specifiec for each resource type which partitions are hidden", displayName = "Template")
	public Map<RDFResourceRole, List<String>> template;

}
