package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

public class ObjectProperty extends ObjectPropertyExpression {
	private IRI property;

	public ObjectProperty(IRI property) {
		this.property = property;
	}

	public IRI getProperty() {
		return property;
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName,
			boolean useUppercaseSyntax) {
		return ManchesterSyntaxUtils.printRes(getPrefixName, namespaceToPrefixsMap, property);
	}
}
