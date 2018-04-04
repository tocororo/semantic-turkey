package it.uniroma2.art.semanticturkey.plugin.impls.rendering;

import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.OntoLexLemonRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with W3C OntoLex-Lemon lexicalizations.
 * 
 */
public class OntoLexLemonRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	public OntoLexLemonRenderingEngine(OntoLexLemonRenderingEngineConfiguration config) {
		super(config);
	}

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
				"{?resource <http://www.w3.org/ns/lemon/ontolex#isDenotedBy>|^<http://www.w3.org/ns/lemon/ontolex#denotes> ?entry.} UNION { ?sense <http://www.w3.org/ns/lemon/ontolex#reference>|<http://www.w3.org/ns/lemon/ontolex#isReferenceOf> ?resource .  ?sense <http://www.w3.org/ns/lemon/ontolex#isSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#sense> ?entry . } ?entry <http://www.w3.org/ns/lemon/ontolex#canonicalForm> [ <http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal ] .\n");
	}

}
