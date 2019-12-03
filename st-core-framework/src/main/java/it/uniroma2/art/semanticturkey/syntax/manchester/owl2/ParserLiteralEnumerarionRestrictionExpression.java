package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.LiteralListContext;
import org.eclipse.rdf4j.model.ValueFactory;

import java.util.Map;

public class ParserLiteralEnumerarionRestrictionExpression extends ParserDescription{

	private ManchesterClassInterface mci = null;

	public ParserLiteralEnumerarionRestrictionExpression(ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) {
		super(valueFactory, prefixToNamespacesMap);
	}

	public ManchesterClassInterface getManchesterClass() {
		return mci;
	}

	// the only entry point for this class to parse the object property (which is the main element)
	@Override
	public void enterLiteralList(LiteralListContext ctx) {
		mci = parseLiteralList(ctx);
	}

}
