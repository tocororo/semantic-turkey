package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.InverseObjectPropertyContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyIRIContext;
import org.eclipse.rdf4j.model.ValueFactory;

import java.util.Map;

public class ParserObjectPropertyExpression extends ParserManchesterAbstract {

	private ObjectPropertyExpression ope = null;

	public ParserObjectPropertyExpression(ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) {
		super(valueFactory, prefixToNamespacesMap);
	}

	public ObjectPropertyExpression getObjectPropertyExpression() {
		return ope;
	}

	// the only entry point for this class to parse the object property (which is the main element)
	@Override
	public void enterObjectPropertyExpression(ObjectPropertyExpressionContext ctx) {
		if (ope == null) {
			ope = parseRestrictionObjectPropertyExpression(ctx);
		}
	}

	private ObjectPropertyExpression parseRestrictionObjectPropertyExpression(
			ObjectPropertyExpressionContext ctx) {
		if (ctx.inverseObjectProperty() != null) {
			return parseInverseObjectProperty(ctx.inverseObjectProperty());
		} else {
			return parseObjectPropertyIRI(ctx.objectPropertyIRI());
		}
	}

	private ObjectProperty parseObjectPropertyIRI(ObjectPropertyIRIContext objectPropertyIRI) {
		return new ObjectProperty(getIRIFromResource(objectPropertyIRI));
	}

	private InverseObjectProperty parseInverseObjectProperty(
			InverseObjectPropertyContext inverseObjectProperty) {
		return new InverseObjectProperty(getIRIFromResource(inverseObjectProperty.objectPropertyIRI()));
	}

}
