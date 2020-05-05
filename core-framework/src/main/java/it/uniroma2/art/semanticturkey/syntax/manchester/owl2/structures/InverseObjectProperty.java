package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures;

import java.util.Map;

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;
import org.eclipse.rdf4j.model.IRI;

public class InverseObjectProperty extends ObjectPropertyExpression {
	private IRI property;

	public InverseObjectProperty(IRI property) {
		this.property = property;
	}

	public IRI getProperty() {
		return property;
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName,
			boolean useUppercaseSyntax) {
		return (useUppercaseSyntax ? "INVERSE" : "inverse") + " "
				+ ManchesterSyntaxUtils.printRes(getPrefixName, namespaceToPrefixsMap, property);
	}
}
