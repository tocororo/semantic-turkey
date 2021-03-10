package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.lime.model.vocabulary.VARTRANS;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.LexicographerView.LexicoSemanticRelation.LexicoSemanticRelationPolicy;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;

/**
 * This class provides services related to the lexicographer view, which provides a simplified, streamlined
 * visualization of OntoLex-Lemon lexicons.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class LexicographerView extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(LexicographerView.class);

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

		Set<Resource> resourceContexts = input.filter(resource, RDF.TYPE, null).contexts();
		if (resourceContexts.isEmpty()) {
			resourceContexts = input.filter(resource, null, null).contexts();
		}
		return resourceContexts.stream().map(c -> fixedRole + "," + c + "," + deprecated)
				.collect(Collectors.joining("|_|"));
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

		Map<IRI, AnnotatedValue<IRI>> morphosyntacticProperty = ctx.specialProps
				.get(OntoLexLemon.MORPHOSYNTACTIC_PROPERTY);

		for (IRI prop : morphosyntacticProperty.keySet()) {
			List<AnnotatedValue<Value>> values = Models.getProperties(input, form, prop).stream()
					.map(value -> {
						AnnotatedValue<Value> annotatedValue = new AnnotatedValue<>(value);
						TripleScopes tripleScope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(
								input.filter(form, prop, value).contexts(), ctx.workingGraph);
						annotatedValue.getAttributes().put("tripleScope",
								SimpleValueFactory.getInstance().createLiteral(tripleScope.toString()));

						return annotatedValue;
					}).collect(Collectors.toList());
			if (!values.isEmpty()) {
				propMap.put((IRI) prop, (AnnotatedValue<IRI>) (Object) morphosyntacticProperty.get(prop));
				valueMap.putAll((IRI) prop, values);
			}
		}

		PredicateObjectsList morphoSyntacticProps = new PredicateObjectsList(propMap, valueMap);
		return morphoSyntacticProps;
	}

	public static class LexicalEntry {

		private Resource id;
		private String nature;
		private String show;
		private PredicateObjectsList morphosyntacticProps;
		private List<Form> lemma;
		private List<Form> otherForms;
		private List<EntryReference> subterms;
		private List<Component> constituents;
		private List<Sense> senses;
		private List<LexicalRelation> related;
		private List<LexicalRelation> translatableAs;

		public LexicalEntry() {
			this.related = new ArrayList<>();
			this.translatableAs = new ArrayList<>();
		}

		@JsonSerialize(using = ToStringSerializer.class)
		public Resource getID() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public String getShow() {
			return show;
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

		public List<EntryReference> getSubterms() {
			return subterms;
		}

		public List<Component> getConstituents() {
			return constituents;
		}

		public List<Sense> getSenses() {
			return senses;
		}

		public List<LexicalRelation> getRelated() {
			return related;
		}

		public List<LexicalRelation> getTranslatableAs() {
			return translatableAs;
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

			String show = computeShow(ctx, lexicalEntry, lemma);

			List<EntryReference> subterms = parseSubterms(ctx, input, lexicalEntry);
			List<Component> constituents = parseConstituents(ctx, input, lexicalEntry);

			Map<Resource, Set<Resource>> plainConcepts2contexts = new HashMap<>();
			input.filter(lexicalEntry, ONTOLEX.EVOKES, null).stream()
					.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())))
					.forEach((c, ctxts) -> plainConcepts2contexts.merge(c, ctxts,
							(s1, s2) -> Sets.union(s1, s2)));
			input.filter(null, ONTOLEX.IS_EVOKED_BY, lexicalEntry).stream()
					.collect(Collectors.groupingBy(s -> s.getSubject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())))
					.forEach((c, ctxts) -> plainConcepts2contexts.merge(c, ctxts,
							(s1, s2) -> Sets.union(s1, s2)));

			Map<Resource, Set<Resource>> plainReferences2contexts = new HashMap<>();
			input.filter(lexicalEntry, ONTOLEX.DENOTES, null).stream()
					.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())))
					.forEach((c, ctxts) -> plainReferences2contexts.merge(c, ctxts,
							(s1, s2) -> Sets.union(s1, s2)));
			input.filter(null, ONTOLEX.IS_DENOTED_BY, lexicalEntry).stream()
					.collect(Collectors.groupingBy(s -> s.getSubject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())))
					.forEach((c, ctxts) -> plainReferences2contexts.merge(c, ctxts,
							(s1, s2) -> Sets.union(s1, s2)));

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
				Map<Resource, Set<Resource>> ref2Contexts = input.filter(senseObj.id, ONTOLEX.REFERENCE, null)
						.stream().collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));
				Map<Resource, Set<Resource>> ref2invContexts = input
						.filter(null, ONTOLEX.IS_REFERENCE_OF, senseObj.id).stream()
						.collect(Collectors.groupingBy(Statement::getSubject,
								Collectors.mapping(Statement::getContext, Collectors.toSet())));

				ref2invContexts.forEach((k, v) -> ref2Contexts.merge(k, v, Sets::union));
				senseObj.reference = ref2Contexts.entrySet().stream().map(entry -> {
					AnnotatedValue<Resource> av = new AnnotatedValue<>(entry.getKey());

					Set<Resource> graphs;
					if (plainReferences2contexts.containsKey(entry.getKey())) {
						Set<Resource> additionalContexts = plainReferences2contexts.remove(entry.getKey());
						graphs = Sets.union(additionalContexts, entry.getValue());
					} else {
						graphs = entry.getValue();
					}

					av.setAttribute("tripleScope",
							SimpleValueFactory.getInstance().createLiteral(NatureRecognitionOrchestrator
									.computeTripleScopeFromGraphs(graphs, ctx.workingGraph).toString()));
					return av;
				}).collect(Collectors.toList());

				Map<Resource, Set<Resource>> concept2ctxts = input
						.filter(senseObj.id, ONTOLEX.IS_LEXICALIZED_SENSE_OF, null).stream()
						.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));
				Map<Resource, Set<Resource>> concept2invCtxts = input
						.filter(null, ONTOLEX.LEXICALIZED_SENSE, senseObj.id).stream()
						.collect(Collectors.groupingBy(s -> (Resource) s.getSubject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));

				concept2invCtxts.forEach((k, v) -> concept2ctxts.merge(k, v, Sets::union));

				senseObj.concept = concept2ctxts.entrySet().stream().map(entry -> {
					ConceptReference ref = new ConceptReference();
					Resource conceptId = entry.getKey();
					ref.id = conceptId;

					Set<Resource> graphs;
					if (plainConcepts2contexts.containsKey(ref.id)) {
						Set<Resource> additionalContexts = plainConcepts2contexts.remove(ref.id);
						graphs = Sets.union(additionalContexts, entry.getValue());
					} else {
						graphs = entry.getValue();
					}
					ref.scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(graphs,
							ctx.workingGraph);
					ref.nature = simplifiedNatureComputation(input, conceptId, RDFResourceRole.concept);
					List<AnnotatedValue<Value>> defs = parseDefinitions(ctx, input, conceptId);
					ref.definition = defs;
					return ref;
				}).collect(Collectors.toList());
			}

			List<Sense> senseList = new ArrayList<>(senses.values());

			for (Map.Entry<Resource, Set<Resource>> entry : plainConcepts2contexts.entrySet()) {
				Sense senseObj = new Sense();
				ConceptReference ref = new ConceptReference();
				Resource conceptId = entry.getKey();
				ref.id = conceptId;

				Set<Resource> graphs = entry.getValue();
				ref.scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(graphs,
						ctx.workingGraph);
				ref.nature = simplifiedNatureComputation(input, conceptId, RDFResourceRole.concept);
				Map<Value, List<Resource>> def2statements = input.filter(conceptId, SKOS.DEFINITION, null)
						.stream().collect(Collectors.groupingBy(Statement::getObject,
								Collectors.mapping(Statement::getContext, Collectors.toList())));

				ref.definition = def2statements.entrySet().stream().map(e -> new AnnotatedValue<>(e.getKey()))
						.collect(Collectors.toList());
				senseObj.concept = Lists.newArrayList(ref);

				senseList.add(senseObj);
			}

			for (Map.Entry<Resource, Set<Resource>> entry : plainReferences2contexts.entrySet()) {
				Sense senseObj = new Sense();
				Resource referenceId = entry.getKey();
				AnnotatedValue<Resource> av = new AnnotatedValue<>(referenceId);

				Set<Resource> graphs = entry.getValue();

				av.setAttribute("tripleScope",
						SimpleValueFactory.getInstance().createLiteral(NatureRecognitionOrchestrator
								.computeTripleScopeFromGraphs(graphs, ctx.workingGraph).toString()));

				// senseObj.scope =
				senseObj.reference = Arrays.asList(av);

				senseList.add(senseObj);
			}

			for (Sense senseObj : senseList) {
				if (senseObj.id != null) {
					senseObj.definition = parseDefinitions(ctx, input, senseObj.id);
					parseSenseRelations(ctx, input, senseObj);
				}
			}

			parseLexicalRelations(ctx, input, lexicalEntryObj);

			lexicalEntryObj.morphosyntacticProps = morphoSyntacticProps;
			lexicalEntryObj.lemma = lemma;
			lexicalEntryObj.show = show;
			lexicalEntryObj.subterms = subterms;
			lexicalEntryObj.constituents = constituents;
			lexicalEntryObj.otherForms = otherForms;
			lexicalEntryObj.senses = senseList;

			return lexicalEntryObj;
		}

		private static String computeShow(Context ctx, Resource lexicalEntry, List<Form> lemma) {
			String show = lemma.stream()
					.flatMap(f -> f.getWrittenRep().stream().map(AnnotatedValue::getValue))
					.map(Value::stringValue).collect(Collectors.joining(", "));
			if (StringUtils.isAllBlank(show)) {
				if (lexicalEntry instanceof BNode) {
					return lexicalEntry.toString();
				} else {
					return ModelUtilities.getQName((IRI) lexicalEntry, ctx.prefix2ns);
				}
			} else {
				return show;
			}
		}

		protected static void parseSenseRelations(Context ctx, Model input, Sense senseObj) {
			LexicoSemanticRelationPolicy<SenseReference, SenseRelation> policy = new SenseRelation.SenseRelationPolicy(
					senseObj);
			LexicoSemanticRelation.parseLexicoSemanticRelations(ctx, input, senseObj.getId(), policy);
		}

		public static void parseLexicalRelations(Context ctx, Model input, LexicalEntry lexicalEntryObj) {
			LexicalRelation.LexicalRelationPolicy policy = new LexicalRelation.LexicalRelationPolicy(
					lexicalEntryObj);
			LexicoSemanticRelation.parseLexicoSemanticRelations(ctx, input, lexicalEntryObj.getID(), policy);
		}

		protected static List<AnnotatedValue<Value>> parseDefinitions(Context ctx, Model input,
				Resource subj) {
			List<AnnotatedValue<Value>> defs = ctx.specialProps.get(SKOS.DEFINITION).keySet().stream()
					.flatMap(p -> StreamSupport.stream(input.getStatements(subj, (IRI) p, null).spliterator(),
							false))
					.collect(Collectors.groupingBy(Statement::getObject,
							Collectors.mapping(Statement::getContext, Collectors.toList())))
					.entrySet().stream().map(e -> {
						AnnotatedValue<Value> av = new AnnotatedValue<>(e.getKey());
						av.getAttributes().put("tripleScope",
								SimpleValueFactory.getInstance()
										.createLiteral(NatureRecognitionOrchestrator
												.computeTripleScopeFromGraphs(e.getValue(), ctx.workingGraph)
												.toString()));

						if (e.getKey() instanceof Resource) {
							Models.getPropertyLiteral(input, (Resource) e.getKey(), RDF.VALUE)
									.ifPresent(lit -> {
										av.getAttributes().put("show", SimpleValueFactory.getInstance()
												.createLiteral(lit.getLabel()));
										lit.getLanguage().ifPresent(lang -> av.getAttributes().put("lang",
												SimpleValueFactory.getInstance().createLiteral(lang)));
									});
						}
						return av;
					}).collect(Collectors.toList());
			return defs;
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

		protected static SenseReference parseSenseReference(Context ctx, Model input, Resource sense,
				Set<Resource> referenceCtxs) {
			SenseReference senseRef = new SenseReference();
			senseRef.id = sense;
			senseRef.nature = simplifiedNatureComputation(input, sense, RDFResourceRole.ontolexLexicalSense);
			senseRef.scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(referenceCtxs,
					ctx.workingGraph);

			Map<Resource, Set<Resource>> entry2invContext = input.filter(sense, ONTOLEX.IS_SENSE_OF, null)
					.stream().collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())));
			Map<Resource, Set<Resource>> entry2Context = input.filter(null, ONTOLEX.SENSE, sense).stream()
					.collect(Collectors.groupingBy(s -> (Resource) s.getSubject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())));

			entry2invContext.forEach((key, value) -> entry2Context.merge(key, value, Sets::union));

			senseRef.entry = entry2invContext.entrySet().stream().map(entry2 -> {
				return parseEntryReference(ctx, input, entry2.getKey(), entry2.getValue());
			}).collect(Collectors.toList());

			return senseRef;
		}

		protected static EntryReference parseEntryReference(Context ctx, Model input, Resource entry,
				Set<Resource> referenceCtxs) {
			EntryReference entryRef = new EntryReference();
			entryRef.id = entry;
			entryRef.nature = simplifiedNatureComputation(input, entry, RDFResourceRole.ontolexLexicalEntry);
			entryRef.scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(referenceCtxs,
					ctx.workingGraph);
			entryRef.lemma = LexicalEntry.parseForms(ctx, input, entry, ONTOLEX.CANONICAL_FORM);
			return entryRef;

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
			Map<Literal, List<Resource>> objects2Contexts = input.filter(form, pred, null).stream()
					.filter(s -> s.getObject() instanceof Literal)
					.collect(Collectors.groupingBy(s -> (Literal) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toList())));

			ValueFactory vf = SimpleValueFactory.getInstance();

			return objects2Contexts.entrySet().stream().map(entry -> {
				AnnotatedValue<Literal> rv = new AnnotatedValue<>(entry.getKey());
				rv.setAttribute("tripleScope", vf.createLiteral(NatureRecognitionOrchestrator
						.computeTripleScopeFromGraphs(entry.getValue(), ctx.workingGraph).toString()));
				return rv;
			}).collect(Collectors.toList());
		}
	}

	public static class Sense {
		@JsonSerialize(using = ToStringSerializer.class)
		private @Nullable Resource id;
		private List<AnnotatedValue<Value>> definition;
		private List<AnnotatedValue<Resource>> reference;
		private List<ConceptReference> concept;
		private List<SenseRelation> related;
		private List<SenseRelation> translations;
		private List<SenseRelation> terminologicallyRelated;

		public Sense() {
			this.related = new ArrayList<>();
			this.translations = new ArrayList<>();
			this.terminologicallyRelated = new ArrayList<>();
		}

		public @Nullable Resource getId() {
			return id;
		}

		public List<ConceptReference> getConcept() {
			return concept;
		}

		public List<AnnotatedValue<Value>> getDefinition() {
			return definition;
		}

		public List<AnnotatedValue<Resource>> getReference() {
			return reference;
		}

		public List<SenseRelation> getRelated() {
			return related;
		}

		public List<SenseRelation> getTranslations() {
			return translations;
		}

		public List<SenseRelation> getTerminologicallyRelated() {
			return terminologicallyRelated;
		}
	}

	public static class ConceptReference {
		@JsonSerialize(using = ToStringSerializer.class)
		private Resource id;
		private String nature;
		private TripleScopes scope;
		private List<AnnotatedValue<Value>> definition;

		public Resource getId() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public List<AnnotatedValue<Value>> getDefinition() {
			return definition;
		}
	}

	public static class LexicoSemanticRelation<T> {
		@JsonSerialize(using = ToStringSerializer.class)
		private @Nullable Resource id;
		private String nature;
		private TripleScopes scope;
		private List<AnnotatedValue<Resource>> category;
		private List<T> source = new ArrayList<>();
		private List<T> target = new ArrayList<>();
		private List<T> related = new ArrayList<>();

		public void setNature(String nature) {
			this.nature = nature;
		}

		public String getNature() {
			return nature;
		}

		public void setScope(TripleScopes scope) {
			this.scope = scope;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public void setId(Resource id) {
			this.id = id;
		}

		public Resource getId() {
			return id;
		}

		public void setCategory(List<AnnotatedValue<Resource>> category) {
			this.category = category;
		}

		public List<AnnotatedValue<Resource>> getCategory() {
			return category;
		}

		public void setSource(List<T> source) {
			this.source = source;
		}

		public List<T> getSource() {
			return source;
		}

		public void setTarget(List<T> target) {
			this.target = target;
		}

		public List<T> getTarget() {
			return target;
		}

		public void setRelated(List<T> related) {
			this.related = related;
		}

		public List<T> getRelated() {
			return related;
		}

		public static abstract class LexicoSemanticRelationPolicy<T, S extends LexicoSemanticRelation<T>> {
			public abstract S newRelation();

			public abstract void handleRelation(Context ctx, Model input, S relationObj);

			protected abstract Map<IRI, AnnotatedValue<IRI>> getRelations(Context ctx);

			protected abstract T parseReference(Context ctx, Model input, Resource subjectId,
					Set<Resource> referenceCtxs);

			@FunctionalInterface
			public static interface Function4<R, A1, A2, A3, A4> {
				R apply(A1 a1, A2 a2, A3 a3, A4 a4);
			}
		}

		public static <T, S extends LexicoSemanticRelation<T>> void parseLexicoSemanticRelations(Context ctx,
				Model input, Resource subjectId, LexicoSemanticRelationPolicy<T, S> policy) {
			Map<Resource, Set<Resource>> related2context = input.filter(null, VARTRANS.RELATES, subjectId)
					.stream().collect(Collectors.groupingBy(Statement::getSubject,
							Collectors.mapping(Statement::getContext, Collectors.toSet())));
			Map<Resource, Set<Resource>> targeted2context = input.filter(null, VARTRANS.TARGET, subjectId)
					.stream().collect(Collectors.groupingBy(Statement::getSubject,
							Collectors.mapping(Statement::getContext, Collectors.toSet())));
			Map<Resource, Set<Resource>> sourced2context = input.filter(null, VARTRANS.SOURCE, subjectId)
					.stream().collect(Collectors.groupingBy(Statement::getSubject,
							Collectors.mapping(Statement::getContext, Collectors.toSet())));

			Map<Resource, Set<Resource>> reifiedRelations2context = new HashMap<>();
			related2context.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));
			targeted2context.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));
			sourced2context.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));

			for (Map.Entry<Resource, Set<Resource>> entry : reifiedRelations2context.entrySet()) {
				Resource reifiedRelation = entry.getKey();
				Set<Resource> graphs = entry.getValue();

				String nature = simplifiedNatureComputation(input, reifiedRelation,
						RDFResourceRole.individual);
				TripleScopes scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(graphs,
						ctx.workingGraph);

				Map<Resource, Set<Resource>> category2context = input
						.filter(reifiedRelation, VARTRANS.CATEGORY, null).stream()
						.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));

				List<T> relatedRefs = parseReferences(ctx, input, reifiedRelation, VARTRANS.RELATES,
						policy::parseReference);
				List<T> sourceRefs = parseReferences(ctx, input, reifiedRelation, VARTRANS.SOURCE,
						policy::parseReference);
				List<T> targetRefs = parseReferences(ctx, input, reifiedRelation, VARTRANS.TARGET,
						policy::parseReference);

				List<AnnotatedValue<Resource>> category = category2context.entrySet().stream().map(e -> {
					Resource cat = e.getKey();
					AnnotatedValue<Resource> rv = new AnnotatedValue<>(cat);
					rv.setAttribute("nature",
							simplifiedNatureComputation(input, cat, RDFResourceRole.undetermined));
					rv.setAttribute("tripleScope",
							SimpleValueFactory.getInstance()
									.createLiteral(NatureRecognitionOrchestrator
											.computeTripleScopeFromGraphs(e.getValue(), ctx.workingGraph)
											.toString()));
					return rv;
				}).collect(Collectors.toList());

				S relationObj = policy.newRelation();
				relationObj.setId(reifiedRelation);
				relationObj.setNature(nature);
				relationObj.setScope(scope);
				relationObj.setCategory(category);
				relationObj.setRelated(relatedRefs);
				relationObj.setSource(sourceRefs);
				relationObj.setTarget(targetRefs);

				policy.handleRelation(ctx, input, relationObj);
			}

			for (Map.Entry<IRI, AnnotatedValue<IRI>> relPropEntry : policy.getRelations(ctx).entrySet()) {

				IRI relProp = relPropEntry.getKey();

				Stream.concat(input.filter(subjectId, relProp, null).stream(),
						input.filter(null, relProp, subjectId).stream())
						.collect(Collectors.groupingBy(
								s -> ImmutablePair.of(s.getSubject(), (Resource) s.getObject()),
								Collectors.mapping(Statement::getContext,
										Collectors.mapping(Resource.class::cast, Collectors.toSet()))))
						.entrySet().stream().map(entry -> {
							S reifiedRelation = policy.newRelation();
							reifiedRelation.setId(null);
							reifiedRelation.setNature(simplifiedNatureComputation(input, subjectId,
									RDFResourceRole.individual));
							reifiedRelation.setScope(NatureRecognitionOrchestrator
									.computeTripleScopeFromGraphs(entry.getValue(), ctx.workingGraph));
							AnnotatedValue<Resource> av = new AnnotatedValue<>(relProp);
							av.setAttribute("nature",
									simplifiedNatureComputation(input, relProp, RDFResourceRole.property));
							av.setAttribute("tripleScope", SimpleValueFactory.getInstance()
									.createLiteral(NatureRecognitionOrchestrator
											.computeTripleScopeFromGraphs(entry.getValue(), ctx.workingGraph)
											.toString()));
							reifiedRelation.setCategory(Lists.newArrayList(av));

							T sourceReference = policy.parseReference(ctx, input, entry.getKey().left,
									entry.getValue());
							T targetReference = policy.parseReference(ctx, input, entry.getKey().right,
									entry.getValue());

							reifiedRelation.setSource(Lists.newArrayList(sourceReference));
							reifiedRelation.setTarget(Lists.newArrayList(targetReference));
							reifiedRelation.setRelated(Collections.emptyList());
							return reifiedRelation;
						}).forEach(reifiedRelation -> policy.handleRelation(ctx, input, reifiedRelation));
			}

		}
	}

	public static <T> List<T> parseReferences(Context ctx, Model input, Resource reifiedRelation, IRI pred,
			LexicoSemanticRelationPolicy.Function4<T, Context, Model, Resource, Set<Resource>> refereneParser) {
		Map<Resource, Set<Resource>> related2context = input.filter(reifiedRelation, pred, null).stream()
				.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
						Collectors.mapping(Statement::getContext, Collectors.toSet())));

		return related2context.entrySet().stream().map(entry -> {
			return refereneParser.apply(ctx, input, entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
	}

	public static List<EntryReference> parseSubterms(Context ctx, Model input, Resource lexicalEntry) {
		Map<Resource, Set<Resource>> subterms2Contexts = input.filter(lexicalEntry, DECOMP.SUBTERM, null)
				.stream().collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
						Collectors.mapping(Statement::getContext, Collectors.toSet())));
		return subterms2Contexts.entrySet().stream()
				.map(entry -> LexicalEntry.parseEntryReference(ctx, input, entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	public static List<Component> parseConstituents(Context ctx, Model input, Resource lexicalEntry) {
		Map<Resource, Set<Resource>> constituent2Contexts = input
				.filter(lexicalEntry, DECOMP.CONSTITUENT, null).stream()
				.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
						Collectors.mapping(Statement::getContext, Collectors.toSet())));
		List<Component> constituents = constituent2Contexts.entrySet().stream()
				.map(entry -> Component.parseComponent(ctx, input, entry.getKey(), lexicalEntry))
				.collect(Collectors.toList());

		OntoLexLemon.sortConstituents(constituents, Component::getId, input, lexicalEntry);

		return constituents;
	}

	public static class Component {
		@JsonSerialize(using = ToStringSerializer.class)
		private Resource id;
		private List<EntryReference> correspondingLexicalEntry;
		private PredicateObjectsList features;

		public Resource getId() {
			return id;
		}

		public List<EntryReference> getCorrespondingLexicalEntry() {
			return correspondingLexicalEntry;
		}

		public PredicateObjectsList getFeatures() {
			return features;
		}

		public static Component parseComponent(Context ctx, Model input, Resource component, Resource owner) {
			Component comp = new Component();
			comp.id = component;
			comp.features = parseMorphosyntacticProperties(ctx, input, component);
			comp.correspondingLexicalEntry = input.filter(component, DECOMP.CORRESPONDS_TO, null).stream()
					.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())))
					.entrySet().stream().map(entry -> LexicalEntry.parseEntryReference(ctx, input,
							entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			return comp;
		}
	}

	public static class SenseRelation extends LexicoSemanticRelation<SenseReference> {
		public static class SenseRelationPolicy
				extends LexicoSemanticRelation.LexicoSemanticRelationPolicy<SenseReference, SenseRelation> {

			private Sense sense;

			public SenseRelationPolicy(Sense sense) {
				this.sense = sense;
			}

			@Override
			public SenseRelation newRelation() {
				return new SenseRelation();
			}

			@Override
			public void handleRelation(Context ctx, Model input, SenseRelation relationObj) {
				Resource reifiedRelation = relationObj.getId();
				if (reifiedRelation != null) {
					boolean relationHandled = false;
					if (input.contains(reifiedRelation, RDF.TYPE, VARTRANS.TRANSLATION)) {
						sense.translations.add(relationObj);
						relationHandled = true;
					}

					if (input.contains(reifiedRelation, RDF.TYPE, VARTRANS.TERMINOLOGICAL_RELATION)) {
						sense.terminologicallyRelated.add(relationObj);
						relationHandled = true;
					}

					if (!relationHandled) {
						sense.related.add(relationObj);
					}
				} else {
					boolean isTranslation = java.util.Optional.ofNullable(relationObj.getCategory())
							.orElse(Collections.emptyList()).stream().map(AnnotatedValue::getValue)
							.anyMatch(VARTRANS.HAS_TRANSLATION::equals);

					if (isTranslation) {
						sense.translations.add(relationObj);
					} else {
						sense.terminologicallyRelated.add(relationObj);
					}
				}
			}

			@Override
			protected Map<IRI, AnnotatedValue<IRI>> getRelations(Context ctx) {
				return ctx.specialProps.get(VARTRANS.SENSE_REL);
			}

			@Override
			protected SenseReference parseReference(Context ctx, Model input, Resource subjectId,
					Set<Resource> referenceCtxs) {
				return LexicalEntry.parseSenseReference(ctx, input, subjectId, referenceCtxs);
			}

		}
	}

	public static class LexicalRelation extends LexicoSemanticRelation<EntryReference> {
		public static class LexicalRelationPolicy
				extends LexicoSemanticRelation.LexicoSemanticRelationPolicy<EntryReference, LexicalRelation> {

			private LexicalEntry entry;

			public LexicalRelationPolicy(LexicalEntry entry) {
				this.entry = entry;
			}

			@Override
			public LexicalRelation newRelation() {
				return new LexicalRelation();
			}

			@Override
			protected EntryReference parseReference(Context ctx, Model input, Resource subjectId,
					Set<Resource> referenceCtxs) {
				return LexicalEntry.parseEntryReference(ctx, input, subjectId, referenceCtxs);
			}

			@Override
			public void handleRelation(Context ctx, Model input, LexicalRelation relationObj) {
				Resource reifiedRelation = relationObj.getId();
				if (reifiedRelation != null) {
					entry.related.add(relationObj);
				} else {
					boolean isTranslation = java.util.Optional.ofNullable(relationObj.getCategory())
							.orElse(Collections.emptyList()).stream().map(AnnotatedValue::getValue)
							.anyMatch(VARTRANS.TRANSLATABLE_AS::equals);

					if (isTranslation) {
						entry.translatableAs.add(relationObj);
					} else {
						entry.related.add(relationObj);
					}
				}
			}

			@Override
			protected Map<IRI, AnnotatedValue<IRI>> getRelations(Context ctx) {
				return ctx.specialProps.get(VARTRANS.LEXICAL_REL);
			}

		}

	}

	public static class SenseReference {
		@JsonSerialize(using = ToStringSerializer.class)
		private Resource id;
		private String nature;
		private TripleScopes scope;
		private List<EntryReference> entry;

		public Resource getId() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public List<EntryReference> getEntry() {
			return entry;
		}

	}

	public static class EntryReference {
		@JsonSerialize(using = ToStringSerializer.class)
		private Resource id;
		private String nature;
		private TripleScopes scope;
		private List<Form> lemma;

		public Resource getId() {
			return id;
		}

		public String getNature() {
			return nature;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public List<Form> getLemma() {
			return lemma;
		}
	}

	private static class Context {
		public Map<IRI, Map<IRI, AnnotatedValue<IRI>>> specialProps;
		public Resource workingGraph;
		public Map<String, String> ns2prefix;
		public Map<String, String> prefix2ns;
	}

	/**
	 * Returns the collection of known morphosyntactic properties
	 * 
	 * @param role
	 * @param lexicon
	 * @param rootsIncluded
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getMorphosyntacticProperties(@Optional RDFResourceRole role,
			@LocallyDefined @Optional Resource lexicon,
			@Optional(defaultValue = "false") boolean rootsIncluded) {

		RepositoryConnection con = getManagedConnection();

		Set<IRI> linguisticCatalogs = OntoLexLemon.getLinguisticCatalogs(con, lexicon);

		List<AnnotatedValue<Resource>> props = new ArrayList<>();
		if (linguisticCatalogs.contains(OntoLexLemon.LEXINFO)) {
			QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			"SELECT DISTINCT ?resource WHERE {                       \n" +
			"    ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>" +  (rootsIncluded ? "*" : "+")+ " ?topMorphosyntacticProperty \n" +
			"    FILTER(isIRI(?resource))                                                                    \n" +
			"}                                                                                               \n" +
			"GROUP BY ?resource "
			// @formatter:on
			);
			qb.setBinding("topMorphosyntacticProperty", OntoLexLemon.MORPHOSYNTACTIC_PROPERTY);
			qb.processStandardAttributes();
			props.addAll(qb.runQuery());
		}

		if (linguisticCatalogs.contains(OntoLexLemon.WN)) {

			QueryBuilder qb = createQueryBuilder(
			// @formatter:off
						" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>						\n" +
						" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>							\n" +
						" PREFIX owl: <http://www.w3.org/2002/07/owl#>									\n" +
						" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>							\n" +
						" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>							\n" +
						" SELECT ?resource  WHERE {				                                        \n" +
						"    ?resource ?p ?o .															\n" +
						" }																				\n" +
						" GROUP BY ?resource 															\n"
						// @formatter:on
			);
			qb.setBinding("resource", OntoLexLemon.WN_PART_OF_SPEECH);
			qb.processStandardAttributes();
			props.addAll(qb.runQuery());
		}

		return props;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public LexicalEntry getLexicalEntryView(Resource lexicalEntry) {
		TupleQuery inputQuery = getManagedConnection().prepareTupleQuery(
		// @formatter:off
		    "PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                                                   \n" +
		    "PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                                                     \n" +
		    "PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>                                                 \n" +				
		    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                                    \n" +				
		    "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                                     \n" +				
		    "PREFIX wn: <://globalwordnet.github.io/schemas/wn#>                                                     \n" +				
		    "SELECT DISTINCT * WHERE {                                                                               \n" +
			"    {                                                                                                   \n" +
			"      ?resource (ontolex:canonicalForm|ontolex:otherForm)? ?lexicalEntryOrForm .                        \n" +
			"      BIND(?lexicalEntryOrForm as ?s)                                                                   \n" +
			"    } UNION {                                                                                           \n" +
			"      ?resource decomp:subterm/ontolex:canonicalForm? ?s .                                              \n" +
			"    } UNION {                                                                                           \n" +
			"      ?resource decomp:constituent/(decomp:correspondsTo/ontolex:canonicalForm?)? ?s .                  \n" +
			"    } UNION {                                                                                           \n" +
			"      ?resource ontolex:denotes|^ontolex:isDenotedBy ?reference .                                       \n" +
			"      BIND(?reference as ?s)                                                                            \n" +
			"    } UNION {                                                                                           \n" +
			"      ?resource ontolex:evokes|^ontolex:isEvokedBy ?lexicalConcept .                                    \n" +
			"      BIND(?lexicalConcept as ?s)                                                                       \n" +
			"    } UNION {                                                                                           \n" +
			"      ?resource (ontolex:sense|^ontolex:isSenseOf)/(                                                    \n" +
			"      wn:definition|skos:definition|                                                                    \n" +
			"      ontolex:reference|^ontolex:isReferenceOf|                                                         \n" +
			"      (ontolex:isLexicalizedSenseOf|^ontolex:lexicalizedSense)/(wn:definition|skos:definition)?         \n" +
			"      )? ?s.                                                                                            \n" +
			"    } UNION {                                                                                           \n" +
			"        ?resource (ontolex:sense|^ontolex:isSenseOf) ?lexicalSense.                                     \n" +
			"        ?lexicalSense (^vartrans:relates|^vartrans:source|^vartrans:target) ?reifiedRelation .          \n" +
			"        {                                                                                               \n" +
			"           ?lexicalSense2 (^vartrans:relates|^vartrans:source|^vartrans:target) ?reifiedRelation .      \n" +
			"      		?lexicalSense2 ((ontolex:isSenseOf|^ontolex:sense)/ontolex:canonicalForm?)? ?t .             \n" +
			"        } UNION {                                                                                       \n" +
			"        }                                                                                               \n" +
			"        BIND(COALESCE(?t, ?reifiedRelation) as ?s)                                                      \n" +
			"    } UNION {                                                                                           \n" +
			"   	 ?resource (ontolex:sense|^ontolex:isSenseOf) ?lexicalSense.                                     \n" +
			"        { ?rel rdfs:subPropertyOf* vartrans:senseRel . } UNION { ?rel a wn:SenseRelType . }             \n" +
			"        ?lexicalSense ?rel ?relatedSense .                                                              \n" +
			"        ?relatedSense ((ontolex:isSenseOf|^ontolex:sense)/                                              \n" +
			"		  ontolex:canonicalForm?)? ?s.                                                                   \n" +
			"    } UNION {                                                                                           \n" +
			"        ?resource (^vartrans:relates|^vartrans:source|^vartrans:target) ?reifiedRelation .              \n" +
			"        {                                                                                               \n" +
			"           ?lexicalEntry2 (^vartrans:relates|^vartrans:source|^vartrans:target) ?reifiedRelation .      \n" +
			"      		?lexicalEntry2 ontolex:canonicalForm? ?t .                                                   \n" +
			"        } UNION {                                                                                       \n" +
			"        }                                                                                               \n" +
			"        BIND(COALESCE(?t, ?reifiedRelation) as ?s)                                                      \n" +
			"    } UNION {                                                                                           \n" +
			"        ?rel rdfs:subPropertyOf* vartrans:lexicalRel .                                                  \n" +
			"        ?resource ?rel ?relatedLexicalEntry .                                                           \n" +
			"        ?relatedLexicalEntry ontolex:canonicalForm? ?s.                                                 \n" +
			"    }                                                                                                   \n" +
			"  GRAPH ?c {                                                                                            \n" +
			"    ?s ?p ?o .                                                                                          \n" +
			"  }                                                                                                     \n" +
			"}                                                                                                       \n"
			// @formatter:on
		);
		inputQuery.setBinding("resource", lexicalEntry);
		Model input = new LinkedHashModel();
		inputQuery.evaluate(new BindingSets2Model(input));

		QueryBuilder morpoSyntacPropQB = createQueryBuilder(
		//@formatter:off
				"SELECT ?resource (?sup as ?attr_superProp) WHERE {\n" +
			    "  {\n" +
				"    VALUES(?sup) {(<http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty>)(<http://www.w3.org/ns/lemon/vartrans#lexicalRel>)(<http://www.w3.org/ns/lemon/vartrans#senseRel>)}\n" +
				"    ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* ?sup\n"+
				"  } UNION  {\n" +
				"    VALUES(?sup ?resource){(<http://www.w3.org/2004/02/skos/core#definition> <http://www.w3.org/2004/02/skos/core#definition>)\n"+
				"      (<http://www.w3.org/2004/02/skos/core#definition> <https://globalwordnet.github.io/schemas/wn#definition>)\n" +
				"      (<http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty> <https://globalwordnet.github.io/schemas/wn#partOfSpeech>)}\n" +
				"  }\n" +
				"} GROUP BY ?resource ?sup ");
		morpoSyntacPropQB.processQName();
		morpoSyntacPropQB.processRendering();
		morpoSyntacPropQB.processRole();
		Map<IRI, Map<IRI, AnnotatedValue<IRI>>> specialProps = morpoSyntacPropQB.runQuery().stream()
				.collect(Collectors.groupingBy(av -> (IRI) av.getAttributes().get("superProp"),
						Collectors.toMap(av -> (IRI)av.getValue(), av -> (AnnotatedValue<IRI>)(Object)av)));

		Context ctx = new Context();

		ctx.specialProps = specialProps;
		ctx.workingGraph = getWorkingGraph();
		ctx.prefix2ns = getProject().getOntologyManager().getNSPrefixMappings(true);
		ctx.ns2prefix = MapUtils.invertMap(ctx.prefix2ns);

		return LexicalEntry.parse(ctx, input, lexicalEntry);
	}

}
