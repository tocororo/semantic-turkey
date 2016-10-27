package it.uniroma2.art.semanticturkey.customrange;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.Tree;

import it.uniroma2.art.coda.pearl.model.ConverterArgumentExpression;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ConverterRDFLiteralArgument;
import it.uniroma2.art.coda.pearl.parser.antlr.AntlrLexer;
import it.uniroma2.art.coda.pearl.parser.antlr.AntlrParser;
import it.uniroma2.art.owlart.vocabulary.XmlSchema;

public class CustomRangeEntryParseUtils {
	
	/**
	 * Parses and creates a tree of a pearl unit.
	 * If the pearl code is valid, returns a tree, otherwise throws an exception.
	 * Note: This method detects only syntactical error, it ignores semantical error
	 * (e.g. duplicate rule, node not defined, prefix not defined, ...). If you want use this method
	 * just to detect errors, use CODACore#setProjectionRulesModelAndParseIt instead
	 * @param ref
	 * @return
	 * @throws RecognitionException
	 */
	public static Tree createPearlUnitTree(String ref) throws RecognitionException {
		CharStream pearlStream = new ANTLRStringStream(ref);
		AntlrLexer lexer = new AntlrLexer(pearlStream);
		TokenStream token = new CommonTokenStream(lexer);
		AntlrParser parser = new AntlrParser(token);
		return (Tree) parser.pearlUnit().getTree();
	}
	
	/**
	 * Parses and creates a tree of a projection operator.
	 * This method is useful to check if a CustomRangeEntryNode ref is valid.
	 * @param ref
	 * @return
	 * @throws RecognitionException
	 */
	public static Tree createProjectionOperatorTree(String ref) throws RecognitionException {
		CharStream pearlStream = new ANTLRStringStream(ref);
		AntlrLexer lexer = new AntlrLexer(pearlStream);
		TokenStream token = new CommonTokenStream(lexer);
		AntlrParser parser = new AntlrParser(token);
		return (Tree) parser.projectionOperator().getTree();
	}
	
	public static UserPromptStruct createUserPromptForNodeEntry(String ref) throws RecognitionException {
		ref = ref.trim();
		UserPromptStruct upStruct;
		
		//Treat separately the case where ref is simply "literal", because parser.projectionOperator()
		//inexplicably throws an exception (no viable alternative at input <EOF>)
		if (ref.equals("literal")) {
			upStruct = new UserPromptStruct("value", "value", "literal");
			return upStruct; 
		}
		
		Tree projOperatorTree = createProjectionOperatorTree(ref);
		int treeChildCount = projOperatorTree.getChildCount();
		String uriOrLiteral = projOperatorTree.getChild(0).getText();
		
		if (uriOrLiteral.equals("uri")) {
			upStruct = new UserPromptStruct("value", "value", "uri");
			if (treeChildCount == 2) {
				//there is also a AST_CONVERTERS child
				Tree converterTree = projOperatorTree.getChild(1);
				if (converterTree.getType() == AntlrParser.AST_CONVERTERS) {
					ConverterMention converter = parseConverters(converterTree);
					upStruct.setConverter(converter);
				}
			}
		} else { //uriOrLiteral.equals("literal")
			upStruct = new UserPromptStruct("value", "value", "literal");
			if (treeChildCount == 2) {
				//just 2 children: 2nd child is AST_CONVERTERS or AST_LANG?
				Tree convertersOrLangTree = projOperatorTree.getChild(1);
				if (convertersOrLangTree.getType() == AntlrParser.AST_CONVERTERS) {
					ConverterMention converter = parseConverters(convertersOrLangTree);
					upStruct.setConverter(converter);
				} else if (convertersOrLangTree.getType() == AntlrParser.AST_LANG) {
					upStruct.setLiteralLang(parseLang(convertersOrLangTree));
				}
			} else { //(treeChildCount == 3)
				//3 children: in this case the 2nd is always AST_CONVERTERS, the 3rd AST_LANG or AST_DATATYPE?
				Tree convertersTree = projOperatorTree.getChild(1);
				upStruct.setConverter(parseConverters(convertersTree));
				
				Tree langTagOrDatatypeTree = projOperatorTree.getChild(2);
				if (langTagOrDatatypeTree.getType() == AntlrParser.AST_LANG) {
					upStruct.setLiteralLang(parseLang(langTagOrDatatypeTree));
				} else { //langTagOrDatatypeTree.getType() == AntlrParser.AST_DATATYPE
					upStruct.setLiteralDatatype(parseDatatype(langTagOrDatatypeTree));
				}
			}
		}
		return upStruct;
	}
	
	/**
	 * Gets a Tree of type AntlrParser.AST_CONVERTERS and return the converterURI
	 * TODO revise this method and the CREntry structures in order to work with converter with params and
	 * converters list
	 * @param convertersTree
	 * @return
	 */
	private static ConverterMention parseConverters(Tree convertersTree) {
		Tree convTree = convertersTree.getChild(0); //AST_CONVERTERS -> AST_CONVERTER
		Tree prefixedOrIri = convTree.getChild(0);
		int childCount = prefixedOrIri.getChildCount();
		String converterURI;
		if (prefixedOrIri.getType() == AntlrParser.AST_PREFIXED_NAME) {
			String prefixed = prefixedOrIri.getChild(0).getText();
			converterURI = prefixed.replace("coda:", "http://art.uniroma2.it/coda/contracts/");
		} else { //if (indConvTree.getType() == AntlrParser.AST_IRI_REF)
			converterURI = prefixedOrIri.getChild(0).getText();
		}
		if (childCount > 1) { //there is at least one AST_CONVERTER_ADDITIONAL_ARGUMENTS child
			Tree argTree = convTree.getChild(1);
			Tree argChild = argTree.getChild(0); //consider only one argument at the moment
			if (argChild.getType() == AntlrParser.AST_LITERAL) { //consider only literal argument at the moment
				String argValue = argChild.getChild(0).getText();
				List<ConverterArgumentExpression> args = new ArrayList<ConverterArgumentExpression>();
				args.add(new ConverterRDFLiteralArgument(argValue));
				return new ConverterMention(converterURI, args);
			} else { //if argument is not literal, ignore it
				return new ConverterMention(converterURI);
			}
		} else {
			return new ConverterMention(converterURI);
		}
		
	}
	
	/**
	 * Gets a Tree of type AntlrParser.AST_DATATYPE and return the datatype URI
	 * @param datatypeTree
	 * @return
	 */
	private static String parseDatatype(Tree datatypeTree) {
		Tree dtChild = datatypeTree.getChild(0);
		if (dtChild.getType() == AntlrParser.AST_PREFIXED_NAME) {
			String prefixed = dtChild.getChild(0).getText();
			prefixed = prefixed.replace("xsd:", XmlSchema.NAMESPACE);
			return prefixed;
		} else { //if (dtChild.getType() == AntlrParser.AST_IRI_REF)
			return dtChild.getChild(0).getText();
		}
	}
	
	/**
	 * Gets a Tree of type AntlrParser.AST_LANG and return the langTag
	 * @param converterslangTreeTree
	 * @return
	 */
	private static String parseLang(Tree langTree) {
		String langTagAnn = langTree.getChild(0).getText();
		return langTagAnn.substring(1);
	}
	
}
