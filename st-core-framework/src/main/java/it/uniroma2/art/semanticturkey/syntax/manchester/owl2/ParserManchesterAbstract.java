package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterClassInterface.PossType;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.*;
import it.uniroma2.art.semanticturkey.vocabulary.XSDFragment;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class ParserManchesterAbstract extends ManchesterOWL2SyntaxParserBaseListener {

	private ValueFactory valueFactory;
	private Map<String, String> prefixToNamespacesMap;

	ManchesterClassInterface mci = null;

	private static String SOME = "some";
	private static String ONLY = "only";
	private static String VALUE = "value";
	private static String SELF = "self";
	private static String MIN = "min";
	private static String MAX = "max";
	private static String EXACTLY = "exactly";

	//the possible facets values
	public static String FACET_LENGTH = "length";
	public static String FACET_MINLENGTH = "minlength";
	public static String FACET_MAXLENGTH = "maxlength";
	public static String FACET_PATTERN = "pattern";
	public static String FACET_LANGRANGE = "langrange";
	public static String FACET_LESSEQ= "<=";
	public static String FACET_LESS = "<";
	public static String FACET_GREATEREQ = ">=";
	public static String FACET_GREATER= ">";


	public ParserManchesterAbstract(ValueFactory valueFactory, Map<String, String> prefixToNamespacesMap) {
		this.valueFactory = valueFactory;
		this.prefixToNamespacesMap = prefixToNamespacesMap;
	}
	
	public ManchesterClassInterface getManchesterClass() {
		return mci;
	}
	
	protected ManchesterClassInterface parseDescriptionInner(DescriptionInnerContext descriptionInnerContext) {
		ManchesterClassInterface manchesterClassInterface;
		List<ConjunctionContext> conjuctionList = descriptionInnerContext.conjunction();
		if(conjuctionList.size()>1){
			//there are multiple restristricn separated by 'OR'
			manchesterClassInterface = new ManchesterOrClass();
			for(ConjunctionContext conjunctionContext : conjuctionList){
				((ManchesterOrClass) manchesterClassInterface).addClassToOrClassList(parseConjuction(conjunctionContext));
			}
		} else {
			//there is only one element, so it is not a real disjunction, just a single conjunction
			manchesterClassInterface = parseConjuction(conjuctionList.get(0));
		}
		return manchesterClassInterface;
	}

	protected ManchesterClassInterface parseConjuction(ConjunctionContext conjunctionContext) {
		ManchesterAndClass mac = new ManchesterAndClass();
		if(conjunctionContext.classIRI() != null){
			//first case in the rule conjuction ->  classIRI 'THAT' notRestriction ( 'AND' notRestriction )*
			//TODO this case is not dealt in the right way, since ManchesterAndClass is not able to store it properly, need a fix
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
			//second case -> primary ('AND' primary)*
			List<PrimaryContext> primaryList = conjunctionContext.primary();
			if(primaryList.size()>1){
				//there are more restriction separated with 'AND'
				for(PrimaryContext pc : primaryList){
					mac.addClassToAndClassList(parsePrimary(pc));
				}
			} else{
				//there is only one element, so do not use the ManchesterAndClass created at the beginning
				return parsePrimary(primaryList.get(0));
			}
		}
		
		return mac;
	}
	
	
	protected ManchesterClassInterface parseNotRestriction(NotRestrictionContext notRestrictionContext)  {
		if(notRestrictionContext.not != null) {
			//a real not restriction
			return new ManchesterNotClass(parseRestriction(notRestrictionContext.restriction()));
		} else{
			// this restriction does not have the 'NOT' in front of it
			return parseRestriction(notRestrictionContext.restriction()); 
		}
	}
	
	protected ManchesterClassInterface parsePrimary(PrimaryContext primaryContext)
			{
		boolean hasNot = false;
		
		if(primaryContext.not != null){
			//it has the 'NOT'
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

	protected ManchesterClassInterface parseAtomic(AtomicContext atomicContext) {
		if(atomicContext.classIRI() != null){
			//classIRI
			ClassIRIContext classIRIContext = atomicContext.classIRI();
			IRI iri = getIRIFromResource(classIRIContext);
			return new ManchesterBaseClass(iri);
		} else if(atomicContext.individualList() != null){
			//'{' individualList '}'
			return parseIndividualList(atomicContext.individualList());
		} else{
			//'(' descriptionInner ')'
			return parseDescriptionInner(atomicContext.descriptionInner());
		}
	}

	protected ManchesterClassInterface parseIndividualList(IndividualListContext individualListContext) {
		//System.out.println("parseIndividualList"); // da cancellare
		List<IndividualContext> individualContextList = individualListContext.individual();
		ManchesterOneOfClass moc = new ManchesterOneOfClass();
		for(IndividualContext individualContext : individualContextList){
			moc.addOneOf(getIndividual(individualContext));
		}
		return moc;
	}

	protected ManchesterClassInterface parseRestriction(RestrictionContext restrictionContext) {
		//it can have an objectPropertyExpression or a dataPropertyExpression
		if(restrictionContext.objectPropertyExpression()!= null){
			ObjectPropertyExpressionContext objectPropertyExpressionContext 
				= restrictionContext.objectPropertyExpression();
			IRI objProp = getIRIFromResource(objectPropertyExpressionContext);
			if(restrictionContext.type.getText().toLowerCase().equals(SOME)){
				//objectPropertyExpression type=SOME primary
				return new ManchesterSomeClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parsePrimary(restrictionContext.primary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals(ONLY)){
				//objectPropertyExpression type=ONLY primary
				return new ManchesterOnlyClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parsePrimary(restrictionContext.primary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals(VALUE)){
				//objectPropertyExpression type=VALUE individual
				return new ManchesterValueClass(hasInverse(objectPropertyExpressionContext), objProp, 
						parseIndividual(restrictionContext.individual()));
			} else if(restrictionContext.type.getText().toLowerCase().equals(SELF)){
				//objectPropertyExpression type=SELF
				return new ManchesterSelfClass(hasInverse(objectPropertyExpressionContext), objProp);
			} else {// min, max or exactly
				String card = restrictionContext.type.getText().toLowerCase();
				PossType posType;
				int number = Integer.parseInt(restrictionContext.nonNegativeInteger().getText());
				if(card.toLowerCase().equals("max")){
					//objectPropertyExpression type=MAX nonNegativeInteger primary?
					posType = PossType.MAX;
				} else if (card.toLowerCase().equals("min")){
					//objectPropertyExpression type=MIN nonNegativeInteger primary?
					posType = PossType.MIN;
				} else{
					//objectPropertyExpression type=EXACTLY nonNegativeInteger primary?
					posType = PossType.EXACTLY;
				}
				if(restrictionContext.primary()!= null){
					//it has the 'primary'
					return new ManchesterCardClass(hasInverse(objectPropertyExpressionContext), posType, 
							number, objProp, parsePrimary(restrictionContext.primary()));
				} else{
					//it does not have the 'primary'
					return new ManchesterCardClass(hasInverse(objectPropertyExpressionContext), posType, 
							number, objProp);
				}
			} 
		} else { //restrictionContext.dataPropertyExpression() != null
			DataPropertyExpressionContext dataPropertyExpressionContext
				= restrictionContext.dataPropertyExpression();
			IRI dataProp = getIRIFromResource(dataPropertyExpressionContext);
			if(restrictionContext.type.getText().toLowerCase().equals("some")){
				//dataPropertyExpression type=SOME dataPrimary
				return new ManchesterSomeClass(dataProp, 
						parseDataPrimary(restrictionContext.dataPrimary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("only")){
				//dataPropertyExpression type=ONLY dataPrimary
				return new ManchesterOnlyClass(dataProp, 
						parseDataPrimary(restrictionContext.dataPrimary()));
			} else if(restrictionContext.type.getText().toLowerCase().equals("value")){
				//dataPropertyExpression type=VALUE literal
				return new ManchesterValueClass(dataProp, 
						parseLiteral(restrictionContext.literal()));
			} else { // min, max or exactly
				String card = restrictionContext.type.getText().toLowerCase();
				PossType posType;
				int number = Integer.parseInt(restrictionContext.nonNegativeInteger().getText());
				if(card.toLowerCase().equals("max")){
					//dataPropertyExpression type=MAX nonNegativeInteger dataPrimary?
					posType = PossType.MAX;
				} else if (card.toLowerCase().equals("min")){
					//dataPropertyExpression type=MIN nonNegativeInteger dataPrimary?
					posType = PossType.MIN;
				} else{
					//dataPropertyExpression type=EXACTLY nonNegativeInteger dataPrimary?
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
	
	

	protected Literal parseLiteral(LiteralContext literalContext) {
		//System.out.println("parseLiteral"); // da cancellare
		return getLiteral(literalContext);
	}

	

	protected Value parseIndividual(IndividualContext individual) {
		//System.out.println("parseIndividual"); // da cancellare
		return getIndividual(individual);
	}

	protected ManchesterClassInterface parseDataPrimary(DataPrimaryContext dataPrimaryContext) {
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

	protected ManchesterClassInterface parseDataAtomic(DataAtomicContext dataAtomicContext) {
		//System.out.println("parseDataAtomic"); // da cancellare
		if(dataAtomicContext.datatype() != null){
			return parseDataType(dataAtomicContext.datatype());
		} else if(dataAtomicContext.literalList() != null){
			return parseLiteralList(dataAtomicContext.literalList());
		} else  if(dataAtomicContext.datatypeRestriction() != null){
			return parseDatatypeRestriction(dataAtomicContext.datatypeRestriction());
		}  else { // dataAtomicContext.dataRange() != null
			return parseDataRange(dataAtomicContext.dataRange());

		}
	}

	protected ManchesterClassInterface parseLiteralList(LiteralListContext literalListContext) {
		//System.out.println("parseLiteralList"); // da cancellare
		List<LiteralContext> literalContextList = literalListContext.literal();
		List<Literal> literalList = new ArrayList<Literal>();
		for(LiteralContext literalContext : literalContextList){
			literalList.add(parseLiteral(literalContext));
		}
		return new ManchesterLiteralListClass(literalList);
	}

	protected ManchesterClassInterface parseDataType(DatatypeContext datatypeContext) {
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

	protected ManchesterClassInterface parseDatatypeRestriction(DatatypeRestrictionContext datatypeRestrictionContext){
		ManchesterClassInterface dataTypeMCI = parseDataType(datatypeRestrictionContext.datatype());
		List<TerminalNode> facetList = datatypeRestrictionContext.FACET();
		List<String> facetStringList = new ArrayList<>();
		for(TerminalNode terminalNode: facetList){
			facetStringList.add(terminalNode.getText());
		}
		List<RestrictionValueContext> restrictionValueList = datatypeRestrictionContext.restrictionValue();
		List<Literal> literalList = new ArrayList<>();
		for(RestrictionValueContext restrictionValueContext : restrictionValueList ){
			literalList.add(parseLiteral(restrictionValueContext.literal()));
		}
		//construct the ManchesterDatatypeRestriction
		return new ManchesterDatatypeRestriction(dataTypeMCI, facetStringList, literalList);
	}

	protected ManchesterClassInterface parseDataRange(DataRangeContext dataRangeContext){
		List<DataConjunctionContext> dataConjunctionContextList = dataRangeContext.dataConjunction();
		if(dataConjunctionContextList.size()==1){
			//there is only one element, so it is not a real disjunction, just a single conjunction
			return parseDataConjunctionContext(dataConjunctionContextList.get(0));
		} else {
			List<ManchesterClassInterface> manchesterDataConjunctionList = new ArrayList<>();
			for (DataConjunctionContext dataConjunctionContext : dataConjunctionContextList) {
				manchesterDataConjunctionList.add(parseDataConjunctionContext(dataConjunctionContext));
			}
			return new ManchesterDataRange(manchesterDataConjunctionList);
		}
	}

	protected ManchesterClassInterface parseDataConjunctionContext(DataConjunctionContext dataConjunctionContext){
		List<DataPrimaryContext> dataPrimaryContextList = dataConjunctionContext.dataPrimary();
		if(dataPrimaryContextList.size() == 1){
			//there is only one element, so it is not a real conjunction, just a single value
			return parseDataPrimary(dataPrimaryContextList.get(0));
		} else {
			List<ManchesterClassInterface> dataPrimaryList = new ArrayList<>();
			for (DataPrimaryContext dataPrimaryContext : dataPrimaryContextList) {
				dataPrimaryList.add(parseDataPrimary(dataPrimaryContext));
			}
			return new ManchesterDataConjunction(dataPrimaryList);
		}
	}

	/************************************************************/
	
	private IRI getIRIFromResource(ClassIRIContext classIRIcontext){
		if(classIRIcontext.IRIREF() != null){
			//it is directly an IRI
			String baseClass = classIRIcontext.IRIREF().getText();
			//remove the starting '<' and the ending '>'
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
	
	protected IRI getIRIFromResource(ObjectPropertyIRIContext objPropIRIContext) {
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
		if(literalContext.typedLiteral() != null) {
			//it is a typedLiteral
			String quotedString = literalContext.typedLiteral().quotedString().getText();
			String classIRI = literalContext.typedLiteral().classIRI().getText();
			return valueFactory.createLiteral(quotedString.substring(1, quotedString.length()-1), classIRI);
		} else if(literalContext.stringLiteralNoLanguage()!=null){
			String quotedString = literalContext.stringLiteralNoLanguage().quotedString().getText();
			return valueFactory.createLiteral(quotedString.substring(1, quotedString.length()-1));
		} else if(literalContext.stringLiteralWithLanguage() != null) {
			String quotedString = literalContext.stringLiteralWithLanguage().quotedString().getText();
			String lang = literalContext.stringLiteralWithLanguage().LANGTAG().getText().substring(1);
			return valueFactory.createLiteral(quotedString.substring(1, quotedString.length()-1), lang);
		} else if (literalContext.integerLiteral() != null) {
			String sign = "";
			if(literalContext.integerLiteral().sign != null && literalContext.integerLiteral().sign.getText().equals("-")) {
				sign = "-";
			}
			String intValue = sign+literalContext.integerLiteral().INTEGER().getText();
			return valueFactory.createLiteral(intValue, XMLSchema.INTEGER);
		} else if (literalContext.decimalLiteral() != null) {
			String sign = "";
			if(literalContext.decimalLiteral().sign != null && literalContext.decimalLiteral().sign.getText().equals("-")) {
				sign = "-";
			}
			String decimalValue = sign+literalContext.decimalLiteral().intPart.getText()+"."+literalContext.decimalLiteral().decPart.getText();
			return valueFactory.createLiteral(decimalValue, XMLSchema.DECIMAL);
		} else if(literalContext.floatingPointLiteral() != null) {
			String sign = "";
			if(literalContext.floatingPointLiteral().sign != null && literalContext.floatingPointLiteral().sign.getText().equals("-")) {
				sign = "-";
			}
			String decimalPart = sign+literalContext.floatingPointLiteral().intPart.getText()+".";
			if (literalContext.floatingPointLiteral().decPart == null ){
				decimalPart+="0";
			} else {
				decimalPart+=literalContext.floatingPointLiteral().decPart.getText();
			}
			//get the exponent part
			ExponentContext exponentContext = literalContext.floatingPointLiteral().exponent();
			String singExp = "";
			if(exponentContext.sign != null && exponentContext.sign.getText().equals("-")) {
				singExp = "-";
			}
			String floatPart = "E"+singExp+exponentContext.expPart.getText();
			//combine the two parts
			String completeValue = decimalPart+floatPart;
			return valueFactory.createLiteral(completeValue, XMLSchema.FLOAT);
		} else {
			//this should never happen
			throw new ManchesterParserRuntimeException("The literal  "+literalContext.getText()+" is not supported");
		}


		/*String literalWithNoises = literalContext.string().getText();
		String label = literalWithNoises.substring(1, literalWithNoises.length()-1);
		if(literalContext.LANGTAG() != null){
			return valueFactory.createLiteral(label, literalContext.LANGTAG().getText().substring(1));
		} else if(literalContext.classIRI() != null){
			return valueFactory.createLiteral(label, getIRIFromResource(literalContext.classIRI()));
		} else{
			return valueFactory.createLiteral(label);
		}*/
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
