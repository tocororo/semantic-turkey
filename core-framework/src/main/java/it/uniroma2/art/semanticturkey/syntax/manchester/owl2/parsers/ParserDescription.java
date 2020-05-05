package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.parsers;

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DescriptionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ManchesterClassInterface;
import org.eclipse.rdf4j.model.ValueFactory;

import java.util.Map;

public class ParserDescription extends ParserManchesterAbstract {

	private ManchesterClassInterface mci = null;

	public ParserDescription(ValueFactory valueFactory, Map<String, String> prefixToNamespacesMap) {
		super(valueFactory, prefixToNamespacesMap);
	}
	
	public ManchesterClassInterface getManchesterClass() {
		return mci;
	}
	
	// the only entry point for this class to parse the description (which is the main element)
	@Override
	public void enterDescription(DescriptionContext ctx){
		//try {
			if(mci == null){
				mci = parseDescriptionInner(ctx.descriptionInner());
			}
//		} catch (ManchesterParserException e) {
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//		}
	}
	

	
}
