package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

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
}