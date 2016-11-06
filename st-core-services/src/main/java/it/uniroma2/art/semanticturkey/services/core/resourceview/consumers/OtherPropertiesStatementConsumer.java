package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class OtherPropertiesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OtherPropertiesStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "properties", Collections.emptySet());
	}

}
