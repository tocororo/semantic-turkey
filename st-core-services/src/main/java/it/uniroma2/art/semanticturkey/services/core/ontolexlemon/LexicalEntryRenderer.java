package it.uniroma2.art.semanticturkey.services.core.ontolexlemon;

import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

public class LexicalEntryRenderer extends BaseRenderingEngine {

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

	private LexicalEntryRenderer() {
		super(conf, true);
	}

	private LexicalEntryRenderer(boolean fallbackToTerm) {
		super(conf, fallbackToTerm);
	}

	public static final LexicalEntryRenderer INSTANCE = new LexicalEntryRenderer();
	public static final LexicalEntryRenderer INSTANCE_WITHOUT_FALLBACK = new LexicalEntryRenderer(false);

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
				"?resource <http://www.w3.org/ns/lemon/ontolex#canonicalForm> [<http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal ] .\n");
	}

}