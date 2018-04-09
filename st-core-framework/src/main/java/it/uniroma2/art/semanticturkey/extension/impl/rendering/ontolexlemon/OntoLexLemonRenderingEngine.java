package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;

/**
 * An implementation of {@link RenderingEngine} dealing with W3C OntoLex-Lemon lexicalizations.
 * 
 */
public class OntoLexLemonRenderingEngine extends BaseRenderingEngine implements RenderingEngine {

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
			//@formatter:off
			"\n{?resource <http://www.w3.org/ns/lemon/ontolex#isDenotedBy>|^<http://www.w3.org/ns/lemon/ontolex#denotes> ?entry.} "+
			"\nUNION " +
			"\n{?sense <http://www.w3.org/ns/lemon/ontolex#reference>|<http://www.w3.org/ns/lemon/ontolex#isReferenceOf> ?resource ." +
			"\n?sense <http://www.w3.org/ns/lemon/ontolex#isSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#sense> ?entry . } " +
			"\nUNION" +
			"\n{BIND(?resource AS ?entry )}"+
			"\n?entry <http://www.w3.org/ns/lemon/ontolex#canonicalForm> [ <http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal ] .\n");
			//@formatter:on
	}

}
