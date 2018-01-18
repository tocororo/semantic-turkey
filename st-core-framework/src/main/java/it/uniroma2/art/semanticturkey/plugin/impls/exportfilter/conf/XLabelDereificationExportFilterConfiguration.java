package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XLabelDereificationExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "XLabel Dereification Export Filter";
	}

	@STProperty(description = "Preserves reified labels in the output")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean preserveReifiedLabels = true;
}
