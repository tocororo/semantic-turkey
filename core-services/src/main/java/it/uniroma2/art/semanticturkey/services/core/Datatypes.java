package it.uniroma2.art.semanticturkey.services.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.datarange.DataRangeAbstract;
import it.uniroma2.art.semanticturkey.datarange.DataRangeDataOneOf;
import it.uniroma2.art.semanticturkey.datarange.DatatypeRestrictionDescription;
import it.uniroma2.art.semanticturkey.datarange.ParseDataRange;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterPrefixNotDefinedException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterSyntacticException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Created;
import it.uniroma2.art.semanticturkey.services.annotations.Deleted;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ManchesterClassInterface;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

	private static final Logger logger = LoggerFactory.getLogger(Datatypes.class);

	private static final Set<IRI> owl2datatypeMap = ImmutableSet.copyOf(new IRI[]{
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl#real"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl#rational"),
			XMLSchema.DECIMAL,
			XMLSchema.INTEGER,
			XMLSchema.NON_NEGATIVE_INTEGER,
			XMLSchema.NON_POSITIVE_INTEGER,
			XMLSchema.POSITIVE_INTEGER,
			XMLSchema.NEGATIVE_INTEGER,
			XMLSchema.LONG,
			XMLSchema.INT,
			XMLSchema.SHORT,
			XMLSchema.BYTE,
			XMLSchema.UNSIGNED_LONG,
			XMLSchema.UNSIGNED_INT,
			XMLSchema.UNSIGNED_SHORT,
			XMLSchema.UNSIGNED_BYTE,
			XMLSchema.STRING,
			XMLSchema.NORMALIZEDSTRING,
			XMLSchema.TOKEN,
			XMLSchema.LANGUAGE,
			XMLSchema.NAME,
			XMLSchema.NCNAME,
			XMLSchema.NMTOKEN,
			XMLSchema.BOOLEAN,
			XMLSchema.HEXBINARY,
			XMLSchema.BASE64BINARY,
			XMLSchema.ANYURI,
			XMLSchema.DATETIME,
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTimeStamp"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral")
	});

	private static final Set<IRI> rdf11XmlSchemaBuiltinDatatypes = ImmutableSet.copyOf(new IRI[]{
			XMLSchema.STRING,
			XMLSchema.BOOLEAN,
			XMLSchema.DECIMAL,
			XMLSchema.INTEGER,
			XMLSchema.DOUBLE,
			XMLSchema.FLOAT,
			XMLSchema.DATE,
			XMLSchema.TIME,
			XMLSchema.DATETIME,
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#dateTimeStamp"),
			XMLSchema.GYEAR,
			XMLSchema.GMONTH,
			XMLSchema.GDAY,
			XMLSchema.GYEARMONTH,
			XMLSchema.GMONTHDAY,
			XMLSchema.DURATION,
			XMLSchema.YEARMONTHDURATION,
			XMLSchema.DAYTIMEDURATION,
			XMLSchema.BYTE,
			XMLSchema.SHORT,
			XMLSchema.INT,
			XMLSchema.LONG,
			XMLSchema.UNSIGNED_BYTE,
			XMLSchema.UNSIGNED_SHORT,
			XMLSchema.UNSIGNED_INT,
			XMLSchema.UNSIGNED_LONG,
			XMLSchema.POSITIVE_INTEGER,
			XMLSchema.NON_NEGATIVE_INTEGER,
			XMLSchema.NEGATIVE_INTEGER,
			XMLSchema.NON_POSITIVE_INTEGER,
			XMLSchema.HEXBINARY,
			XMLSchema.BASE64BINARY,
			XMLSchema.ANYURI,
			XMLSchema.LANGUAGE,
			XMLSchema.NORMALIZEDSTRING,
			XMLSchema.TOKEN,
			XMLSchema.NMTOKEN,
			XMLSchema.NAME,
			XMLSchema.NCNAME});

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
	public void deleteDatatype(@LocallyDefined @Deleted(role=RDFResourceRole.dataRange) IRI datatype) {
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
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'C')")
	public void setDatatypeEnumerationRestrictions(
			@LocallyDefined @Modified(role = RDFResourceRole.dataRange) IRI datatype, List<Literal> literals) {
		RepositoryConnection conn = getManagedConnection();
		deleteDatatypeRestrictionImpl(conn, datatype); //delete the previous

		Model modelAdditions = new LinkedHashModel();

		BNode datarange = conn.getValueFactory().createBNode();

		modelAdditions.add(conn.getValueFactory().createStatement(datatype, OWL.EQUIVALENTCLASS, datarange));
		modelAdditions
				.add(conn.getValueFactory().createStatement(datarange, RDF.TYPE, RDFS.DATATYPE));

		// if values is null or empty, create an empty RDF List
		if (literals == null || literals.isEmpty()) {
			modelAdditions
					.add(conn.getValueFactory().createStatement(datarange, OWL.ONEOF, RDF.NIL));
		} else {
			BNode tempList = conn.getValueFactory().createBNode();
			// add the first element to the list
			modelAdditions
					.add(conn.getValueFactory().createStatement(datarange, OWL.ONEOF, tempList));
			modelAdditions
					.add(conn.getValueFactory().createStatement(tempList, RDF.TYPE, RDF.LIST));
			modelAdditions.add(
					conn.getValueFactory().createStatement(tempList, RDF.FIRST, literals.get(0)));
			// iteration on the other elements of the list
			for (int i = 1; i < literals.size(); ++i) {
				BNode newTempList = conn.getValueFactory().createBNode();
				modelAdditions.add(
						conn.getValueFactory().createStatement(tempList, RDF.REST, newTempList));
				modelAdditions.add(
						conn.getValueFactory().createStatement(newTempList, RDF.TYPE, RDF.LIST));
				modelAdditions.add(conn.getValueFactory().createStatement(newTempList, RDF.FIRST,
						literals.get(i)));
				tempList = newTempList;
			}
			modelAdditions.add(conn.getValueFactory().createStatement(tempList, RDF.REST, RDF.NIL));
		}

		conn.add(modelAdditions, getWorkingGraph());
	}


	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'C')")
	public void setDatatypeManchesterRestriction(
			@LocallyDefined @Modified(role = RDFResourceRole.dataRange) IRI datatype, String manchExpr)
			throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		RepositoryConnection conn = getManagedConnection();
		deleteDatatypeRestrictionImpl(conn, datatype); //delete the previous
		Map<String, String> prefixToNamespacesMap = getProject().getNewOntologyManager()
				.getNSPrefixMappings(false);
		ManchesterClassInterface mci = ManchesterSyntaxUtils.parseDatatypeRestrictionExpression(manchExpr,
				conn.getValueFactory(), prefixToNamespacesMap);

		List<Statement> statList = new ArrayList<>();
		// it is possible to cast the Resource to a BNode, because the input mci should have a bnode as
		// starting element
		BNode newBnode = (BNode) ManchesterSyntaxUtils.parseManchesterExpr(mci, statList,
				conn.getValueFactory());

		conn.add(statList, getWorkingGraph());

		// add the subClass o equivalentClass property between the main ClassURI and the new BNode
		IRI prop = OWL.EQUIVALENTCLASS;
		conn.add(conn.getValueFactory().createStatement(datatype, prop, newBnode), getWorkingGraph());

		AnnotatedValue<BNode> annBNode = new AnnotatedValue<BNode>(newBnode);
		annBNode.setAttribute("role", RDFResourceRole.cls.name());
	}

	/**
	 * Creates a restriction with a set of facets for a datatype
	 * Example of datatype restriction definition in OWL (source: https://www.w3.org/TR/owl2-syntax/#Datatype_Definitions)
	 * a:SSN rdf:type rdfs:Datatype .
	 * a:SSN owl:equivalentClass _:x .
	 * _:x rdf:type rdfs:Datatype .
	 * _:x owl:onDatatype xsd:string .
	 * _:x owl:withRestrictions ( _:y ) .
	 * _:y xsd:pattern "[0-9]{3}-[0-9]{2}-[0-9]{4}"
	 *
	 * @param datatype
	 * @param base     the xsd datatype for which the restriction is based on
	 * @param facets   mappings between xsd facet and value
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'C')")
	public void setDatatypeFacetsRestriction(@LocallyDefined @Modified(role = RDFResourceRole.dataRange) IRI datatype,
			IRI base, Map<IRI, Literal> facets) {
		RepositoryConnection conn = getManagedConnection();
		deleteDatatypeRestrictionImpl(conn, datatype); //first remove the old restriction
		String query =
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
						"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
						"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
						"INSERT DATA {\n" +
						"GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
						NTriplesUtil.toNTriplesString(datatype) + " owl:equivalentClass " + " _:restriction .\n" +
						"_:restriction rdf:type rdfs:Datatype .\n" +
						"_:restriction owl:onDatatype " + NTriplesUtil.toNTriplesString(base) + " .\n" +
						"_:restriction owl:withRestrictions _:facetsList .\n";
		Iterator<Entry<IRI, Literal>> facetsIterator = facets.entrySet().iterator();
		if (facetsIterator.hasNext()) {
			query += buildFacetsSparqlInsert("", facetsIterator, "_:facetsList", 0);
		} else {
			throw new IllegalArgumentException("Facets map cannot be empty");
		}
		query += "}\n}"; //close graph and insert brackets
		conn.prepareUpdate(query).execute();
	}

	private String buildFacetsSparqlInsert(String query, Iterator<Entry<IRI, Literal>> facetsIterator, String facetsListNodeId, int index) {
		Entry<IRI, Literal> f = facetsIterator.next();
		IRI facet = f.getKey();
		Literal value = f.getValue();
		String firstBNodeId = "_:first" + index;
		String restNode = facetsIterator.hasNext() ? "_:restList" + index : "rdf:nil";
		query +=
				facetsListNodeId + " a rdf:List .\n" +
						facetsListNodeId + " rdf:first " + firstBNodeId + " .\n" +
						firstBNodeId + " " + NTriplesUtil.toNTriplesString(facet) + " " + NTriplesUtil.toNTriplesString(value) + " .\n" +
						facetsListNodeId + " rdf:rest " + restNode + " .\n";
		if (facetsIterator.hasNext()) {
			return buildFacetsSparqlInsert(query, facetsIterator, restNode, ++index);
		} else {
			return query;
		}
	}

	/**
	 * Delete the restriction of the given datatype
	 *
	 * @param datatype
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(datatype)', 'D')")
	public void deleteDatatypeRestriction(@LocallyDefined @Modified(role = RDFResourceRole.dataRange) IRI datatype) {
		RepositoryConnection conn = getManagedConnection();
		deleteDatatypeRestrictionImpl(conn, datatype);
	}

	private void deleteDatatypeRestrictionImpl(RepositoryConnection conn, IRI datatype) {
		//I don't know if the restriction of the given datatype is based on facets or enumerations, so perform both the remove
		this.removeDatatypeEnumerationTriples(conn, datatype);
		this.removeDatatypeFacetsTriples(conn, datatype);
	}

	/**
	 * Removes the triples that define a datatype restriction based on facets
	 *
	 * @param conn
	 * @param datatype
	 */
	private void removeDatatypeFacetsTriples(RepositoryConnection conn, IRI datatype) {
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
				"DELETE {\n" +
				NTriplesUtil.toNTriplesString(datatype) + " owl:equivalentClass ?restriction .\n" +
				"?restriction rdf:type rdfs:Datatype .\n" +
				"?restriction owl:onDatatype ?base .\n" +
				"?restriction owl:withRestrictions ?facetsList .\n" +
				"?restNode ?restPred ?restObj .\n" +
				"?facetNode ?facetPred ?facetValue .\n" +
				"}\n" +
				"WHERE {\n" +
				"GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
				NTriplesUtil.toNTriplesString(datatype) + " owl:equivalentClass ?restriction .\n" +
				"?restriction rdf:type rdfs:Datatype .\n" +
				"?restriction owl:onDatatype ?base .\n" +
				"?restriction owl:withRestrictions ?facetsList .\n" +
				"?facetsList a rdf:List .\n" +
				"?facetsList rdf:rest* ?restNode .\n" +
				"?restNode ?restPred ?restObj .\n" +
				"?restNode rdf:first ?facetNode .\n" +
				"?facetNode ?facetPred ?facetValue .\n" +
				"}\n" + //close graph
				"}";
		conn.prepareUpdate(query).execute();
	}

	/**
	 * Removes the triples that define a datatype restriction based on enumeration
	 *
	 * @param conn
	 * @param datatype
	 */
	private void removeDatatypeEnumerationTriples(RepositoryConnection conn, IRI datatype) {
		// prepare a SPARQL update to remove
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
				"DELETE {\n" +
				"GRAPH ?workingGraph {\n" +
				NTriplesUtil.toNTriplesString(datatype) + " owl:equivalentClass ?datarange .\n" +
				"?datarange rdf:type rdfs:Datatype .\n" +
				"?datarange owl:oneOf ?list .\n" +
				"?elemInList ?p ?o .\n" +
				"}\n" + //close delete
				"}\n" + //close graph
				"WHERE{\n" +
				"GRAPH ?workingGraph {\n" +
				NTriplesUtil.toNTriplesString(datatype) + " owl:equivalentClass ?datarange .\n" +
				"?datarange rdf:type rdfs:Datatype .\n" +
				"?datarange owl:oneOf ?list .\n" +
				//get all the element of the list (including the list itself since it is an element as well)
				"?list rdf:rest* ?elemInList .\n" +
				// which is not the subject of any triple)
				"OPTIONAL{\n" +
				"?elemInList ?p ?o .\n" +
				"}\n" + //close optional
				"}\n" + //close graph
				"}"; //close where
		Update update = conn.prepareUpdate(query);
		update.setBinding("workingGraph", getWorkingGraph());
		update.execute();
	}

	@STServiceOperation
	@Read
	public DatatypeRestrictionDescription getRestrictionDescription(BNode restriction) {
		RepositoryConnection conn = getManagedConnection();
		return describeDatatypeRestriction(conn, restriction);
	}

	@STServiceOperation
	@Read
	public Map<IRI, DatatypeRestrictionDescription> getDatatypeRestrictions() {
		Map<IRI, DatatypeRestrictionDescription> restrictionsMap = new HashMap<>();
		//collect all the datatypes which have an rdfs:Datatype bnode equivalent class
		RepositoryConnection conn = getManagedConnection();
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
				"SELECT ?datatype ?r WHERE { \n" +
				"GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{\n" +
				"?datatype a rdfs:Datatype .\n" +
				"?datatype owl:equivalentClass ?r .\n" +
				"?r a rdfs:Datatype .\n" +
				"FILTER(isURI(?datatype))\n" +
				"FILTER(isBlank(?r))\n" +
				"}\n" +
				"}";
		TupleQuery tq = conn.prepareTupleQuery(query);
		TupleQueryResult results = tq.evaluate();
		Map<IRI, BNode> datatypeRestrictionMap = new HashMap<>();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			IRI datatype = (IRI) bs.getValue("datatype");
			BNode restriction = (BNode) bs.getValue("r");
			datatypeRestrictionMap.put(datatype, restriction);
		}
		for (Entry<IRI, BNode> drEntry: datatypeRestrictionMap.entrySet()) {
			restrictionsMap.put(drEntry.getKey(), describeDatatypeRestriction(conn, drEntry.getValue()));
		}
		return restrictionsMap;
	}

	private DatatypeRestrictionDescription describeDatatypeRestriction(RepositoryConnection conn, BNode restriction) {
		DatatypeRestrictionDescription description = new DatatypeRestrictionDescription();
		/*
		 * It not known if the restriction is described through facets or enumerations, so it tries
		 * first to get the facets, if the query doesn't return any results tries to get the enumerations
		 */
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
				"PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
				"SELECT ?facet ?value ?base WHERE { \n" +
				"GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + "{\n" +
				"?r a rdfs:Datatype .\n" +
				"?r owl:onDatatype ?base .\n" +
				"?r owl:withRestrictions ?list .\n" +
				"?list rdf:rest*/rdf:first ?f .\n" +
				"?f ?facet ?value .\n" +
				"}\n" +
				"}";
		TupleQuery tq = conn.prepareTupleQuery(query);
		tq.setBinding("r", restriction);
		TupleQueryResult results = tq.evaluate();
		if (results.hasNext()) {
			Map<IRI, Value> facetsMap = new HashMap<>(); //map between attribute (base datatype, or facets) and value
			while (results.hasNext()) {
				BindingSet bs = results.next();
				IRI base = (IRI) bs.getValue("base"); //this is always the same for each bs, set it each time anyway
				IRI facet = (IRI) bs.getValue("facet");
				Value value = bs.getValue("value");
				facetsMap.put(facet, value);
				facetsMap.put(OWL.ONDATATYPE, base);
			}
			description.setFacets(facetsMap);
		} else { //no results from query about the facets => get the enumerations
			/*
			 * The following has been copied from the Properties.getDatarangeLiterals() service
			 */
			Collection<AnnotatedValue<Literal>> literalList = new ArrayList<>();

			DataRangeDataOneOf dataOneOf = null;
			DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(restriction,
					getManagedConnection());
			if (dataRangeAbstract instanceof DataRangeDataOneOf) {
				dataOneOf = (DataRangeDataOneOf) dataRangeAbstract;
			}

			List<Literal> literalTempList = dataOneOf.getLiteralList();
			for (Literal literal : literalTempList) {
				literalList.add(new AnnotatedValue<Literal>(literal));
			}
			description.setEnumerations(literalList);
		}
		return description;
	}

}