package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;

/**
 * Maps a statement in which {@link SESAME#NIL} encodes {@code null} into a {@link QuadPattern}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class NILDecoder implements Function<Statement, QuadPattern> {
	
	public static final NILDecoder INSTANCE = new NILDecoder();

	@Override
	public QuadPattern apply(Statement t) {
		Resource subj = t.getSubject();
		if (CHANGELOG.isNull(subj)) {
			subj = null;
		}
		
		IRI pred = t.getPredicate();
		if (CHANGELOG.isNull(pred)) {
			pred = null;
		}

		Value obj = t.getObject();
		if (CHANGELOG.isNull(obj)) {
			obj = null;
		}
		
		Resource ctx = t.getContext();
		if (CHANGELOG.isNull(ctx)) {
			ctx = null;
		}
		
		return new QuadPattern(subj, pred, obj, ctx);
	}

}
