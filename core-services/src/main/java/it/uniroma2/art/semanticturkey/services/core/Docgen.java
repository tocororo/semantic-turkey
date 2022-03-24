package it.uniroma2.art.semanticturkey.services.core;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import it.uniroma2.art.semanticturkey.config.customview.CustomView;
import it.uniroma2.art.semanticturkey.config.customview.PropertyChainView;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * This class provides services for documentation generation.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Docgen extends STServiceAdapter {

    public static final Comparator<Value> RDF4J_VALUE_COMPARATOR = new ValueComparator();
    public static final Comparator<AnnotatedValue<? extends Value>> ANNOTATED_VALUES_SHOW_COMPARATOR;
    public static final Literal EMPTY_LITERAL = SimpleValueFactory.getInstance().createLiteral("");
    private static final Logger logger = LoggerFactory.getLogger(Docgen.class);

    static {
        ANNOTATED_VALUES_SHOW_COMPARATOR = Comparator.comparing(av -> av.getAttributes().get("show"), RDF4J_VALUE_COMPARATOR);
    }

    @Autowired
    private ProjectCustomViewsManager projCvMgr;

    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf', 'R')")
    public OWLDocumentation buildOWLDocumentation() {
        RepositoryConnection con = getManagedConnection();
        List<String> computedLanguages = BaseRenderingEngine.computeLanguages(stServiceContext, "*");

        Map<String, String> namespaceToPrefixesMap = new HashMap<>();
        OWLDocumentation doc = new OWLDocumentation();

        populateDocumentationBase(con, computedLanguages, namespaceToPrefixesMap, doc);

        QueryBuilder resourcesQB = createQueryBuilder(
                "SELECT ?resource WHERE {\n" +
                        "  ?resource a [] .\n" +
                        "  FILTER(isIRI(?resource))\n" +
                        "}\n" +
                        "GROUP BY ?resource \n");
        resourcesQB.processStandardAttributes();
        resourcesQB.setBinding("workingGraph", getWorkingGraph());
        Map<Resource, AnnotatedValue<Resource>> resourceMap = resourcesQB.runQuery().stream().collect(toMap(AnnotatedValue::getValue, Function.identity()));
        Map<Resource, Map<String, Value>> resource2attributes = Maps.transformValues(resourceMap, AnnotatedValue::getAttributes);
        Map<RDFResourceRole, List<AnnotatedValue<Resource>>> role2resources = resourceMap.values().stream().collect(groupingBy(av -> STServiceAdapter.getRoleFromNature(av.getAttributes().getOrDefault("nature", EMPTY_LITERAL).stringValue()), Collectors.toList()));
        Set<Resource> locallyDefinedResources = resourceMap.values().stream().filter(av -> STServiceAdapter.getGraphFromNature(av.getAttributes().getOrDefault("nature", EMPTY_LITERAL).stringValue()).map(getWorkingGraph()::equals).orElse(false)).map(AnnotatedValue::getValue).collect(toSet());
        resourceMap.values().forEach(av -> {
            if (locallyDefinedResources.contains(av.getValue()))
                av.getAttributes().put("localIRI", Values.literal(true));
        });
        doc.classes = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.cls)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.objectProperties = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.objectProperty)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.datatypeProperties = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.datatypeProperty)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.annotationProperties = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.annotationProperty)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.ontologyProperties = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.ontologyProperty)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.properties = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.property)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.namedIndividuals = ListUtils.emptyIfNull(role2resources.get(RDFResourceRole.individual)).stream().filter(av -> locallyDefinedResources.contains(av.getValue())).sorted(ANNOTATED_VALUES_SHOW_COMPARATOR).collect(toList());
        doc.classDetails = new ArrayList<>(doc.classes.size());
        doc.namedIndividualDetails = new ArrayList<>(doc.namedIndividuals.size());
        doc.objectPropertyDetails = new ArrayList<>(doc.objectProperties.size());
        doc.datatypePropertyDetails = new ArrayList<>(doc.datatypeProperties.size());
        doc.annotationPropertyDetails = new ArrayList<>(doc.annotationProperties.size());
        doc.ontologyPropertyDetails = new ArrayList<>(doc.ontologyProperties.size());
        doc.propertiesDetails = new ArrayList<>(doc.properties.size());

        Collection<List<IRI>> definitionPropertyChains = getChainsForPredicateIncluded(SKOS.DEFINITION);

        String classDetailsQueryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "\n" +
                "SELECT DISTINCT *\n" +
                "WHERE \n" +
                "{\n" +
                "    GRAPH ?workingGraph {\n" +
                "        ?subject a owl:Class .\n" +
                "        FILTER(isIRI(?subject))\n" +
                "        {\n" +
                "            BIND(?subject as ?resource)\n" +
                "            BIND(rdf:ID as ?prop)\n" +
                "        } UNION {\n" +
                "            ?subject " + getChainsForPredicateIncludedAsSPARQL(SKOS.DEFINITION) + " ?resource .\n" +
                "            BIND(skos:definition as ?prop)\n" +
                "        } UNION {\n" +
                "            ?subject " + getChainsForPredicateIncludedAsSPARQL(RDFS.COMMENT) + " ?resource .\n" +
                "            BIND(skos:definition as ?prop)\n" +
                "        } UNION {\n" +
                "            {\n" +
                "                BIND(rdfs:subClassOf as ?prop)\n" +
                "                ?subject rdfs:subClassOf ?resource .\n" +
                "\t\t\t} UNION {\n" +
                "                BIND(owl:equivalentClass as ?prop)\n" +
                "                ?subject owl:equivalentClass ?resource .\n" +
                "            } UNION {\n" +
                "                BIND(owl:disjointWith as ?prop)\n" +
                "                ?subject owl:disjointWith ?resource .  \n" +
                "            } UNION {\n" +
                "                BIND(rdfs:domain as ?prop)\n" +
                "                ?subject ^rdfs:domain ?resource\n" +
                "            } UNION {\n" +
                "                BIND(rdfs:range as ?prop)\n" +
                "                ?subject ^rdfs:range ?resource\n" +
                "            }\n" +
                "            OPTIONAL {\n" +
                "                FILTER(isBlank(?resource))\n" +
                "                ?resource ((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first?|owl:complementOf|owl:allValuesFrom|owl:someValuesFrom|owl:onClass|owl:onProperty|owl:onProperty/owl:inverseOf|owl:onProperty/owl:propertyChainAxiom|owl:onProperty/owl:propertyChainAxiom/rdf:rest*/(rdf:first|rdf:first/owl:inverseOf)?)* ?t .\n" +
                "                ?t ?p ?o .\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "ORDER BY ?subject\n";


        TupleQuery classDetailsQuery = con.prepareTupleQuery(classDetailsQueryString);
        classDetailsQuery.setBinding("workingGraph", getWorkingGraph());
        try (TupleQueryResult classDetailsResult = classDetailsQuery.evaluate()) {
            PeekingIterator<BindingSet> it = PeekingIterator.peekingIterator(classDetailsResult.iterator());

            Resource currentSubject = null;
            List<BindingSet> relevantBindingSets = new ArrayList<>();
            while (it.hasNext()) {
                while (it.peek() != null && Objects.equals(it.peek().getValue("subject"), currentSubject)) {
                    BindingSet bs = it.next();
                    relevantBindingSets.add(bs);
                }

                if (!relevantBindingSets.isEmpty()) {
                    ClassDetails classDetails = new ClassDetails();
                    classDetails.resource = resourceMap.get(currentSubject);
                    classDetails.definitions = relevantBindingSets.stream().filter(bs -> SKOS.DEFINITION.equals(bs.getValue("prop"))).map(bs -> bs.getValue("resource")).filter(Literal.class::isInstance).map(Literal.class
                            ::cast).map(AnnotatedValue::new).collect(toList());
                    classDetails.superClasses = relevantBindingSets.stream().filter(bs -> RDFS.SUBCLASSOF.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    classDetails.equivalentClasses = relevantBindingSets.stream().filter(bs -> OWL.EQUIVALENTCLASS.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    classDetails.disjointClasses = relevantBindingSets.stream().filter(bs -> OWL.DISJOINTWITH.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    classDetails.inDomainOf = relevantBindingSets.stream().filter(bs -> RDFS.DOMAIN.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderProperty((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    classDetails.inRangeOf = relevantBindingSets.stream().filter(bs -> RDFS.RANGE.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderProperty((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());

                    doc.classDetails.add(classDetails);
                }
                relevantBindingSets.clear();
                currentSubject = it.peek() != null ? (Resource) it.peek().getValue("subject") : null;

            }
        }


        getPropertiesDetails(doc.objectPropertyDetails, namespaceToPrefixesMap, resourceMap, resource2attributes, con, OWL.OBJECTPROPERTY);
        getPropertiesDetails(doc.datatypePropertyDetails, namespaceToPrefixesMap, resourceMap, resource2attributes, con, OWL.DATATYPEPROPERTY);
        getPropertiesDetails(doc.annotationPropertyDetails, namespaceToPrefixesMap, resourceMap, resource2attributes, con, OWL.ANNOTATIONPROPERTY);
        getPropertiesDetails(doc.ontologyPropertyDetails, namespaceToPrefixesMap, resourceMap, resource2attributes, con, OWL.ONTOLOGYPROPERTY);
        getPropertiesDetails(doc.propertiesDetails, namespaceToPrefixesMap, resourceMap, resource2attributes, con, RDF.PROPERTY);

        String namedIndividualsDetailsQueryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "\n" +
                "SELECT DISTINCT *\n" +
                "WHERE \n" +
                "{\n" +
                "    GRAPH ?workingGraph {\n" +
                "        ?subject a [] .\n" +
                "        FILTER(isIRI(?subject))\n" +
                "        {\n" +
                "            BIND(?subject as ?resource)\n" +
                "            BIND(rdf:ID as ?prop)\n" +
                "        } UNION {\n" +
                "            {\n" +
                "                BIND(rdf:type as ?prop)\n" +
                "                ?subject rdf:type ?resource .\n" +
                "\t\t\t}\n" +
                "            OPTIONAL {\n" +
                "                FILTER(isBlank(?resource))\n" +
                "                ?resource ((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first?|owl:complementOf|owl:allValuesFrom|owl:someValuesFrom|owl:onClass|owl:onProperty|owl:onProperty/owl:inverseOf|owl:onProperty/owl:propertyChainAxiom|owl:onProperty/owl:propertyChainAxiom/rdf:rest*/(rdf:first|rdf:first/owl:inverseOf)?)* ?t .\n" +
                "                ?t ?p ?o .\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "ORDER BY ?subject\n";


        TupleQuery namedIndividualsDetailsQuery = con.prepareTupleQuery(namedIndividualsDetailsQueryString);
        namedIndividualsDetailsQuery.setBinding("workingGraph", getWorkingGraph());
        try (TupleQueryResult namedIndividualsDetailsResults = namedIndividualsDetailsQuery.evaluate()) {
            PeekingIterator<BindingSet> it = PeekingIterator.peekingIterator(namedIndividualsDetailsResults.iterator());

            Resource currentSubject = null;
            List<BindingSet> relevantBindingSets = new ArrayList<>();
            while (it.hasNext()) {
                while (it.peek() != null && Objects.equals(it.peek().getValue("subject"), currentSubject)) {
                    BindingSet bs = it.next();
                    relevantBindingSets.add(bs);
                }

                if (!relevantBindingSets.isEmpty()) {
                    Resource s = currentSubject;
                    if (doc.namedIndividuals.stream().anyMatch(av -> av.getValue().equals(s))) {

                        NamedIndividualDetails namedIndividualDetails = new NamedIndividualDetails();
                        namedIndividualDetails.resource = resourceMap.get(currentSubject);
                        namedIndividualDetails.types = relevantBindingSets.stream().filter(bs -> RDF.TYPE.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());

                        doc.namedIndividualDetails.add(namedIndividualDetails);
                    }
                }
                relevantBindingSets.clear();
                currentSubject = it.peek() != null ? (Resource) it.peek().getValue("subject") : null;

            }
        }

        doc.classDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.namedIndividualDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.objectPropertyDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.datatypePropertyDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.annotationPropertyDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.ontologyPropertyDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        doc.propertiesDetails.sort(Comparator.comparing(d -> d.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));

        return doc;
    }

    private void populateDocumentationBase(RepositoryConnection con, List<String> computedLanguages, Map<String, String> namespaceToPrefixesMap, DocumentationBase doc) {
        doc.namespaces = getProject().getOntologyManager().getNSPrefixMappings(false).entrySet().stream()
                .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Namespace::getPrefix))
                .collect(Collectors.toList());

        doc.ontology = new OntologyDetails();
        TupleQuery ontologyDetailsQuery = con.prepareTupleQuery("PREFIX dct: <http://purl.org/dc/terms/>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "PREFIX org: <http://www.w3.org/ns/org#>\n" +
                "\n" +
                "SELECT * {\n" +
                "    GRAPH ?workingGraph {\n" +
                "        {\n" +
                "        \t?workingGraph owl:versionIRI ?thisVersion    \n" +
                "        } UNION {\n" +
                "            ?workingGraph owl:versionInfo ?revision    \n" +
                "        } UNION {\n" +
                "            ?workingGraph owl:priorVersion ?previousVersion    \n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:title ?title    \n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:description ?description    \n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:issued ?released    \n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:abstract ?summary    \n" +
                "        } UNION {\n" +
                "            ?workingGraph owl:imports ?imports    \n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:creator ?creator .\n" +
                "            ?creator foaf:name ?creator_name .\n" +
                "            ?creator foaf:homepage ?creator_homepage .\n" +
                "            OPTIONAL {\n" +
                "               ?creator org:memberOf ?organization .\n" +
                "               ?organization foaf:name ?organization_name .\n" +
                "               " +
                "?organization foaf:homepage ?organization_homepage .\n" +
                "            " +
                "}\n" +
                "        } UNION {\n" +
                "            ?workingGraph dct:contributor ?contributor .\n" +
                "            ?contributor foaf:name ?contributor_name .\n" +
                "            ?contributor foaf:homepage ?contributor_homepage .\n" +
                "            OPTIONAL {\n" +
                "               ?contributor org:memberOf ?organization .\n" +
                "               ?organization foaf:name ?organization_name .\n" +
                "               ?organization foaf:homepage ?organization_homepage .\n" +
                "            }\n" +
                "        }\n" +
                "    }" +
                "}");
        ontologyDetailsQuery.setBinding("workingGraph", getWorkingGraph());
        List<BindingSet> ontologyBindingSets = QueryResults.asList(ontologyDetailsQuery.evaluate());

        doc.ontology.latest = new AnnotatedValue<>((IRI) getWorkingGraph());
        doc.ontology.thisVersion = ontologyBindingSets.stream().map(bs -> bs.getValue("thisVersion")).filter(Objects::nonNull).map(IRI.class::cast).map(AnnotatedValue::new).findAny().orElse(null);
        doc.ontology.previousVersion = ontologyBindingSets.stream().map(bs -> bs.getValue("previousVersion")).filter(Objects::nonNull).map(IRI.class::cast).map(AnnotatedValue::new).findAny().orElse(null);
        doc.ontology.revision = ontologyBindingSets.stream().map(bs -> bs.getValue("revision")).filter(Objects::nonNull).map(Value::stringValue).findAny().orElse(null);
        doc.ontology.imports = ontologyBindingSets.stream().map(bs -> bs.getValue("imports")).filter(Objects::nonNull).map(IRI.class::cast).collect(toList());

        doc.ontology.released = ontologyBindingSets.stream().map(bs -> bs.getValue("released")).map(l -> Literals.getCalendarValue(l, null)).filter(Objects::nonNull).map(l -> l.toGregorianCalendar().toZonedDateTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).findAny().orElse(null);
        doc.ontology.title = computeLiteralAttribute(computedLanguages, ontologyBindingSets, "title");
        doc.ontology.description = computeLiteralAttribute(computedLanguages, ontologyBindingSets, "description");
        doc.ontology.summary = computeLiteralAttribute(computedLanguages, ontologyBindingSets, "summary");
        doc.ontology.creators = ontologyBindingSets.stream().filter(bs -> bs.hasBinding("creator")).map(bs -> {
            AnnotatedValue<Resource> av = new AnnotatedValue<>((Resource) bs.getValue("creator"));
            av.getAttributes().put("name", bs.getValue("creator_name"));
            av.getAttributes().put("homepage", bs.getValue("creator_homepage"));

            if (bs.hasBinding("organization")) {
                av.getAttributes().put("organization_name", bs.getValue("organization_name"));
                av.getAttributes().put("organization_homepage", bs.getValue("organization_homepage"));
            }

            return av;
        }).sorted(Comparator.comparing((AnnotatedValue<Resource> av) -> av.getAttributes().get("name"), RDF4J_VALUE_COMPARATOR)).collect(toList());
        doc.ontology.contributors = ontologyBindingSets.stream().filter(bs -> bs.hasBinding("contributor")).map(bs -> {
            AnnotatedValue<Resource> av = new AnnotatedValue<>((Resource) bs.getValue("contributor"));
            av.getAttributes().put("name", bs.getValue("contributor_name"));
            av.getAttributes().put("homepage", bs.getValue("contributor_homepage"));

            if (bs.hasBinding("organization")) {
                av.getAttributes().put("organization_name", bs.getValue("organization_name"));
                av.getAttributes().put("organization_homepage", bs.getValue("organization_homepage"));
            }

            return av;
        }).sorted(Comparator.comparing((AnnotatedValue<Resource> av) -> av.getAttributes().get("name"), RDF4J_VALUE_COMPARATOR)).collect(toList());


        doc.namespaces.forEach(ns -> namespaceToPrefixesMap.put(ns.getName(), ns.getPrefix()));
    }

    private AnnotatedValue<Literal> computeLiteralAttribute(List<String> computedLanguages, List<BindingSet> ontologyBindingSets, String varName) {
        String firstLang = computedLanguages.isEmpty() ? "*" : computedLanguages.iterator().next();
        AnnotatedValue<Literal> value = ontologyBindingSets.stream().map(bs -> bs.getValue(varName)).filter(Objects::nonNull).map(Literal.class::cast).filter(l -> "*".equals(firstLang) || l.getLanguage().map(firstLang::equals).orElse(false)).map(l -> new AnnotatedValue<>(l)).findAny().orElse(null);
        if (value == null) {
            value = ontologyBindingSets.stream().map(bs -> bs.getValue(varName)).filter(Objects::nonNull).map(Literal.class::cast).filter(l -> l.getLanguage().map("en"::equals).orElse(false)).map(l -> new AnnotatedValue<>(l)).findAny().orElse(null);
            if (value == null) {
                value = ontologyBindingSets.stream().map(bs -> bs.getValue(varName)).filter(Objects::nonNull).map(Literal.class::cast).sorted(Comparator.comparing(l -> l.getLanguage().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder()))).map(l -> new AnnotatedValue<>(l)).findFirst().orElse(null);
            }
        }
        return value;
    }

    private void getPropertiesDetails(List<PropertiesDetails> propertiesDetails, Map<String, String> namespaceToPrefixesMap, Map<Resource, AnnotatedValue<Resource>> resourceMap, Map<Resource, Map<String, Value>> resource2attributes, RepositoryConnection con, IRI propertyMetaClass) {
        String propertyDetailsQueryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "\n" +
                "SELECT DISTINCT *\n" +
                "WHERE \n" +
                "{\n" +
                "    GRAPH ?workingGraph {\n" +
                "        ?subject a " + RenderUtils.toSPARQL(propertyMetaClass) + " .\n" +
                "        {\n" +
                "            BIND(?subject as ?resource)\n" +
                "            BIND(rdf:ID as ?prop)\n" +
                "        } UNION {\n" +
                "            ?subject " + getChainsForPredicateIncludedAsSPARQL(SKOS.DEFINITION) + " ?resource .\n" +
                "            BIND(skos:definition as ?prop)\n" +
                "        } UNION {\n" +
                "            ?subject " + getChainsForPredicateIncludedAsSPARQL(RDFS.COMMENT) + " ?resource .\n" +
                "            BIND(skos:definition as ?prop)\n" +
                "        } UNION {\n" +
                "            {\n" +
                "                BIND(rdf:type as ?prop)\n" +
                "                ?subject rdf:type ?resource .\n" +
                "\t\t\t} UNION {\n" +
                "                BIND(rdfs:subPropertyOf as ?prop)\n" +
                "                ?subject rdfs:subPropertyOf ?resource .\n" +
                "            } UNION {\n" +
                "                BIND(rdfs:domain as ?prop)\n" +
                "                ?subject rdfs:domain ?resource .  \n" +
                "            } UNION {\n" +
                "                BIND(rdfs:range as ?prop)\n" +
                "                ?subject rdfs:range ?resource\n" +
                "            } UNION {\n" +
                "                BIND(owl:inverseOf as ?prop)\n" +
                "                ?subject owl:inverseOf ?resource\n" +
                "            } UNION {\n" +
                "                BIND(owl:propertyDisjointWith as ?prop)\n" +
                "                ?subject owl:propertyDisjointWith ?resource\n" +
                "            }\n" +
                "            OPTIONAL {\n" +
                "                FILTER(isBlank(?resource))\n" +
                "                ?resource ((owl:intersectionOf|owl:unionOf)/rdf:rest*/rdf:first?|owl:complementOf|owl:allValuesFrom|owl:someValuesFrom|owl:onClass|owl:onProperty|owl:onProperty/owl:inverseOf|owl:onProperty/owl:propertyChainAxiom|owl:onProperty/owl:propertyChainAxiom/rdf:rest*/(rdf:first|rdf:first/owl:inverseOf)?)* ?t .\n" +
                "                ?t ?p ?o .\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "ORDER BY ?subject\n";


        TupleQuery propertiesDetailsQuery = con.prepareTupleQuery(propertyDetailsQueryString);
        propertiesDetailsQuery.setBinding("workingGraph", getWorkingGraph());
        try (TupleQueryResult propertiesDetailsResult = propertiesDetailsQuery.evaluate()) {
            PeekingIterator<BindingSet> it = PeekingIterator.peekingIterator(propertiesDetailsResult.iterator());

            Resource currentSubject = null;
            List<BindingSet> relevantBindingSets = new ArrayList<>();
            while (it.hasNext()) {
                while (it.peek() != null && Objects.equals(it.peek().getValue("subject"), currentSubject)) {
                    BindingSet bs = it.next();
                    relevantBindingSets.add(bs);
                }

                if (!relevantBindingSets.isEmpty()) {
                    PropertiesDetails propertyDetails = new PropertiesDetails();
                    propertyDetails.resource = resourceMap.get(currentSubject);
                    propertyDetails.definitions = relevantBindingSets.stream().filter(bs -> SKOS.DEFINITION.equals(bs.getValue("prop"))).map(bs -> bs.getValue("resource")).filter(Literal.class::isInstance).map(Literal.class
                            ::cast).map(AnnotatedValue::new).collect(toList());
                    Set<Value> types = relevantBindingSets.stream().filter(bs -> RDFS.SUBCLASSOF.equals(bs.getValue("prop"))).map(bs -> bs.getValue("resource")).collect(toSet());
                    Map<IRI, String> type2characteristic = new HashMap<>();
                    type2characteristic.put(OWL.FUNCTIONALPROPERTY, "functional");
                    type2characteristic.put(OWL.INVERSEFUNCTIONALPROPERTY, "inverse functional");
                    type2characteristic.put(OWL.SYMMETRICPROPERTY, "symmetric");
                    type2characteristic.put(OWL.ASYMMETRICPROPERTY, "asymmetric");
                    type2characteristic.put(OWL.TRANSITIVEPROPERTY, "transitive");
                    type2characteristic.put(OWL.REFLEXIVEPROPERTY, "reflexive");
                    type2characteristic.put(OWL.IRREFLEXIVEPROPERTY, "irreflexive");

                    propertyDetails.characteristics = Maps.filterEntries(type2characteristic, e -> types.contains(e.getKey())).values().stream().collect(toList());
                    propertyDetails.superProperties = relevantBindingSets.stream().filter(bs -> RDFS.SUBPROPERTYOF.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderProperty((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    propertyDetails.domains = relevantBindingSets.stream().filter(bs -> RDFS.DOMAIN.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    propertyDetails.ranges = relevantBindingSets.stream().filter(bs -> RDFS.RANGE.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderClass((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    propertyDetails.inverses = relevantBindingSets.stream().filter(bs -> OWL.INVERSEOF.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderProperty((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());
                    propertyDetails.disjointProperties = relevantBindingSets.stream().filter(bs -> OWL.PROPERTYDISJOINTWITH.equals(bs.getValue("prop"))).collect(groupingBy(bs -> bs.getValue("resource"), Collectors.toList())).entrySet().stream().map(entry -> renderProperty((Resource) entry.getKey(), entry.getValue(), resource2attributes, namespaceToPrefixesMap)).collect(toList());

                    propertiesDetails.add(propertyDetails);
                }
                relevantBindingSets.clear();
                currentSubject = it.peek() != null ? (Resource) it.peek().getValue("subject") : null;

            }
        }
    }

    private AnnotatedValue<Resource> renderClass(Resource cls, List<BindingSet> bindingSets, Map<Resource, Map<String, Value>> resource2attributes, Map<String, String> namespaceToPrefixesMap) {
        if (resource2attributes.containsKey(cls)) {
            AnnotatedValue<Resource> av = new AnnotatedValue<>(cls);
            av.getAttributes().putAll(resource2attributes.getOrDefault(cls, Collections.emptyMap()));
            return av;
        }

        AnnotatedValue<Resource> av = new AnnotatedValue<>(cls);
        String show;
        if (cls.isIRI()) {
            show = cls.stringValue();
        } else {

            Model triples = bindingSets.stream().flatMap(bs -> {
                Resource s = (Resource) bs.getValue("t");
                IRI p = (IRI) bs.getValue("p");
                Value o = bs.getValue("o");

                if (ObjectUtils.allNotNull(s, p, o)) {
                    return Stream.of(SimpleValueFactory.getInstance().createStatement(s, p, o));
                } else {
                    return Stream.empty();
                }
            }).collect(Collectors.toCollection(LinkedHashModel::new));

            show = AbstractStatementConsumer.computeShow(cls, resource2attributes, triples, true).getLeft();
        }
        av.setAttribute("role", Values.literal(RDFResourceRole.cls.name()));
        av.setAttribute("show", Values.literal(show));
        return av;
    }

    private AnnotatedValue<Resource> renderProperty(Resource prop, List<BindingSet> bindingSets, Map<Resource, Map<String, Value>> resource2attributes, Map<String, String> namespaceToPrefixesMap) {
        if (resource2attributes.containsKey(prop)) {
            AnnotatedValue<Resource> av = new AnnotatedValue<>(prop);
            av.getAttributes().putAll(resource2attributes.getOrDefault(prop, Collections.emptyMap()));
            return av;
        }

        AnnotatedValue<Resource> av = new AnnotatedValue<>(prop);
        String show;
        if (prop.isIRI()) {
            show = prop.stringValue();
        } else {

            Model triples = bindingSets.stream().flatMap(bs -> {
                Resource s = (Resource) bs.getValue("t");
                IRI p = (IRI) bs.getValue("p");
                Value o = bs.getValue("o");

                if (ObjectUtils.allNotNull(s, p, o)) {
                    return Stream.of(SimpleValueFactory.getInstance().createStatement(s, p, o));
                } else {
                    return Stream.empty();
                }
            }).collect(Collectors.toCollection(LinkedHashModel::new));

            show = AbstractStatementConsumer.computeShow(prop, resource2attributes, triples, true).getLeft();
        }
        av.setAttribute("role", Values.literal(RDFResourceRole.objectProperty.name()));
        av.setAttribute("show", Values.literal(show));
        return av;
    }

    private String getChainsForPredicateIncludedAsSPARQL(IRI pred) {
        Collection<List<IRI>> predPropertyChains = getChainsForPredicateIncluded(pred);
        return predPropertyChains.stream().map(chain -> "(" + chain.stream().map(RenderUtils::toSPARQL).collect(joining("/")) + ")").collect(joining("|"));
    }

    private Collection<List<IRI>> getChainsForPredicateIncluded(IRI pred) {
        Collection<List<IRI>> predPropertyChains = getChainsForPredicate(pred);
        if (predPropertyChains.isEmpty()) {
            predPropertyChains = Collections.singletonList(Collections.singletonList(pred));
        } else {
            predPropertyChains = predPropertyChains.stream().map(chain -> {
                List<IRI> newList = new ArrayList<>(chain.size() + 1);
                newList.add(pred);
                newList.addAll(chain);
                return newList;
            }).collect(toList());
        }
        return predPropertyChains;
    }

    private Collection<List<IRI>> getChainsForPredicate(IRI pred) {
        CustomView cv = projCvMgr.getCustomViewManager(getProject()).getCustomViewForProperty(pred);
        if (cv instanceof PropertyChainView) {
            return Collections.singletonList(((PropertyChainView) cv).properties);
        } else {
            return new ArrayList<>();
        }
    }

    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf', 'R')")
    public SKOSDocumentation buildSKOSDocumentation() {
        RepositoryConnection con = getManagedConnection();
        List<String> computedLanguages = BaseRenderingEngine.computeLanguages(stServiceContext, "*");

        Map<String, String> namespaceToPrefixesMap = new HashMap<>();

        SKOSDocumentation doc = new SKOSDocumentation();

        populateDocumentationBase(con, computedLanguages, namespaceToPrefixesMap, doc);

        Map<String, String> prefixesToNamespaceMap = MapUtils.invertMap(namespaceToPrefixesMap);

        QueryBuilder resourcesQB = createQueryBuilder(
                "SELECT ?resource WHERE {\n" +
                        "  ?resource a [] .\n" +
                        "  FILTER(isIRI(?resource))\n" +
                        "}\n" +
                        "GROUP BY ?resource \n");
        resourcesQB.processStandardAttributes();
        resourcesQB.setBinding("workingGraph", getWorkingGraph());
        Map<Resource, AnnotatedValue<Resource>> resourceMap = resourcesQB.runQuery().stream().collect(toMap(AnnotatedValue::getValue, Function.identity()));
        Map<Resource, Map<String, Value>> resource2attributes = Maps.transformValues(resourceMap, AnnotatedValue::getAttributes);
        Map<RDFResourceRole, List<AnnotatedValue<Resource>>> role2resources = resourceMap.values().stream().collect(groupingBy(av -> STServiceAdapter.getRoleFromNature(av.getAttributes().getOrDefault("nature", EMPTY_LITERAL).stringValue()), Collectors.toList()));
        Set<Resource> locallyDefinedResources = resourceMap.values().stream().filter(av -> STServiceAdapter.getGraphFromNature(av.getAttributes().getOrDefault("nature", EMPTY_LITERAL).stringValue()).map(getWorkingGraph()::equals).orElse(false)).map(AnnotatedValue::getValue).collect(toSet());
        resourceMap.values().forEach(av -> {
            Resource value = av.getValue();
            if (locallyDefinedResources.contains(value) && ImmutableSet.of(RDFResourceRole.concept, RDFResourceRole.conceptScheme).contains(getRoleFromNature(av.getAttributes().get("nature").stringValue())))
                av.getAttributes().put("crossReference", ModelUtilities.toLiteral(value, prefixesToNamespaceMap));
        });

        TupleQuery propertiesQuery = con.prepareTupleQuery(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "\n" +
                        "SELECT ?resource (MIN(?label) AS ?show) WHERE {\n" +
                        "  {\n" +
                        "    ?resource a rdf:Property .\n" +
                        "  " +
                        "} UNION {\n" +
                        "      ?resource a owl:ObjectProperty .\n" +
                        "  " +
                        "} UNION {\n" +
                        "      ?resource a owl:DatatypeProperty .\n" +
                        "  } UNION {\n" +
                        "      ?resource a owl:AnnotationProperty .\n" +
                        "  } UNION {\n" +
                        "      ?resource a owl:OntologyProperty .\n" +
                        "  }\n" +
                        "  OPTIONAL {\n" +
                        "      ?resource rdfs:label ?labelEn .\n" +
                        "      FILTER(LANG(?labelEn) = \"en\")\n" +
                        "  }\n" +
                        "  OPTIONAL {\n" +
                        "      ?resource rdfs:label ?labelWo .\n" +
                        "      FILTER(LANG(?labelWo) = \"\")\n" +
                        "  }\n" +
                        "  BIND(COALESCE(?labelEn, ?labelWo) AS ?label)\n" +
                        "  FILTER(BOUND(?label))\n" +
                        "}\n" +
                        "GROUP BY ?resource\n");
        Map<IRI, Literal> prop2show = QueryResults.stream(propertiesQuery.evaluate()).collect(toMap(bs -> (IRI) bs.getValue("resource"), bs -> (Literal) bs.getValue("show")));


        doc.conceptDetails = getSKOSEntityDetails(con, ConceptDetails::new, SKOS.CONCEPT,prefixesToNamespaceMap, resource2attributes, prop2show);
        doc.conceptSchemeDetails = getSKOSEntityDetails(con, ConceptSchemeDetails::new, SKOS.CONCEPT_SCHEME,prefixesToNamespaceMap, resource2attributes, prop2show);

        return doc;
    }

    private <T extends SKOSEntityDetails> List<T> getSKOSEntityDetails(RepositoryConnection con, Supplier<T> instantiator, IRI metaclass, Map<String, String> prefixesToNamespaceMap, Map<Resource, Map<String, Value>> resource2attributes, Map<IRI, Literal> prop2show) {
        List<T> rv = new ArrayList<>();

        TupleQuery entityDetailsQuery = con.prepareTupleQuery("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
                "\n" +
                "SELECT * \n" +
                "WHERE {\n" +
                "    GRAPH ?workingGraph {\n" +
                "        ?s rdf:type " + RenderUtils.toSPARQL(metaclass) + " ; \n" +
                "            ?p ?o .\t\n" +
                "        OPTIONAL {\n" +
                "         ?o a skosxl:Label ;\n" +
                "            skosxl:literalForm ?oPreview .\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "ORDER BY ?s");
        entityDetailsQuery.setBinding("workingGraph", getWorkingGraph());
        try (TupleQueryResult entityDetailsQueryResult = entityDetailsQuery.evaluate()) {
            PeekingIterator<BindingSet> it = PeekingIterator.peekingIterator(entityDetailsQueryResult.iterator());

            Resource currentSubject = null;
            List<BindingSet> relevantBindingSets = new ArrayList<>();
            while (it.hasNext()) {
                while (it.peek() != null && Objects.equals(it.peek().getValue("s"), currentSubject)) {
                    BindingSet bs = it.next();
                    relevantBindingSets.add(bs);
                }

                if (!relevantBindingSets.isEmpty()) {
                    T entityDetails = instantiator.get();
                    entityDetails.resource = new AnnotatedValue<>(currentSubject);
                    addAnnotations(prefixesToNamespaceMap, resource2attributes, entityDetails.resource);
                    entityDetails.definitions = relevantBindingSets.stream().filter(bs -> SKOS.DEFINITION.equals(bs.getValue("p"))).map(bs -> bs.getValue("o")).filter(Literal.class::isInstance).map(Literal.class
                            ::cast).sorted(Comparator.comparing(l -> l.getLanguage().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder()))).map(AnnotatedValue::new).collect(toList());

                    Multimap<IRI, AnnotatedValue<Value>> description = HashMultimap.create();

                    for (BindingSet bs : relevantBindingSets) {
                        IRI p = (IRI) bs.getValue("p");

                        if (Objects.equals(p, SKOS.DEFINITION)) continue;

                        Value o = bs.getValue("o");
                        AnnotatedValue<Value> annotatedO = new AnnotatedValue<>(o);
                        Value oPreview = bs.getValue("oPreview");
                        if (oPreview != null) {
                            annotatedO.setAttribute("show", ModelUtilities.toLiteral(oPreview, prefixesToNamespaceMap));
                        }
                        addAnnotations(prefixesToNamespaceMap, resource2attributes, annotatedO);
                        description.put(p, annotatedO);
                    }
                    entityDetails.description = new ArrayList<>(description.size());
                    for (IRI prop : description.keySet()) {
                        AnnotatedValue<IRI> annotatedProp = new AnnotatedValue<>(prop);
                        Literal propShow = prop2show.get(prop);
                        if (propShow != null) {
                            annotatedProp.setAttribute("show", propShow);
                        }
                        addAnnotations(prefixesToNamespaceMap, resource2attributes, annotatedProp);

                        List<AnnotatedValue<Value>> annotatedObjects = description.get(prop).stream().collect(toList());
                        annotatedObjects.sort(Comparator.comparing(av -> {
                            if (av.getLanguage() != null) return av.getLanguage();

                            Value langAttr = av.getAttributes().getOrDefault("show", EMPTY_LITERAL);
                            if (langAttr.isLiteral()) {
                                String lang = ((Literal)langAttr).getLanguage().orElse(null);
                                if (lang != null) return lang;
                            }

                            return av.getValue().stringValue();
                        }));
                        entityDetails.description.add(MyPair.of(annotatedProp, annotatedObjects));
                        entityDetails.description.sort(Comparator.comparing(p -> p.property, ANNOTATED_VALUES_SHOW_COMPARATOR));
                    }
                    rv.add(entityDetails);


                }
                relevantBindingSets.clear();
                currentSubject = it.peek() != null ? (Resource) it.peek().getValue("s") : null;

            }
        }
        rv.sort(Comparator.comparing(c -> c.resource, ANNOTATED_VALUES_SHOW_COMPARATOR));
        return rv;
    }

    private void addAnnotations(Map<String, String> prefixesToNamespaceMap, Map<Resource, Map<String, Value>> resource2attributes, AnnotatedValue<?> annotatedValue) {
        resource2attributes.getOrDefault(annotatedValue.getValue(), Collections.emptyMap()).entrySet().forEach(entry -> {
            if (!annotatedValue.getAttributes().containsKey(entry.getKey())) {
                annotatedValue.getAttributes().put(entry.getKey(), entry.getValue());
            }
        });
        if (annotatedValue.getValue() instanceof Resource) {
            if (!annotatedValue.getAttributes().containsKey("show")) {
                Literal show = ModelUtilities.toLiteral((Resource) annotatedValue.getValue(), prefixesToNamespaceMap);
                annotatedValue.setAttribute("show", show);
            }
            Literal showLit = (Literal) annotatedValue.getAttributes().get("show");
            showLit.getLanguage().ifPresent(l -> annotatedValue.setAttribute("language", l));
        } else {
            Optional.ofNullable(annotatedValue.getLanguage()).ifPresent(l -> annotatedValue.setAttribute("language", l));
            if (!Objects.equals(annotatedValue.getDatatype(), RDF.LANGSTRING.stringValue())) {
                annotatedValue.setAttribute("datatype", ModelUtilities.toLiteral(((Literal) annotatedValue.getValue()).getDatatype(), prefixesToNamespaceMap));
            }
        }
    }

    public static class OntologyDetails {
        public AnnotatedValue<IRI> latest;
        public AnnotatedValue<IRI> thisVersion;
        public AnnotatedValue<IRI> previousVersion;
        public String revision;
        public AnnotatedValue<Literal> title;
        public AnnotatedValue<Literal> description;
        public AnnotatedValue<Literal> summary;
        public List<IRI> imports;
        public String released;
        public List<AnnotatedValue<Resource>> creators;
        public List<AnnotatedValue<Resource>> contributors;
    }

    public static class ClassDetails {
        public AnnotatedValue<Resource> resource;
        public List<AnnotatedValue<Literal>> definitions;
        public List<AnnotatedValue<Resource>> superClasses;
        public List<AnnotatedValue<Resource>> equivalentClasses;
        public List<AnnotatedValue<Resource>> disjointClasses;
        public List<AnnotatedValue<Resource>> inDomainOf;
        public List<AnnotatedValue<Resource>> inRangeOf;
    }

    public static class PropertiesDetails {
        public AnnotatedValue<Resource> resource;
        public List<AnnotatedValue<Literal>> definitions;
        public List<String> characteristics;
        public List<AnnotatedValue<Resource>> superProperties;
        public List<AnnotatedValue<Resource>> domains;
        public List<AnnotatedValue<Resource>> ranges;
        public List<AnnotatedValue<Resource>> inverses;
        public List<AnnotatedValue<Resource>> disjointProperties;
    }

    public static class NamedIndividualDetails {
        public AnnotatedValue<Resource> resource;
        public List<AnnotatedValue<Resource>> types;
    }

    public static class DocumentationBase {
        public OntologyDetails ontology;
        public List<Namespace> namespaces;
    }

    public static class OWLDocumentation extends DocumentationBase {
        public List<AnnotatedValue<Resource>> classes;
        public List<AnnotatedValue<Resource>> objectProperties;
        public List<AnnotatedValue<Resource>> datatypeProperties;
        public List<AnnotatedValue<Resource>> annotationProperties;
        public List<AnnotatedValue<Resource>> ontologyProperties;
        public List<AnnotatedValue<Resource>> properties;
        public List<AnnotatedValue<Resource>> namedIndividuals;

        public List<ClassDetails> classDetails;
        public ArrayList<PropertiesDetails> objectPropertyDetails;
        public ArrayList<PropertiesDetails> datatypePropertyDetails;
        public ArrayList<PropertiesDetails> annotationPropertyDetails;
        public ArrayList<PropertiesDetails> ontologyPropertyDetails;
        public ArrayList<PropertiesDetails> propertiesDetails;
        public List<NamedIndividualDetails> namedIndividualDetails;
    }

    public static class MyPair {
        public AnnotatedValue<IRI> property;
        public List<AnnotatedValue<Value>> values;

        public static MyPair of(AnnotatedValue<IRI> property, List<AnnotatedValue<Value>> values) {
            MyPair myPair = new MyPair();
            myPair.property = property;
            myPair.values = values;
            return myPair;
        }
    }

    public static class SKOSEntityDetails {
        public AnnotatedValue<Resource> resource;
        public List<AnnotatedValue<Literal>> definitions;
        public List<MyPair> description;
    }

    public static class ConceptDetails extends SKOSEntityDetails {
    }

    public static class ConceptSchemeDetails extends SKOSEntityDetails {
    }

    public static class SKOSDocumentation extends DocumentationBase {
        public List<ConceptDetails> conceptDetails;
        public List<ConceptSchemeDetails> conceptSchemeDetails;
    }

}