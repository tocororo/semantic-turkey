package it.uniroma2.art.semanticturkey.services.core.ontolexlemon;

import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

public class FormRenderer extends BaseRenderingEngine {

	private static AbstractLabelBasedRenderingEngineConfiguration conf;

	static {
		conf = new AbstractLabelBasedRenderingEngineConfiguration() {

			@Override
			public String getShortName() {
				return "foo";
			}

		};
		conf.languages = null;
	}

	private FormRenderer() {
		super(conf);
	}
	
	private FormRenderer(boolean fallbackToTerm) {
		super(conf, fallbackToTerm);
	}

	public static final FormRenderer INSTANCE = new FormRenderer();
	public static final FormRenderer INSTANCE_WITHOUT_FALLBACK = new FormRenderer(false);

	@Override
	public void getGraphPatternInternal(StringBuilder gp) {
		gp.append("?resource <http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal .\n");
	}

}