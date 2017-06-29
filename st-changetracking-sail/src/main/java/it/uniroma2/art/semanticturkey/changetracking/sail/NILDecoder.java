package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

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
		if (SESAME.NIL.equals(subj)) {
			subj = null;
		}
		
		IRI pred = t.getPredicate();
		if (SESAME.NIL.equals(pred)) {
			pred = null;
		}

		Value obj = t.getObject();
		if (SESAME.NIL.equals(obj)) {
			obj = null;
		}
		
		Resource ctx = t.getContext();
		if (SESAME.NIL.equals(ctx)) {
			ctx = null;
		}
		
		return new QuadPattern(subj, pred, obj, ctx);
	}

}
