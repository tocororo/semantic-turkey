package it.uniroma2.art.semanticturkey.customform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator.NodeType;

public class CustomFormParseUtils {
	
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
