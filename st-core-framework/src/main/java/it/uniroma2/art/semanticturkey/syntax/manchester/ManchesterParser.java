/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is ART OWL API.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * The ART OWL API were developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ART OWL API can be obtained at 
 * http://art.uniroma2.it/owlart
 *
 */

package it.uniroma2.art.semanticturkey.syntax.manchester;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
import it.uniroma2.art.semanticturkey.syntax.manchester.ManchesterClassInterface.PossType;

public class ManchesterParser {

	private ManchesterParser() {
	}

	public static ManchesterClassInterface parse(String manchExpr, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap)
			throws RecognitionException, ManchesterParserException {

		ANTLRStringStream charStream = new ANTLRStringStream(manchExpr);
		ManchesterSyntaxParserLexer lexer = new ManchesterSyntaxParserLexer(charStream);
		CommonTokenStream token = new CommonTokenStream(lexer);
		ManchesterSyntaxParserParser parser = new ManchesterSyntaxParserParser(token);

		Tree tree = (Tree) parser.manchesterExpression().getTree();
		ManchesterClassInterface manchesterClassInterface = parseElem(tree, valueFactory, 
				prefixToNamespacesMap);
		if(manchesterClassInterface == null){
			throw new ManchesterParserException(manchExpr+"does not follow the manchester syntax");
		}
		try{
			manchesterClassInterface.print("");
		} catch (Exception e){
			//if you are not able to print the Manchester expression, the input expression was wrong
			throw new ManchesterParserException(manchExpr+"does not follow the manchester syntax");
		}
		
		return manchesterClassInterface;
	}

	private static ManchesterClassInterface parseElem(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		ManchesterClassInterface manchClass = null;
		int type = child.getType();
		switch (type) {
		case ManchesterSyntaxParserParser.AST_BASECLASS:
			manchClass = parseBaseClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_AND:
			manchClass = parseAndClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_OR:
			manchClass = parseOrClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_ONLY:
			manchClass = parseOnlyClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_SOME:
			manchClass = parseSomeClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_CARDINALITY:
			manchClass = parseCardClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_ONEOFLIST:
			manchClass = parseOneOfClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_NOT:
			manchClass = parseNotClass(child, valueFactory, prefixToNamespacesMap);
			break;
		case ManchesterSyntaxParserParser.AST_VALUE:
			manchClass = parseValueClass(child, valueFactory, prefixToNamespacesMap);
			break;

		}
		return manchClass;

	}

	private static ManchesterClassInterface parseBaseClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {

		IRI baseClassURI = getUriFromUriOrPrefixedClass(child.getChild(0), valueFactory, 
				prefixToNamespacesMap);

		return new ManchesterBaseClass(baseClassURI);
	}


	private static ManchesterClassInterface parseAndClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		ManchesterAndClass manchClass = new ManchesterAndClass();
		for (int i = 0; i < child.getChildCount(); ++i) {
			manchClass.addClassToAndClassList(parseElem(child.getChild(i), valueFactory, 
					prefixToNamespacesMap));
		}
		return manchClass;
	}

	private static ManchesterClassInterface parseOrClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		ManchesterOrClass manchesterOrClass = new ManchesterOrClass();
		for (int i = 0; i < child.getChildCount(); ++i) {
			manchesterOrClass.addClassToOrClassList(parseElem(child.getChild(i), valueFactory, 
					prefixToNamespacesMap));
		}
		return manchesterOrClass;
	}

	private static ManchesterClassInterface parseOnlyClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		IRI propURI = getUriFromUriOrPrefixedClass(child.getChild(0), valueFactory, 
				prefixToNamespacesMap);
		ManchesterClassInterface manchClass = parseElem(child.getChild(1), valueFactory, 
				prefixToNamespacesMap);

		return new ManchesterOnlyClass(propURI, manchClass);
	}

	private static ManchesterClassInterface parseSomeClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException  {
		IRI propURI = getUriFromUriOrPrefixedClass(child.getChild(0), valueFactory, 
				prefixToNamespacesMap);
		ManchesterClassInterface manchClass = parseElem(child.getChild(1), valueFactory, 
				prefixToNamespacesMap);

		return new ManchesterSomeClass(propURI, manchClass);
	}

	private static ManchesterClassInterface parseCardClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException  {
		IRI propURI = getUriFromUriOrPrefixedClass(child.getChild(0), valueFactory, 
				prefixToNamespacesMap);
		PossType oper = PossType.valueOf(child.getChild(1).getText().toUpperCase());
		int card = Integer.parseInt(child.getChild(2).getText());

		return new ManchesterCardClass(oper, card, propURI);
	}

	private static ManchesterClassInterface parseOneOfClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		ManchesterOneOfClass manchesterOneOfClass = new ManchesterOneOfClass();
		for (int i = 0; i < child.getChildCount(); ++i) {
			IRI res = getUriFromUriOrPrefixedClass(child.getChild(i), valueFactory, 
					prefixToNamespacesMap);
			manchesterOneOfClass.addOneOf(res);
		}
		return manchesterOneOfClass;
	}

	private static ManchesterClassInterface parseNotClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		return new ManchesterNotClass(parseElem(child.getChild(0), valueFactory, 
				prefixToNamespacesMap));
	}

	private static ManchesterClassInterface parseValueClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException  {
		IRI propURI = getUriFromUriOrPrefixedClass(child.getChild(0), valueFactory, 
				prefixToNamespacesMap);
		String value = child.getChild(1).getText();
		Value valueNode;
		if (value.startsWith("\"")) {
			// it is a literal, if there are 3 child, the third one is the language, with 4 child, the
			// the fourth one is the datatype
			value = value.substring(1, value.length() - 1); // remove the " at the beginning and at the end
			if (child.getChildCount() == 2) {
				valueNode = generateLiteral(value, null, null, valueFactory);
			} else if (child.getChildCount() == 3) {
				String lang = child.getChild(2).getText().substring(1);
				valueNode = generateLiteral(value, lang, null, valueFactory);
			} else { // it has 4 child
				IRI datatype = getUriFromUriOrPrefixedClass(child.getChild(3), valueFactory, prefixToNamespacesMap);
				valueNode = generateLiteral(value, null, datatype, valueFactory);

			}

		} else {
			// it is a URI
			valueNode = getUriFromUriOrPrefixedClass(child.getChild(1), valueFactory, 
					prefixToNamespacesMap);
		}

		return new ManchesterValueClass(propURI, valueNode);
	}
	
	
	private static IRI getUriFromUriOrPrefixedClass(Tree child, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		if (child.getType() == ManchesterSyntaxParserParser.AST_PREFIXED_NAME) {
			String qname = child.getChild(0).getText();
			//split the qname in prefix and local name
			String[] qnameArray = qname.split(":");
			String namespace = prefixToNamespacesMap.get(qnameArray[0]);
			if(namespace == null){
				throw new ManchesterParserException("There is no prefix for the namespace: "+qnameArray[0]);
			}
			return valueFactory.createIRI(namespace, qnameArray[0]);
		} else {
			String baseClass = child.getText();
			if (baseClass.startsWith("<")) {
				baseClass = baseClass.substring(1, baseClass.length() - 1);
			}
			return valueFactory.createIRI(baseClass);
		}
	}
	
	private static Literal generateLiteral(String label, String lang, IRI type, ValueFactory valueFactory){
		Literal literal;
		if(lang == null && type == null){
			literal = valueFactory.createLiteral(label);
		} else if(lang != null) {
			literal = valueFactory.createLiteral(label, lang);
		} else {
			literal = valueFactory.createLiteral(label, type);
		}
		
		return literal;
	}

}
