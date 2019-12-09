package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.parsers.ParserDatatypeRestrictionExpression;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.parsers.ParserDescription;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.parsers.ParserLiteralEnumerarionRestrictionExpression;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.parsers.ParserObjectPropertyExpression;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.*;
import it.uniroma2.art.semanticturkey.vocabulary.XSDFragment;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.exceptions.NotClassAxiomException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ManchesterClassInterface.PossType;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DescriptionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.ObjectPropertyExpressionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DatatypeRestrictionContext;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.LiteralListContext;

public class ManchesterSyntaxUtils {

	public static String OWL_SELF = "http://www.w3.org/2002/07/owl#hasSelf";
	public static String OWL_MINQUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#minQualifiedCardinality";
	public static String OWL_MAXQUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#maxQualifiedCardinality";
	public static String OWL_QUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#qualifiedCardinality";
	public static String OWL_ONCLASS = "http://www.w3.org/2002/07/owl#onClass";

	public static String printRes(boolean getPrefixName, Map<String, String> namespaceToPrefixsMap, IRI res) {
		if (!getPrefixName) {
			return "<" + res.stringValue() + ">";
		}

		String prefix = namespaceToPrefixsMap.get(res.getNamespace());

		if (prefix == null) {
			return "<" + res.stringValue() + ">";
		} else {
			return prefix + ":" + res.getLocalName();
		}
	}

	public static ManchesterClassInterface parseCompleteExpression(String mancExp, ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		// Get our lexer
		// ManchesterOWL2SyntaxParserLexer lexer = new
		// ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		BailSimpleLexer lexer = new BailSimpleLexer(CharStreams.fromString(mancExp));
		// Get a list of matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// Pass the tokens to the parser
		ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
		// set the error handler that does not try to recover from error, it just throw exception
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			DescriptionContext descriptionContext = parser.description();

			// Walk it and attach our listener
			ParseTreeWalker walker = new ParseTreeWalker();
			ParserDescription parserDescription = new ParserDescription(valueFactory, prefixToNamespacesMap);
			walker.walk(parserDescription, descriptionContext);
			ManchesterClassInterface mci = parserDescription.getManchesterClass();

			if (mci instanceof ManchesterBaseClass) {
				throw new ManchesterParserException(
						"The expression " + mancExp + " cannot be composed of a " + "single IRI/QName");
			}

			return mci;
		} catch (ManchesterParserRuntimeException e) {
			throw new ManchesterParserException(e);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ManchesterParserException(e);
		}
	}

	public static ObjectPropertyExpression parseObjectPropertyExpression(String objectPropertyExpression,
			ValueFactory valueFactory, Map<String, String> prefixToNamespacesMap)
			throws ManchesterParserException {
		// Get our lexer
		// ManchesterOWL2SyntaxParserLexer lexer = new
		// ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		BailSimpleLexer lexer = new BailSimpleLexer(CharStreams.fromString(objectPropertyExpression));
		// Get a list of matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// Pass the tokens to the parser
		ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
		// set the error handler that does not try to recover from error, it just throw exception
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			ObjectPropertyExpressionContext objectPropertyExpressionContext = parser
					.objectPropertyExpression();

			// Walk it and attach our listener
			ParseTreeWalker walker = new ParseTreeWalker();
			ParserObjectPropertyExpression parserOpe = new ParserObjectPropertyExpression(valueFactory,
					prefixToNamespacesMap);
			walker.walk(parserOpe, objectPropertyExpressionContext);
			return parserOpe.getObjectPropertyExpression();
		} catch (ManchesterParserRuntimeException e) {
			throw new ManchesterParserException(e);
		}

	}

	public static ManchesterClassInterface parseDatatypeRestrictionExpression(String mancExp, ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		// Get our lexer
		// ManchesterOWL2SyntaxParserLexer lexer = new
		// ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		BailSimpleLexer lexer = new BailSimpleLexer(CharStreams.fromString(mancExp));
		// Get a list of matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// Pass the tokens to the parser
		ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
		// set the error handler that does not try to recover from error, it just throw exception
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			DatatypeRestrictionContext datatypeRestrictionContext = parser.datatypeRestriction();

			// Walk it and attach our listener
			ParseTreeWalker walker = new ParseTreeWalker();
			ParserDescription parserDescription = new ParserDatatypeRestrictionExpression(valueFactory, prefixToNamespacesMap);
			walker.walk(parserDescription, datatypeRestrictionContext);
			ManchesterClassInterface mci = parserDescription.getManchesterClass();

			if (mci instanceof ManchesterBaseClass) {
				throw new ManchesterParserException(
						"The expression " + mancExp + " cannot be composed of a " + "single IRI/QName");
			}

			return mci;
		} catch (ManchesterParserRuntimeException e) {
			throw new ManchesterParserException(e);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ManchesterParserException(e);
		}
	}

	public static ManchesterClassInterface parseLiteralEnumerationExpression(String mancExp, ValueFactory valueFactory,
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException {
		// Get our lexer
		// ManchesterOWL2SyntaxParserLexer lexer = new
		// ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		BailSimpleLexer lexer = new BailSimpleLexer(CharStreams.fromString(mancExp));
		// Get a list of matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// Pass the tokens to the parser
		ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
		// set the error handler that does not try to recover from error, it just throw exception
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			LiteralListContext literalListContext = parser.literalList();

			// Walk it and attach our listener
			ParseTreeWalker walker = new ParseTreeWalker();
			ParserDescription parserDescription = new ParserLiteralEnumerarionRestrictionExpression(valueFactory, prefixToNamespacesMap);
			walker.walk(parserDescription, literalListContext);
			ManchesterClassInterface mci = parserDescription.getManchesterClass();

			if (mci instanceof ManchesterBaseClass) {
				throw new ManchesterParserException(
						"The expression " + mancExp + " cannot be composed of a " + "single IRI/QName");
			}

			return mci;
		} catch (ManchesterParserRuntimeException e) {
			throw new ManchesterParserException(e);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ManchesterParserException(e);
		}
	}



	public static Resource parseManchesterExpr(ManchesterClassInterface mci, List<Statement> statList,
			ValueFactory valueFactory) {
		// Value Value = null;
		// check the type
		if (mci instanceof ManchesterBaseClass) { // PossType.BASE
			// it is a class, so add nothing to the statList
			ManchesterBaseClass mbc = (ManchesterBaseClass) mci;
			return mbc.getBaseClass();
		} else if (mci instanceof ManchesterAndClass) { // PossType.AND
			ManchesterAndClass mac = (ManchesterAndClass) mci;
			List<ManchesterClassInterface> macClassList = mac.getAndClassList();
			List<Resource> andClassList = new ArrayList<>();
			for (ManchesterClassInterface mciInner : macClassList) {
				andClassList.add(parseManchesterExpr(mciInner, statList, valueFactory));
			}
			return createRDFList(OWL.CLASS, OWL.INTERSECTIONOF, statList, valueFactory, andClassList);
		} else if (mci instanceof ManchesterOrClass) { // PossType.OR
			ManchesterOrClass moc = (ManchesterOrClass) mci;
			List<ManchesterClassInterface> mocClassList = moc.getOrClassList();
			List<Resource> orClassList = new ArrayList<>();
			for (ManchesterClassInterface mciInner : mocClassList) {
				orClassList.add(parseManchesterExpr(mciInner, statList, valueFactory));
			}
			return createRDFList(OWL.CLASS, OWL.UNIONOF, statList, valueFactory, orClassList);
		} else if (mci instanceof ManchesterCardClass) {// PossType.MAX || PossType.MIN || PossType.EXACTLY
			ManchesterCardClass mcc = (ManchesterCardClass) mci;
			int card = mcc.getCard();
			Literal cardLiteral = valueFactory.createLiteral(Integer.toString(card),
					XMLSchema.NON_NEGATIVE_INTEGER);
			IRI prop = mcc.getProp();
			PossType type = mcc.getType();
			ManchesterClassInterface classQualCard = mcc.getClassCard();

			BNode restrictionBnode = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(restrictionBnode, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, mcc.hasInverse(), restrictionBnode, prop));
			IRI cardTypeUri = null;
			if (classQualCard == null) {
				if (type.equals(PossType.MAX)) {
					cardTypeUri = OWL.MAXCARDINALITY;
				} else if (type.equals(PossType.MIN)) {
					cardTypeUri = OWL.MINCARDINALITY;
				} else {
					cardTypeUri = OWL.CARDINALITY;
				}
			} else {
				if (type.equals(PossType.MAX)) {
					cardTypeUri = valueFactory.createIRI(OWL_MAXQUALIFIEDCARDINALITY);
				} else if (type.equals(PossType.MIN)) {
					cardTypeUri = valueFactory.createIRI(OWL_MINQUALIFIEDCARDINALITY);
				} else {
					cardTypeUri = valueFactory.createIRI(OWL_QUALIFIEDCARDINALITY);
				}
				Resource classQualCardClass = parseManchesterExpr(classQualCard, statList, valueFactory);

				statList.add(valueFactory.createStatement(restrictionBnode,
						valueFactory.createIRI(OWL_ONCLASS), classQualCardClass));
			}

			statList.add(valueFactory.createStatement(restrictionBnode, cardTypeUri, cardLiteral));
			return restrictionBnode;
		} else if (mci instanceof ManchesterNotClass) { // PossType.NOT
			ManchesterNotClass mnc = (ManchesterNotClass) mci;
			BNode notClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(notClass, RDF.TYPE, OWL.CLASS));
			statList.add(valueFactory.createStatement(notClass, OWL.COMPLEMENTOF,
					parseManchesterExpr(mnc.getNotClass(), statList, valueFactory)));
			return notClass;
		} else if (mci instanceof ManchesterOneOfClass) { // PossType.ONEOF
			ManchesterOneOfClass moc = (ManchesterOneOfClass) mci;
			List<IRI> individualList = moc.getOneOfList();
			return createRDFList(OWL.CLASS, OWL.ONEOF, statList, valueFactory, individualList);
		} else if (mci instanceof ManchesterLiteralListClass) {
			ManchesterLiteralListClass mllc = (ManchesterLiteralListClass) mci;
			List<Literal> literalList = mllc.getLiteralList();
			return createRDFList(RDFS.DATATYPE, OWL.ONEOF, statList, valueFactory, literalList);
		} else if (mci instanceof ManchesterOnlyClass) { // PossType.ONLY
			ManchesterOnlyClass moc = (ManchesterOnlyClass) mci;
			IRI prop = moc.getOnlyProp();
			Resource onlyInnerClass = parseManchesterExpr(moc.getOnlyClass(), statList, valueFactory);
			BNode onlyClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(onlyClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, moc.hasInverse(), onlyClass, prop));
			statList.add(valueFactory.createStatement(onlyClass, OWL.ALLVALUESFROM, onlyInnerClass));
			return onlyClass;
		} else if (mci instanceof ManchesterSomeClass) { // PossType.SOME
			ManchesterSomeClass msc = (ManchesterSomeClass) mci;
			IRI prop = msc.getSomeProp();
			Resource someInnerClass = parseManchesterExpr(msc.getSomeClass(), statList, valueFactory);
			BNode someClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(someClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, msc.hasInverse(), someClass, prop));
			statList.add(valueFactory.createStatement(someClass, OWL.SOMEVALUESFROM, someInnerClass));
			return someClass;
		} else if (mci instanceof ManchesterValueClass) { // PossType.VALUE
			ManchesterValueClass mvc = (ManchesterValueClass) mci;
			IRI prop = mvc.getProp();
			Value value = mvc.getValue();
			BNode valueClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(valueClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, mvc.hasInverse(), valueClass, prop));
			statList.add(valueFactory.createStatement(valueClass, OWL.HASVALUE, value));
			return valueClass;
		} else if (mci instanceof ManchesterSelfClass) { // PossType.SELF
			ManchesterSelfClass msc = (ManchesterSelfClass) mci;
			BNode selfClass = valueFactory.createBNode();
			IRI prop = msc.getProp();
			statList.add(valueFactory.createStatement(selfClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, msc.hasInverse(), selfClass, prop));
			statList.add(valueFactory.createStatement(selfClass, valueFactory.createIRI(OWL_SELF),
					valueFactory.createLiteral("true", XMLSchema.BOOLEAN)));
			return selfClass;
		}  else if (mci instanceof ManchesterDatatypeRestriction ) {
			/*
			 _:x rdf:type rdfs:Datatype .
			 _:x owl:onDatatype T(DT) .
			 _:x owl:withRestrictions T(SEQ _:y1 ... _:yn) .
			 _:y1 F1 lt1 .
			 ...
			 _:yn Fn ltn .
			 */
			ManchesterDatatypeRestriction mdtr = (ManchesterDatatypeRestriction) mci;
			ManchesterClassInterface datatypeMCI = mdtr.getDatatypeMCI();
			Resource datatypeRes = parseManchesterExpr(datatypeMCI, statList, valueFactory);
			List<String> facetList = mdtr.getFacetStringList();
			List<Literal> literalList = mdtr.getLiteralList();
			//since the list that is going to be generated has "complex" value in it (each RDF.FIRST is not linked with a direct resource but with a "complex" one)
			//calculate these complex resource and the BNode to which these resources are linked to
			List<BNode> bnodeList = new ArrayList<>();
			for(int i=0; i<facetList.size(); ++i){
				BNode facetsAndLiteralBNode = valueFactory.createBNode();
				statList.add(valueFactory.createStatement(facetsAndLiteralBNode, getPropforFacets(facetList.get(i), valueFactory), literalList.get(i)));
				bnodeList.add(facetsAndLiteralBNode);
			}
			BNode mainBnode = createRDFList(RDFS.DATATYPE, OWL.WITHRESTRICTIONS, statList, valueFactory, bnodeList);
			statList.add(valueFactory.createStatement(mainBnode, OWL.ONDATATYPE, datatypeRes));
			return mainBnode;
		} else if(mci instanceof ManchesterDataRange){
			ManchesterDataRange mdr = (ManchesterDataRange) mci;
			List<ManchesterClassInterface> dataConjunctionList = mdr.getDataConjunctionList();
			if(dataConjunctionList.size()==1){
				//there is only one element so no need for the OWL.UNIOFOF
				return parseManchesterExpr(dataConjunctionList.get(0), statList, valueFactory);
			} else {

				List<Resource> firstElemInList = new ArrayList<>();
				for(ManchesterClassInterface dataConjunction : dataConjunctionList){
					firstElemInList.add(parseManchesterExpr(dataConjunction, statList, valueFactory));
				}
				return createRDFList(RDFS.DATATYPE, OWL.UNIONOF, statList, valueFactory, firstElemInList);
			}
		} else if(mci instanceof ManchesterDataConjunction) {
			ManchesterDataConjunction mdc = (ManchesterDataConjunction) mci;
			List<ManchesterClassInterface> dataPrimaryList = mdc.getDataPrimaryList();
			if(dataPrimaryList.size() == 1){
				//there is only one element so no need for the OWL.INTERCECTIONOF
				return parseManchesterExpr(dataPrimaryList.get(0), statList, valueFactory);
			} else {
				List<Resource> firstElemInList = new ArrayList<>();
				for(ManchesterClassInterface dataPrimary : dataPrimaryList){
					firstElemInList.add(parseManchesterExpr(dataPrimary, statList, valueFactory));
				}
				return createRDFList(RDFS.DATATYPE, OWL.INTERSECTIONOF, statList, valueFactory, firstElemInList);
			}
		} else {
			// this should never happen
			// TODO decide what to do in this case
			return null;
		}
	}


	private static BNode createRDFList(IRI mainElemType, IRI propForList, List<Statement> statList,
			ValueFactory valueFactory, List<? extends Value> firstElemInList){
		BNode mainBNode = valueFactory.createBNode();

		statList.add(valueFactory.createStatement(mainBNode, RDF.TYPE, mainElemType));
		BNode intersectionOfBNode = valueFactory.createBNode();
		statList.add(valueFactory.createStatement(mainBNode, propForList, intersectionOfBNode));
		BNode prevBNodeInList = intersectionOfBNode;
		boolean first = true;
		for(Value firstElem : firstElemInList){
			if (first) {
				// it is the first element in the list, so it should not be "linked" with
				// anything using RDF.REST
				first = false;
			} else {
				// it is not the first element in the list, so it should be "linked" with
				// the previous element in the list using RDF.REST
				BNode currentBNodeInList = valueFactory.createBNode();
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, currentBNodeInList));
				prevBNodeInList = currentBNodeInList;
			}
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.TYPE, RDF.LIST));
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, firstElem));
		}
		// set the last rest as nil, to "close" the list
		statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));

		return mainBNode;
	}

	private static IRI getPropforFacets(String facet, ValueFactory valueFactory) {
		/*
			facet 	length 	xsd:length
			facet 	minLength 	xsd:minLength
			facet 	maxLength 	xsd:maxLength
			facet 	pattern 	xsd:pattern
			facet 	langRange 	rdf:langRange
			facet 	<= 	xsd:minInclusive
			facet 	< 	xsd:minExclusive
			facet 	>= 	xsd:maxInclusive
			facet 	> 	xsd:maxExclusive
		 */
		IRI propFromFacet = null;

		if(facet.toLowerCase().equals(ParserDescription.FACET_LENGTH)){
			propFromFacet = XSDFragment.LENGTH;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_MINLENGTH)) {
			propFromFacet = XSDFragment.MINLENGTH;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_MAXLENGTH)) {
			propFromFacet = XSDFragment.MAXLENGTH;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_PATTERN)) {
			propFromFacet = XSDFragment.PATTERN;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_LANGRANGE)) {
			propFromFacet = XSDFragment.LANGRANGE;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_LESSEQ)) {
			propFromFacet = XSDFragment.MININCLUSIVE;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_LESS)) {
			propFromFacet = XSDFragment.MINEXCLUSIVE;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_GREATEREQ)) {
			propFromFacet = XSDFragment.MAXINCLUSIVE;
		} else if(facet.toLowerCase().equals(ParserDescription.FACET_GREATER)) {
			propFromFacet = XSDFragment.MAXEXCLUSIVE;
		}

		return propFromFacet;
	}

	private static String getFacetFromIri(IRI iriFacet) {

		String facet = null;
		if(iriFacet.equals(XSDFragment.LENGTH)){
			facet = ParserDescription.FACET_LENGTH;
		} else if(iriFacet.equals(XSDFragment.MINLENGTH)) {
			facet = ParserDescription.FACET_MINLENGTH;
		} else if(iriFacet.equals(XSDFragment.MAXLENGTH)) {
			facet = ParserDescription.FACET_MAXLENGTH;
		} else if(iriFacet.equals(XSDFragment.PATTERN)) {
			facet = ParserDescription.FACET_PATTERN;
		} else if(iriFacet.equals(XSDFragment.LANGRANGE)) {
			facet = ParserDescription.FACET_LANGRANGE;
		} else if(iriFacet.equals(XSDFragment.MININCLUSIVE)) {
			facet = ParserDescription.FACET_LESSEQ;
		} else if(iriFacet.equals(XSDFragment.MINEXCLUSIVE)) {
			facet = ParserDescription.FACET_LESS;
		} else if(iriFacet.equals(XSDFragment.MAXINCLUSIVE)) {
			facet = ParserDescription.FACET_GREATEREQ;
		} else if(iriFacet.equals(XSDFragment.MAXEXCLUSIVE)) {
			facet = ParserDescription.FACET_GREATER;
		}
		return facet;
	}


	public static Resource parseObjectPropertyExpression(ObjectPropertyExpression ope,
			List<Statement> statList, ValueFactory valueFactory) {
		if (ope instanceof ObjectProperty) {
			return ((ObjectProperty) ope).getProperty();
		} else if (ope instanceof InverseObjectProperty) {
			BNode invProp = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(invProp, RDF.TYPE, OWL.OBJECTPROPERTY));
			statList.add(valueFactory.createStatement(invProp, OWL.INVERSEOF,
					((InverseObjectProperty) ope).getProperty()));
			return invProp;
		} else {
			throw new IllegalArgumentException(
					"Unsupported object property expression type: " + ope.getClass().getName());
		}
	}

	private static List<Statement> generateInverseTriples(ValueFactory valueFactory, boolean hasInverse,
			BNode bnode, IRI prop) {
		List<Statement> statList = new ArrayList<>();
		if (!hasInverse) {
			statList.add(valueFactory.createStatement(bnode, OWL.ONPROPERTY, prop));
		} else {
			BNode inverseBnode = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnode, OWL.ONPROPERTY, inverseBnode));
			statList.add(valueFactory.createStatement(inverseBnode, OWL.INVERSEOF, prop));
		}
		return statList;
	}

	/**
	 * return true if the bnode represents a class axiom, false otherwise
	 * 
	 * @param bnode
	 * @param resources
	 * @param repositoryConnection
	 * @return
	 */
	public static boolean isClassAxiom(BNode bnode, Resource[] resources,
			RepositoryConnection repositoryConnection) {
		List<Statement> statList = new ArrayList<>();
		ManchesterClassInterface mci;
		try {
			mci = ManchesterSyntaxUtils.getManchClassFromBNode(bnode, resources, statList,
					repositoryConnection);
		} catch (NotClassAxiomException e) {
			return false;
		}
		return true;
	}

	public static String getManchExprFromBNode(BNode bnode, Resource[] graphs, List<Statement> tripleList,
			boolean useUppercaseSyntax, RepositoryConnection conn) throws NotClassAxiomException {
		ManchesterClassInterface mci = getManchClassFromBNode(bnode, graphs, tripleList, conn);
		if (mci != null) {
			return mci.getManchExpr(useUppercaseSyntax);
		}
		return "";
	}

	public static String getManchExprFromBNode(BNode bnode, Map<String, String> namespaceToPrefixMap,
			boolean getPrefixName, Resource[] graphs, List<Statement> tripleList, boolean useUppercaseSyntax,
			RepositoryConnection conn) throws NotClassAxiomException {
		ManchesterClassInterface mci = getManchClassFromBNode(bnode, graphs, tripleList, conn);
		if (mci != null) {
			return mci.getManchExpr(namespaceToPrefixMap, getPrefixName, useUppercaseSyntax);
		}
		return "";
	}

	public static ManchesterClassInterface getManchClassFromBNode(BNode bnode, Resource[] graphs,
			List<Statement> tripleList, RepositoryConnection conn) throws NotClassAxiomException {
		// do a SPARQL DESCRIBE to obtain all the triple regarding this restriction
		String query = "DESCRIBE ?res WHERE {BIND (?inputRes AS ?res)}";
		GraphQuery graphQuery = conn.prepareGraphQuery(query);
		graphQuery.setBinding("inputRes", bnode);
		graphQuery.setIncludeInferred(false);
		SimpleDataset dataset = new SimpleDataset();
		for (Resource graphIRI : graphs) {
			if (graphIRI instanceof IRI) {
				dataset.addDefaultGraph((IRI) graphIRI);
			}
		}
		graphQuery.setDataset(dataset);
		GraphQueryResult graphQueryResult = graphQuery.evaluate();
		Model model = QueryResults.asModel(graphQueryResult);
		return getManchClassFromBNode(bnode, graphs, tripleList, model);

	}

	public static String getManchExprFromBNode(BNode bnode, Map<String, String> namespaceToPrefixMap,
			boolean getPrefixName, Resource[] graphs, List<Statement> tripleList, boolean useUppercaseSyntax,
			Model model) throws NotClassAxiomException {
		ManchesterClassInterface mci = getManchClassFromBNode(bnode, graphs, tripleList, model);
		if (mci != null) {
			return mci.getManchExpr(namespaceToPrefixMap, getPrefixName, useUppercaseSyntax);
		}
		return "";
	}

	public static ManchesterClassInterface getManchClassFromBNode(BNode bnode, Resource[] graphs,
			List<Statement> tripleList, Model model) throws NotClassAxiomException {
		// get all the triples having the input bnode as subject
		// RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
		Model filteredModel = model.filter(bnode, null, null, graphs);

		// check the predicate, which can be:
		// - OWL.INTERSECTIONOF
		// - OWL.UNIONOF
		// - OWL.COMPLEMENTOF
		// - OWL.ONEOF (for both Individuals and Literals)
		// - OWL.ONPROPERTY
		// - RDF.FIRST
		// - RDF.REST
		// - OWL.MAXCARDINALITY
		// - OWL.MINCARDINALITY
		// - OWL.CARDINALITY
		// - OWL.ALLVALUESFROM
		// - OWL.SOMEVALUESFROM
		// - OWL.HASVALUE
		// - OWL.SELF
		// - OWL.ONDATATYPE
		// - OWL.WITHRESTRICTIONS

		IRI prop = null;
		Value value = null;
		BNode objBnode = null;
		IRI objURI = null;
		BNode objBnode2 = null; // used with OWL.WITHRESTRICTIONS
		IRI objURI2 = null; // used with OWL.WITHRESTRICTIONS
		BNode onClassNode = null;
		IRI onClassIRI = null;
		PossType type = null;
		int card = -1;
		boolean inverse = false;
		IRI typeInRDF = null;


		// while (statements.hasNext()) {
		for (Statement stat : filteredModel) {
			// Statement stat = statements.next();
			if (tripleList != null) {
				tripleList.add(stat);
			}
			IRI pred = stat.getPredicate();
			if (pred.equals(OWL.INTERSECTIONOF)) {
				type = PossType.AND;
				objBnode = (BNode) stat.getObject();
			} else if (pred.equals(OWL.UNIONOF)) {
				type = PossType.OR;
				objBnode = (BNode) stat.getObject();
			} else if (pred.equals(OWL.COMPLEMENTOF)) {
				type = PossType.NOT;
				if (stat.getObject() instanceof BNode) {
					objBnode = (BNode) stat.getObject();
				} else {
					// since it is not a bnode, then it is a URI
					objURI = (IRI) stat.getObject();
				}
			} else if (pred.equals(OWL.ONEOF)) {
				// be careful, it can be a list of individuals or a list of literals
				type = PossType.ONEOF;
				objBnode = (BNode) stat.getObject();
			} else if (pred.equals(OWL.ONPROPERTY)) {
				// check if the object is a bnode or a URI
				Value obj = stat.getObject();
				if (obj instanceof BNode) {
					// this means that the restriction has an inverse object property, so you need to get the
					// real property
					inverse = true;
					// prop = (IRI)conn.getStatements((BNode)obj, OWL.INVERSEOF, null,
					// graphs).next().getObject(); // OLD
					prop = (IRI) Models.objectIRI(model.filter((BNode) obj, OWL.INVERSEOF, null, graphs))
							.get();
				} else {
					prop = (IRI) stat.getObject();
				}
				// it is not complete, there should be another triple in the same statIter with one of
				// the following predicate:
				// - OWL.MAXCARDINALITY
				// - OWL.MAXQUALIFIEDCARDINALITY
				// - OWL.MINCARDINALITY
				// - OWL.MINQUALIFIEDCARDINALITY
				// - OWL.CARDINALITY
				// - OWL.QUALIFIEDCARDINALITY
				// - OWL.ALLVALUESFROM
				// - OWL.SOMEVALUESFROM
				// - OWL.HASVALUE
				// - OWL.SELF
				// but its value is process in another else if
			} else if (pred.equals(OWL.MAXCARDINALITY)
					|| pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_MAXQUALIFIEDCARDINALITY))) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MAX;
			} else if (pred.equals(OWL.MINCARDINALITY)
					|| pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_MINQUALIFIEDCARDINALITY))) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MIN;
			} else if (pred.equals(OWL.CARDINALITY)
					|| pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_QUALIFIEDCARDINALITY))) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.EXACTLY;
			} else if (pred.equals(OWL.ALLVALUESFROM)) {
				type = PossType.ONLY;
				if (stat.getObject() instanceof BNode) {
					objBnode = (BNode) stat.getObject();
				} else {
					// since it is not a bnode, then it is a URI
					objURI = (IRI) stat.getObject();
				}
			} else if (pred.equals(OWL.SOMEVALUESFROM)) {
				type = PossType.SOME;
				if (stat.getObject() instanceof BNode) {
					objBnode = (BNode) stat.getObject();
				} else {
					// since it is not a bnode, then it is a URI
					objURI = (IRI) stat.getObject();
				}
			} else if (pred.equals(OWL.HASVALUE)) {
				value = stat.getObject();
				type = PossType.VALUE;
			} else if (pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_SELF))) {
				type = PossType.SELF;
			} else if (pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_ONCLASS))) {
				if (stat.getObject() instanceof BNode) {
					onClassNode = (BNode) stat.getObject();
				} else {
					onClassIRI = (IRI) stat.getObject();
				}
			} else if(pred.equals(RDF.TYPE)){
				typeInRDF = (IRI) stat.getObject();
			} else if(pred.equals(OWL.ONDATATYPE)) {
				type = PossType.DATATYPERESTRICTION;
				if (stat.getObject() instanceof BNode) {
					objBnode = (BNode) stat.getObject();
				} else {
					// since it is not a bnode, then it is a URI
					objURI = (IRI) stat.getObject();
				}
			} else if(pred.equals(OWL.WITHRESTRICTIONS)){
				type = PossType.DATATYPERESTRICTION; // a redundancy, just to be sure
				if (stat.getObject() instanceof BNode) {
					objBnode2 = (BNode) stat.getObject();
				} else {
					// since it is not a bnode, then it is a URI
					objURI2 = (IRI) stat.getObject();
				}
			}
		}

		if (type == null) {
			// it is not a class axiom, so return null, since no ManchesterClassInterface can be
			// associated to this bnode
			throw new NotClassAxiomException(bnode, graphs);
		}

		// all the information regarding this restriction have been process, now use the extracted
		// information to create the right object
		ManchesterClassInterface mci = null;
		if (type.equals(PossType.VALUE)) {
			if (prop == null || value == null) {
				// some required triple is missing, so it it not really a class axiom
				throw new NotClassAxiomException(bnode, graphs);
			}
			mci = new ManchesterValueClass(inverse, prop, value);
		} else if (type.equals(PossType.EXACTLY) || type.equals(PossType.MAX) || type.equals(PossType.MIN)) {
			if (onClassNode == null && onClassIRI == null) {
				if (prop == null || card == -1) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterCardClass(inverse, type, card, prop);
			} else {
				if (onClassNode != null) {
					if (prop == null || card == -1) {
						// some required triple is missing, so it it not really a class axiom
						throw new NotClassAxiomException(bnode, graphs);
					}
					mci = new ManchesterCardClass(inverse, type, card, prop,
							getManchClassFromBNode(onClassNode, graphs, tripleList, model));
				} else {
					if (prop == null || onClassIRI == null || card == -1) {
						// some required triple is missing, so it it not really a class axiom
						throw new NotClassAxiomException(bnode, graphs);
					}
					mci = new ManchesterCardClass(inverse, type, card, prop,
							new ManchesterBaseClass(onClassIRI));
				}
			}
		} else if (type.equals(PossType.AND)) {
			//two restrictions use the PossType.AND, so distinguish among them
			List<ManchesterClassInterface> andResourceList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRestForManchesterAxiom(objBnode, andResourceList, graphs, tripleList, model);
			if(typeInRDF.equals(OWL.CLASS)){
				mci = new ManchesterAndClass(andResourceList);
			} else {
				mci = new ManchesterDataConjunction(andResourceList);
			}

		} else if (type.equals(PossType.OR)) {
			//two restrictions use the PossType.AND, so distinguish among them
			List<ManchesterClassInterface> orResourceList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRestForManchesterAxiom(objBnode, orResourceList, graphs, tripleList, model);
			if(typeInRDF.equals(OWL.CLASS)){
				mci = new ManchesterOrClass(orResourceList);
			} else {
				mci = new ManchesterDataRange(orResourceList);
			}
		} else if (type.equals(PossType.NOT)) {
			if (objBnode != null) {
				mci = new ManchesterNotClass(getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				if (objURI == null) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterNotClass(new ManchesterBaseClass(objURI));
			}
		} else if (type.equals(PossType.ONEOF)) {
			// this one deals with both the list of individuals and the list of literals
			// List<ManchesterClassInterface> oneOfList = new ArrayList<ManchesterClassInterface>();
			List<Value> oneOfList = parseListFirstRest(objBnode, graphs, tripleList, model);
			boolean containsIRI = false;
			if (oneOfList.size() > 0) {
				// check the first element, and see if the list contains URI or literal
				if (oneOfList.get(0) instanceof IRI) {
					containsIRI = true;
				}
			}
			//decide if the data are IRI or Literal and instantiate the right structure
			if(containsIRI){
				mci = new ManchesterOneOfClass();
			} else {
				mci = new ManchesterLiteralListClass();
			}
			for (Value oneOfValue : oneOfList) {
				if (containsIRI) {
					((ManchesterOneOfClass) mci).addOneOf((IRI) oneOfValue);
				} else {
					((ManchesterLiteralListClass) mci).addOneOf((Literal) oneOfValue);
				}
			}
		} else if (type.equals(PossType.ONLY)) {
			if (objBnode != null) {
				if (prop == null) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterOnlyClass(inverse, prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				if (prop == null || objURI == null) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterOnlyClass(inverse, prop, new ManchesterBaseClass(objURI));
			}
		} else if (type.equals(PossType.SOME)) {
			if (objBnode != null) {
				if (prop == null) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterSomeClass(inverse, prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				if (prop == null || objURI == null) {
					// some required triple is missing, so it it not really a class axiom
					throw new NotClassAxiomException(bnode, graphs);
				}
				mci = new ManchesterSomeClass(inverse, prop, new ManchesterBaseClass(objURI));
			}
		} else if (type.equals(PossType.SELF)) {
			if (prop == null) {
				// some required triple is missing, so it it not really a class axiom
				throw new NotClassAxiomException(bnode, graphs);
			}
			mci = new ManchesterSelfClass(inverse, prop);
		} else if(type.equals(PossType.DATATYPERESTRICTION)) {
			ManchesterClassInterface dataTypeMCI = null;
			List<String> facetStringList = new ArrayList<>();
			List<Literal> literalList = new ArrayList<>();
			if(objBnode!=null){
				//the obj should always be an IRI, so there is something wrong
				throw new NotClassAxiomException(bnode, graphs);
			} else {
				dataTypeMCI = new ManchesterBaseClass(objURI);
			}
			if(objBnode2!=null){
				parseListFirstRestForManchesterAxiomForDatatypeRestriction(objBnode2, facetStringList,
						literalList, graphs, tripleList, model);
			} else {
				//the obj should always be a bnode, so there is something wrong
				throw new NotClassAxiomException(bnode, graphs);
			}

			mci = new ManchesterDatatypeRestriction(dataTypeMCI, facetStringList, literalList);
		} else {
			// this should never happen
		}
		return mci;
	}

	private static void parseListFirstRestForManchesterAxiom(BNode bnode,
			List<ManchesterClassInterface> manchClassList, Resource[] graphs, List<Statement> tripleList,
			Model model) throws NotClassAxiomException {
		// RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
		// while (statements.hasNext()) {

		Model firstModel = model.filter(bnode, RDF.FIRST, null, graphs);
		Model restModel = model.filter(bnode, RDF.REST, null, graphs);

		if (firstModel.isEmpty() || restModel.isEmpty()) {
			throw new NotClassAxiomException(bnode, graphs);
		}

		Value firstValue = Models.object(firstModel).get();
		if (firstValue instanceof IRI) {
			manchClassList.add(new ManchesterBaseClass((IRI) firstValue));
		} else if (firstValue instanceof BNode) {
			// it is a bnode, so it is a restriction itself
			manchClassList.add((getManchClassFromBNode((BNode) firstValue, graphs, tripleList, model)));
		} else {
			// something is wrong with this value, it is a Literal
			throw new NotClassAxiomException(bnode, graphs);
		}

		Value restValue = Models.object(restModel).get();
		// the first could be a bnode or RDF.Res.NIL
		if (restValue instanceof BNode) {
			parseListFirstRestForManchesterAxiom((BNode) restValue, manchClassList, graphs, tripleList,
					model);
		} else if (restValue.equals(RDF.NIL)) {
			// it is RDF.NIL, the end of the list
		} else {
			// something is wrong with this value, it is a Literal, so return false
			throw new NotClassAxiomException(bnode, graphs);
		}
	}

	private static List<Value> parseListFirstRest(Resource bnode, Resource[] graphs,
			List<Statement> tripleList, Model model) {
		List<Value> valueList = new ArrayList<>();
		// while (statements.hasNext()) {
		for (Statement stat : model.filter(bnode, null, null, graphs)) {
			if (tripleList != null) {
				tripleList.add(stat);
			}
			IRI pred = stat.getPredicate();
			if (pred.equals(RDF.FIRST)) {
				// the first could be a URI, a bnode or a literal, but in this case I'm not interested
				// in finding out
				valueList.add(stat.getObject());
			} else if (pred.equals(RDF.REST)) {
				// the rest could be a bnode or RDF.Res.NIL
				if (stat.getObject() instanceof BNode) {
					valueList.addAll(parseListFirstRest((BNode) stat.getObject(), graphs, tripleList, model));
				} else {
					// it is RDF.NIL, so set it to null
				}
			} else { // pred.equals(RDF.List)
				// nothing to do in this case
			}
		}
		return valueList;
	}


	private static void parseListFirstRestForManchesterAxiomForDatatypeRestriction(BNode bnode, List<String> facetStringList,
			List<Literal> literalList, Resource[] graphs, List<Statement> tripleList, Model model) throws NotClassAxiomException {
		Model firstModel = model.filter(bnode, RDF.FIRST, null, graphs);
		Model restModel = model.filter(bnode, RDF.REST, null, graphs);
		if (firstModel.isEmpty() || restModel.isEmpty()) {
			throw new NotClassAxiomException(bnode, graphs);
		}
		Value firstValue = Models.object(firstModel).get();
		if(!(firstValue instanceof BNode)){
			throw new NotClassAxiomException(bnode, graphs);
		}
		Model modelForFirst = model.filter((BNode)firstValue, null, null, graphs);
		facetStringList.add(getFacetFromIri(Models.predicate(modelForFirst).get()));
		Value obj = Models.object(modelForFirst).get();
		if(!(obj instanceof  Literal)){
			throw new NotClassAxiomException(bnode, graphs);
		} else {
			literalList.add((Literal) obj);
		}

		Value restValue = Models.object(restModel).get();
		// the first could be a bnode or RDF.Res.NIL
		if (restValue instanceof BNode) {
			parseListFirstRestForManchesterAxiomForDatatypeRestriction((BNode) restValue, facetStringList, literalList,
					graphs, tripleList, model);
		} else if (restValue.equals(RDF.NIL)) {
			// it is RDF.NIL, the end of the list
		} else {
			// something is wrong with this value, it is a Literal, so return false
			throw new NotClassAxiomException(bnode, graphs);
		}
	}

	public static class BailSimpleLexer extends ManchesterOWL2SyntaxParserLexer {

		public BailSimpleLexer(CharStream input) {
			super(input);
		}

		public void recover(LexerNoViableAltException e) {
			throw new ManchesterParserRuntimeException(e);
		}

	}
}
