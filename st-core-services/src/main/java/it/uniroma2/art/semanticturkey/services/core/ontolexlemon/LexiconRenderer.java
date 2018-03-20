package it.uniroma2.art.semanticturkey.services.core.ontolexlemon;

import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

public class LexiconRenderer extends BaseRenderingEngine {

	private static AbstractLabelBasedRenderingEngineConfiguration conf;

	static {
		conf = new AbstractLabelBasedRenderingEngineConfiguration() {

			@Override
			public String getShortName() {
				return "foo";
			}

		};
	}

	private LexiconRenderer() {
		super(conf, true);
	}
	
	private LexiconRenderer(boolean fallbackToTerm) {
		super(conf, fallbackToTerm);
	}

	public static final LexiconRenderer INSTANCE = new LexiconRenderer();
	public static final LexiconRenderer INSTANCE_WITHOUT_FALLBACK = new LexiconRenderer(false);

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append("?resource <http://purl.org/dc/terms/title> ?labelInternal .\n");
	}

}