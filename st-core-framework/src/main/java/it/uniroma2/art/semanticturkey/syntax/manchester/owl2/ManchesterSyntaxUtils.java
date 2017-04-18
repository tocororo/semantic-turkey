package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.eclipse.rdf4j.repository.RepositoryResult;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterClassInterface.PossType;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterOWL2SyntaxParserParser.DescriptionContext;



public class ManchesterSyntaxUtils {
	
	public static String OWL_SELF = "http://www.w3.org/2002/07/owl#hasSelf";
	public static String OWL_MINQUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#minQualifiedCardinality";
	public static String OWL_MAXQUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#maxQualifiedCardinality";
	public static String OWL_QUALIFIEDCARDINALITY = "http://www.w3.org/2002/07/owl#qualifiedCardinality";
	public static String OWL_ONCLASS = "http://www.w3.org/2002/07/owl#onClass";
	
	
	public static ManchesterClassInterface parseCompleteExpression(String mancExp, ValueFactory valueFactory, 
			Map<String, String> prefixToNamespacesMap) throws ManchesterParserException{
		// Get our lexer
		//ManchesterOWL2SyntaxParserLexer lexer = new ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		BailSimpleLexer lexer = new BailSimpleLexer(CharStreams.fromString(mancExp));
		// Get a list of matched tokens
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    // Pass the tokens to the parser
	    ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
	    //set the error handler that does not try to recover from error, it just throw exception
	    parser.setErrorHandler(new BailErrorStrategy());
	    try{
		    DescriptionContext descriptionContext = parser.description();
		    
		    // Walk it and attach our listener
		    ParseTreeWalker walker = new ParseTreeWalker();
		    ParserDescription parserDescription = new ParserDescription(valueFactory, 
		    		prefixToNamespacesMap);
	    	walker.walk(parserDescription, descriptionContext);
	    	ManchesterClassInterface mci = parserDescription.getManchesterClass();
	    	
	    	if(mci instanceof ManchesterBaseClass){
	    		throw new ManchesterParserException("The expression "+mancExp+" cannot be composed of a "
	    				+ "single IRI/QName");
	    	}
	    	
	    	return mci;
	    } catch(ManchesterParserRuntimeException e){
	    	throw new ManchesterParserException(e);
	    } catch (StringIndexOutOfBoundsException e){
	    	throw new ManchesterParserException(e);
	    }
	    
	}
	
	public static Resource parseManchesterExpr(ManchesterClassInterface mci, List<Statement> statList, 
			ValueFactory valueFactory) {
		//Value Value = null;
		// check the type
		if (mci instanceof ManchesterBaseClass ) { // PossType.BASE
			// it is a class, so add nothing to the statList
			ManchesterBaseClass mbc = (ManchesterBaseClass) mci;
			return mbc.getBaseClass();
		} else if (mci instanceof ManchesterAndClass) { // PossType.AND
			BNode bnodeForAnd = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeForAnd, RDF.TYPE, OWL.CLASS));

			BNode bnodeIntersectionOf = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeForAnd, OWL.INTERSECTIONOF, bnodeIntersectionOf));

			boolean first = true;
			ManchesterAndClass mac = (ManchesterAndClass) mci;
			List<ManchesterClassInterface> macClassList = mac.getAndClassList();
			BNode prevBNodeInList = bnodeIntersectionOf;
			for (ManchesterClassInterface mciInner : macClassList) {
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
				Resource innerClass = parseManchesterExpr(mciInner, statList, valueFactory);
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.TYPE, RDF.LIST));
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, innerClass));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeForAnd;
		} else if (mci instanceof ManchesterOrClass) { // PossType.OR
			BNode bnodeForOr = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeForOr, RDF.TYPE, OWL.CLASS));

			BNode bnodeUnionOf = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeForOr, OWL.UNIONOF, bnodeUnionOf));

			boolean first = true;
			ManchesterOrClass moc = (ManchesterOrClass) mci;
			List<ManchesterClassInterface> mocClassList = moc.getOrClassList();
			BNode prevBNodeInList = bnodeUnionOf;
			for (ManchesterClassInterface mciInner : mocClassList) {
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
				Resource innerClass = parseManchesterExpr(mciInner, statList, valueFactory);
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.TYPE, RDF.LIST));
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, innerClass));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeForOr;
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
			if(classQualCard != null){
				if (type.equals(PossType.MAX)) {
					cardTypeUri = OWL.MAXCARDINALITY;
				} else if (type.equals(PossType.MIN)) {
					cardTypeUri = OWL.MINCARDINALITY;
				} else {
					cardTypeUri = OWL.CARDINALITY;
				}
			} else{
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
		} else if (mci instanceof ManchesterOneOfClass) { //PossType.ONEOF
			ManchesterOneOfClass moc = (ManchesterOneOfClass) mci;
			List<IRI> individualList = moc.getOneOfList();
			boolean first = true;
			BNode bnodeClass = valueFactory.createBNode();
			BNode oneOfClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeClass, RDF.TYPE, OWL.CLASS));
			statList.add(valueFactory.createStatement(bnodeClass, OWL.ONEOF, oneOfClass));
			BNode prevBNodeInList = oneOfClass;
			for (IRI individual : individualList) {
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
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, individual));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeClass;
		} else if(mci instanceof ManchesterLiteralListClass){
			ManchesterLiteralListClass mllc = (ManchesterLiteralListClass) mci;
			BNode bnodeDatatype = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeDatatype, RDF.TYPE, RDFS.DATATYPE));
			List<Literal> literalList = mllc.getLiteralList();
			boolean first = true;
			BNode oneOfClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnodeDatatype, OWL.ONEOF, oneOfClass));
			BNode prevBNodeInList = oneOfClass;
			for(Literal literal : literalList){
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
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, literal));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeDatatype;
		}else if (mci instanceof ManchesterOnlyClass ) { //PossType.ONLY
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
		} else if (mci instanceof ManchesterValueClass ) { //PossType.VALUE
			ManchesterValueClass mvc = (ManchesterValueClass) mci;
			IRI prop = mvc.getProp();
			Value value = mvc.getValue();
			BNode valueClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(valueClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, mvc.hasInverse(), valueClass, prop));
			statList.add(valueFactory.createStatement(valueClass, OWL.HASVALUE, value));
			return valueClass;
		} else if(mci instanceof ManchesterSelfClass){ //PossType.SELF
			ManchesterSelfClass msc = (ManchesterSelfClass) mci;
			BNode selfClass = valueFactory.createBNode();
			IRI prop = msc.getProp();
			statList.add(valueFactory.createStatement(selfClass, RDF.TYPE, OWL.RESTRICTION));
			statList.addAll(generateInverseTriples(valueFactory, msc.hasInverse(), selfClass, prop));
			statList.add(valueFactory.createStatement(selfClass, valueFactory.createIRI(OWL_SELF), 
					valueFactory.createLiteral("true", XMLSchema.BOOLEAN)));
			return selfClass;
		}else {
			// this should never happen
			//TODO decide what to do in this case
			return null;
		}
	}
	
	private static List<Statement> generateInverseTriples(ValueFactory valueFactory, 
			boolean hasInverse, BNode bnode, IRI prop){
		List<Statement> statList = new ArrayList<>();
		if(!hasInverse){
			statList.add(valueFactory.createStatement(bnode, OWL.ONPROPERTY, prop));
		} else {
			BNode inverseBnode = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(bnode, OWL.ONPROPERTY, inverseBnode));
			statList.add(valueFactory.createStatement(inverseBnode, OWL.INVERSEOF, prop));
		}
		return statList;
	}

	public static String getManchExprFromBNode(BNode bnode, Resource[] graphs, 
			List<Statement> tripleList, boolean useUppercaseSyntax, RepositoryConnection conn) {
		ManchesterClassInterface mci = getManchClassFromBNode(bnode, graphs, tripleList, conn);
		if (mci != null) {
			return mci.getManchExpr(useUppercaseSyntax);
		}
		return "";
	}
	
	public static String getManchExprFromBNode(BNode bnode, Map<String, String> namespaceToPrefixMap, 
			boolean getPrefixName, Resource[] graphs, List<Statement> tripleList, 
			boolean useUppercaseSyntax, RepositoryConnection conn) {
		ManchesterClassInterface mci = getManchClassFromBNode(bnode, graphs, tripleList, conn);
		if (mci != null) {
			return mci.getManchExpr(namespaceToPrefixMap, getPrefixName, useUppercaseSyntax);
		}
		return "";
	}
	

	public static ManchesterClassInterface getManchClassFromBNode(BNode bnode,
			Resource[] graphs, List<Statement> tripleList, RepositoryConnection conn) {
		//do a SPARQL DESCRIBE to obtain all the triple regarding this restriction 
		String query = "DESCRIBE ?res WHERE {BIND (?inputRes AS ?res)}";
		GraphQuery graphQuery = conn.prepareGraphQuery(query);
		graphQuery.setBinding("inputRes", bnode);
		graphQuery.setIncludeInferred(false);
		SimpleDataset dataset = new SimpleDataset();
		for(Resource graphIRI : graphs){
			if(graphIRI instanceof IRI){
				dataset.addDefaultGraph((IRI) graphIRI);
			}
		}
		graphQuery.setDataset(dataset);
		GraphQueryResult graphQueryResult = graphQuery.evaluate();
		Model model = QueryResults.asModel(graphQueryResult);
		return getManchClassFromBNode(bnode, graphs, tripleList, model);
		
	}
	
	public static ManchesterClassInterface getManchClassFromBNode(BNode bnode,
			Resource[] graphs, List<Statement> tripleList, Model model) {
		//get all the triples having the input bnode as subject
		//RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
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

		IRI prop = null;
		Value value = null;
		BNode objBnode = null;
		IRI objURI = null;
		BNode onClassNode = null;
		IRI onClassIRI = null;
		PossType type = null;
		int card = 0;
		boolean inverse = false;
		//while (statements.hasNext()) {
		for(Statement stat : filteredModel) {
			//Statement stat = statements.next();
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
				//be careful, it can be a list of individuals or a list of literals
				type = PossType.ONEOF;
				objBnode = (BNode) stat.getObject();
			} else if (pred.equals(OWL.ONPROPERTY)) {
				//check if the object is a bnode or a URI
				Value obj = stat.getObject();
				if(obj instanceof BNode){
					//this means that the restriction has an inverse object property, so you need to get the 
					// real property
					inverse = true;
					//prop = (IRI)conn.getStatements((BNode)obj, OWL.INVERSEOF, null, graphs).next().getObject(); // OLD
					prop = (IRI)Models.objectIRI(model.filter((BNode)obj, OWL.INVERSEOF, null, graphs)).get();
				} else{
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
			}
			else if (pred.equals(OWL.MAXCARDINALITY) || 
					pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_MAXQUALIFIEDCARDINALITY))) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MAX;
			} else if (pred.equals(OWL.MINCARDINALITY) || 
					pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_MINQUALIFIEDCARDINALITY))) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MIN;
			} else if (pred.equals(OWL.CARDINALITY) || 
					pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_QUALIFIEDCARDINALITY))) {
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
			} else if(pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_SELF))){
				type = PossType.SELF;
			} else if(pred.equals(SimpleValueFactory.getInstance().createIRI(OWL_ONCLASS))){
				if(stat.getObject() instanceof BNode) {
					onClassNode = (BNode) stat.getObject();
				} else{
					onClassIRI = (IRI) stat.getObject();
				}
			}
		}

		// all the information regarding this restriction are been process, now use the extracted
		// information to create the right object
		ManchesterClassInterface mci = null;
		if (type.equals(PossType.VALUE)) {
			mci = new ManchesterValueClass(inverse, prop, value);
		} else if (type.equals(PossType.EXACTLY) || type.equals(PossType.MAX) || type.equals(PossType.MIN)) {
			if(onClassNode == null && onClassIRI == null){
				mci = new ManchesterCardClass(inverse, type, card, prop);
			} else{
				if(onClassNode != null){
					mci = new ManchesterCardClass(inverse, type, card, prop, 
							getManchClassFromBNode(onClassNode, graphs, tripleList, model));
				} else{
					mci = new ManchesterCardClass(inverse, type, card, prop, 
							new ManchesterBaseClass(onClassIRI));
				}
			}
		} else if (type.equals(PossType.AND)) {
			List<ManchesterClassInterface> andClassList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRest(objBnode, andClassList, graphs, tripleList, model);
			mci = new ManchesterAndClass(andClassList);
		} else if (type.equals(PossType.OR)) {
			List<ManchesterClassInterface> orClassList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRest(objBnode, orClassList, graphs, tripleList, model);
			mci = new ManchesterOrClass(orClassList);
		} else if (type.equals(PossType.NOT)) {
			if (objBnode != null) {
				mci = new ManchesterNotClass(getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				mci = new ManchesterNotClass(new ManchesterBaseClass(objURI));
			}
		} else if (type.equals(PossType.ONEOF)) { 
			// this one deals with both the list of individuals and the list of literals
			//List<ManchesterClassInterface> oneOfList = new ArrayList<ManchesterClassInterface>();
			List<Value> oneOfList = parseListFirstRest(objBnode, graphs, tripleList, model);
			mci = new ManchesterOneOfClass();
			boolean containsIRI = false;
			if(oneOfList.size()>0 ){
				//check the first element, and see if the list contains URI or literal
				if(oneOfList.get(0) instanceof IRI){
					containsIRI = true;
				}
			}
			for (Value oneOfValue : oneOfList) {
				if(containsIRI){
					((ManchesterOneOfClass) mci).addOneOf((IRI) oneOfValue);
				} else{
					((ManchesterLiteralListClass)mci).addOneOf((Literal) oneOfValue);
				}
			}
		} else if (type.equals(PossType.ONLY)) {
			if (objBnode != null) {
				mci = new ManchesterOnlyClass(inverse, prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				mci = new ManchesterOnlyClass(inverse, prop, new ManchesterBaseClass(objURI));
			}
		} else if (type.equals(PossType.SOME)) {
			if (objBnode != null) {
				mci = new ManchesterSomeClass(inverse, prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, model));
			} else { // the class is a URI
				mci = new ManchesterSomeClass(inverse, prop, new ManchesterBaseClass(objURI));
			}
		} else if(type.equals(PossType.SELF)){
			mci = new ManchesterSelfClass(inverse, prop);
		}else {
			// this should never happen
		}
		return mci;
	}
	
	private static void parseListFirstRest(Resource bnode, List<ManchesterClassInterface> manchClassList,
			Resource[] graphs, List<Statement> tripleList, Model model) {
		//RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
		//while (statements.hasNext()) {
		for(Statement stat : model.filter(bnode, null, null, graphs)){
			//Statement stat = statements.next();
			if(tripleList != null){
				tripleList.add(stat);
			}
			IRI pred = stat.getPredicate();
			if (pred.equals(RDF.FIRST)) {
				// the first could be a URI or a bnode
				if (stat.getObject() instanceof IRI) {
					manchClassList.add(new ManchesterBaseClass((IRI) stat.getObject()));
				} else {
					// it is a bnode, so it is a restriction itself
					manchClassList
							.add((getManchClassFromBNode((BNode) stat.getObject(), graphs, 
									tripleList, model)));
				}
			} else if (pred.equals(RDF.REST)) {
				// the first could be a bnode or RDF.Res.NIL
				if (stat.getObject() instanceof BNode) {
					parseListFirstRest((BNode) stat.getObject(), manchClassList, graphs, 
							tripleList, model);
				} else {
					// it is RDF.NIL, so set it to null
				}
			} else { //pred.equals(RDF.List)
				// nothing to do in this case
			}
		}
	}
	
	private static List<Value> parseListFirstRest(Resource bnode, Resource[] graphs, 
			List<Statement> tripleList, Model model) {
		List <Value> valueList = new ArrayList<>();
		//while (statements.hasNext()) {
		for(Statement stat : model.filter(bnode, null, null, graphs)){
			if(tripleList != null){
				tripleList.add(stat);
			}
			IRI pred = stat.getPredicate();
			if (pred.equals(RDF.FIRST)) {
				// the first could be a URI, a bnode or a literal, but in this case I'm not interested 
				// in finding out
				valueList.add(stat.getObject());
			} else if (pred.equals(RDF.REST)) {
				// the first could be a bnode or RDF.Res.NIL
				if (stat.getObject() instanceof BNode) {
					parseListFirstRest((BNode) stat.getObject(), graphs, tripleList, model);
				} else {
					// it is RDF.NIL, so set it to null
				}
			} else { //pred.equals(RDF.List)
				// nothing to do in this case
			}
		}
		return valueList;
	}
	
	
	public static class BailSimpleLexer extends ManchesterOWL2SyntaxParserLexer {

		public BailSimpleLexer(CharStream input) {
			super(input);
		}
		
		public void recover(LexerNoViableAltException e){
			throw new ManchesterParserRuntimeException(e);
		}
		
	}
}
