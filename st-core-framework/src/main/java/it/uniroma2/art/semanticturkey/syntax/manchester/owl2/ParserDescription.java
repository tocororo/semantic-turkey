package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.Token;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterClassInterface.PossType;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.AtomicContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ClassIRIContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ConjunctionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DataAtomicContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DataPrimaryContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DataPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DatatypeContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DatatypeIRIContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DescriptionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DescriptionInnerContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.IndividualContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.IndividualListContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.LiteralContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.LiteralListContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.NotRestrictionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyIRIContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.PrefixedNameContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.PrimaryContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.RestrictionContext;

public class ParserDescription extends ManchesterOWL2SyntaxParserBaseListener {

	private ValueFactory valueFactory; 
	private Map<String, String> prefixToNamespacesMap;
	
	ManchesterClassInterface mci = null;
	
	
	public ParserDescription(ValueFactory valueFactory, Map<String, String> prefixToNamespacesMap) {
		this.valueFactory = valueFactory;
		this.prefixToNamespacesMap = prefixToNamespacesMap;
	}
	
	public ManchesterClassInterface getManchesterClass() {
		return mci;
	}
	
	// the only entry point for this class to parse the description (which is the main element)
	@Override
	public void enterDescription(DescriptionContext ctx){
		//System.out.println("\nenterDescription");
		//try {
			if(mci == null){
				mci = parseDescription(ctx.descriptionInner());
			}
//		} catch (ManchesterParserException e) {
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//		}
	}
	
	private ManchesterClassInterface parseDescription(DescriptionInnerContext descriptionInnerContext) {
		//System.out.println("parseDescription"); // da cancellare
		ManchesterClassInterface manchesterClassInterface;
		List<ConjunctionContext> conjuctionList = descriptionInnerContext.conjunction();
		if(conjuctionList.size()>1){
			//System.out.println("D1"); //da cancellare
			manchesterClassInterface = new ManchesterOrClass();
			for(ConjunctionContext conjunctionContext : conjuctionList){
				((ManchesterOrClass) manchesterClassInterface).addClassToOrClassList(parseConjuction(conjunctionContext));
			}
		} else {
			//System.out.println("D2"); //da cancellare
			//there is only one element, so it is not a real conjuction
			manchesterClassInterface = parseConjuction(conjuctionList.get(0));
		}
		return manchesterClassInterface;
	}

	private ManchesterClassInterface parseConjuction(ConjunctionContext conjunctionContext) {
		//System.out.println("parseConjunction"); // da cancellare
		ManchesterAndClass mac = new ManchesterAndClass();
		if(conjunctionContext.classIRI() != null){
			//System.out.println("C1"); //da cancellare
			//first case in the rule conjuction ( classIRI 'that' notRestriction ( 'and' notRestriction )*  )
			ClassIRIContext classIRIContext = conjunctionContext.classIRI();
			IRI iri = getIRIFromResource(classIRIContext);
			mac.addClassToAndClassList(new ManchesterBaseClass(iri));
			
			List<NotRestrictionContext> notRestrictionList = conjunctionContext.notRestriction();
			if(notRestrictionList != null){
				for(NotRestrictionContext nrc : notRestrictionList){
					mac.addClassToAndClassList(parseNotRestriction(nrc));
				}
			}
		} else{
			//System.out.println("C2"); //da cancellare
			//second case ( primary ('and' primary)* )
			List<PrimaryContext> primaryList = conjunctionContext.primary();
			if(primaryList.size()>1){
				//System.out.println("C2_1"); //da cancellare
				//there are more restriction separated with 'and'
				for(PrimaryContext pc : primaryList){
					mac.addClassToAndClassList(parsePrimary(pc));
				}
			} else{
				//System.out.println("C2_2"); //da cancellare
				//there is only one element, so do not use the ManchesterAndClass created at the beginning
				return parsePrimary(primaryList.get(0));
			}
		}
		
		return mac;
	}
	
	
	private ManchesterClassInterface parseNotRestriction(NotRestrictionContext notRestrictionContext)  {
		//System.out.println("parseNotRestriction"); // da cancellare
		if(notRestrictionContext.not != null) { 
			return new ManchesterNotClass(parseRestriction(notRestrictionContext.restriction()));
		} else{
			return parseRestriction(notRestrictionContext.restriction()); 
		}
	}
	
	private ManchesterClassInterface parsePrimary(PrimaryContext primaryContext) 
			{
		//System.out.println("parsePrimary"); // da cancellare
		boolean hasNot = false;
		
		if(primaryContext.not != null){
			//it has the 'not'
			hasNot = true;
		}
		ManchesterClassInterface mci;
		if(primaryContext.restriction()!=null){
			//it has the restriction
			RestrictionContext restrictionContext = primaryContext.restriction();
			mci = parseRestriction(restrictionContext);
		} else{
			//it has the atomic
			AtomicContext atomicContext = primaryContext.atomic();
			mci = parseAtomic(atomicContext);
		}
		if(hasNot){
			return new ManchesterNotClass(mci);
		} else {
			return mci;
		}
			
	}

	private ManchesterClassInterface parseAtomic(AtomicContext atomicContext) {
		//System.out.println("parseAtomic"); // da cancellare
		if(atomicContext.classIRI() != null){
			ClassIRIContext classIRIContext = atomicContext.classIRI();
			IRI iri = getIRIFromResource(classIRIContext);
			//System.out.println("A1 e iri = "+iri.stringValue()); //da cancellare
			return new ManchesterBaseClass(iri);
		} else if(atomicContext.individualList() != null){
			//System.out.println("A2"); //da cancellare
			return parseIndividualList(atomicContext.individualList());
		} else{
			//System.out.println("A3"); //da cancellare
			return parseDescription(atomicContext.descriptionInner());
		}
	}

	private ManchesterClassInterface parseIndividualList(IndividualListContext individualListContext) {
		//System.out.println("parseIndividualList"); // da cancellare
		List<IndividualContext> individualContextList = individualListContext.individual();
		ManchesterOneOfClass moc = new ManchesterOneOfClass();
		for(IndividualContext individualContext : individualContextList){
			moc.addOneOf(getIndividual(individualContext));
		}
		return moc;
	}

	private ManchesterClassInterface parseRestriction(RestrictionContext restrictionContext) {
		//System.out.println("parseRestriction"); // da cancellare
		//it can have an objectPropertyExpression or a dataPropertyExpression
		if(restrictionContext.objectPropertyExpression()!= null){
			ObjectPropertyExpressionContext objectPropertyExpressionContext 
				= restrictionContext.objectPropertyExpression();
			IRI objProp = getIRIFromResource(objectPropertyExpressionContext);
			//System.out.println("R1 e objProp = "+objProp.stringValue()); //da cancellare
			if(restrictionContext.type.getText().toLowerCase().equals("some")){
				return new ManchesterSomeClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parsePrimary(restrictionContext.primary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("only")){
				return new ManchesterOnlyClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parsePrimary(restrictionContext.primary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("value")){
				return new ManchesterValueClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parseIndividual(restrictionContext.individual()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("self")){
				return new ManchesterSelfClass(hasInverse(objectPropertyExpressionContext), objProp);
			} else {// min, max or exactly
				String card = restrictionContext.type.getText().toLowerCase();
				PossType posType;
				int number = Integer.parseInt(restrictionContext.nonNegativeInteger().getText());
				if(card.toLowerCase().equals("max")){
					posType = PossType.MAX;
				} else if (card.toLowerCase().equals("min")){
					posType = PossType.MIN;
				} else{
					posType = PossType.EXACTLY;
				}
				if(restrictionContext.primary()!= null){
					return new ManchesterCardClass(hasInverse(objectPropertyExpressionContext), posType, 
							number, objProp, parsePrimary(restrictionContext.primary()));
				} else{
					return new ManchesterCardClass(hasInverse(objectPropertyExpressionContext), posType, 
							number, objProp);
				}
			} 
		} else { //restrictionContext.dataPropertyExpression() != null
			//System.out.println("R2"); //da cancellare
			DataPropertyExpressionContext dataPropertyExpressionContext 
				= restrictionContext.dataPropertyExpression();
			IRI dataProp = getIRIFromResource(dataPropertyExpressionContext);
			if(restrictionContext.type.getText().toLowerCase().equals("some")){
				return new ManchesterSomeClass(dataProp, 
						parseDataPrimary(restrictionContext.dataPrimary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("only")){
				return new ManchesterOnlyClass(dataProp, 
						parseDataPrimary(restrictionContext.dataPrimary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("value")){
				return new ManchesterValueClass(dataProp, 
						parseLiteral(restrictionContext.literal()));
			} else { // min, max or exactly
				String card = restrictionContext.type.getText().toLowerCase();
				PossType posType;
				int number = Integer.parseInt(restrictionContext.nonNegativeInteger().getText());
				if(card.toLowerCase().equals("max")){
					posType = PossType.MAX;
				} else if (card.toLowerCase().equals("min")){
					posType = PossType.MIN;
				} else{
					posType = PossType.EXACTLY;
				}
				if(restrictionContext.dataPrimary()!= null){
					return new ManchesterCardClass(posType, number, dataProp, 
							parseDataPrimary(restrictionContext.dataPrimary()));
				} else{
					return new ManchesterCardClass(posType, number, dataProp);
				}
			}
		}
	}
	
	

	private Literal parseLiteral(LiteralContext literalContext) {
		//System.out.println("parseLiteral"); // da cancellare
		return getLiteral(literalContext);
	}

	

	private Value parseIndividual(IndividualContext individual) {
		//System.out.println("parseIndividual"); // da cancellare
		return getIndividual(individual);
	}

	private ManchesterClassInterface parseDataPrimary(DataPrimaryContext dataPrimaryContext) {
		//System.out.println("parseDataPrimary"); // da cancellare
		boolean hasNot = false;
		if(dataPrimaryContext.not != null){
			hasNot = true;
		}
		ManchesterClassInterface mci = parseDataAtomic(dataPrimaryContext.dataAtomic());
		if(hasNot){
			return new ManchesterNotClass(mci);
		} else {
			return mci;
		}
	}

	private ManchesterClassInterface parseDataAtomic(DataAtomicContext dataAtomicContext) {
		//System.out.println("parseDataAtomic"); // da cancellare
		if(dataAtomicContext.datatype() != null){
			return parseDataType(dataAtomicContext.datatype());
		} else{
			return parseLiteralList(dataAtomicContext.literalList());
		}
	}

	private ManchesterClassInterface parseLiteralList(LiteralListContext literalListContext) {
		//System.out.println("parseLiteralList"); // da cancellare
		List<LiteralContext> literalContextList = literalListContext.literal();
		List<Literal> literalList = new ArrayList<Literal>();
		for(LiteralContext literalContext : literalContextList){
			literalList.add(parseLiteral(literalContext));
		}
		return new ManchesterLiteralListClass(literalList);
	}

	private ManchesterClassInterface parseDataType(DatatypeContext datatypeContext) {
		//System.out.println("parseDataType"); // da cancellare
		if(datatypeContext.datatypeIRI() != null){
			DatatypeIRIContext datatypeIRIContext = datatypeContext.datatypeIRI();
			return new ManchesterBaseClass(getIRIFromResource(datatypeIRIContext));
		} else{
			Token abbr = datatypeContext.abbr;
			if(abbr.getText().equals("integer")){
				return new ManchesterBaseClass(XMLSchema.INTEGER);
			} else if(abbr.getText().equals("decimal")){
				return new ManchesterBaseClass(XMLSchema.DECIMAL);
			} else if(abbr.getText().equals("float")){
				return new ManchesterBaseClass(XMLSchema.FLOAT);
			} else if(abbr.getText().equals("string")){
				return new ManchesterBaseClass(XMLSchema.STRING);
			} else{
				throw new ManchesterParserRuntimeException("The abbreviated type "+abbr.getText()+" is not supported");
			}
		}
	}

	/************************************************************/
	
	private IRI getIRIFromResource(ClassIRIContext classIRIcontext){
		if(classIRIcontext.IRIREF() != null){
			//it is directly an IRI
			String baseClass = classIRIcontext.IRIREF().getText();
			return valueFactory.createIRI(baseClass.substring(1,  baseClass.length() - 1));
		} else{
			//it is a prefixedName
			return resolvePrefixedName(classIRIcontext.prefixedName());
		}
	}
	
	private IRI getIRIFromResource(ObjectPropertyExpressionContext objectPropertyExpressionContext) {
		ObjectPropertyIRIContext objPropIRIContext;
		if(objectPropertyExpressionContext.objectPropertyIRI() != null){
			objPropIRIContext = objectPropertyExpressionContext.objectPropertyIRI();
		} else{
			objPropIRIContext = objectPropertyExpressionContext.inverseObjectProperty().objectPropertyIRI();
		}
		return getIRIFromResource(objPropIRIContext);
	}
	
	private IRI getIRIFromResource(ObjectPropertyIRIContext objPropIRIContext) {
		if(objPropIRIContext.IRIREF() != null){
			//it is directly an IRI
			String objProp = objPropIRIContext.IRIREF().getText();
			return valueFactory.createIRI(objProp.substring(1,  objProp.length() - 1));
		} else{
			//it is a prefixedName
			return resolvePrefixedName(objPropIRIContext.prefixedName());
		}
	}
	
	private IRI getIRIFromResource(DatatypeIRIContext datatypeIRIContext) {
		if(datatypeIRIContext.IRIREF() != null){
			//it is directly an IRI
			String baseClass = datatypeIRIContext.IRIREF().getText();
			return valueFactory.createIRI(baseClass.substring(1,  baseClass.length() - 1));
		} else{
			//it is a prefixedName
			return resolvePrefixedName(datatypeIRIContext.prefixedName());
		}
	}
	
	private IRI getIRIFromResource(DataPropertyExpressionContext dataPropertyExpressionContext) {
		if(dataPropertyExpressionContext.IRIREF() != null){
			String baseDataProp = dataPropertyExpressionContext.IRIREF().getText();
			return valueFactory.createIRI(baseDataProp.substring(1,  baseDataProp.length() - 1));
		} else{
			return resolvePrefixedName(dataPropertyExpressionContext.prefixedName());
		}
	}

	private boolean hasInverse(ObjectPropertyExpressionContext objectPropertyExpressionContext){
		return (objectPropertyExpressionContext.inverseObjectProperty() != null);
	}

	private IRI resolvePrefixedName(PrefixedNameContext prefixedNameContext) {
		String qname = prefixedNameContext.PNAME_LN().getText();
		String[] qnameArray = qname.split(":");
		String namespace = prefixToNamespacesMap.get(qnameArray[0]);
		if(namespace == null){
			throw new ManchesterParserRuntimeException("There is no prefix for the namespace: "+qnameArray[0]);
		}
		return valueFactory.createIRI(namespace, qnameArray[1]);
	}

	private Literal getLiteral(LiteralContext literalContext) {
		String literalWithNoises = literalContext.string().getText();
		String label = literalWithNoises.substring(1, literalWithNoises.length()-1);
		if(literalContext.LANGTAG() != null){
			return valueFactory.createLiteral(label, literalContext.LANGTAG().getText().substring(1));
		} else if(literalContext.classIRI() != null){
			return valueFactory.createLiteral(label, getIRIFromResource(literalContext.classIRI()));
		} else{
			return valueFactory.createLiteral(label);
		}
	}
	
	private IRI getIndividual(IndividualContext individualContext) {
		if(individualContext.IRIREF() != null){
			//it is directly an IRI
			String individual = individualContext.IRIREF().getText();
			return valueFactory.createIRI(individual.substring(1,  individual.length() - 1));
		} else{
			//it is a prefixedName
			return resolvePrefixedName(individualContext.prefixedName());
		}
	}
	
}
