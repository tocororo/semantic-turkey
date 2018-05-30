package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.Map;

public abstract class ObjectPropertyExpression {

	public abstract String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax);

}
