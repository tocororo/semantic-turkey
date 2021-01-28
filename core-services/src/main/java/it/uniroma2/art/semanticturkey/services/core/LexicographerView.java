package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;

/**
 * This class provides services related to the lexicographer view, which provides a simplified, streamlined
 * visualization of OntoLex-Lemon lexicons.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class LexicographerView extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(LexicographerView.class);

	private static final IRI MORPHOSYNTACTIC_PROPERTY = SimpleValueFactory.getInstance()
			.createIRI("http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty");

	public static class BindingSets2Model extends AbstractTupleQueryResultHandler {

		private Model model;

		public BindingSets2Model(Model model) {
			this.model = model;
		}

		public void startQueryResult(java.util.List<String> bindingNames)
				throws org.eclipse.rdf4j.query.TupleQueryResultHandlerException {
			model.clear();
		};

		public void handleSolution(org.eclipse.rdf4j.query.BindingSet bindingSet)
				throws org.eclipse.rdf4j.query.TupleQueryResultHandlerException {
			Resource subj = (Resource) bindingSet.getValue("s");
			IRI pred = (IRI) bindingSet.getValue("p");
			Value obj = (Value) bindingSet.getValue("o");
			Resource ctx = (Resource) bindingSet.getValue("c");

			if (ctx != null) {
				model.add(subj, pred, obj, ctx);
			} else {
				model.add(subj, pred, obj);
			}
		};
	}

	public static String simplifiedNatureComputation(Model input, Resource resource,
			RDFResourceRole fixedRole) {
		boolean deprecated = input.contains(resource, OWL.DEPRECATED, BooleanLiteral.TRUE)
				|| input.contains(resource, RDF.TYPE, OWL.DEPRECATEDCLASS)
				|| input.contains(resource, RDF.TYPE, OWL.DEPRECATEDPROPERTY);

		return input.filter(resource, RDF.TYPE, null).contexts().stream()
				.map(c -> fixedRole + "," + c + "," + deprecated).collect(Collectors.joining("|_|"));
	}

	public static @Nullable String computeQName(Map<String, String> ns2prefix, IRI resource) {
		String prefix = ns2prefix.get(resource.getNamespace());
		if (prefix != null) {
			return prefix + ":" + resource.getLocalName();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected static PredicateObjectsList parseMorphosyntacticProperties(Context ctx, Model input,
			Resource form) {
		Map<IRI, AnnotatedValue<IRI>> propMap = new LinkedHashMap<>();
		Multimap<IRI, AnnotatedValue<? extends Value>> valueMap = HashMultimap.create();

		for (Resource prop : ctx.morphosyntacticProps.keySet()) {
			List<AnnotatedValue<Value>> values = Models.getProperties(input, form, (IRI) prop).stream()
					.map(value -> {
						AnnotatedValue<Value> annotatedValue = new AnnotatedValue<>(value);
						TripleScopes tripleScope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(
								input.filter(form, (IRI) prop, value).contexts(), ctx.workingGraph);
						annotatedValue.getAttributes().put("tripleScope",
								SimpleValueFactory.getInstance().createLiteral(tripleScope.toString()));

						return annotatedValue;
					}).collect(Collectors.toList());
			if (!values.isEmpty()) {
				propMap.put((IRI) prop, (AnnotatedValue<IRI>) (Object) ctx.morphosyntacticProps.get(prop));
				valueMap.putAll((IRI) prop, values);
			}
		}

		PredicateObjectsList morphoSyntacticProps = new PredicateObjectsList(propMap, valueMap);
		return morphoSyntacticProps;
	}

	public static class LexicalEntry {

		private Resource id;
		private String nature;
		private PredicateObjectsList morphosyntacticProps;
		private List<Form> lemma;
		private List<Form> otherForms;
		private List<Sense> senses;

		@JsonSerialize(using = ToStringSerializer.class)
		public Resource getID() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public PredicateObjectsList getMorphosyntacticProps() {
			return morphosyntacticProps;
		}

		public List<Form> getLemma() {
			return lemma;
		}

		public List<Form> getOtherForms() {
			return otherForms;
		}

		public List<Sense> getSenses() {
			return senses;
		}

		public static LexicalEntry parse(Context ctx, Model input, Resource lexicalEntry) {
			LexicalEntry lexicalEntryObj = new LexicalEntry();
			lexicalEntryObj.id = lexicalEntry;
			lexicalEntryObj.nature = simplifiedNatureComputation(input, lexicalEntry,
					RDFResourceRole.ontolexLexicalEntry);

			PredicateObjectsList morphoSyntacticProps = parseMorphosyntacticProperties(ctx, input,
					lexicalEntry);

			List<Form> lemma = parseForms(ctx, input, lexicalEntry, ONTOLEX.CANONICAL_FORM);
			List<Form> otherForms = parseForms(ctx, input, lexicalEntry, ONTOLEX.OTHER_FORM);

			Map<Resource, Sense> senses = new HashMap<>();

			for (Statement stmt : input.filter(lexicalEntry, ONTOLEX.SENSE, null)) {
				Resource sense = (Resource) stmt.getObject();

				Sense senseObj = senses.computeIfAbsent(sense, s -> {
					Sense so = new Sense();
					so.id = s;
					return so;
				});

			}

			for (Statement stmt : input.filter(null, ONTOLEX.IS_SENSE_OF, lexicalEntry)) {
				Resource sense = (Resource) stmt.getSubject();

				Sense senseObj = senses.computeIfAbsent(sense, s -> {
					Sense so = new Sense();
					so.id = s;
					return so;
				});
			}

			for (Sense senseObj : senses.values()) {
				Map<Value, List<Statement>> ref2statements = input
						.filter(senseObj.id, ONTOLEX.REFERENCE, null).stream()
						.collect(Collectors.groupingBy(Statement::getObject));
				Map<Value, List<Statement>> ref2invStatements = input
						.filter(null, ONTOLEX.IS_REFERENCE_OF, senseObj.id).stream()
						.collect(Collectors.groupingBy(Statement::getSubject));

				ref2invStatements.forEach((k, v) -> ref2statements.merge(k, v, (v1, v2) -> {
					List<Statement> v3 = new ArrayList<>(v1);
					v3.addAll(v2);
					return v3;
				}));
				senseObj.reference = ref2statements.keySet().stream()
						.map(r -> new AnnotatedValue<>((Resource) r)).collect(Collectors.toList());
			}

			for (Sense senseObj : senses.values()) {
				Map<Value, List<Statement>> concept2statements = input
						.filter(senseObj.id, ONTOLEX.IS_LEXICALIZED_SENSE_OF, null).stream()
						.collect(Collectors.groupingBy(Statement::getObject));
				Map<Value, List<Statement>> concept2invStatements = input
						.filter(null, ONTOLEX.LEXICALIZED_SENSE, senseObj.id).stream()
						.collect(Collectors.groupingBy(Statement::getSubject));

				concept2invStatements.forEach((k, v) -> concept2statements.merge(k, v, (v1, v2) -> {
					List<Statement> v3 = new ArrayList<>(v1);
					v3.addAll(v2);
					return v3;
				}));
				senseObj.concept = concept2statements.keySet().stream()
						.map(c -> new AnnotatedValue<>((Resource) c)).collect(Collectors.toList());

				for (AnnotatedValue<Resource> c : senseObj.concept) {
					Map<Value, List<Statement>> def2statements = input
							.filter(c.getValue(), SKOS.DEFINITION, null).stream()
							.collect(Collectors.groupingBy(Statement::getObject));
					senseObj.definition = def2statements.keySet().stream().filter(IRI.class::isInstance)
							.map(def -> new AnnotatedValue<>((Literal) def)).collect(Collectors.toList());
				}
			}

			lexicalEntryObj.morphosyntacticProps = morphoSyntacticProps;
			lexicalEntryObj.lemma = lemma;
			lexicalEntryObj.otherForms = otherForms;
			lexicalEntryObj.senses = new ArrayList<>(senses.values());

			return lexicalEntryObj;
		}

		protected static List<Form> parseForms(Context ctx, Model input, Resource lexicalEntry, IRI pred) {
			return Models.getPropertyResources(input, lexicalEntry, pred).stream().map(form -> {
				Form formObj = Form.parse(ctx, input, (Resource) form);
				TripleScopes scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(
						input.filter(lexicalEntry, pred, form).contexts(), ctx.workingGraph);
				formObj.scope = scope;

				return formObj;
			}).collect(Collectors.toList());
		}
	}

	public static class Form {

		private Resource id;
		private String nature;
		private TripleScopes scope;

		private List<AnnotatedValue<Literal>> writtenRep;
		private List<AnnotatedValue<Literal>> phoneticRep;
		private PredicateObjectsList morphosyntacticProps;

		@JsonSerialize(using = ToStringSerializer.class)
		public Resource getID() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public List<AnnotatedValue<Literal>> getWrittenRep() {
			return writtenRep;
		}

		public List<AnnotatedValue<Literal>> getPhoneticRep() {
			return phoneticRep;
		}

		public PredicateObjectsList getMorphosyntacticProps() {
			return morphosyntacticProps;
		}

		@SuppressWarnings("unchecked")
		public static Form parse(Context ctx, Model input, Resource form) {
			String nature = simplifiedNatureComputation(input, form, RDFResourceRole.ontolexForm);
			List<AnnotatedValue<Literal>> writtenRep = parseRepresentations(ctx, input, form,
					ONTOLEX.WRITTEN_REP);
			List<AnnotatedValue<Literal>> phoneticRep = parseRepresentations(ctx, input, form,
					ONTOLEX.PHONETIC_REP);

			PredicateObjectsList morphoSyntacticProps = parseMorphosyntacticProperties(ctx, input, form);

			Form formObj = new Form();
			formObj.id = form;
			formObj.nature = nature;
			formObj.writtenRep = writtenRep;
			formObj.phoneticRep = phoneticRep;
			formObj.morphosyntacticProps = morphoSyntacticProps;

			return formObj;
		}

		protected static List<AnnotatedValue<Literal>> parseRepresentations(Context ctx, Model input,
				Resource form, IRI pred) {
			return Models.getPropertyLiterals(input, form, pred).stream().map(r -> new AnnotatedValue<>(r))
					.collect(Collectors.toList());
		}
	}

	public static class Sense {
		@JsonSerialize(using = ToStringSerializer.class)
		private @Nullable Resource id;
		private List<AnnotatedValue<Literal>> definition;
		private List<AnnotatedValue<Resource>> reference;
		private List<AnnotatedValue<Resource>> concept;

		public @Nullable Resource getId() {
			return id;
		}

		public List<AnnotatedValue<Resource>> getConcept() {
			return concept;
		}

		public List<AnnotatedValue<Literal>> getDefinition() {
			return definition;
		}

		public List<AnnotatedValue<Resource>> getReference() {
			return reference;
		}

	}

	private static class Context {
		public Map<Resource, AnnotatedValue<Resource>> morphosyntacticProps;
		public Resource workingGraph;
		public Map<String, String> ns2prefix;
	}

	/**
	 * Returns the collection of known morphosyntactic properties
	 * 
	 * @param role
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getMorphosyntacticProperties(@Optional RDFResourceRole role) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			"SELECT DISTINCT ?resource WHERE {                       \n" +
			"    ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>+ ?topMorphosyntacticProperty \n" +
			"    FILTER(isIRI(?resource))                                                                    \n" +
			"}                                                                                               \n" +
			"GROUP BY ?resource "
			// @formatter:on
		);
		qb.setBinding("topMorphosyntacticProperty", MORPHOSYNTACTIC_PROPERTY);
		qb.processStandardAttributes();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public LexicalEntry getLexicalEntryView(Resource lexicalEntry) {
		TupleQuery inputQuery = getManagedConnection().prepareTupleQuery(
		// @formatter:off
			"SELECT * WHERE {                                                    \n" +
			"    ?resource (<http://www.w3.org/ns/lemon/ontolex#canonicalForm>|<http://www.w3.org/ns/lemon/ontolex#otherForm>|(<http://www.w3.org/ns/lemon/ontolex#sense>|^<http://www.w3.org/ns/lemon/ontolex#isSenseOf>)/(<http://www.w3.org/ns/lemon/ontolex#isReferenceOf>|<http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#lexicalizedSense>)?)? ?s . \n" +
			"    GRAPH ?c {                                                      \n" +
			"    	?s ?p ?o .                                                   \n" +
			"    }                                                               \n" +
			"}                                                                   \n"
			// @formatter:on
		);
		inputQuery.setBinding("resource", lexicalEntry);
		Model input = new LinkedHashModel();
		inputQuery.evaluate(new BindingSets2Model(input));

		QueryBuilder morpoSyntacPropQB = createQueryBuilder(
				"SELECT ?resource WHERE { ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>+ <http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty> } GROUP BY ?resource ");
		morpoSyntacPropQB.processQName();
		morpoSyntacPropQB.processRendering();
		morpoSyntacPropQB.processRole();
		Map<Resource, AnnotatedValue<Resource>> morphoSyntacticProps = morpoSyntacPropQB.runQuery().stream()
				.collect(Collectors.toMap(AnnotatedValue::getValue, Function.identity()));

		Context ctx = new Context();
		ctx.morphosyntacticProps = morphoSyntacticProps;
		ctx.workingGraph = getWorkingGraph();
		ctx.ns2prefix = MapUtils.invertMap(getProject().getNewOntologyManager().getNSPrefixMappings(true));

		return LexicalEntry.parse(ctx, input, lexicalEntry);
	}

}
