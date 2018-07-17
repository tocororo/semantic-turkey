package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
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
				"\n{"+
				"\n{?resource <"+ONTOLEX.IS_DENOTED_BY.stringValue()+">|^<"+ONTOLEX.DENOTES.stringValue()+">|<" + ONTOLEX.IS_EVOKED_BY + ">|^<" + ONTOLEX.EVOKES + "> ?entry.} "+
				"\nUNION " +
				"\n{?sense <"+ONTOLEX.REFERENCE.stringValue()+">|^<"+ONTOLEX.IS_REFERENCE_OF.stringValue()+">|<" + ONTOLEX.IS_LEXICALIZED_SENSE_OF + ">|^<" + ONTOLEX.LEXICALIZED_SENSE +"> ?resource ." +
				"\n?sense <"+ONTOLEX.IS_SENSE_OF.stringValue()+">|^<"+ONTOLEX.SENSE.stringValue()+"> ?entry . } " +
				"\n?entry <"+ONTOLEX.CANONICAL_FORM.stringValue()+"> [ <"+ONTOLEX.WRITTEN_REP.stringValue()+"> ?labelInternal ] .\n" +
				"\n}" +
				"\nUNION" +
				"\n{?resource <"+DCTERMS.TITLE+"> ?labelInternal . }" +
				"\nUNION" +
				"\n{?resource rdfs:label ?labelInternal . }");
				//@formatter:on
	}

}
