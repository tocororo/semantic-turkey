package it.uniroma2.art.semanticturkey.customform;

import java.util.ArrayList;
import java.util.Collection;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;

public class CustomFormNode extends CustomForm {
	
	CustomFormNode(String id, String name, String description, String ref) {
		super(id, name, description, ref);
	}

	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException {
		Collection<UserPromptStruct> form = new ArrayList<>();
		String ref = getRef();
		UserPromptStruct upStruct = CustomFormParseUtils.createUserPromptForNodeForm(ref, codaCore);
		form.add(upStruct);
		return form;
	}
	
}
