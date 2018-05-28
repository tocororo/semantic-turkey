package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.InverseObjectPropertyContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyIRIContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.PrefixedNameContext;

public class ParserObjectPropertyExpression extends ManchesterOWL2SyntaxParserBaseListener {

	private ValueFactory valueFactory;
	private Map<String, String> prefixToNamespacesMap;

	private ObjectPropertyExpression ope = null;

	public ParserObjectPropertyExpression(ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) {
		this.valueFactory = valueFactory;
		this.prefixToNamespacesMap = prefixToNamespacesMap;
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

	private IRI getIRIFromResource(ObjectPropertyIRIContext objPropIRIContext) {
		if (objPropIRIContext.IRIREF() != null) {
			// it is directly an IRI
			String objProp = objPropIRIContext.IRIREF().getText();
			return valueFactory.createIRI(objProp.substring(1, objProp.length() - 1));
		} else {
			// it is a prefixedName
			return resolvePrefixedName(objPropIRIContext.prefixedName());
		}
	}

	private IRI resolvePrefixedName(PrefixedNameContext prefixedNameContext) {
		String qname = prefixedNameContext.PNAME_LN().getText();
		String[] qnameArray = qname.split(":");
		String namespace = prefixToNamespacesMap.get(qnameArray[0]);
		if (namespace == null) {
			throw new ManchesterParserRuntimeException(
					"There is no prefix for the namespace: " + qnameArray[0]);
		}
		return valueFactory.createIRI(namespace, qnameArray[1]);
	}
}
