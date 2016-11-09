package it.uniroma2.art.semanticturkey.syntax.manchester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import it.uniroma2.art.semanticturkey.syntax.manchester.ManchesterClassInterface.PossType;


public class ManchesterSyntaxUtils {
	
	public static Resource parseManchesterExpr(ManchesterClassInterface mci, List<Statement> statList, 
			ValueFactory valueFactory) {
		//Value Value = null;
		// check the type
		if (mci.getType() == PossType.BASE) {
			// it is a class, so add nothing to the statList
			ManchesterBaseClass mbc = (ManchesterBaseClass) mci;
			return mbc.getBaseClass();
		} else if (mci.getType() == PossType.AND) {
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
				Value innerClass = parseManchesterExpr(mciInner, statList, valueFactory);
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.TYPE, RDF.LIST));
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, innerClass));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeForAnd;
		} else if (mci.getType() == PossType.OR) {
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
				Value innerClass = parseManchesterExpr(mciInner, statList, valueFactory);
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.TYPE, RDF.LIST));
				statList.add(valueFactory.createStatement(prevBNodeInList, RDF.FIRST, innerClass));
			}
			// set the last rest as nil, to "close" the list
			statList.add(valueFactory.createStatement(prevBNodeInList, RDF.REST, RDF.NIL));
			return bnodeForOr;
		} else if (mci.getType() == PossType.MAX || mci.getType() == PossType.MIN
				|| mci.getType() == PossType.EXACTLY) {
			ManchesterCardClass mcc = (ManchesterCardClass) mci;
			int card = mcc.getCard();
			Literal cardLiteral = valueFactory.createLiteral(Integer.toString(card), 
					XMLSchema.NON_NEGATIVE_INTEGER);
			IRI prop = mcc.getProp();
			PossType type = mcc.getType();

			BNode restrictionBnode = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(restrictionBnode, RDF.TYPE, OWL.RESTRICTION));
			statList.add(valueFactory.createStatement(restrictionBnode, OWL.ONPROPERTY, prop));
			IRI cardTypeUri = null;
			if (type == PossType.MAX) {
				cardTypeUri = OWL.MAXCARDINALITY;
			} else if (type == PossType.MIN) {
				cardTypeUri = OWL.MINCARDINALITY;
			} else {
				cardTypeUri = OWL.CARDINALITY;
			}
			statList.add(valueFactory.createStatement(restrictionBnode, cardTypeUri, cardLiteral));
			return restrictionBnode;
		} else if (mci.getType() == PossType.NOT) {
			ManchesterNotClass mnc = (ManchesterNotClass) mci;
			BNode notClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(notClass, RDF.TYPE, OWL.CLASS));
			statList.add(valueFactory.createStatement(notClass, OWL.COMPLEMENTOF,
					parseManchesterExpr(mnc.getNotClass(), statList, valueFactory)));
			return notClass;
		} else if (mci.getType() == PossType.ONEOF) {
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
		} else if (mci.getType() == PossType.ONLY) {
			ManchesterOnlyClass moc = (ManchesterOnlyClass) mci;
			IRI prop = moc.getOnlyProp();
			Value onlyInnerClass = parseManchesterExpr(moc.getOnlyClass(), statList, valueFactory);
			BNode onlyClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(onlyClass, RDF.TYPE, OWL.RESTRICTION));
			statList.add(valueFactory.createStatement(onlyClass, OWL.ONPROPERTY, prop));
			statList.add(valueFactory.createStatement(onlyClass, OWL.ALLVALUESFROM, onlyInnerClass));
			return onlyClass;
		} else if (mci.getType() == PossType.SOME) {
			ManchesterSomeClass msc = (ManchesterSomeClass) mci;
			IRI prop = msc.getSomeProp();
			Value someInnerClass = parseManchesterExpr(msc.getSomeClass(), statList, valueFactory);
			BNode someClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(someClass, RDF.TYPE, OWL.RESTRICTION));
			statList.add(valueFactory.createStatement(someClass, OWL.ONPROPERTY, prop));
			statList.add(valueFactory.createStatement(someClass, OWL.SOMEVALUESFROM, someInnerClass));
			return someClass;
		} else if (mci.getType() == PossType.VALUE) {
			ManchesterValueClass mvc = (ManchesterValueClass) mci;
			IRI prop = mvc.getProp();
			Value value = mvc.getValue();
			BNode valueClass = valueFactory.createBNode();
			statList.add(valueFactory.createStatement(valueClass, RDF.TYPE, OWL.RESTRICTION));
			statList.add(valueFactory.createStatement(valueClass, OWL.ONPROPERTY, prop));
			statList.add(valueFactory.createStatement(valueClass, OWL.HASVALUE, value));
			return valueClass;
		} else {
			// this should never happen
			//TODO decide what to do in this case
			return null;
		}
		

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
		RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
		
		// check the predicate, which can be:
		// - OWL.INTERSECTIONOF
		// - OWL.UNIONOF
		// - OWL.COMPLEMENTOF
		// - OWL.ONEOF
		// - OWL.ONPROPERTY
		// - RDF.FIRST
		// - RDF.REST
		// - OWL.MAXCARDINALITY
		// - OWL.MINCARDINALITY
		// - OWL.CARDINALITY
		// - OWL.ALLVALUESFROM
		// - OWL.SOMEVALUESFROM
		// - OWL.HASVALUE

		IRI prop = null;
		Value value = null;
		BNode objBnode = null;
		IRI objURI = null;
		PossType type = null;
		int card = 0;
		while (statements.hasNext()) {
			Statement stat = statements.next();
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
				type = PossType.ONEOF;
				objBnode = (BNode) stat.getObject();
			} else if (pred.equals(OWL.ONPROPERTY)) {
				prop = (IRI) stat.getObject();
				// it is not complete, there should be another triple in the same statIter with one of
				// the following predicate:
				// - OWL.MAXCARDINALITY
				// - OWL.MINCARDINALITY
				// - OWL.CARDINALITY
				// - OWL.ALLVALUESFROM
				// - OWL.SOMEVALUESFROM
				// - OWL.HASVALUE
				// but its value is process in another else if
			}
			/*
			 * else if (pred.equals(RDF.FIRST)) { // the first could be a URI or a bnode if
			 * (stat.getObject().isURIurce()) { firstManchClass = new
			 * ManchesterBaseClass(stat.getObject().asURIurce()); } else { // it is a bnode, so it is a
			 * restriction itself firstManchClass = getManchClassFromBNode(stat.getObject().asBNode(),
			 * graphs); } } else if (pred.equals(RDF.REST)) { // the first could be a bnode or RDF.NIL
			 * if (stat.getObject().isBlank()) { restManchClass =
			 * getManchClassFromBNode(stat.getObject().asBNode(), graphs); } else { // it is RDF.NIL, so
			 * set it to null restManchClass = null; } }
			 */
			else if (pred.equals(OWL.MAXCARDINALITY)) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MAX;
			} else if (pred.equals(OWL.MINCARDINALITY)) {
				card = Integer.parseInt(stat.getObject().stringValue());
				type = PossType.MIN;
			} else if (pred.equals(OWL.CARDINALITY)) {
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
			}
		}

		// all the information regarding this restriction are been process, now use the extracted
		// information to create the right object
		ManchesterClassInterface mci = null;
		if (type == PossType.VALUE) {
			mci = new ManchesterValueClass(prop, value);
		} else if (type == PossType.EXACTLY) {
			mci = new ManchesterCardClass(type, card, prop);
		} else if (type == PossType.MAX) {
			mci = new ManchesterCardClass(type, card, prop);
		} else if (type == PossType.MIN) {
			mci = new ManchesterCardClass(type, card, prop);
		} else if (type == PossType.AND) {
			List<ManchesterClassInterface> andClassList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRest(objBnode, andClassList, graphs, tripleList, conn);
			mci = new ManchesterAndClass(andClassList);
		} else if (type == PossType.OR) {
			List<ManchesterClassInterface> orClassList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRest(objBnode, orClassList, graphs, tripleList, conn);
			mci = new ManchesterOrClass(orClassList);
		} else if (type == PossType.NOT) {
			if (objBnode != null) {
				mci = new ManchesterNotClass(getManchClassFromBNode(objBnode, graphs, tripleList, conn));
			} else { // the class is a URI
				mci = new ManchesterNotClass(new ManchesterBaseClass(objURI));
			}
		} else if (type == PossType.ONEOF) {
			List<ManchesterClassInterface> oneOfList = new ArrayList<ManchesterClassInterface>();
			parseListFirstRest(objBnode, oneOfList, graphs, tripleList, conn);
			mci = new ManchesterOneOfClass();
			for (ManchesterClassInterface oneOfValue : oneOfList) {
				((ManchesterOneOfClass) mci).addOneOf(((ManchesterBaseClass) oneOfValue).getBaseClass());
			}
		} else if (type == PossType.ONLY) {
			if (objBnode != null) {
				mci = new ManchesterOnlyClass(prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, conn));
			} else { // the class is a URI
				mci = new ManchesterOnlyClass(prop, new ManchesterBaseClass(objURI));
			}
		} else if (type == PossType.SOME) {
			if (objBnode != null) {
				mci = new ManchesterSomeClass(prop,
						getManchClassFromBNode(objBnode, graphs, tripleList, conn));
			} else { // the class is a URI
				mci = new ManchesterSomeClass(prop, new ManchesterBaseClass(objURI));
			}
		} else {
			// this should never happen
		}
		return mci;
	}
	
	private static void parseListFirstRest(Resource bnode, List<ManchesterClassInterface> manchClassList,
			Resource[] graphs, List<Statement> tripleList, RepositoryConnection conn) {
		RepositoryResult<Statement> statements = conn.getStatements(bnode, null, null, graphs);
		while (statements.hasNext()) {
			Statement stat = statements.next();
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
									tripleList, conn)));
				}
			} else if (pred.equals(RDF.REST)) {
				// the first could be a bnode or RDF.Res.NIL
				if (stat.getObject() instanceof BNode) {
					parseListFirstRest((BNode) stat.getObject(), manchClassList, graphs, 
							tripleList, conn);
				} else {
					// it is RDF.NIL, so set it to null
				}
			} else { //pred.equals(RDF.List)
				// nothing to do in this case
			}
		}
	}
}
