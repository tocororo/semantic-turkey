package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.lime.model.vocabulary.VARTRANS;
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
						.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
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
					Map<Value, List<Resource>> def2statements = input.filter(conceptId, SKOS.DEFINITION, null)
							.stream().collect(Collectors.groupingBy(Statement::getObject,
									Collectors.mapping(Statement::getContext, Collectors.toList())));

					ref.definition = def2statements.entrySet().stream()
							.map(e -> new AnnotatedValue<>(e.getKey())).collect(Collectors.toList());
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
					Map<Value, List<Resource>> def2statements = input
							.filter(senseObj.id, SKOS.DEFINITION, null).stream()
							.collect(Collectors.groupingBy(Statement::getObject,
									Collectors.mapping(Statement::getContext, Collectors.toList())));

					senseObj.definition = def2statements.entrySet().stream()
							.map(e -> new AnnotatedValue<>(e.getKey())).collect(Collectors.toList());

					Map<Resource, Set<Resource>> related2context = input
							.filter(null, VARTRANS.RELATES, senseObj.id).stream()
							.collect(Collectors.groupingBy(Statement::getSubject,
									Collectors.mapping(Statement::getContext, Collectors.toSet())));
					Map<Resource, Set<Resource>> targeted2context = input
							.filter(null, VARTRANS.TARGET, senseObj.id).stream()
							.collect(Collectors.groupingBy(Statement::getSubject,
									Collectors.mapping(Statement::getContext, Collectors.toSet())));
					Map<Resource, Set<Resource>> sourced2context = input
							.filter(null, VARTRANS.SOURCE, senseObj.id).stream()
							.collect(Collectors.groupingBy(Statement::getSubject,
									Collectors.mapping(Statement::getContext, Collectors.toSet())));

					Map<Resource, Set<Resource>> reifiedRelations2context = new HashMap<>();
					related2context
							.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));
					targeted2context
							.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));
					sourced2context
							.forEach((key, value) -> reifiedRelations2context.merge(key, value, Sets::union));

					List<SenseRelation> related = new ArrayList<>();
					List<SenseRelation> terminologicallyRelated = new ArrayList<>();
					List<SenseRelation> translations = new ArrayList<>();

					for (Map.Entry<Resource, Set<Resource>> entry : reifiedRelations2context.entrySet()) {
						Resource reifiedRelation = entry.getKey();
						Set<Resource> graphs = entry.getValue();

						String nature = simplifiedNatureComputation(input, reifiedRelation,
								RDFResourceRole.individual);
						TripleScopes scope = NatureRecognitionOrchestrator
								.computeTripleScopeFromGraphs(graphs, ctx.workingGraph);

						Map<Resource, Set<Resource>> category2context = input
								.filter(reifiedRelation, VARTRANS.CATEGORY, null).stream()
								.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
										Collectors.mapping(Statement::getContext, Collectors.toSet())));

						List<SenseReference> relatedSenseRefs = parseSenseReferences(ctx, input,
								reifiedRelation, VARTRANS.RELATES);
						List<SenseReference> sourceSenseRefs = parseSenseReferences(ctx, input,
								reifiedRelation, VARTRANS.SOURCE);
						List<SenseReference> targetSenseRefs = parseSenseReferences(ctx, input,
								reifiedRelation, VARTRANS.TARGET);

						List<AnnotatedValue<Resource>> category = category2context.entrySet().stream()
								.map(e -> {
									Resource cat = e.getKey();
									AnnotatedValue<Resource> rv = new AnnotatedValue<>(cat);
									rv.setAttribute("nature", simplifiedNatureComputation(input, cat,
											RDFResourceRole.individual));
									rv.setAttribute("tripleScope",
											SimpleValueFactory.getInstance()
													.createLiteral(
															NatureRecognitionOrchestrator
																	.computeTripleScopeFromGraphs(
																			e.getValue(), ctx.workingGraph)
																	.toString()));
									return rv;
								}).collect(Collectors.toList());

						SenseRelation relationObj = new SenseRelation();
						relationObj.id = reifiedRelation;
						relationObj.nature = nature;
						relationObj.scope = scope;
						relationObj.category = category;
						relationObj.related = relatedSenseRefs;
						relationObj.source = sourceSenseRefs;
						relationObj.target = targetSenseRefs;

						boolean relationHandled = false;
						if (input.contains(reifiedRelation, RDF.TYPE, VARTRANS.TRANSLATION)) {
							translations.add(relationObj);
							relationHandled = true;
						}

						if (input.contains(reifiedRelation, RDF.TYPE, VARTRANS.TERMINOLOGICAL_RELATION)) {
							terminologicallyRelated.add(relationObj);
							relationHandled = true;
						}

						if (!relationHandled) {
							related.add(relationObj);
						}
					}

					senseObj.related = related;
					senseObj.terminologicallyRelated = terminologicallyRelated;
					senseObj.translations = translations;

				}
			}

			lexicalEntryObj.morphosyntacticProps = morphoSyntacticProps;
			lexicalEntryObj.lemma = lemma;
			lexicalEntryObj.otherForms = otherForms;
			lexicalEntryObj.senses = senseList;

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

		protected static List<SenseReference> parseSenseReferences(Context ctx, Model input,
				Resource reifiedRelation, IRI pred) {
			Map<Resource, Set<Resource>> related2context = input.filter(reifiedRelation, pred, null).stream()
					.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
							Collectors.mapping(Statement::getContext, Collectors.toSet())));

			return related2context.entrySet().stream().map(entry -> {
				SenseReference senseRef = new SenseReference();
				senseRef.id = entry.getKey();
				senseRef.nature = simplifiedNatureComputation(input, entry.getKey(),
						RDFResourceRole.ontolexLexicalSense);
				senseRef.scope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(entry.getValue(),
						ctx.workingGraph);

				Map<Resource, Set<Resource>> entry2invContext = input
						.filter(entry.getKey(), ONTOLEX.IS_SENSE_OF, null).stream()
						.collect(Collectors.groupingBy(s -> (Resource) s.getObject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));
				Map<Resource, Set<Resource>> entry2Context = input.filter(null, ONTOLEX.SENSE, entry.getKey())
						.stream().collect(Collectors.groupingBy(s -> (Resource) s.getSubject(),
								Collectors.mapping(Statement::getContext, Collectors.toSet())));

				entry2invContext.forEach((key, value) -> entry2Context.merge(key, value, Sets::union));

				senseRef.entry = entry2invContext.entrySet().stream().map(entry2 -> {
					EntryReference entryRef = new EntryReference();
					entryRef.id = entry.getKey();
					entryRef.nature = simplifiedNatureComputation(input, entry2.getKey(),
							RDFResourceRole.ontolexLexicalEntry);
					entryRef.scope = NatureRecognitionOrchestrator
							.computeTripleScopeFromGraphs(entry2.getValue(), ctx.workingGraph);
					entryRef.lemma = LexicalEntry.parseForms(ctx, input, entry2.getKey(),
							ONTOLEX.CANONICAL_FORM);
					return entryRef;
				}).collect(Collectors.toList());

				return senseRef;
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

	public static class SenseRelation {
		@JsonSerialize(using = ToStringSerializer.class)
		private @Nullable Resource id;
		private String nature;
		private TripleScopes scope;
		private List<AnnotatedValue<Resource>> category;
		private List<SenseReference> source;
		private List<SenseReference> target;
		private List<SenseReference> related;

		public String getNature() {
			return nature;
		}

		public TripleScopes getScope() {
			return scope;
		}

		public Resource getId() {
			return id;
		}

		public List<AnnotatedValue<Resource>> getCategory() {
			return category;
		}

		public List<SenseReference> getSource() {
			return source;
		}

		public List<SenseReference> getTarget() {
			return target;
		}

		public List<SenseReference> getRelated() {
			return related;
		}
	}

	public static class SenseReference {
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

		public List<EntryReference> getLemma() {
			return entry;
		}

	}

	public static class EntryReference {
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
		public Map<Resource, AnnotatedValue<Resource>> morphosyntacticProps;
		public Resource workingGraph;
		public Map<String, String> ns2prefix;
	}

	/**
	 * Returns the collection of known morphosyntactic properties
	 * 
	 * @param role
	 * @param rootsIncluded
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<Resource>> getMorphosyntacticProperties(@Optional RDFResourceRole role,
			@Optional(defaultValue = "false") boolean rootsIncluded) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			"SELECT DISTINCT ?resource WHERE {                       \n" +
			"    ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>" +  (rootsIncluded ? "*" : "+")+ " ?topMorphosyntacticProperty \n" +
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
			"SELECT DISTINCT * WHERE {                                                                                                                                                                                          \n" +
			"    BIND(<http://linguistic.linkeddata.es/id/apertium/lexiconIT/casa-n-it> as ?resource)                                                                                                                           \n" +
			"    {                                                                                                                                                                                                              \n" +
			"      ?resource <http://www.w3.org/ns/lemon/ontolex#canonicalForm>|<http://www.w3.org/ns/lemon/ontolex#otherForm> ?form .                                                                                          \n" +
			"      BIND(?form as ?s)                                                                                                                                                                                            \n" +
			"    } UNION {                                                                                                                                                                                                      \n" +
			"      ?resource <http://www.w3.org/ns/lemon/ontolex#denotes>|^<http://www.w3.org/ns/lemon/ontolex#isDenotedBy> ?reference .                                                                                        \n" +
			"      BIND(?reference as ?s)                                                                                                                                                                                       \n" +
			"    } UNION {                                                                                                                                                                                                      \n" +
			"      ?resource <http://www.w3.org/ns/lemon/ontolex#evokes>|^<http://www.w3.org/ns/lemon/ontolex#isEvokedBy> ?lexicalConcept .                                                                                     \n" +
			"      BIND(?lexicalConcept as ?s)                                                                                                                                                                                  \n" +
			"    } UNION {                                                                                                                                                                                                      \n" +
			"      ?resource (<http://www.w3.org/ns/lemon/ontolex#sense>|^<http://www.w3.org/ns/lemon/ontolex#isSenseOf>)/(                                                                                                     \n" +
			"      <http://www.w3.org/ns/lemon/ontolex#reference>|^<http://www.w3.org/ns/lemon/ontolex#isReferenceOf>|                                                                                                          \n" +
			"      <http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#lexicalizedSense>                                                                                             \n" +
			"      )? ?s.                                                                                                                                                                                                       \n" +
			"    } UNION {                                                                                                                                                                                                      \n" +
			"      ?resource (<http://www.w3.org/ns/lemon/ontolex#sense>|^<http://www.w3.org/ns/lemon/ontolex#isSenseOf>) ?lexicalSense.                                                                                        \n" +
			"      {                                                                                                                                                                                                            \n" +
			"		?lexicalSense (^<http://www.w3.org/ns/lemon/vartrans#relates>|^<http://www.w3.org/ns/lemon/vartrans#source>|^<http://www.w3.org/ns/lemon/vartrans#target>)/                                                 \n" +
			"		  ((<http://www.w3.org/ns/lemon/vartrans#relates>|<http://www.w3.org/ns/lemon/vartrans#source>|<http://www.w3.org/ns/lemon/vartrans#target>)/((                                                             \n" +
			"			<http://www.w3.org/ns/lemon/ontolex#isSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#sense>)/(<http://www.w3.org/ns/lemon/ontolex#canonicalForm>|<http://www.w3.org/ns/lemon/ontolex#otherForm>)?)?)? ?s.\n" +
			"      } UNION {                                                                                                                                                                                                    \n" +
			"        ?rel <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://www.w3.org/ns/lemon/vartrans#senseRel> .                                                                                                \n" +
			"        ?lexicalSense ?rel ?relatedSense .                                                                                                                                                                         \n" +
			"        ?relatedSense ((<http://www.w3.org/ns/lemon/ontolex#isSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#sense>)/                                                                                               \n" +
			"		  (<http://www.w3.org/ns/lemon/ontolex#canonicalForm>|<http://www.w3.org/ns/lemon/ontolex#otherForm>)?)? ?s.                                                                                                \n" +
			"      }                                                                                                                                                                                                            \n" +
			"    }                                                                                                                                                                                                              \n" +
			"  GRAPH ?c {                                                                                                                                                                                                       \n" +
			"    ?s ?p ?o .                                                                                                                                                                                                     \n" +
			"  }                                                                                                                                                                                                                \n" +
			"}                                                                                                                                                                                                                  \n"
			// @formatter:on
		);
		inputQuery.setBinding("resource", lexicalEntry);
		Model input = new LinkedHashModel();
		inputQuery.evaluate(new BindingSets2Model(input));

		QueryBuilder morpoSyntacPropQB = createQueryBuilder(
				"SELECT ?resource WHERE { ?resource <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty> } GROUP BY ?resource ");
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
