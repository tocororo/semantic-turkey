package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.InverseObjectPropertyContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyIRIContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.PrefixedNameContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DatatypeRestrictionContext;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import java.util.Map;

public class ParserDatatypeRestrictionExpression extends ParserDescription{

	private ManchesterClassInterface mci = null;

	public ParserDatatypeRestrictionExpression(ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) {
		super(valueFactory, prefixToNamespacesMap);
	}

	public ManchesterClassInterface getManchesterClass() {
		return mci;
	}

	// the only entry point for this class to parse the object property (which is the main element)
	@Override
	public void enterDatatypeRestriction(DatatypeRestrictionContext ctx) {
		mci = parseDatatypeRestriction(ctx);
	}

}
