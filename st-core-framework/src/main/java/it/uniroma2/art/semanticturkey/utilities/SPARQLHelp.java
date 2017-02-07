package it.uniroma2.art.semanticturkey.utilities;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.queryrender.RenderUtils;

public class SPARQLHelp {
	
	public static String toSPARQL(Value value){
		StringBuilder builder = new StringBuilder();
		if (value instanceof IRI) {
			IRI aURI = (IRI)value;
			builder.append("<").append(aURI.toString()).append(">");
		}
		else if (value instanceof BNode) {
			builder.append("_:").append(((BNode)value).getID());
		}
		else if (value instanceof Literal) {
			Literal aLit = (Literal)value;

			builder.append("\"\"\"").append(RenderUtils.escape(aLit.getLabel())).append("\"\"\"");

			if (Literals.isLanguageLiteral(aLit)) {
				builder.append("@").append(aLit.getLanguage().get());
			}
			else {
				builder.append("^^<").append(aLit.getDatatype().toString()).append(">");
			}
		}

		return builder.toString();
		
	}
}
