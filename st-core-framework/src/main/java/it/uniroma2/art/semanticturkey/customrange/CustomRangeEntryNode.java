package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.PRParserException;

import java.util.ArrayList;
import java.util.Collection;

public class CustomRangeEntryNode extends CustomRangeEntry {
	
	CustomRangeEntryNode(String id, String name, String description, String ref) {
		super(id, name, description, ref);
	}

	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException {
		Collection<UserPromptStruct> form = new ArrayList<UserPromptStruct>();
		String ref = getRef();
		/* the ref in case of node CRE contains a rdfType (uri or literal), followed by an optional
		 * datatype (in case of literal) and an optional converter. */
		if (ref.startsWith("uri")){
			UserPromptStruct upStruct = new UserPromptStruct("value", "uri");
			if (ref.contains("(") && ref.contains(")")){
				String converter = ref.substring(ref.indexOf("(")+1, ref.indexOf(")"));
				upStruct.setConverter(converter);
			} else {//if no converter is specified
				upStruct.setConverter("http://art.uniroma2.it/coda/contracts/default");
			}
			form.add(upStruct);
		} else if (ref.startsWith("literal")){
			UserPromptStruct upStruct = new UserPromptStruct("value", "literal");
			if (ref.contains("(") && ref.endsWith(")")){
				String converter = ref.substring(ref.lastIndexOf("(")+1, ref.indexOf(")"));
				upStruct.setConverter(converter);
				ref = ref.substring(0, ref.lastIndexOf("("));//remove the converter from the end of the ref
			} else {//if no converter is specified
				upStruct.setConverter("http://art.uniroma2.it/coda/contracts/default");
			}
			if (ref.contains("^^")){
				String datatype = ref.substring(ref.indexOf("^^")+2);
				upStruct.setLiteralDatatype(datatype);
			} else if (ref.contains("@")){
				String lang = ref.substring(ref.indexOf("@")+1);
				upStruct.setLiteralLang(lang);
			}
			form.add(upStruct);
		} else {
			throw new PRParserException("Invalid ref in CustomRangeEntry " + getId());
		}
		return form;
	}
	

}
