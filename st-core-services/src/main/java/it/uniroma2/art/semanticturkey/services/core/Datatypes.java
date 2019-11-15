package it.uniroma2.art.semanticturkey.services.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * This class provides services for manipulating datatypes.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Datatypes extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Datatypes.class);

	@Autowired
	private CustomFormManager cfManager;

	private static final Set<IRI> owl2datatypeMap = ImmutableSet.copyOf(new IRI[] {
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl#real"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl#rational"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#decimal"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#integer"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#nonNegativeInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#nonPositiveInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#positiveInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#negativeInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#long"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#int"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#short"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#byte"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedLong"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedInt"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedShort"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedByte"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#string"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#normalizedString"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#token"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#language"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#Name"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#NCName"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#NMTOKEN"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#boolean"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#hexBinary"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#base64Binary"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTimeStamp"),
			SimpleValueFactory.getInstance()
					.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral")

	});

	private static final Set<IRI> rdf11XmlSchemaBuiltinDatatypes = ImmutableSet.copyOf(new IRI[] {
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#string"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#boolean"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#decimal"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#integer"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#double"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#float"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#date"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#time"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTimeStamp"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#gYear"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#gMonth"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#gDay"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#gYearMonth"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#gMonthDay"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#duration"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#yearMonthDuration"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dayTimeDuration"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#byte"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#short"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#int"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#long"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedByte"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedShort"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedInt"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#unsignedLong"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#positiveInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#nonNegativeInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#negativeInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#nonPositiveInteger"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#hexBinary"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#base64Binary"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#language"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#normalizedString"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#token"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#NMTOKEN"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#Name"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#NCName") });

	private static final Set<IRI> builtinDatatypes = Sets.union(rdf11XmlSchemaBuiltinDatatypes,
			owl2datatypeMap);

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'C')")
	public AnnotatedValue<IRI> createDatatype(
			@NotLocallyDefined @Created(role = RDFResourceRole.dataRange) IRI newDatatype) {

		if (owl2datatypeMap.contains(newDatatype)) {
			throw new IllegalArgumentException(
					"The datatype IRI matches an item of the OWL2 Datatype Map: " + newDatatype);
		}

		RepositoryConnection conn = getManagedConnection();
		conn.add(newDatatype, RDF.TYPE, RDFS.DATATYPE, getWorkingGraph());

		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<>(newDatatype);
		annotatedValue.setAttribute("role", RDFResourceRole.dataRange.name());
		annotatedValue.setAttribute("explicit", true);

		return annotatedValue;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'D')")
	public void deleteDatatype(@LocallyDefined @Created(role = RDFResourceRole.dataRange) IRI datatype) {

		RepositoryConnection conn = getManagedConnection();
		conn.remove(datatype, null, null, getWorkingGraph());
		conn.remove((Resource) null, null, datatype, getWorkingGraph());
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'R')")
	public Collection<AnnotatedValue<Resource>> getDatatypes() {
		Collection<AnnotatedValue<Resource>> declaredDts = getDeclaredDatatypes();
		Collection<AnnotatedValue<Resource>> builtinDts = getBuiltinDatatypes();

		Set<Resource> declaredDatatypeSet = declaredDts.stream().map(AnnotatedValue::getValue)
				.collect(toSet());

		ArrayList<AnnotatedValue<Resource>> datatypes = new ArrayList<>(
				declaredDts.size() + builtinDts.size());
		datatypes.addAll(declaredDts);

		for (AnnotatedValue<Resource> adt : builtinDts) {
			if (!declaredDatatypeSet.contains(adt.getValue())) {
				datatypes.add(adt);
			}
		}

		return datatypes;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'R')")
	public Collection<AnnotatedValue<Resource>> getDeclaredDatatypes() {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +                                      
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>                                \n" +                                      
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                        \n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                         \n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                      	  \n" +
            "                                                                             \n" +                                      
			//adding the nature in the SELECT, which should be removed when the appropriate processor is used
			" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE { 		  \n" + 
			"   ?resource a rdfs:Datatype .                                               \n" +
			"   FILTER(isIRI(?resource))                                                  \n" +
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                           \n" + 
			" GROUP BY ?resource                                                          \n" 
			// @formatter:on
		);
		qb.processRendering();
		qb.processQName();

		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'R')")
	public Collection<AnnotatedValue<Resource>> getOWL2DatatypeMap() {
		return getIntrinsicDatatypesHelper(owl2datatypeMap);
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'R')")
	public Collection<AnnotatedValue<Resource>> getRDF11XmlSchemaBuiltinDatatypes() {
		return getIntrinsicDatatypesHelper(rdf11XmlSchemaBuiltinDatatypes);
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'R')")
	public Collection<AnnotatedValue<Resource>> getBuiltinDatatypes() {
		return getIntrinsicDatatypesHelper(builtinDatatypes);
	}

	public Collection<AnnotatedValue<Resource>> getIntrinsicDatatypesHelper(Set<IRI> datatypes)
			throws QueryBuilderException, QueryEvaluationException {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +                                      
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>                                \n" +                                      
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                        \n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                         \n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                      	  \n" +
            "                                                                             \n" +                                      
			//adding the nature in the SELECT, which should be removed when the appropriate processor is used
			" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE { 		  \n" + 
			"   VALUES(?resource) {                                                       \n" + 
			datatypes.stream().map(dt -> "(" + RenderUtils.toSPARQL(dt) + ")").collect(Collectors.joining("\n")) +
			"   }                                                                         \n" + 
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                           \n" + 
			" GROUP BY ?resource                                                          \n"
			// @formatter:on
		);
		qb.processRendering();
		qb.processQName();

		return qb.runQuery();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public void setDatatypeRestriction(IRI datatype, IRI restriction, Literal value)  {
		RepositoryConnection conn = getManagedConnection();
		ValueFactory vf = conn.getValueFactory();

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		BNode restrictionList = getRestrictionList(datatype, conn);
		if (restrictionList == null) { //still no restrictions => set the first restriction
			BNode newRestrictionBNode = vf.createBNode();
			BNode newRestrictionList = addRestrictionInList(newRestrictionBNode, RDF.NIL, conn, modelAdditions);
			modelAdditions.add(datatype, OWL.WITHRESTRICTIONS, newRestrictionList); //add the new ?datatype owl:withRestrictions _:newList
			modelAdditions.add(newRestrictionBNode, restriction, value); //(es _:r xsd:pattern "[0-9]+")
		} else {
			/*
			 * restriction list not empty, two scenarios:
			 * - the restriction has already a value, so it must be replaced
			 * - the restriction has not a value
			 */
			BNode oldRestrictionBNode = getRestrictionNode(restrictionList, restriction, conn);
			if (oldRestrictionBNode != null) { //restriction already existing => replace
				Literal oldValue = (Literal) conn.getStatements(oldRestrictionBNode, restriction, null, getWorkingGraph()).next().getObject();
				modelRemovals.add(oldRestrictionBNode, restriction, oldValue);
				modelAdditions.add(oldRestrictionBNode, restriction, value);
			} else { //restriction still not exists => create (the old restrictionList "shifts" as rdf:rest list
				BNode newRestrictionBNode = vf.createBNode();
				BNode newRestrictionList = addRestrictionInList(newRestrictionBNode, restrictionList, conn, modelAdditions);
				modelRemovals.add(datatype, OWL.WITHRESTRICTIONS, restrictionList); //remove the old ?datatype owl:withRestrictions _:oldList
				modelAdditions.add(datatype, OWL.WITHRESTRICTIONS, newRestrictionList); //add the new ?datatype owl:withRestrictions _:newList
				modelAdditions.add(newRestrictionBNode, restriction, value); //(es _:r xsd:pattern "[0-9]+")
			}
		}
		conn.add(modelAdditions, getWorkingGraph());
		conn.remove(modelRemovals, getWorkingGraph());
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public void deleteDatatypeRestriction(IRI datatype, IRI restriction)  {
		RepositoryConnection conn = getManagedConnection();

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		BNode restrictionList = getRestrictionList(datatype, conn);
		//No null-check on restrictionList: this service should be invoked only if the datatype has the restriction

		/*
		 * removes the triple _:r ?restriction ?value
		 */
		BNode restrictionBNode = getRestrictionNode(restrictionList, restriction, conn);
		Literal value = (Literal) conn.getStatements(restrictionBNode, restriction, null, getWorkingGraph()).next().getObject();
		modelRemovals.add(restrictionBNode, restriction, value);
		/*
		 * Now shifts the rest. Two cases:
		 * - the restriction was the first element of the list => the rest list shifts as object of ?datatype owl:withRestrictions
		 * - the restriction was not the first element of the list => the rest list shifts as rest of the previous list
		 */
		if (conn.hasStatement(restrictionList, RDF.FIRST, restrictionBNode, false, getWorkingGraph())) { //first
			Resource restList = (Resource) conn.getStatements(restrictionList, RDF.REST, null, getWorkingGraph()).next().getObject();
			//remove the old list
			modelRemovals.add(datatype, OWL.WITHRESTRICTIONS, restrictionList);
			modelRemovals.add(restrictionList, RDF.TYPE, RDF.LIST);
			modelRemovals.add(restrictionList, RDF.FIRST, restrictionBNode);
			modelRemovals.add(restrictionList, RDF.REST, restList);
			//shift the rest
			if (restList.equals(RDF.NIL)) { //if the restList is nil, completely remove the restrictions to the datatype
				modelRemovals.add(datatype, OWL.WITHRESTRICTIONS, restrictionList);
			} else { //set the restList as object of ?datatype owl:withRestrictions
				modelAdditions.add(datatype, OWL.WITHRESTRICTIONS, restList);
			}
		} else { //not first
			//remove the node and shifts the rest list
			Resource listNode = conn.getStatements(null, RDF.FIRST, restrictionBNode, getWorkingGraph()).next().getSubject();
			Resource prevList = conn.getStatements(null, RDF.REST, listNode, getWorkingGraph()).next().getSubject();
			Resource restList = (Resource) conn.getStatements(listNode, RDF.REST, null, getWorkingGraph()).next().getObject();
			//remove the old list
			modelRemovals.add(prevList, RDF.REST, listNode);
			modelRemovals.add(listNode, RDF.TYPE, RDF.LIST);
			modelRemovals.add(listNode, RDF.FIRST, restrictionBNode);
			modelRemovals.add(listNode, RDF.REST, restList);
			//set the restList as rest of the previous one
			modelAdditions.add(prevList, RDF.REST, restList);
		}
		conn.add(modelAdditions, getWorkingGraph());
		conn.remove(modelRemovals, getWorkingGraph());
	}

	/**
	 * Returns the node representing the restrictions list for the given datatype (?datatype owl:withRestrictions ?restList).
	 * If the datatype has still no restriction list, returns null
	 * @param datatype
	 * @param conn
	 * @return
	 */
	private BNode getRestrictionList(IRI datatype, RepositoryConnection conn) {
		RepositoryResult<Statement> stmts = conn.getStatements(datatype, OWL.WITHRESTRICTIONS, null, getWorkingGraph());
		if (stmts.hasNext()) {
			return (BNode) stmts.next().getObject();
		} else {
			return null;
		}
	}

	/**
	 * Returns the BNode that represents the subject of the triple
	 * _:b ?restrictionPred ?value
	 * for the given restriction.
	 * If the restriction has still no value, returns null
	 * @param restrictionList
	 * @param restrictionPred
	 * @param conn
	 * @return
	 */
	private BNode getRestrictionNode(BNode restrictionList, IRI restrictionPred, RepositoryConnection conn) {
		Resource graphs = getWorkingGraph();
		RepositoryResult<Statement> results = conn.getStatements(restrictionList, RDF.FIRST, null, graphs);
		BNode restrictionNode = (BNode) results.next().getObject();
		if (conn.hasStatement(restrictionNode, restrictionPred, null, false, graphs)) { //searched restriction is the first
			return restrictionNode;
		} else { //look for the restriction in the rest of the list
			results = conn.getStatements(restrictionList, RDF.REST, null, false, graphs);
			Resource rest = (Resource) results.next().getObject();
			if (rest.equals(RDF.NIL)) {
				return null;
			} else {
				return getRestrictionNode((BNode) rest, restrictionPred, conn);
			}
		}
	}

	/**
	 * Adds a restriction node to a restriction list: creates and returns a new list where the old list "shifts"
	 * as rest and the restriction node is the new first.
	 * @param restrictionBNode
	 * @param list
	 * @param repoConnection
	 * @param modelAdditions
	 * @return
	 */
	private BNode addRestrictionInList(BNode restrictionBNode, Resource list, RepositoryConnection repoConnection, Model modelAdditions){
		BNode newRestrictionListBNode = repoConnection.getValueFactory().createBNode();
		modelAdditions.add(newRestrictionListBNode, RDF.TYPE, RDF.LIST);
		modelAdditions.add(newRestrictionListBNode, RDF.FIRST, restrictionBNode);
		modelAdditions.add(newRestrictionListBNode, RDF.REST, list);
		return newRestrictionListBNode;
	}

	@STServiceOperation
	@Read
	public Map<IRI, Map<IRI, Literal>> getDatatypeRestrictions() {
		Map<IRI, Map<IRI, Literal>> restrictionsMap = new HashMap<>();
		RepositoryConnection conn = getManagedConnection();
		String query =
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
				"SELECT ?datatype ?facet ?value WHERE { \n" +
				"GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{\n" +
				"?datatype a rdfs:Datatype .\n" +
				"?datatype owl:withRestrictions ?list .\n" +
				"?list rdf:rest*/rdf:first ?r .\n" +
				"?r ?facet ?value .\n" +
				"}\n" +
				"}";
		System.out.println(query);
		TupleQuery tq = conn.prepareTupleQuery(query);
		TupleQueryResult results = tq.evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			IRI datatype = (IRI) bs.getValue("datatype");
			IRI facet = (IRI) bs.getValue("facet");
			Literal value = (Literal) bs.getValue("value");
			Map<IRI, Literal> dtEntry = restrictionsMap.computeIfAbsent(datatype, k -> new HashMap<>());
			dtEntry.put(facet, value);
		}
		return restrictionsMap;
	}

}