package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.HashMap;
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

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
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
		Resource graphsArray[] = new Resource[1] ;
		graphsArray[0] = getWorkingGraph();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		Map<String, String> namespaceToPrefixsMap = new HashMap<String, String>();
		for(String prefix: prefixToNamespacesMap.keySet()){
			namespaceToPrefixsMap.put(prefix, prefixToNamespacesMap.get(prefix));
		}
		//now iterate over the two lists
		Map<String, String>bnodeToExprEquivalentMap = new HashMap<>();
		typeToMapBNodeToExpr.put("equivalentClass", bnodeToExprEquivalentMap);
		for(BNode bnode : equivalentClassNodeList){
			String expr = getSingleManchExpression(bnode, graphsArray, new ArrayList<>(), namespaceToPrefixsMap, 
					usePrefixes, useUppercaseSyntax);
			bnodeToExprEquivalentMap.put(bnode.stringValue(), expr);
		}
		Map<String, String>bnodeToExprSubClassMap = new HashMap<>();
		typeToMapBNodeToExpr.put("subClassOf", bnodeToExprSubClassMap);
		for(BNode bnode : equivalentClassNodeList){
			String expr = getSingleManchExpression(bnode, graphsArray, new ArrayList<>(), namespaceToPrefixsMap, 
					usePrefixes, useUppercaseSyntax);
			bnodeToExprSubClassMap.put(bnode.stringValue(), expr);
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
	 */
	@STServiceOperation
	@Read
	public String getExpression(BNode bnode, @Optional(defaultValue = "true") boolean usePrefixes,
			@Optional(defaultValue = "true") boolean useUppercaseSyntax){
		
		List<Statement> statList = new ArrayList<>();
		Resource graphsArray[] = new Resource[1] ;
		graphsArray[0] = getWorkingGraph();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		Map<String, String> namespaceToPrefixsMap = new HashMap<String, String>();
		for(String prefix: prefixToNamespacesMap.keySet()){
			namespaceToPrefixsMap.put(prefix, prefixToNamespacesMap.get(prefix));
		}
		return getSingleManchExpression(bnode, graphsArray, statList, namespaceToPrefixsMap, 
				usePrefixes, useUppercaseSyntax);
		
	}
	
	private String getSingleManchExpression(BNode bnode, Resource[] graphsArray, List<Statement> statList, 
			Map<String, String> namespaceToPrefixsMap, boolean usePrefixes, boolean useUppercaseSyntax){
		ManchesterClassInterface mci = ManchesterSyntaxUtils.getManchClassFromBNode(bnode, graphsArray, 
				statList, getManagedConnection());
		return mci.getManchExpr(namespaceToPrefixsMap, usePrefixes, useUppercaseSyntax);
	}
	
	/**
	 * Remove all the RDF triples used to store the Restriction associated to the input classIRI using the 
	 * specific relation (owl:equivalentClassand and rdfs:subClassOf) 
	 * @param classIri (OPTIONL) the input classIRI
	 * @param exprType (OPTIONL) the relation (owl:equivalentClassand or rdfs:subClassOf)  linking the Restriction to 
	 * the input clasIRI 
	 * @param bnode the bnode representing the restriction
	 */
	@STServiceOperation
	@Write
	public void removeExpression(@Optional IRI classIri, @Optional IRI exprType, BNode bnode){
		RepositoryConnection conn = getManagedConnection();
		List<Statement> statList = new ArrayList<>();
		Resource graphsArray[] = new Resource[1] ;
		graphsArray[0] = getWorkingGraph();
		ManchesterSyntaxUtils.getManchClassFromBNode(bnode, graphsArray, statList, conn);
		conn.remove(statList, getWorkingGraph());
		//delete the subClass o equivalentClass property between the main ClassURI and the BNode
		//TODO decide whether to check that expreType is either owl:equivalentClassand or rdfs:subClassOf 
		if(classIri != null && exprType != null){
			conn.remove(conn.getValueFactory().createStatement(classIri, exprType, bnode), getWorkingGraph());
		} else{
			//since the classIRI and/or exprType is not specified, try to get the from the ontology and remove
			conn.remove(conn.getStatements(null, null, bnode, getWorkingGraph()));
		}
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
	 * @return the newly created resource (mainly a bnode)
	 * @throws ManchesterParserException
	 */
	@STServiceOperation
	@Write
	public Resource createRestriction(IRI classIri, IRI exprType, String manchExpr) 
			throws ManchesterParserException{
		RepositoryConnection conn = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager().getNSPrefixMappings(false);
		ManchesterClassInterface mci = ManchesterSyntaxUtils.parseCompleteExpression(manchExpr, 
				conn.getValueFactory(), prefixToNamespacesMap);
		
		List<Statement> statList = new ArrayList<>();
		Resource newResource = ManchesterSyntaxUtils.parseManchesterExpr(mci, statList, conn.getValueFactory());
		
		conn.add(statList, getWorkingGraph());
		
		// add the subClass o equivalentClass property between the main ClassURI and the new BNode
		//TODO decide whether to check that expreType is either owl:equivalentClassand or rdfs:subClassOf 
		conn.add(conn.getValueFactory().createStatement(classIri, exprType, newResource), getWorkingGraph());
		
		
		return newResource;
	}
}
