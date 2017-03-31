package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

public class ManchesterSelfClass extends ManchesterClassInterface {

	private boolean inverse = false;
	private IRI prop;
	
	public ManchesterSelfClass(IRI prop) {
		super(PossType.SELF);
		this.prop = prop;
	}
	
	public ManchesterSelfClass(boolean inverse, IRI prop) {
		super(PossType.SELF);
		this.inverse = inverse;
		this.prop = prop;
	}
	
	public boolean hasInverse(){
		return inverse;
	}
	
	public IRI getProp() {
		return prop;
	}
	
	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n"+tab+getType());
		if(inverse){
			sb.append("\n" + tab + "\t inverse");
		}
		sb.append("\n"+tab+"\t"+prop.stringValue());
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {

		String inverseOrEmpty="";
		if(inverse){
			inverseOrEmpty="inverse ";
		}
		
		if(useUppercaseSyntax){
			return  inverseOrEmpty.toUpperCase() + printRes(getPrefixName, namespaceToPrefixsMap, prop)+" SELF";
		} else {
			return  inverseOrEmpty + printRes(getPrefixName, namespaceToPrefixsMap, prop) + " Self";
		}
		
	}
	
}
