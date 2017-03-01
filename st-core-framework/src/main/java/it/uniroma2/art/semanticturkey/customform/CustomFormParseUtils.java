package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.Tree;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator.NodeType;
import it.uniroma2.art.coda.pearl.parser.antlr.AntlrLexer;
import it.uniroma2.art.coda.pearl.parser.antlr.AntlrParser;

public class CustomFormParseUtils {
	
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
	 * This method is useful to check if a {@link CustomFormNode} ref is valid.
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
	
	public static UserPromptStruct createUserPromptForNodeForm(String ref, CODACore codaCore) throws PRParserException {
		ref = ref.trim();
		UserPromptStruct upStruct;
		
		//Treat separately the case where ref is simply "literal", because parser.projectionOperator()
		//inexplicably throws an exception (no viable alternative at input <EOF>)
		if (ref.equals("literal")) {
			upStruct = new UserPromptStruct("value", "value", "literal");
			return upStruct; 
		}
		
		ProjectionOperator projOperator = getProjectionOperator(codaCore, ref);
		if (projOperator.getNodeType() == NodeType.literal) {
			upStruct = new UserPromptStruct("value", "value", NodeType.literal.toString());
			Optional<String> datatype = projOperator.getDatatype();
			if (datatype.isPresent()) {
				upStruct.setLiteralDatatype(datatype.get());
			}
			Optional<String> lang = projOperator.getLanguage();
			if (lang.isPresent()) {
				upStruct.setLiteralLang(lang.get());
			}
			List<ConverterMention> convList = projOperator.getConverterMentions();
			if (!convList.isEmpty()) {
				ConverterMention converter = convList.get(0);
				upStruct.setConverter(converter);
			}
		} else { //NodeType.uri
			upStruct = new UserPromptStruct("value", "value", NodeType.uri.toString());
			List<ConverterMention> convList = projOperator.getConverterMentions();
			if (!convList.isEmpty()) {
				ConverterMention converter = convList.get(0);
				upStruct.setConverter(converter);
			}
		}
		return upStruct;
	}
	
	public static ProjectionOperator getProjectionOperator(CODACore codaCore, String ref) throws PRParserException {
		// useful prefix-ns mappings in CRE node rule
		Map<String, String> prefixMapping = new HashMap<String, String>();
		prefixMapping.put(XMLSchema.PREFIX, XMLSchema.NAMESPACE);
		prefixMapping.put("coda", "http://art.uniroma2.it/coda/contracts/");
		return codaCore.parseProjectionOperator(ref, prefixMapping);
	}
	
}
