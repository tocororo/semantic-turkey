package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class OtherPropertiesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OtherPropertiesStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "properties", Collections.emptySet());
	}

}
