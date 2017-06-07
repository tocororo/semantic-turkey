package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.NotClassAxiomException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterClassInterface;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;


/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */

@STService
public class ManchesterHandler extends STServiceAdapter {

	/**
	 * returns all Manchester expression associated to the given classIRI (using owl:equivalentClassand and 
	 * rdfs:subClassOf ) and the starting bnode
	 * @param classIri the input classIRI
	 * @param usePrefixes true if the returned expression should be in qname, false for complete IRI
	 * @param useUppercaseSyntax true if the name of the symbols used in the Manchester should be in uppercase,
	 * false for lowercase 
	 * @return a map of with two keys, equivalent and sublcass, and for each key a map of Bnode and 
	 * associated Manchester Expression
	 */
	@STServiceOperation
	@Read
	public Map<String, Map<String, String>> getAllDLExpression(IRI classIri,
			@Optional(defaultValue = "true") boolean usePrefixes,
			@Optional(defaultValue = "true") boolean useUppercaseSyntax){
		Map<String, Map<String, String>> typeToMapBNodeToExpr = new HashMap<String, Map<String,String>>();
		RepositoryConnection conn = getManagedConnection();
		List<BNode> subClassBNodeList = new ArrayList<>();
		List<BNode> equivalentClassNodeList = new ArrayList<>();
		RepositoryResult<Statement> repRes = conn.getStatements(classIri, null, null);
		while(repRes.hasNext()){
			Statement statement = repRes.next();
			IRI pred = statement.getPredicate();
			Value obj = statement.getObject();
			if(pred.equals(OWL.EQUIVALENTCLASS) && obj instanceof BNode){
				equivalentClassNodeList.add((BNode) obj);
			} else if(pred.equals(RDFS.SUBCLASSOF) && obj instanceof BNode){
				subClassBNodeList.add((BNode) obj);
			}
		}
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		Map<String, String> namespaceToPrefixsMap = new HashMap<String, String>();
		for(String prefix: prefixToNamespacesMap.keySet()){
			namespaceToPrefixsMap.put(prefix, prefixToNamespacesMap.get(prefix));
		}
		//now iterate over the two lists
		Map<String, String>bnodeToExprEquivalentMap = new HashMap<>();
		typeToMapBNodeToExpr.put("equivalentClass", bnodeToExprEquivalentMap);
		for(BNode bnode : equivalentClassNodeList){
			String expr;
			try {
				expr = getSingleManchExpression(bnode, getUserNamedGraphs(), new ArrayList<>(), namespaceToPrefixsMap, 
						usePrefixes, useUppercaseSyntax);
				bnodeToExprEquivalentMap.put(bnode.stringValue(), expr);
			} catch (NotClassAxiomException e) {
				//do nothing
			}
			
		}
		Map<String, String>bnodeToExprSubClassMap = new HashMap<>();
		typeToMapBNodeToExpr.put("subClassOf", bnodeToExprSubClassMap);
		for(BNode bnode : equivalentClassNodeList){
			String expr;
			try {
				expr = getSingleManchExpression(bnode, getUserNamedGraphs(), new ArrayList<>(), namespaceToPrefixsMap, 
						usePrefixes, useUppercaseSyntax);
				bnodeToExprSubClassMap.put(bnode.stringValue(), expr);
			} catch (NotClassAxiomException e) {
				//do nothing
			}
			
		}
		
		
		
		return typeToMapBNodeToExpr;
	}
	
	/**
	 * returns the associated Manchester expression for the given bnode
	 * @param bnode the input bnode
	 * @param usePrefixes true if the returned expression should be in qname, false for complete IRI
	 * @param useUppercaseSyntax true if the name of the symbols used in the Manchester should be in uppercase,
	 * false for lowercase 
	 * @return the Manchester expression associated to the given bnode
	 * @throws NotClassAxiomException 
	 */
	@STServiceOperation
	@Read
	public String getExpression(BNode bnode, @Optional(defaultValue = "true") boolean usePrefixes,
			@Optional(defaultValue = "true") boolean useUppercaseSyntax) throws NotClassAxiomException{
		
		List<Statement> statList = new ArrayList<>();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		Map<String, String> namespaceToPrefixsMap = new HashMap<String, String>();
		for(String prefix: prefixToNamespacesMap.keySet()){
			namespaceToPrefixsMap.put(prefix, prefixToNamespacesMap.get(prefix));
		}
		return getSingleManchExpression(bnode, getUserNamedGraphs(), statList, namespaceToPrefixsMap, 
				usePrefixes, useUppercaseSyntax);
		
	}
	
	private String getSingleManchExpression(BNode bnode, Resource[] graphsArray, List<Statement> statList, 
			Map<String, String> namespaceToPrefixsMap, boolean usePrefixes, boolean useUppercaseSyntax) throws NotClassAxiomException{
		ManchesterClassInterface mci = ManchesterSyntaxUtils.getManchClassFromBNode(bnode, graphsArray, 
				statList, getManagedConnection());
		String expr = mci.getManchExpr(namespaceToPrefixsMap, usePrefixes, useUppercaseSyntax);
		
		if(expr.startsWith("(") && expr.endsWith("")){
			//remove the starting '(' and the end ')' 
			expr = expr.substring(1, expr.length()-1).trim();
		}
		
		return expr;
	}
	
	/**
	 * return true if the bnode represents a Class Axiom, false otherwise
	 * @param bnode the bnode to test
	 * @return true if the bnode represents a Class Axiom, false otherwise
	 */
	@STServiceOperation
	@Read
	public Boolean isClassAxiom(BNode bnode) {
		return ManchesterSyntaxUtils.isClassAxiom(bnode, getUserNamedGraphs(), getManagedConnection());
	}
	
	/**
	 * returns true if the expression is compliant with the syntax, false otherwise
	 * @return true if the expression is compliant with the syntax, false otherwise
	 */
	@STServiceOperation
	@Read
	public Boolean checkExpression(String manchExpr) {
		RepositoryConnection conn = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		try {
			ManchesterSyntaxUtils.parseCompleteExpression(manchExpr, conn.getValueFactory(), prefixToNamespacesMap);
		} catch (ManchesterParserException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Create the restriction in the ontology (all the necessary RDf triples) 
	 * @param classIri the class which the restriction should be associated to
	 * @param exprType the property linking the classIRI to the restriction (owl:equivalentClassand or rdfs:subClassOf)
	 * @param manchExpr the Manchester expression defying the restriction
	 * @return the newly created bnode 
	 * @throws ManchesterParserException
	 */
	@STServiceOperation
	@Write
	public AnnotatedValue<BNode> createRestriction(IRI classIri, IRI exprType, String manchExpr) 
			throws ManchesterParserException{
		RepositoryConnection conn = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		ManchesterClassInterface mci = ManchesterSyntaxUtils.parseCompleteExpression(manchExpr, 
				conn.getValueFactory(), prefixToNamespacesMap);
		
		List<Statement> statList = new ArrayList<>();
		//it is possible to cast the Resource to a BNode, because the input mci should have a bnode as 
		// starting element
		BNode newBnode = (BNode) ManchesterSyntaxUtils.parseManchesterExpr(mci, statList, conn.getValueFactory());
		
		conn.add(statList, getWorkingGraph());
		
		// add the subClass o equivalentClass property between the main ClassURI and the new BNode
		//TODO decide whether to check that expreType is either owl:equivalentClassand or rdfs:subClassOf 
		conn.add(conn.getValueFactory().createStatement(classIri, exprType, newBnode), getWorkingGraph());
		
		
		AnnotatedValue<BNode> annBNode = new AnnotatedValue<BNode>(newBnode);
		annBNode.setAttribute("role", RDFResourceRole.cls.name());
		
		return annBNode;
	}
	
	/**
	 * Remove all the RDF triples used to store the Restriction associated to the input classIRI using the 
	 * specific relation (owl:equivalentClassand and rdfs:subClassOf) 
	 * @param classIri (OPTIONL) the input classIRI
	 * @param exprType (OPTIONL) the relation (owl:equivalentClassand or rdfs:subClassOf)  linking the Restriction to 
	 * the input clasIRI 
	 * @param bnode the bnode representing the restriction
	 * @throws NotClassAxiomException 
	 */
	@STServiceOperation
	@Write
	public void removeExpression(@Optional IRI classIri, @Optional IRI exprType, BNode bnode) throws NotClassAxiomException{
		RepositoryConnection conn = getManagedConnection();
		List<Statement> statList = new ArrayList<>();
		ManchesterSyntaxUtils.getManchClassFromBNode(bnode, getUserNamedGraphs(), statList, conn);
		conn.remove(statList, getDeleteGraph());
		//delete the subClass o equivalentClass property between the main ClassURI and the BNode
		//TODO decide whether to check that expreType is either owl:equivalentClassand or rdfs:subClassOf 
		if(classIri != null && exprType != null){
			conn.remove(conn.getValueFactory().createStatement(classIri, exprType, bnode), getDeleteGraph());
		} else{
			//since the classIRI and/or exprType is not specified, try to get the from the ontology and remove
			conn.remove(conn.getStatements(null, null, bnode, getDeleteGraph()));
		}
	}
	
	/**
	 * Update the restriction by removing all the RDF triples used to store the old Restriction and then creting the new RDF triples.
	 * It uses the same BNode as the old restriction to represent the restriction itself
	 * the input clasIRI 
	 * @param bnode the bnode representing the restriction
	 * @return the same bnode passed in input and used to create the updated restriction 
	 * @throws ManchesterParserException 
	 * @throws NotClassAxiomException 
	 */
	@STServiceOperation
	@Write
	public AnnotatedValue<BNode> updateExpression(String newManchExpr, BNode bnode) 
			throws ManchesterParserException, NotClassAxiomException{
		//first of all, parse the new Expression to be sure that it is a valid one
		RepositoryConnection conn = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		ManchesterClassInterface mci = ManchesterSyntaxUtils.parseCompleteExpression(newManchExpr, 
				conn.getValueFactory(), prefixToNamespacesMap);
		
		//now remove the old triples
		List<Statement> statList = new ArrayList<>();
		ManchesterSyntaxUtils.getManchClassFromBNode(bnode, getUserNamedGraphs(), statList, conn);
		conn.remove(statList, getDeleteGraph());
		
		
		//then add the new triples
		statList = new ArrayList<>();
		BNode newBNode = (BNode) ManchesterSyntaxUtils.parseManchesterExpr(mci, statList, conn.getValueFactory());
		
		//since the Restriction should have the same bnode as "entry point", search in the generated triples
		//the ones having the newBNode and replace them with the old bnode
		List<Statement> tempStatList = new ArrayList<>(); 
		Iterator<Statement> iter = statList.iterator();
		while(iter.hasNext()){
			Statement stat = iter.next();
			if(stat.getSubject().equals(newBNode)){
				tempStatList.add(conn.getValueFactory().createStatement(bnode, stat.getPredicate(), stat.getObject()));
				iter.remove();
			} else if(stat.getObject().equals(newBNode)){
				tempStatList.add(conn.getValueFactory().createStatement(stat.getSubject(), stat.getPredicate(), bnode));
				iter.remove();
			}
		}
		statList.addAll(tempStatList);
		
		conn.add(statList, getWorkingGraph());
		
		AnnotatedValue<BNode> annBNode = new AnnotatedValue<BNode>(bnode);
		annBNode.setAttribute("role", RDFResourceRole.cls.name());
		
		return annBNode;
	}
}
